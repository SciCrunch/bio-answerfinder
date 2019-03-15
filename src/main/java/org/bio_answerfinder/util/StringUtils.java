package org.bio_answerfinder.util;


import org.bio_answerfinder.common.types.Range;
import org.bio_answerfinder.common.types.TokenInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 2/6/16.
 */
public class StringUtils {
    public static int countMatches(String str, String str2Match) {
        int count = 0;
        int idx = str.indexOf(str2Match);
        while (idx != -1) {
            count++;
            idx = str.indexOf(str2Match, idx + 1);
        }
        return count;
    }

    public static TokenInfo[] tokenize(String sentence) {
        char[] carr = sentence.toCharArray();
        List<TokenInfo> tiList = new ArrayList<TokenInfo>(15);
        StringBuilder buf = new StringBuilder();
        int startIdx = -1;
        for (int i = 0; i < carr.length; i++) {
            if (Character.isWhitespace(carr[i])) {
                if (buf.length() > 0) {
                    TokenInfo ti = new TokenInfo(buf.toString(), startIdx);
                    tiList.add(ti);
                    buf = new StringBuilder();
                    startIdx = -1;
                }
                continue;
            }
            if (startIdx == -1)
                startIdx = i;
            buf.append(carr[i]);
        }
        if (buf.length() > 0) {
            TokenInfo ti = new TokenInfo(buf.toString(), startIdx);
            tiList.add(ti);
        }
        TokenInfo[] tiArr = new TokenInfo[tiList.size()];
        tiArr = tiList.toArray(tiArr);
        return tiArr;
    }

    /**
     * All occurrences of <code>''</code> is replaced for both passed strings
     * with <code>"</code> before any match attempt.
     *
     * @param s1 checked to be contained
     * @param s2 containing
     * @return
     */
    public static Range containingRangeNoWhiteSpace(String s1, String s2) {
        if (s1 == null || s2 == null)
            throw new IllegalArgumentException(
                    "Arguments 's1' and 's2' both must be not null!");
        s1 = s1.replaceAll("'\\s?'", "\"");
        s2 = s2.replaceAll("'\\s?'", "\"");
        s1 = s1.replaceAll("`\\s?`", "\"");
        s2 = s2.replaceAll("`\\s?`", "\"");
      /*
       * if (s1.equals("`") || s1.equals("'")) { s1 = "\""; }
       */

        char[] carr1 = s1.toCharArray();
        char[] carr2 = s2.toCharArray();
        int i = 0, j = 0;
        while (j < carr2.length) {
            while (j < carr2.length && Character.isWhitespace(carr2[j])) {
                j++;
            }
            if (carr1[i] == carr2[j]) {
                int endIdx = matchNoWhiteSpace(carr1, carr2, j);
                if (endIdx != -1) {
                    return new Range(j, endIdx);
                } else {
                    j++;
                    continue;
                }
            }
            j++;
        }
        return null;
    }

    protected static int matchNoWhiteSpace(char[] carr1, char[] carr2,
                                           int startIdx) {
        int i = 0, j = startIdx;
        while (i < carr1.length) {
            while (i < carr1.length && Character.isWhitespace(carr1[i])) {
                i++;
            }
            while (j < carr2.length && Character.isWhitespace(carr2[j])) {
                j++;
            }
            if (j >= carr2.length) {
                break;
            }
            if (i >= carr1.length) {
                break;
            }

            if (carr1[i] != carr2[j]) {
                return -1;
            }
            i++;
            j++;
            if (j >= carr2.length) {
                break;
            }
        }
        if (i == carr1.length) {
            return j;
        }
        return -1;
    }

    /**
     * Checks if the given token is all in upper case.
     *
     * @param tok a string token
     * @return true if the given token is all in upper case
     */
    public static boolean isAllCapital(String tok) {
        if (Character.isUpperCase(tok.charAt(0))) {
            int len = tok.length();
            for (int i = 1; i < len; i++) {
                if (!Character.isUpperCase(tok.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean isAllLetters(String tok) {
        int len = tok.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isLetter(tok.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllSpecial(String token) {
        int len = token.length();
        for (int i = 0; i < len; i++) {
            if (Character.isLetterOrDigit(token.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasNumbers(String tok, int minNumNumbers) {
        int len = tok.length();
        int count = 0;
        for (int i = 0; i < len; i++) {

            if (Character.isDigit(tok.charAt(i))) {
                count++;
            }
            if (count >= minNumNumbers) {
                return true;
            }
        }
        return false;
    }

    public static int align2LeftBoundaryOfMatchingToken(int startIdx,
                                                        String sentence) {
        if (startIdx == 0) {
            return startIdx;
        }
        char[] arr = sentence.toCharArray();
        if (Character.isWhitespace(arr[startIdx - 1])) {
            return startIdx;
        } else {
            int i = startIdx - 1;
            while (i >= 0 && !Character.isWhitespace(arr[i])) {
                if ((arr[i] == '(' || arr[i] == ';' || arr[i] == ':' || arr[i] == ',')) {
                    break;
                }
                i--;
            }
            return (i > 0) ? i + 1 : 0;
        }
    }

    public static int align2RightBoundaryOfMatchingToken(int endIdx,
                                                         String sentence) {
        if (endIdx >= (sentence.length() - 1)) {
            int idx = sentence.length() - 1;
            // if (sentence.charAt(idx) == '.')
            // idx--;
            return idx;
        }
        char[] arr = sentence.toCharArray();
        if (Character.isWhitespace(arr[endIdx])) {
            return endIdx;
        } else {
            int i = endIdx;
            int sentLen = sentence.length();
            while (i < sentLen && !Character.isWhitespace(arr[i])) {
                if (arr[i] == '.' && ((i + 1) >= sentLen || arr[i + 1] == '\n')) {
                    break;
                }
                if ((arr[i] == ',' || arr[i] == ';' || arr[i] == ':' || arr[i] == ')')
                        && (i + 1) < sentLen
                        && (Character.isWhitespace(arr[i + 1]) || arr[i + 1] == '.')) {
                    break;
                }
                i++;
            }
            return i;
        }
    }

    /**
     * Checks if two strings are equal disregarding any whitespace differences.
     *
     * @param s1
     * @param s2
     * @return true if two strings are equal disregarding any whitespace
     * differences, otherwise false.
     */
    public static boolean matchesNoWhiteSpace(String s1, String s2) {
        if (s1 == null || s2 == null)
            throw new IllegalArgumentException(
                    "Arguments 's1' and 's2' both must be not null!");
        char[] carr1 = s1.toCharArray();
        char[] carr2 = s2.toCharArray();
        int i = 0, j = 0;
        while (i < carr1.length) {
            while (i < carr1.length && Character.isWhitespace(carr1[i])) {
                i++;
            }
            while (j < carr2.length && Character.isWhitespace(carr2[j])) {
                j++;
            }
            if (i >= carr1.length || j >= carr2.length) {
                break;
            }
            if (carr1[i] != carr2[j]) {
                return false;
            }
            i++;
            j++;
        }
        if (j < carr2.length) {
            while (j < carr2.length && Character.isWhitespace(carr2[j])) {
                j++;
            }
        }

        return (i == carr1.length && j == carr2.length);
    }

    public static int countChar(String s, char c) {
        int len = s.length(), count = 0;
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static boolean isVowel(char c) {
        return (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u');
    }

    public static String stripWS(String text) {
        int len = text.length();
        StringBuilder sb = new StringBuilder(len);
        for(int i = 0; i < len; i++) {
            if ( !Character.isWhitespace(text.charAt(i))) {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }
}
