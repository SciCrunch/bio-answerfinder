package org.bio_answerfinder.util;

import org.bio_answerfinder.nlp.sentence.SentenceLexer2;
import org.bio_answerfinder.nlp.sentence.TokenInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 2/25/19.
 */
public class NERUtils {
    public static List<TokenInfo> toTokens(String sentence)
            throws IOException {
        List<TokenInfo> tiList = new ArrayList<TokenInfo>();
        SentenceLexer2 sl = new SentenceLexer2(sentence);
        TokenInfo ti;
        while ((ti = sl.getNextTI()) != null) {
            tiList.add(ti);
        }
        return tiList;
    }
}
