package org.bio_answerfinder.evaluation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecord.Snippet;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.util.*;

/**
 * Created by bozyurt on 7/19/19.
 */
public class GSCSVGenerator {
    static final String HOME_DIR = System.getProperty("user.home");

    public static void prepGCAnswerCSV(String csvFile) throws Exception {
        final List<String> gsQuestions = FileUtils.loadSentences(HOME_DIR +
                "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt");
        Map<String, Integer> map = new HashMap<>();
        int idx = 0;
        for (String q : gsQuestions) {
            map.put(StringUtils.stripWS(q), idx);
            idx++;
        }

        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        System.out.println("# of questionRecords:" + questionRecords.size());
        Set<String> seenSet = new HashSet<>();
        for (QuestionRecord qr : questionRecords) {
            String questionStr = qr.getQuestion();
            String key = StringUtils.stripWS(questionStr);
            if (!seenSet.contains(key)) {
                seenSet.add(key);
            } else {
                System.out.println("Seen before:" + questionStr + " with id "+ qr.getId());
            }
        }

        BufferedWriter out = null;
        List<GSAnswer> gsAnswers = new ArrayList<>();
        CSVFormat csvFormat = CSVFormat.EXCEL;
        int count = 0;

        try {
            seenSet = new HashSet<>();
            out = FileUtils.newUTF8CharSetWriter(csvFile);
            CSVPrinter csvPrinter = new CSVPrinter(out, csvFormat);
            csvPrinter.printRecord(new Object[]{"question", "exact answer", "snippets"});
            for (QuestionRecord qr : questionRecords) {

                String questionStr = qr.getQuestion();
                String key = StringUtils.stripWS(questionStr);
                if (map.containsKey(key) && !seenSet.contains(key)) {
                    Object[] row = toCSVRecord(qr);
                    int locIdx = map.get(key);
                    gsAnswers.add(new GSAnswer(row, locIdx));
                    count++;
                    seenSet.add(key);
                }
            }
            Collections.sort(gsAnswers, (o1, o2) -> Integer.compare(o1.locIdx, o2.locIdx));
            for (GSAnswer gsa : gsAnswers) {
                csvPrinter.printRecord(gsa.row);
            }
            System.out.println("# of questions:" + count);
            System.out.println("wrote " + csvFile);
        } finally {
            FileUtils.close(out);
        }

    }

    public static class GSAnswer {
        Object[] row;
        int locIdx;

        public GSAnswer(Object[] row, int locIdx) {
            this.row = row;
            this.locIdx = locIdx;
        }
    }

    public static Object[] toCSVRecord(QuestionRecord qr) {
        QuestionRecord.Answer answer = qr.getAnswer();
        Object[] row = new Object[3];
        String questionStr = qr.getQuestion();
        row[0] = questionStr;
        StringBuilder sb = new StringBuilder(2000);
        List<QuestionRecord.AnswerItem> exactAnswers = answer.getExactAnswer();
        if (exactAnswers.size() > 0) {
            for (QuestionRecord.AnswerItem ai : exactAnswers) {
                sb.append(ai.getAnswers()).append("\n");
            }
        } else {
            sb.append("N/A");
        }
        row[1] = sb.toString().trim();
        List<Snippet> snippets = answer.getSnippets();
        sb = new StringBuilder();
        if (snippets.size() > 0) {
            Set<String> seenSet = new HashSet<>();
            for (Snippet snippet : snippets) {
                String docURL = snippet.getDocumentURL();
                String text = snippet.getText();
                if (!seenSet.contains(text)) {
                    sb.append(text);
                    sb.append(" (").append(docURL).append(")\n");
                    seenSet.add(text);
                }
            }
        } else {
            sb.append("N/A");
        }
        row[2] = sb.toString().trim();
        return row;
    }

    public static void main(String[] args) throws Exception {
        prepGCAnswerCSV("/tmp/gold_standard_answers.csv");
    }
}
