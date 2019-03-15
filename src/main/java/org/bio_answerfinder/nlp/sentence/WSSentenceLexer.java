package org.bio_answerfinder.nlp.sentence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class WSSentenceLexer {
    char[] carr;
    int curCursorIdx = 0;
    StringBuilder sb = new StringBuilder();

    public WSSentenceLexer(String sentence) {
        carr = sentence.toCharArray();
    }

    public TokenInfo getNextTI() throws IOException {
        if (curCursorIdx < carr.length) {
            skipWS();
            int i = curCursorIdx;
            sb.setLength(0);
            while (i < carr.length && !Character.isWhitespace(carr[i])) {
                sb.append(carr[i]);
                i++;
            }
            TokenInfo ti = new TokenInfo(sb.toString(), curCursorIdx, i);
            curCursorIdx = i;
            return ti;
        }
        return null;
    }

    protected void skipWS() {
        while (curCursorIdx < carr.length
                && Character.isWhitespace(carr[curCursorIdx])) {
            curCursorIdx++;
        }
    }

    public List<TokenInfo> tokenize() throws IOException {
        List<TokenInfo> tiList = new ArrayList<>(10);
        TokenInfo ti;
        while( (ti = getNextTI()) != null) {
            tiList.add(ti);
        }
        return tiList;
    }
}
