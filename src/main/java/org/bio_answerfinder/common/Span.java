package org.bio_answerfinder.common;

/**
 * Created by bozyurt on 6/12/17.
 */
public class Span {
    final String token;
    final int startIdx;
    final int endIdx;

    public Span(String token, int startIdx, int endIdx) {
        this.token = token;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }

    public String getToken() {
        return token;
    }

    public int getStartIdx() {
        return startIdx;
    }

    public int getEndIdx() {
        return endIdx;
    }
}
