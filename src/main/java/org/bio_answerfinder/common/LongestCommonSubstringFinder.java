package org.bio_answerfinder.common;


import org.bio_answerfinder.common.types.Range;

import java.util.HashSet;
import java.util.Set;

/**
 * based on https://en.wikipedia.org/wiki/Longest_common_substring_problem
 * Punctuation character matching is relaxed.
 * <p>
 * Created by bozyurt on 11/8/17.
 */
public class LongestCommonSubstringFinder {
    int[][] L;

    public LongestCommonSubstringFinder(int initialS1Len, int initialS2Len) {
        L = new int[initialS1Len][initialS2Len];
    }

    void ensureSize(int r, int n) {
        if (L.length < r || L[0].length < n) {
            L = new int[r][n];
        }
    }

    void initialize(int r, int n, int value) {
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < n; j++) {
                L[i][j] = value;
            }
        }
    }


    public LCSSet getLongestCommonSubStrings(String[] tokens1, String[] tokens2) {
        int r = tokens1.length;
        int n = tokens2.length;
        Set<Range> lcsSet = new HashSet<>();
        ensureSize(r, n);
        initialize(r, n, 0);
        int z = 0;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < n; j++) {
                if (areSame(tokens1[i], tokens2[j])) {
                    if (i == 0 || j == 0) {
                        L[i][j] = 1;
                    } else {
                        L[i][j] = L[i - 1][j - 1] + 1;
                    }
                    if (L[i][j] > z) {
                        z = L[i][j];
                        lcsSet.add(new Range(i - z + 1, i));
                    } else {
                        if (L[i][j] == z) {
                            lcsSet.add(new Range(i - z + 1, i));
                        }
                    }
                } else {
                    L[i][j] = 0;
                }
            }
        }
        return new LCSSet(z, lcsSet);
    }

    boolean areSame(String token1, String token2) {
        if (token1.equals(token2)) {
            return true;
        } else {
            if (isPunctuation(token1) && isPunctuation(token2)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPunctuation(String token) {
        if (token.length() > 3) {
            return false;
        }
        char[] carr = token.toCharArray();
        for (char c : carr) {
            if (c != '.' && c != ',' && c != ';' && c != ':' && c != '!' && c != '?') {
                return false;
            }
        }
        return true;
    }


    public static class LCSSet {
        final int longestMatchSize;
        final Set<Range> lcsSet;

        public LCSSet(int longestMatchSize, Set<Range> lcsSet) {
            this.longestMatchSize = longestMatchSize;
            this.lcsSet = lcsSet;
        }

        public int getLongestMatchSize() {
            return longestMatchSize;
        }

        public Set<Range> getLcsSet() {
            return lcsSet;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LCSSet{");
            sb.append("longestMatchSize=").append(longestMatchSize);
            sb.append(", lcsSet=").append(lcsSet);
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        LongestCommonSubstringFinder finder = new LongestCommonSubstringFinder(2,2);
        String[] tokens1 = new String[] {"A","B","A","B"};
        String[] tokens2 = new String[] {"B","A","B","A"};

        LCSSet lcsSet = finder.getLongestCommonSubStrings(tokens1, tokens2);
        System.out.println(lcsSet);


    }
}
