package org.bio_answerfinder.common;

/**
 * Created by bozyurt on 9/19/17.
 */

public class WordVector {
    final float weight;
    final float[] gloveVector;
    String term;

    public WordVector(float weight, float[] gloveVector, String term) {
        this.weight = weight;
        this.gloveVector = gloveVector;
        this.term = term;
    }

    public WordVector(float weight, float[] gloveVector) {
        this.weight = weight;
        this.gloveVector = gloveVector;
    }

    public float getWeight() {
        return weight;
    }

    public float[] getGloveVector() {
        return gloveVector;
    }

    public String getTerm() {
        return term;
    }
}
