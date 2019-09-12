package org.bio_answerfinder.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bio_answerfinder.engine.*;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.Profiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 10/3/17.
 */
public class QAEngineEvaluator {
    IQAEngine qaEngine;
    String noRankQuestionsFile = "/tmp/no_match_questions.txt";
    Profiler profiler = new Profiler("evaluate");

    public QAEngineEvaluator(IQAEngine qaEngine) throws IOException {
        this.qaEngine = qaEngine;
    }


    public void evaluate(Map<String, List<AnswerSentence>> questionMap, int topN) throws Exception {

        double sum = 0;
        int count = 1;
        if (new File(noRankQuestionsFile).isFile()) {
            new File(noRankQuestionsFile).delete();
        }
        QuestionResultStatsCollector collector = null;
        if (qaEngine instanceof Interceptor) {
            collector = ((Interceptor) qaEngine).getQuestionResultsCollector();
        }
        if (qaEngine instanceof Logger) {
            ((Logger) qaEngine).startLogging("/tmp/question_results.txt");
        }
        Map<String,String> options = new HashMap<>(7);

        for (String question : questionMap.keySet()) {
            List<AnswerSentence> gsaList = questionMap.get(question);
            profiler.start("answerQuestion");
            List<AnswerSentence> asList = qaEngine.answerQuestion(question, topN, options);
            profiler.stop("answerQuestion");
            profiler.showStats();
            int rank = getRank4Question(asList, gsaList);
            if (rank > 0) {
                sum += 1.0 / rank;
                if (collector != null) {
                    collector.toss();
                }
            } else {
                String qid = gsaList.get(0).getQid();
                FileUtils.appendLine(noRankQuestionsFile, qid + "," + question);
                if (collector != null) {
                    collector.keep();
                    collector.save("/tmp/no_rank_questions.json");
                }
            }

            double MRR = sum/ count;
            System.out.println("current rank:" + rank + " current MRR:" + MRR);
            if (qaEngine instanceof Logger) {
                ((Logger) qaEngine).logQuestionResults(question, MRR, rank);
            }
            count++;
        }

        double MRR = sum / questionMap.size();
        System.out.println("MRR:" + MRR);
        if (qaEngine instanceof Logger) {
            ((Logger) qaEngine).stopLogging();
        }

    }


    public static int getRank4Question(List<AnswerSentence> asList, List<AnswerSentence> gsaList) {
        Map<String, AnswerSentence> gsMap = new HashMap<>();
        for (AnswerSentence gsa : gsaList) {
            gsMap.put(gsa.getSentence(), gsa);
        }
        int i = 1;
        for (AnswerSentence as : asList) {
            String sentence = as.getSentence();
            sentence = sentence.replaceAll("_", " ");
            if (gsMap.containsKey(sentence)) {
                return i;
            }
            i++;
        }
        return 0;
    }

    public static Map<String, List<AnswerSentence>> replaceQuestionText(String qid2KeyMapFile,
                                                                        Map<String, List<AnswerSentence>> questionMap) throws IOException {
        Map<String, List<AnswerSentence>> map = new HashMap<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(qid2KeyMapFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (CSVRecord cr : records) {
                String qid = cr.get(0);
                String key = cr.get(1);
                String question = cr.get(2);
                List<AnswerSentence> answerSentences = questionMap.get(key);
                if (answerSentences != null) {
                    map.put(question, answerSentences);
                }
            }
        } finally {
            FileUtils.close(in);
        }

        return map;
    }

    public static Map<String, List<AnswerSentence>> extractQuestions(String dnnEvalCSVFile) throws IOException {

        BufferedReader in = null;
        Map<String, List<AnswerSentence>> questionMap = new LinkedHashMap<>();
        try {
            in = FileUtils.newUTF8CharSetReader(dnnEvalCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    continue;
                }
                String question = cr.get(3);
                String qid = cr.get(1);
                String answer = cr.get(4);
                String pmid = cr.get(2);
                String label = cr.get(5);
                question = question.replaceAll("_", " ");
                answer = answer.replaceAll("_", " ");
                if (label.equals("1")) {
                    List<AnswerSentence> asList = questionMap.get(question);
                    if (asList == null) {
                        asList = new ArrayList<>(5);
                        questionMap.put(question, asList);
                    }
                    AnswerSentence as = new AnswerSentence(-1, qid, pmid, -1);
                    as.setSentence(answer);
                    asList.add(as);
                }

            }
        } finally {
            FileUtils.close(in);
        }
        return questionMap;
    }


    public static void main(String[] args) throws Exception {
        final String HOME_DIR = System.getProperty("user.home");
        String csvFile = HOME_DIR + "/data/bioasq/asel/bioasq_asel_nc_val_balanced.csv";
        csvFile = HOME_DIR + "/data/bioasq/asel/bioasq_asel_nc_train_balanced.csv";
        Map<String, List<AnswerSentence>> questionMap = QAEngineEvaluator.extractQuestions(csvFile);
        //String mapFile = HOME_DIR + "/dev/java/bnerkit/data/bioasq/bioasq_asel_nc_val_balanced_qid_map.csv";
        String mapFile = HOME_DIR + "/dev/java/bnerkit/data/bioasq/bioasq_asel_nc_train_balanced_qid_map.csv";
        questionMap = QAEngineEvaluator.replaceQuestionText(mapFile, questionMap);

        // WMDQAEngine engine = new WMDQAEngine();
        // QAEngine1 engine = new QAEngine1();
        QAEngineDL engine = new QAEngineDL();
        //  engine.setRemoveStopWords(true);
        // engine.setUseWor2VecVectors(false);

        //ISentenceRanker sentenceRanker = new LexicalMatchingSentenceRanker();
        //engine.setSentenceRanker(sentenceRanker);

        //QAEngine engine = new QAEngine();
        try {
            engine.initialize();
            QAEngineEvaluator evaluator = new QAEngineEvaluator(engine);

            evaluator.evaluate(questionMap, 200);

        } finally {
            engine.shutdown();
        }
    }
}
