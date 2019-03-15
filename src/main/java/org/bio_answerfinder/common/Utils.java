package org.bio_answerfinder.common;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import org.bio_answerfinder.common.dependency.DependencyTreeFactory;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/12/17.
 */
public class Utils {

    public static List<SpanPOS> tokenizeWithPOS(String sentence, List<String> posTags) {
        List<Span> spanList = tokenize(sentence);
        Assertion.assertTrue(spanList.size() == posTags.size());
        List<SpanPOS> spList = new ArrayList<>(spanList.size());
        for (int i = 0; i < posTags.size(); i++) {
            Span span = spanList.get(i);
            String posTag = posTags.get(i);
            spList.add(new SpanPOS(posTag, span));
        }
        return spList;
    }


    public static void addLemmas(List<SpanPOS> spanPOSList) {
        for (SpanPOS sp : spanPOSList) {
            String lemma = DependencyTreeFactory.getLemma(sp.getToken().toLowerCase(), sp.getPosTag());
            if (lemma == null) {
                if (sp.getPosTag().startsWith("N") && sp.getToken().endsWith("s")) {
                    int len = sp.getToken().length();
                    if (!StringUtils.isVowel(sp.getToken().charAt(len - 2))) {
                        lemma = sp.getToken().substring(0, len - 1);
                        sp.setLemma(lemma);
                        continue;
                    }
                }
                lemma = sp.getToken();
            }
            sp.setLemma(lemma);
        }
    }

    public static List<SpanPOS> locateSpansInSentence(String phrase, List<SpanPOS> spList) {
        List<SpanPOS> locatedList = new ArrayList<>(2);
        String[] tokens = phrase.split("\\s+");
        boolean inPhrase = false;
        int idx = 0;
        for (SpanPOS sp : spList) {
            if (inPhrase) {
                if (sp.getToken().equals(tokens[idx])) {
                    locatedList.add(sp);
                    idx++;
                    if (idx >= tokens.length) {
                        break;
                    }
                } else {
                    idx = 0;
                    locatedList.clear();
                }
            } else {
                if (sp.getToken().equals(tokens[idx])) {
                    inPhrase = true;
                    locatedList.add(sp);
                    idx++;
                    if (idx >= tokens.length) {
                        break;
                    }
                }
            }
        }
        return locatedList;
    }

    public static List<SpanPOS> locateSpansInSentence(int startIdx, int endIdx, List<SpanPOS> spList) {
        List<SpanPOS> locatedList = new ArrayList<>(2);
        boolean inPhrase = false;
        for (SpanPOS sp : spList) {
            if (inPhrase) {
                if (sp.getStartIdx() <= endIdx) {
                    locatedList.add(sp);
                }
                if (sp.getEndIdx() == endIdx) {
                    break;
                }
            } else {
                if (sp.getStartIdx() == startIdx) {
                    locatedList.add(sp);
                    inPhrase = true;
                    if (sp.getEndIdx() == endIdx) {
                        break;
                    }
                }
            }
        }
        return locatedList;
    }

    public static int spanIndexOf(int locIdx, List<Span> spanList) {
        for (int i = 0; i < spanList.size(); i++) {
            Span span = spanList.get(i);
            if (span.getStartIdx() <= locIdx && span.getEndIdx() >= locIdx) {
                return i;
            }
        }
        return -1;
    }

    public static int spanPOSIndexOf(int locIdx, List<SpanPOS> spanList) {
        for (int i = 0; i < spanList.size(); i++) {
            SpanPOS span = spanList.get(i);
            if (span.getStartIdx() <= locIdx && span.getEndIdx() >= locIdx) {
                return i;
            }
        }
        return -1;
    }

    public static List<Span> tokenize(String sentence) {
        int i = 0, len = sentence.length();
        List<Span> tokens = new ArrayList<>(20);
        StringBuilder buf = new StringBuilder();
        int start = 0;
        while (i < len) {
            char c = sentence.charAt(i);
            if (Character.isWhitespace(c)) {
                if (buf.length() > 0) {
                    tokens.add(new Span(buf.toString(), start, i - 1));
                    buf.setLength(0);
                    start = -1;
                }
            } else {
                if (start < 0) {
                    start = i;
                }
                buf.append(c);
            }
            i++;
        }
        if (buf.length() > 0) {
            tokens.add(new Span(buf.toString(), start, len - 1));
        }
        return tokens;
    }

    public static POSTaggerME initializePOSTagger() throws IOException {
        POSModel model;
        InputStream pin = null;
        try {
            pin = Utils.class.getClassLoader().getResourceAsStream(
                    "opennlp/models/en-pos-maxent.bin");
            model = new POSModel(pin);
            return new POSTaggerME(model);
        } finally {
            FileUtils.close(pin);
        }
    }
}
