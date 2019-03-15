package org.bio_answerfinder.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 11/22/17.
 */
public class ResultDoc {
    String pmid;
    List<ResultSentence> sentences = new ArrayList<>(5);
    float score;


    public ResultDoc(String pmid) {
        this.pmid = pmid;
    }

    public ResultDoc(String pmid, float score) {
        this.pmid = pmid;
        this.score = score;
    }

    public void addSentence(ResultSentence rs) {
        this.sentences.add(rs);
    }

    public String getPmid() {
        return pmid;
    }


    public List<ResultSentence> getSentences() {
        return sentences;
    }


    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public static class ResultSentence {
        String sentence;
        float score;
        int focusTypeEntityCount;

        public ResultSentence(String sentence, float score, int focusTypeEntityCount) {
            this.sentence = sentence;
            this.score = score;
            this.focusTypeEntityCount = focusTypeEntityCount;
        }

        public String getSentence() {
            return sentence;
        }

        public float getScore() {
            return score;
        }

        public int getFocusTypeEntityCount() {
            return focusTypeEntityCount;
        }
    }
}
