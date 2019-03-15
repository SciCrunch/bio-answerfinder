package org.bio_answerfinder.common.types;

/**
 * Created by bozyurt on 7/26/17.
 */
public class Ngram {
    final String phrase;
    final float score;

    public Ngram(String phrase, float score) {
        this.phrase = phrase;
        this.score = score;
    }

    public String getPhrase() {
        return phrase;
    }

    public float getScore() {
        return score;
    }
}
