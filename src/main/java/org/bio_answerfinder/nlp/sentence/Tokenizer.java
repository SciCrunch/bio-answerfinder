package org.bio_answerfinder.nlp.sentence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around SentenceLexer2
 * Created by bozyurt on 11/27/16.
 */
public class Tokenizer {

    public List<String> tokenize(String sentence) {
        List<String> tokens = new ArrayList<String>();
        SentenceLexer2 lexer = new SentenceLexer2(sentence);
        TokenInfo ti;
        try {
            while ((ti = lexer.getNextTI()) != null) {
                String token = ti.getTokValue();
                int idx;

                if (token.length() > 3 && (idx = token.indexOf('/')) != -1) {
                    if (idx + 1 < token.length() && token.indexOf('/', idx + 1) == -1) {
                        tokens.add(token.substring(0, idx));
                        tokens.add("/");
                        tokens.add(token.substring(idx + 1));
                    } else {
                        tokens.add(token);
                    }
                } else if (token.equals("&")){
                    tokens.add("and");
                } else {
                    tokens.add(ti.getTokValue());
                }
            }
            return tokens;
        } catch (IOException x) {
            x.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Tokenizer() {
    }
}
