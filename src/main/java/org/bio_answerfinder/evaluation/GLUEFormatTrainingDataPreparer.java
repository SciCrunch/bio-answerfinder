package org.bio_answerfinder.evaluation;

import opennlp.tools.postag.POSTaggerME;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.common.Utils;
import org.bio_answerfinder.nlp.sentence.Tokenizer;
import org.bio_answerfinder.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares training, dev and test set in in GLUE TSV format from manually checked BioASQ qaengine1 results.
 * <p>
 * Created by bozyurt on 12/10/18.
 */
public class GLUEFormatTrainingDataPreparer {
    static Pattern idPattern = Pattern.compile("^\\d+\\)");
    static Pattern suffixPattern = Pattern.compile("\\[[\\d\\.]+\\] PMID:\\d+$");


    public static List<Pair<String, String>> unique2(List<Pair<String, String>> qlList, Set<String> seenSet) {
        List<Pair<String, String>> filteredList = new ArrayList<>(qlList.size());
        for (Pair<String, String> pair : qlList) {
            if (!seenSet.contains(pair.getFirst())) {
                seenSet.add(pair.getFirst());
                filteredList.add(pair);
            } else {
                System.out.println("Duplicate question:" + pair.getFirst());
            }
        }
        return filteredList;
    }

    public static void prepareQuestionSearchTermClassificationSet(String gsQSCFile, String outDir,
                                                                  Set<String> excludeQuestionSet,
                                                                  Set<String> trainQuestionSet) throws Exception {
        Set<String> excludeQuestionNoWSSet = new HashSet<>();
        Set<String> trainQuestionNoWSSet = new HashSet<>();
        for(String question : excludeQuestionSet) {
            question = StringUtils.stripWS(question);
            excludeQuestionNoWSSet.add(question);
        }
        for(String question : trainQuestionSet) {
            question = StringUtils.stripWS(question);
            trainQuestionNoWSSet.add(question);
        }

        //POSTaggerME posTaggerME = EngineUtils.initializePOSTagger();
        BufferedReader in = null;
        int noQuestions = 0;
        List<Pair<String, String>> remainingList = new ArrayList<>();
        List<Pair<String, String>> testList = new ArrayList<>();
        List<Pair<String, String>> trainList = new ArrayList<>();

        try {
            in = FileUtils.newUTF8CharSetReader(gsQSCFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                String line2 = in.readLine();
                line2 = line2.trim();
                String question = line;
                //String[] tokens = line.split("\\s+");
                //String[] tags = posTaggerME.tag(tokens);
                String key = StringUtils.stripWS(question);
                if (excludeQuestionNoWSSet.contains(key)) {
                    testList.add(new Pair<>(question, line2));
                } else if (trainQuestionNoWSSet.contains(question)) {
                    trainList.add(new Pair<>(question, line2));
                } else {
                    remainingList.add(new Pair<>(question, line2));
                }
                noQuestions++;
            }
            System.out.println("# of questions:" + noQuestions);
        } finally {
            FileUtils.close(in);
        }
        Set<String> seenSet = new HashSet<>();
        testList = unique2(testList, seenSet);
        trainList = unique2(trainList, seenSet);
        remainingList = unique2(remainingList, seenSet);
        Collections.shuffle(remainingList);
        testList.addAll(remainingList.subList(0, 100));
        trainList.addAll(remainingList.subList(100, remainingList.size()));
        System.out.println("train size:" + trainList.size());
        System.out.println("test size:" + testList.size());
        saveQSCSet(trainList, outDir + "/qsc_set_train.txt");
        saveQSCSet(testList, outDir + "/qsc_set_test.txt");
    }

    public static void prepareQuestionSearchTermAnnotationSet(String qaResultsFile, String outDir) throws Exception {
        POSTaggerME posTaggerME = Utils.initializePOSTagger();
        BufferedReader in = null;
        Tokenizer tokenizer = new Tokenizer();
        int noQuestions = 0;
        List<Pair<String, String>> annotationList = new ArrayList<>();

        try {
            in = FileUtils.newUTF8CharSetReader(qaResultsFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Question:")) {
                    String question = line.substring("Question:".length());
                    List<String> tokens = tokenizer.tokenize(question);
                    String tokenizedQuestion = GenUtils.join(tokens, " ");

                    String toks[] = new String[tokens.size()];
                    toks = tokens.toArray(toks);
                    String[] tags = posTaggerME.tag(toks);
                    String classLabels = toClassLabels(toks, tags);
                    System.out.println(tokenizedQuestion);
                    System.out.println(classLabels);
                    System.out.println(GenUtils.join(tags, " "));
                    annotationList.add(new Pair<>(tokenizedQuestion, classLabels));
                    noQuestions++;
                }
            }
            System.out.println("# of questions:" + noQuestions);
        } finally {
            FileUtils.close(in);
        }
        Set<String> seenSet = new HashSet<>();
        annotationList = unique2(annotationList, seenSet);
        System.out.println("Annotation List size:" + annotationList.size());
        saveQSCSet(annotationList, outDir + "/qsc_set_annotation.txt");
    }

    public static void prepareQuestionPOSTrainingDataSet(String qscSetFile, String outFile) throws Exception {
        POSTaggerME posTaggerME = Utils.initializePOSTagger();
        BufferedReader in = null;
        BufferedWriter out = null;
        Tokenizer tokenizer = new Tokenizer();
        try {
            in = FileUtils.newUTF8CharSetReader(qscSetFile);
            out = FileUtils.newUTF8CharSetWriter(outFile);
            String line;
            int i = 0;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                i++;
                if ((i % 2) == 1) {
                    List<String> tokens = tokenizer.tokenize(line);
                    String[] toks = new String[tokens.size()];
                    toks = tokens.toArray(toks);
                    String[] tags = posTaggerME.tag(toks);
                    String posTags = GenUtils.join(tags, " ");
                    out.write(line);
                    out.newLine();
                    out.write(posTags);
                    out.newLine();
                }
            }
        } finally {
            FileUtils.close(in);
            FileUtils.close(out);
        }
    }

    static void saveQSCSet(List<Pair<String, String>> questionList, String outFile) throws IOException {
        BufferedWriter out = null;
        try {
            out = FileUtils.newUTF8CharSetWriter(outFile);
            for (Pair<String, String> qp : questionList) {
                out.write(qp.getFirst());
                out.newLine();
                out.write(qp.getSecond());
                out.newLine();
            }
            System.out.println("saved " + outFile);
        } finally {
            FileUtils.close(out);
        }
    }

    static String toClassLabels(String[] toks, String[] tags) {
        StringBuilder sb = new StringBuilder(200);
        for (int i = 0; i < toks.length; i++) {
            String token = toks[i];
            String tag = tags[i];
            String label = "0";
            if (tag.startsWith("N") || tag.startsWith("J")) {
                label = "1";
            }
            sb.append(label);
            for (int j = 0; j < token.length(); j++) {
                sb.append(' ');
            }
        }

        return sb.toString().trim();
    }


    public static List<QuestionAnswers> unique(List<QuestionAnswers> qaList) {
        List<QuestionAnswers> filteredList = new ArrayList<>(qaList.size());
        Set<String> seenSet = new HashSet<>();
        for (QuestionAnswers qa : qaList) {
            if (!seenSet.contains(qa.question)) {
                seenSet.add(qa.question);
                filteredList.add(qa);
            } else {
                System.out.println("Duplicate Question: " + qa.question);
            }
        }
        return filteredList;
    }

    public static void prepareKFoldCVData(String tsvFile, String outDir, int numFolds) throws IOException {
        new File(outDir).mkdirs();
        if (numFolds < 4) {
            numFolds = 4;
        }
        List<QuestionAnswers> qaList = loadQuestionAnswersList(tsvFile);
        System.out.println("qaList.size:" + qaList.size());
        Random random = new Random(28647425L);
        Collections.shuffle(qaList, random);
        int foldSize = Math.round(qaList.size() / (float) numFolds);
        for (int i = 0; i < numFolds; i++) {
            File foldDir = new File(outDir, "fold_" + (i + 1));
            foldDir.mkdir();
            int end = (i + 1) * foldSize;
            if ((i + 1) == numFolds) {
                end = qaList.size();
            }
            List<QuestionAnswers> devList = new ArrayList<>(qaList.subList(i * foldSize, end));
            List<QuestionAnswers> trainList;
            if (i == 0) {
                trainList = new ArrayList<>(qaList.subList(foldSize, qaList.size()));
            } else if ((i + 1) == numFolds) {
                trainList = new ArrayList<>(qaList.subList(0, qaList.size() - foldSize));
            } else {
                trainList = new ArrayList<>(qaList.subList(0, (i) * foldSize));
                trainList.addAll(qaList.subList((i + 1) * foldSize, qaList.size()));
            }
            createTSVFile(new File(foldDir, "train.tsv").getAbsolutePath(), trainList, null);
            createTSVFile(new File(foldDir, "dev.tsv").getAbsolutePath(), devList, null);
        }
    }

    public static List<QuestionAnswers> loadQuestionAnswersList(String tsvFile) throws IOException {
        List<QuestionAnswers> qaList = new ArrayList<>();
        BufferedReader in = null;
        QuestionAnswers current = null;
        try {
            in = FileUtils.newUTF8CharSetReader(tsvFile);
            String line;
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] tokens = line.split("\t");
                int label = NumberUtils.getInt(tokens[0]);
                String question = tokens[1];
                if (current == null || !current.question.equals(question)) {
                    current = new QuestionAnswers(question);
                    qaList.add(current);
                }
                current.addCandidate(tokens[2]);
                if (label == 1) {
                    current.addAnswer(tokens[2], -1);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        return qaList;
    }

    @SuppressWarnings("Duplicates")
    public static void prepareFullSet(String qaResultsFile, String outTSVDir, int devQuestionCount,
                                      Random rnd) throws IOException {
        Pattern rankPattern = Pattern.compile("rank:([\\d,]+)");

        BufferedReader in = null;
        int noQuestions = 0;
        List<QuestionAnswers> qaList = new ArrayList<>(800);
        try {
            in = FileUtils.newUTF8CharSetReader(qaResultsFile);
            String line;
            QuestionAnswers curQA = null;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Question:")) {
                    String question = line.substring("Question:".length());
                    curQA = new QuestionAnswers(question);
                }
                if (line.startsWith("Answer:")) {
                    noQuestions++;
                    line = in.readLine();
                    Matcher m = rankPattern.matcher(line);
                    if (m.find()) {
                        int rank = Integer.parseInt(m.group(1));
                        line = in.readLine();
                        if (line.startsWith("Manual:")) {
                            line = in.readLine();
                            m = rankPattern.matcher(line);
                            if (m.find()) {
                                String rankStr = m.group(1);
                                int[] ranks = extractRanks(rankStr);
                                if (ranks[0] < 101) {
                                    qaList.add(curQA);
                                    extractAnswerCandidates(in, ranks, curQA);

                                }

                            }
                        }
                    }
                }
            }
        } finally {
            FileUtils.close(in);
        }
        qaList = unique(qaList);
        System.out.println("qaList.size:" + qaList.size());
        calcDescStats(qaList);
        Collections.shuffle(qaList, rnd);
        List<QuestionAnswers> devQaList = qaList.subList(0, devQuestionCount);
        List<QuestionAnswers> trainQaList = qaList.subList(devQuestionCount, qaList.size());
        new File(outTSVDir).mkdirs();
        String testTSVFile = outTSVDir + "/test.tsv";
        String trainTSVFile = outTSVDir + "/train.tsv";

        createTSVFile(trainTSVFile, trainQaList, rnd);
        createTSVFile(testTSVFile, devQaList, rnd);
    }

    static void calcDescStats(List<QuestionAnswers> qaList) {
        double qSum = 0, aSum = 0;
        int qMax = -1, aMax = -1, total = 0;
        for (QuestionAnswers qa : qaList) {
            String question = qa.question;
            String[] tokens = question.split("\\s+");
            if (qMax < tokens.length) {
                qMax = tokens.length;
            }
            qSum += tokens.length;
            for (String candidate : qa.candidates) {
                tokens = candidate.split("\\s+");
                if (aMax < tokens.length) {
                    aMax = tokens.length;
                }
                aSum += tokens.length;
            }
            total += qa.candidates.size();
        }
        System.out.println(String.format("Question (# of tokens) max:%d avg:%.1f", qMax, qSum / qaList.size()));
        System.out.println(String.format("Candidate (# of tokens) max:%d avg:%.1f", aMax, aSum / total));
    }

    static void createTSVFile(String outTSVFile, List<QuestionAnswers> qaList, Random rnd) throws IOException {
        List<String> lines = new ArrayList<>(qaList.size() * 100);
        StringBuilder sb = new StringBuilder(500);
        List<String> tempList = new ArrayList<>(100);
        for (QuestionAnswers qa : qaList) {
            String question = qa.question;
            tempList.clear();
            for (String candidate : qa.candidates) {
                sb.setLength(0);
                String label = qa.isAnswer(candidate) ? "1" : "0";
                sb.append(label).append("\t").append(question).append("\t").append(candidate);
                tempList.add(sb.toString().trim());
            }
            if (rnd != null) {
                Collections.shuffle(tempList, rnd);
            }
            lines.addAll(tempList);
        }
        BufferedWriter out = null;
        try {
            out = FileUtils.newUTF8CharSetWriter(outTSVFile);
            out.write("label\tquestion\tanswer_candidate");
            out.newLine();
            for (String line : lines) {
                out.write(line);
                out.newLine();
            }
        } finally {
            FileUtils.close(out);
        }
        System.out.println("wrote " + outTSVFile);
    }

    static int indexOf(int[] arr, int refValue) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == refValue) {
                return i;
            }
        }
        return -1;
    }

    static int extractId(String line) {
        Matcher matcher = idPattern.matcher(line);
        if (matcher.find()) {
            return Integer.parseInt(line.substring(0, matcher.end() - 1));
        }
        return -1;
    }

    public static List<String> extractAnswerCandidates(BufferedReader in, int[] ranks, QuestionAnswers qa) throws IOException {
        List<String> lines = new ArrayList<>();
        do {
            String line = in.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.equals("===========================")) {
                break;
            }
            lines.add(line);
        } while (true);
        List<String> candidates = new ArrayList<>(200);
        int i = 0;
        int count = 0;
        StringBuilder sb = new StringBuilder(300);
        while (i < lines.size()) {
            String line = lines.get(i);
            Matcher matcher = idPattern.matcher(line);
            if (matcher.find()) {
                if (sb.length() > 0) {
                    String content = sb.toString().trim();

                    String candidate = extractAnswerCandidate(content);
                    Assertion.assertNotNull(candidate);
                    if (count < 100) {
                        int idx = extractId(content);
                        Assertion.assertTrue(idx != -1);
                        qa.addCandidate(candidate);
                        if (indexOf(ranks, idx) != -1) {
                            qa.addAnswer(candidate, i);
                        }
                        candidates.add(candidate);
                        count++;
                    } else {
                        break;
                    }
                    sb.setLength(0);
                }
                sb.append(line).append(' ');

            } else {
                sb.append(line).append(' ');
            }
            i++;
        }
        if (sb.length() > 0 && count < 100) {
            String content = sb.toString().trim();
            String candidate = extractAnswerCandidate(content);
            Assertion.assertNotNull(candidate);
            candidates.add(candidate);
            int idx = extractId(content);
            Assertion.assertTrue(idx != -1);
            qa.addCandidate(candidate);
            if (indexOf(ranks, idx) != -1) {
                qa.addAnswer(candidate, i);
            }
        }
        return candidates;
    }

    static String extractAnswerCandidate(String line) {
        Matcher matcher = idPattern.matcher(line);
        if (matcher.find()) {
            line = line.substring(matcher.end() + 1);
            matcher = suffixPattern.matcher(line);
            if (matcher.find()) {
                return line.substring(0, matcher.start());
            }
        }
        return null;
    }


    public static int[] extractRanks(String rankStr) {
        if (rankStr.indexOf(',') != -1) {
            String[] tokens = rankStr.split(",");
            int[] ranks = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                ranks[i] = Integer.parseInt(tokens[i]);
            }
            return ranks;
        } else {
            return new int[]{Integer.parseInt(rankStr)};
        }
    }

    public static class QuestionAnswers {
        String question;
        List<String> candidates = new ArrayList<>();
        List<Pair<Integer, String>> answers = new ArrayList<>(1);


        public QuestionAnswers(String question) {
            this.question = question;
        }

        public void addCandidate(String candidateSentence) {
            this.candidates.add(candidateSentence);
        }

        public void addAnswer(String answer, int answerIdx) {
            answers.add(new Pair<>(answerIdx, answer));
        }

        boolean isAnswer(String candidate) {
            for (Pair<Integer, String> answer : answers) {
                if (answer.getSecond().equals(candidate)) {
                    return true;
                }
            }
            return false;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getCandidates() {
            return candidates;
        }

        public boolean isAnAnswer(String candidate) {
            for(Pair<Integer,String> pair : answers) {
                if (pair.getSecond().equals(candidate)) {
                    return true;
                }
            }
            return false;
        }

        public List<Pair<Integer, String>> getAnswers() {
            return answers;
        }
    }

    public static Set<String> loadQuestionSet(String tsvFile) throws Exception {
        Set<String> questionSet = new HashSet<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(tsvFile);
            String line;
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] tokens = line.split("\t");
                    Assertion.assertEquals(tokens.length, 3);
                    questionSet.add(tokens[1]);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        return questionSet;
    }


    public static void prepBERTCVData() throws Exception {
        String homeDir = System.getProperty("user.home");
        String trainTSVFile = homeDir + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/train.tsv";
        String outDir = "/tmp/bert_cv";
        prepareKFoldCVData(trainTSVFile, outDir, 5);
    }

    public static void prepareFullSplits() throws IOException {
        Random rnd = new Random(12864763L);
        String homeDir = System.getProperty("user.home");
        String qaResultsFile = homeDir + "/dev/java/bnerkit/data/bioasq/qaengine1/question_results_wmd_defn_focus.txt";
        for(int i = 2; i <= 5; i++) {
            String outTSVDir = "/tmp/bioasq_manual_100_" + i;
            prepareFullSet(qaResultsFile, outTSVDir, 100, rnd);
        }
    }

    public static void main(String[] args) throws Exception {
        testDriver();

        // prepBERTCVData();
        //prepareFullSplits();

    }

    static void testDriver() throws Exception {
        String homeDir = System.getProperty("user.home");
        String qaResultsFile = homeDir + "/dev/java/bnerkit/data/bioasq/qaengine1/question_results_wmd_defn_focus.txt";
        String outTSVDir = "/tmp/biasq_manual_100";
        // prepareFullSet(qaResultsFile, outTSVDir, 100);
        String testTSVFile = homeDir + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/test.tsv";
        String trainTSVFile = homeDir + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/train.tsv";
        Set<String> testQuestionSet = loadQuestionSet(testTSVFile);
        Set<String> trainQuestionSet = loadQuestionSet(trainTSVFile);

        String qscSetAnnotFile = homeDir + "/dev/java/bnerkit/data/bioasq/qsc_set.txt";
        // prepareQuestionSearchTermClassificationSet(qscSetAnnotFile, "/tmp",  testQuestionSet, trainQuestionSet);

        String qscSetTrainFile = homeDir + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/qsc/qsc_set_train.txt";
        String qscSetTestFile = homeDir + "/dev/java/bnerkit/data/bioasq/bioasq_manual_100/qsc/qsc_set_test.txt";

        qscSetTrainFile = homeDir + "/dev/java/bio-answerfinder/data/rank_test/rank_train_data.dat";
        qscSetTestFile = homeDir + "/dev/java/bio-answerfinder/data/rank_test/rank_test_data.dat";

        //prepareQuestionPOSTrainingDataSet(qscSetTrainFile, "/tmp/qsc_set_pos_tags_train.txt");
        // prepareQuestionPOSTrainingDataSet(qscSetTestFile, "/tmp/qsc_set_pos_tags_test.txt");
        prepareQuestionPOSTrainingDataSet(qscSetTrainFile, "/tmp/rank_train_pos_data.dat");
        prepareQuestionPOSTrainingDataSet(qscSetTestFile, "/tmp/rank_test_pos_data.dat");
    }
}
