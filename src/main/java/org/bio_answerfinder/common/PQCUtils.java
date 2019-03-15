package org.bio_answerfinder.common;


import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.NumberUtils;
import org.bio_answerfinder.util.StringUtils;

import java.util.*;

/**
 * Created by bozyurt on 12/17/17.
 */
public class PQCUtils {
    static Set<String> stopWordSet = new HashSet<>();

    static {
        stopWordSet.add("role");
        stopWordSet.add("name");
        stopWordSet.add("kind");
        stopWordSet.add("type");
        stopWordSet.add("subtype");
        stopWordSet.add("function");
        stopWordSet.add("obtain");
        stopWordSet.add("main");
        stopWordSet.add("most");
        stopWordSet.add("aim");
    }

    public static boolean isEligible(SearchQuery.SearchTerm st) {
        if (!st.isPhrase() && stopWordSet.contains(st.getTerm().toLowerCase())) {
            return false;
        }
        return true;
    }

    public static String prepTerm(SearchQuery.SearchTerm st) {
        if (st.isPhrase()) {
            String phrase = st.getTerm();
            if (phrase.startsWith("most")) {
                String[] tokens = phrase.split("\\s+");
                int start = 1;
                if (tokens.length > 2) {
                    start = 2;
                }
                phrase = GenUtils.join(Arrays.asList(Arrays.copyOfRange(tokens, start, tokens.length)), " ");
            } else if (phrase.startsWith("main")) {
                int idx = phrase.indexOf(" ");
                phrase = phrase.substring(idx + 1);
            } else if (phrase.endsWith("'s")) { // Parkinson 's, Alzheimer 's etc
                phrase = phrase.substring(0, phrase.length() - 2).trim();
            } else if (phrase.endsWith(" '")) {
                phrase = phrase.substring(0, phrase.length() - 2).trim();
            }

            return "\"" + phrase + "\"[All Fields]";
        }
        return st.getTerm() + "[All Fields]";
    }

    public static String prepESTerm(SearchQuery.SearchTerm st) {
        String suffix = ( new StringBuilder()).append("^").append(NumberUtils.formatDecimal(st.getWeight(), 1)).toString();
        if (st.isPhrase()) {
            String phrase = st.getTerm();
            if (phrase.startsWith("most")) {
                String[] tokens = phrase.split("\\s+");
                int start = 1;
                if (tokens.length > 2) {
                    start = 2;
                }
                phrase = GenUtils.join(Arrays.asList(Arrays.copyOfRange(tokens, start, tokens.length)), " ");
            } else if (phrase.startsWith("main")) {
                int idx = phrase.indexOf(" ");
                phrase = phrase.substring(idx + 1);
            } else if (phrase.endsWith("'s")) { // Parkinson 's, Alzheimer 's etc
                phrase = phrase.substring(0, phrase.length() - 2).trim();
            } else if (phrase.endsWith(" '")) {
                phrase = phrase.substring(0, phrase.length() - 2).trim();
            }

            return "\"" + phrase + "\"" + suffix;
        }
        return st.getTerm() + suffix;
    }

    public static boolean allNonPhrase(SearchQuery.QueryPart qp) {
        for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
            if (st.isPhrase()) {
                return false;
            }
        }
        return true;
    }

    public static boolean allCapital(SearchQuery.QueryPart qp) {
        for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
            if (st.isPhrase()) {
                return false;
            } else {
                if (!StringUtils.isAllCapital(st.getTerm())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getLongestCommonPrefix(SearchQuery.QueryPart qp) {
        List<String> terms = new ArrayList<>(qp.getSearchTerms().size());
        for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
            terms.add(st.getTerm());
        }
        return getLongestCommonPrefix(terms);
    }


    public static String getLongestCommonPrefix(List<String> terms) {
        StringBuilder sb = new StringBuilder();
        int minLen = Integer.MAX_VALUE;
        for (String term : terms) {
            if (term.length() < minLen) {
                minLen = term.length();
            }
        }
        for (int i = 0; i < minLen; i++) {
            boolean ok = true;
            char refChar = terms.get(0).charAt(i);
            for (int j = 1; j < terms.size(); j++) {
                if (refChar != terms.get(j).charAt(i)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                sb.append(refChar);
            } else {
                break;
            }
        }

        String s = sb.toString().trim();
        return s.length() < 4 ? null : s;
    }
}
