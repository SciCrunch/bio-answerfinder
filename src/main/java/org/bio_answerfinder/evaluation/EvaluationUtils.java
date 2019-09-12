package org.bio_answerfinder.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.math.NumberUtils;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by bozyurt on 8/26/19.
 */
public class EvaluationUtils {

    public static Map<String, Integer> extractRank(String questionRankAnnotCSVFile, int methodIdx) throws IOException {
        Map<String, Integer> question2RankMap = new HashMap<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(questionRankAnnotCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    System.out.println(cr.get(methodIdx));
                    continue;
                }
                String question = cr.get(0);
                String rank = cr.get(methodIdx);
                String key = StringUtils.stripWS(question);
                // rank annotation is 0 based i.e. 0 -> 1, 1 -> 2 etc
                if (rank.length() == 1 && Character.isDigit(rank.charAt(0))) {
                    int qr = NumberUtils.toInt(rank) + 1;
                    question2RankMap.put(key, qr);
                } else {
                    question2RankMap.put(key, 0);
                }
                System.out.println(rank + " " + question);
            }
        } finally {
            FileUtils.close(in);
        }
        return question2RankMap;
    }

    public static void calcPerformance(String questionRankAnnotCSVFile, int methodIdx) throws Exception {
        Map<String, Integer> question2RankMap = extractRank(questionRankAnnotCSVFile, methodIdx);
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        int count = 0;
        int factoidCount = 0;
        double sum = 0;
        Set<String> uniqSet = new HashSet<>();
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            if (question2RankMap.containsKey(key)) {
                if (!uniqSet.contains(key)) {
                    uniqSet.add(key);
                } else {
                    System.out.println("Duplicate question:" + qr.getQuestion());
                    continue;
                }
                count++;
                if (qr.getType().equalsIgnoreCase("factoid")) {
                    factoidCount++;
                    int rank = question2RankMap.get(key);
                    if (rank > 0) {
                        sum += 1.0 / rank;
                    }
                }
            }
        }
        double mrr = sum / factoidCount;
        System.out.println(String.format("# of factoid questions: %d # of questions:%d", factoidCount, count));
        System.out.println("Factoid MRR:" + mrr);
    }

    public static void showPerformance(List<QuestionInfo> qiList, String type, String rankResultCSVFile) throws IOException {
        double sum = 0;
        double sumPAt1 = 0;
        Map<Integer, QuestionInfo> map = new HashMap<>();
        for (QuestionInfo qi : qiList) {
            map.put(qi.getQid(), qi);
        }
        BufferedReader in = null;
        int count = 0;
        try {
            in = FileUtils.newUTF8CharSetReader(rankResultCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                String rankStr = cr.get(0);
                int qid = NumberUtils.toInt(cr.get(1));
                if (map.containsKey(qid)) {
                    if (!rankStr.equals("N/A")) {
                        int rank = NumberUtils.toInt(rankStr);
                        if (rank == 1) {
                            sumPAt1 += 1.0;
                        }
                        sum += 1.0 / rank;
                    }
                    count++;
                }
            }
            int size = qiList.size();
            double mrr = sum / size;
            double pao = sumPAt1 / size;
            System.out.println(String.format("Type:%s (%d) MRR: %.3f Precision@1: %.2f", type, size, mrr, pao));

        } finally {
            FileUtils.close(in);
        }


    }

    public static List<QuestionInfo> getQuestionInfos(String questionAnswerCandidatesCSVFile) throws IOException {
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        Map<String, QuestionRecord> qrMap = new HashMap<>();
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
        List<QuestionInfo> qiList = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(questionAnswerCandidatesCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            Set<String> questionSet = new HashSet<>();
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    // System.out.println(cr.get(2));
                    continue;
                }
                int qid = NumberUtils.toInt(cr.get(0));
                String question = cr.get(2);
                if (!questionSet.contains(question)) {
                    String key = StringUtils.stripWS(question);
                    QuestionRecord qr = qrMap.get(key);
                    if (qr == null) {
                        System.out.println(question);

                    }
                    Assertion.assertNotNull(qr);
                    String questionType = qr.getType();
                    QuestionInfo qi = new QuestionInfo(qid, question, questionType);
                    qiList.add(qi);
                    questionSet.add(question);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        return qiList;
    }


    public static class QuestionInfo {
        final int qid;
        final String question;
        final String type;

        public QuestionInfo(int qid, String question, String type) {
            this.qid = qid;
            this.question = question;
            this.type = type;
        }

        public int getQid() {
            return qid;
        }

        public String getQuestion() {
            return question;
        }

        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("QuestionInfo{");
            sb.append("qid=").append(qid);
            sb.append(", type='").append(type).append('\'');
            sb.append(", question='").append(question).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String csvFile = HOME_DIR + "/dev/java/bio-answerfinder/scripts/question_method_ranks_annotation_table.csv";

        //calcPerformance(csvFile, 4);
        String questionAnswerCandidatesCSVFile = HOME_DIR + "/dev/java/bio-answerfinder/data/evaluation/bert_question_answer_candidates.csv";

        String rootDir = HOME_DIR + "/dev/java/bio-answerfinder/data/evaluation";

        List<String> rankResultFiles = Arrays.asList("annotator_1_bert_rank.csv", "annotator_2_bert_rank.csv",
                "annotator_3_bert_rank.csv",
                "annotator_1_rwmd_rank.csv", "annotator_2_rwmd_rank.csv", "annotator_3_rwmd_rank.csv");

        for (String rankResultFile : rankResultFiles) {
            String rankResultCSVFile = rootDir + "/" + rankResultFile;
            List<QuestionInfo> qiList = getQuestionInfos(questionAnswerCandidatesCSVFile);
            Map<String, List<QuestionInfo>> typeMap = qiList.stream().collect(Collectors.groupingBy(QuestionInfo::getType));
            System.out.println(new File(rankResultCSVFile).getName());
            for (String type : typeMap.keySet()) {
                List<QuestionInfo> questionInfos = typeMap.get(type);
                showPerformance(questionInfos, type, rankResultCSVFile);
            }
            System.out.println("-------------------------------------");

        }
    }
}
