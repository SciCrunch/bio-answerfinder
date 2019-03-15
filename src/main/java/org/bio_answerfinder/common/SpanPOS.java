package org.bio_answerfinder.common;

/**
 * Created by bozyurt on 6/12/17.
 */
public class SpanPOS {
    final String posTag;
    final Span span;
    String lemma;

    public SpanPOS(String posTag, Span span) {
        this.posTag = posTag;
        this.span = span;
    }

    public String getToken() {
        return span.getToken();
    }

    public int getStartIdx() {
        return span.getStartIdx();
    }

    public int getEndIdx() {
        return span.getEndIdx();
    }

    public String getPosTag() {
        return posTag;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
}
