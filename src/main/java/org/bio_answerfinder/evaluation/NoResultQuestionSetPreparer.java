package org.bio_answerfinder.evaluation;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 4/15/19.
 * #DNI
 */
public class NoResultQuestionSetPreparer {
    static String noResultQuestionsFile = "${home}/dev/java/bnerkit/data/qa_paper/NOTES";
    static Pattern pattern = Pattern.compile("[\\(\\[]CHECK");


    public static List<String> loadNoResultQuestions() throws IOException {
        String[] lines = FileUtils.readLines(FileUtils.adjustPath(noResultQuestionsFile), false, CharSetEncoding.UTF8);
        List<String> questions = new ArrayList<>(lines.length);
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                line = line.substring(0, matcher.start()).trim();
                questions.add(line);
            } else {
                questions.add(line);
            }
        }
        return questions;
    }

    public static List<Pair<String, String>> loadQSCLabeledSetFile(String qscLabeledSetFile) throws IOException {
        BufferedReader in = null;
        List<Pair<String, String>> list = new ArrayList<>();
        try {
            in = FileUtils.newUTF8CharSetReader(qscLabeledSetFile);
            String line;
            while ((line = in.readLine()) != null) {
                String line2 = in.readLine();
                list.add(new Pair<>(line, line2));
            }
        } finally {
            FileUtils.close(in);
        }
        return list;
    }


    public static void buildQSCLabeledSetFile(List<String> noResultQuestions, String outQSCFile) throws IOException {
        String trainQSCFile = "${home}/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc/qsc_set_train.txt";
        String testQSCFile = "${home}/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc/qsc_set_test.txt";
        List<Pair<String, String>> pairList = loadQSCLabeledSetFile(FileUtils.adjustPath(trainQSCFile));
        pairList.addAll(loadQSCLabeledSetFile(FileUtils.adjustPath(testQSCFile)));
        Map<String, Pair<String, String>> pairMap = new HashMap<>();
        for (Pair<String, String> pair : pairList) {
            String key = StringUtils.stripWS(pair.getFirst());
            pairMap.put(key, pair);
        }
        List<String> outList = new ArrayList<>();
        int missing = 0;
        for (String noResultQuestion : noResultQuestions) {
            String key = StringUtils.stripWS(noResultQuestion);
            Pair<String, String> pair = pairMap.get(key);
            if (pair != null) {
                outList.add(pair.getFirst());
                outList.add(pair.getSecond());
            } else {
                missing++;
                System.err.println("Missing >> " + noResultQuestion);
            }
        }
        System.out.println("missing: " + missing);
        FileUtils.saveList(outList, outQSCFile, CharSetEncoding.UTF8);
        System.out.println("saved " + outQSCFile);
    }


    public static void prepareNoResultsQuestionsForRankTesting(String noResultQuestionsFile) throws Exception {
        String[] lines = FileUtils.readLines(FileUtils.adjustPath(noResultQuestionsFile), false, CharSetEncoding.UTF8);
        List<String> candidateQuestions = new ArrayList<>(100);
        for (int i = 0; i < lines.length; i += 2) {
            String question = lines[i];
            String[] labels = lines[i + 1].split("\\s+");
            int keywordCount = 0;
            for (String label : labels) {
                if (!label.equals("0")) {
                    keywordCount++;
                }
            }
            if (keywordCount > 1) {
                candidateQuestions.add(question);
            }
        }
        System.out.println("# of candidateQuestions:" + candidateQuestions.size());
        Collections.shuffle(candidateQuestions);
        FileUtils.saveList(candidateQuestions.subList(0, 100), "/tmp/rank_test_questions.txt",
                CharSetEncoding.UTF8);
    }


    public static void prepRankTrainTestData(String fullRankDataFile, String rankTestQuestionsFile) throws Exception {
        Set<String> questionSet = new HashSet<>(
                Arrays.asList(FileUtils.readLines(FileUtils.adjustPath(rankTestQuestionsFile), false,
                        CharSetEncoding.UTF8)));
        String[] lines = FileUtils.readLines(FileUtils.adjustPath(fullRankDataFile), false, CharSetEncoding.UTF8);
        List<String> trainData = new ArrayList<>();
        List<String> testData = new ArrayList<>();
        for (int i = 0; i < lines.length; i += 2) {
            String question = lines[i];
            String labels = lines[i + 1];
            if (questionSet.contains(question)) {
                testData.add(question);
                testData.add(labels);
            } else {
                trainData.add(question);
                trainData.add(labels);
            }
        }
        FileUtils.saveList(trainData, "/tmp/rank_train_data.dat", CharSetEncoding.UTF8);
        FileUtils.saveList(testData, "/tmp/rank_test_data.dat", CharSetEncoding.UTF8);
    }


    public static void main(String[] args) throws Exception {
        /*
        List<String> questions = loadNoResultQuestions();
        questions.forEach(System.out::println);
        System.out.println("# of unanswered questions: " + questions.size());

        buildQSCLabeledSetFile(questions, "/tmp/no_results_qsc_set.txt");
        */

    //    prepareNoResultsQuestionsForRankTesting(
    //            "${home}/dev/java/bio-answerfinder/data/rank_test/no_results_qsc_set.txt");

        prepRankTrainTestData("${home}/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc/qsc_set_rank_full_uniq.txt",
                "${home}/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt");

    }

}
