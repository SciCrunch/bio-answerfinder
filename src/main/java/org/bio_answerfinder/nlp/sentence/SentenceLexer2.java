package org.bio_answerfinder.nlp.sentence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SentenceLexer2 {
    char[] carr;
    int curTType = -1;
    int curCursorIdx = 0;
    boolean inApostrophe = false;
    boolean inURL = false;
    TokenInfo curTI;
    StringBuilder sb = new StringBuilder();
    List<TokenInfo> tokQueue = new ArrayList<TokenInfo>();
    /**
     * list of recognized common acronyms
     */
    static List<char[]> acrList =  LexerUtils.getAbbreviationList();



    public SentenceLexer2(String sentence) {
        carr = sentence.toCharArray();
    }

    public TokenInfo getNextTI() throws IOException {
        if (curCursorIdx < carr.length) {
            skipWS();
            int i = curCursorIdx;
            sb.setLength(0);
            int len = 0;
            while (i < carr.length && !Character.isWhitespace(carr[i])) {
                if (delimitsToken(carr, i)) {
                    if (len == 0) {
                        sb.append(carr[i]);
                        i++;
                    }
                    if (inURL) {
                        inURL = false;
                    }
                    break;

                } else if (!inApostrophe && carr[i] == '\'') {
                    if (canFollowApos(carr, i)) {
                        // apostrophe
                        if (carr[i + 1] == 's'
                                && Character.isWhitespace(carr[i + 2])
                                && (i == 0 || !Character
                                .isWhitespace(carr[i - 1]))) {
                            // exception: possessives that are already delimited
                            inApostrophe = true;
                            break;
                        } else if (Character.isWhitespace(carr[i + 1])) {
                            inApostrophe = true;
                            // FIXME
                            // break;
                        }
                    } else if (!Character.isDigit(carr[i - 1])) {
                        // to handle certain chemical compounds
                        break;
                    }
                } else if (inApostrophe) {
                    inApostrophe = false;
                }
                sb.append(carr[i]);
                i++;
                len++;
            }
            if (inURL && i < carr.length && Character.isWhitespace(carr[i])) {
                inURL = false;
            }
            TokenInfo ti = new TokenInfo(sb.toString(), curCursorIdx, i);
            curCursorIdx = i;
            return ti;
        }
        return null;
    }

    protected boolean canFollowApos(char[] carr, int i) {
        if ((i + 1) >= carr.length || (i + 2) >= carr.length) {
            return false;
        }
        return (carr[i + 1] == 's' && Character.isWhitespace(carr[i + 2]))
                || Character.isWhitespace(carr[i + 1])
                || carr[i + 1] == '.'
                || (carr[i + 1] == '-' && (i - 1 < 0 || !Character
                .isDigit(carr[i - 1])));
    }

    protected void skipWS() {
        while (curCursorIdx < carr.length
                && Character.isWhitespace(carr[curCursorIdx])) {
            curCursorIdx++;
        }
    }

    protected boolean isSpecialChar(char c) {
        return c == ',' || c == '.' || c == '?' || c == '!' || c == ';'
                || c == ':' || c == '(' || c == ')' || c == '[' || c == ']'
                || c == '"' || c == '%';
    }

    protected boolean isOpenParen(char c) {
        return c == '(' || c == '[';
    }

    protected boolean isCloseParen(char c) {
        return c == ')' || c == ']';
    }

    protected boolean looksLikeGeneName(char[] ca, int i, boolean forward) {
        int numUpper = 0;
        int numDigits = 0;
        if (forward) {
            while (i + 1 < ca.length && !Character.isWhitespace(ca[i])) {
                if (Character.isUpperCase(ca[i])) {
                    numUpper++;
                } else if (Character.isDigit(ca[i])) {
                    numDigits++;
                }
                i++;
            }
            if (numDigits > 0 || numUpper > 1) {
                return true;
            }
        } else {
            while (i >= 0 && !Character.isWhitespace(ca[i])) {
                if (Character.isUpperCase(ca[i])) {
                    numUpper++;
                } else if (Character.isDigit(ca[i])) {
                    numDigits++;
                }
                i--;
            }
            if (numDigits > 0 || numUpper > 1) {
                return true;
            }
        }
        return false;
    }

    protected boolean delimitsToken(char[] ca, int i) {
        if (isSpecialChar(ca[i])) {
            if (ca[i] == '.') {
                if (inURL) {
                    return false;
                }
                if (isURL(ca, i)) {
                    inURL = true;
                    return false;
                }
                if (i + 1 < ca.length) {
                    boolean ok = Character.isWhitespace(ca[i + 1]);
                    if (!ok) {
                        boolean isDigit = Character.isDigit(ca[i + 1]);
                        if (isDigit)
                            return false;
                        boolean acronym = isAcronym(ca, i);
                        if (!acronym) {
                            return !isOrganismName(ca, i)
                                    && !looksLikeGeneName(ca, i + 1, true);
                        }
                        return !acronym;
                    }
                    return !isAcronym(ca, i);
                } else {
                    return true;
                }
            } else if (isCloseParen(ca[i])) {
                if (i + 1 < ca.length) {
                    boolean wsNext = Character.isWhitespace(ca[i + 1]);
                    if (wsNext) {
                        return true;
                    } else {
                        if (ca[i+1] == '?') {
                            return true;
                        } else if (ca[i+1] == '.' && (i+2) == ca.length) {
                            return true;
                        }
                    }
                    return false;
                } else {
                    return true;
                }
            } else if (isOpenParen(ca[i])) {
                if (i - 1 >= 0) {
                    boolean ws = Character.isWhitespace(ca[i - 1]);
                    if (!ws) {
                        if (Character.isLetter(ca[i-1])) {
                            return false;
                        }
                        return isOpenParen(ca[i - 1])
                                || ca[i - 1] == '-'
                                || !Character.isLetter(ca[i - 1])
                                || (i + 1 < ca.length && Character
                                .isDigit(ca[i + 1]));
                    }
                    return true;
                } else {
                    return true;
                }
            } else if (ca[i] == ',') {
                if (i + 1 < ca.length) {
                    boolean isDigit = Character.isDigit(ca[i + 1]);
                    return !isDigit;
                } else {
                    return true;
                }
            } else if (ca[i] == ':') {
                if (i - 1 >= 0 && Character.isDigit(ca[i - 1])
                        && i + 1 < ca.length && Character.isDigit(ca[i + 1])) {
                    return false;
                }
                if (isURL(ca, i)) {
                    inURL = true;
                    return false;
                }
                return true;
            } else if (ca[i] == ';')  {
                // organism names contains ; so do not split at ;
                return false;
            } else {
                return true;
            }
        } else if (ca[i] == '\'') {
            if (ca.length <= (i + 1)) {
                return true;
            } else if (!canFollowApos(ca, i) && ca[i + 1] != '-') {
                return true;
            }
        }
        return false;
    }

    protected boolean isOrganismName(char[] ca, int i) {
        if (i - 1 >= 0 && i + 1 < ca.length) {
            if (Character.isUpperCase(ca[i - 1])
                    && Character.isLowerCase(ca[i + 1])) {
                return true;
            }
        }
        return false;
    }

    protected boolean isURL(char[] ca, int i) {
        if ((i + 1) < ca.length && ca[i] == ':' && ca[i + 1] == '/') {
            return true;
        } else if ((i - 2) >= 0 && ca[i] == '.' && ca[i - 1] == 'w' && ca[i - 2] == 'w') {
            return true;
        }
        return false;
    }

    protected boolean isAcronym(char[] ca, int i) {
        for (char[] ac : acrList) {
            if ((i - ac.length) >= 0) {
                boolean ok = true;
                for (int j = i - ac.length + 1; j < i; j++) {
                    if (ca[j] != ac[j - i + ac.length - 1]) {
                        ok = false;
                        break;
                    }
                }
                if (ok)
                    return ok;
            }
        }
        return false;
    }


    public static void main(String[] args) throws Exception {
        String sentence = "B6.Cg-Tg(PDGFB-APP)5Lms/J mice are used.";
        sentence = "ACSF vs. those incubated";
        sentence = "P{w+mCovoD1-18=ovoD1-18} strain";
        sentence = "129S1/SvlmJ=129S and C57BL/6J=B6 mice";
        sentence = "The genotypes used for the experiments were wild type (Oregon R) , PofD119/PofD119 , Su(var)3-906/Su(var)3-9evo , Setdb110.1a/Setdb110.1a and G9aRG5/G9aRG5 .";
        sentence = "Homozygous mutant third instar larvae were collected from the fly stocks Df(1)w67c23y w;PofD119, w;Setdb110.1a/CyO GFP  and w;G9aRG5.";
        sentence = "List phosphorylation consensus motifs for Casein Kinase 1(CK1)?";
        sentence = "What is the major adverse effect of adriamycin(doxorubicin)?";
        SentenceLexer2 lexer = new SentenceLexer2(sentence);
        TokenInfo ti;
        while((ti = lexer.getNextTI()) != null) {
            System.out.println(ti);
        }
    }
}
