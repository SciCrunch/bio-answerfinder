package org.bio_answerfinder.engine;

import org.bio_answerfinder.engine.BERTRerankerServiceClient.RankedSentence;
import org.bio_answerfinder.util.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by bozyurt on 3/11/19.
 */
public class BERTRerankerServiceClientTest {
    static String HOME_DIR = System.getProperty("user.home");

    @Test
    public void testService() throws Exception {
        List<QuestionInfo> qiList = loadData(HOME_DIR +
                "/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/test.tsv");
        System.out.println(qiList.size());
        BERTRerankerServiceClient client = new BERTRerankerServiceClient();

        QuestionInfo qi = qiList.get(0);

        List<RankedSentence> ranked = client.rerank(qi.getQuestion(), qi.getCandidates());
        assertNotNull(ranked);
        assertEquals(100,ranked.size());
        for(RankedSentence rs : ranked) {
            System.out.println(rs);
        }

    }

    public static List<QuestionInfo> loadData(String tsvFile) throws IOException {
        List<String> lines = FileUtils.loadSentences(tsvFile);

        List<QuestionInfo> qiList = new ArrayList<>();
        String currentQuestion = null;

        boolean first = true;
        for(String line : lines) {
            if (first) {
                first = false;
                continue;
            }
            String[] parts = line.split("\t");
            String question = parts[1];
            String candidate = parts[2];
            if (currentQuestion == null || !currentQuestion.equals(question)) {
                QuestionInfo qi = new QuestionInfo(question);
                currentQuestion = question;
                qiList.add(qi);
            }
            qiList.get(qiList.size() - 1).addAnswerCandidate(candidate);
        }

        return qiList;
    }

    public static class QuestionInfo {
        String question;
        List<String> candidates = new ArrayList<>();

        public QuestionInfo(String question) {
            this.question = question;
        }

        public void addAnswerCandidate(String candidate) {
            candidates.add(candidate);
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getCandidates() {
            return candidates;
        }
    }
}
