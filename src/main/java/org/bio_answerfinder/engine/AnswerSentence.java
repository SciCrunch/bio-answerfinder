package org.bio_answerfinder.engine;

/**
 * Created by bozyurt on 9/13/17.
 */
public class AnswerSentence {
    int id;
    String qid;
    String pmid;
    String sentence;
    float score;

    public AnswerSentence(int id, String qid, String pmid, float score) {
        this.id = id;
        this.qid = qid;
        this.pmid = pmid;
        this.score = score;
    }

    public void setSentence(String sentence) {
        this.sentence = sentence;
    }

    public int getId() {
        return id;
    }

    public String getQid() {
        return qid;
    }

    public String getPmid() {
        return pmid;
    }

    public String getSentence() {
        return sentence;
    }

    public float getScore() {
        return score;
    }
}
