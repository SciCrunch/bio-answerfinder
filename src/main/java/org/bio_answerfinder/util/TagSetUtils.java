package org.bio_answerfinder.util;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.bio_answerfinder.common.POSTagSet;
import org.bio_answerfinder.common.PTB2SyntacticLabelSet;

/**
 * Utility class for string representation to constant representation conversion
 * of Penn Treebank II phrase and word part of speech tags.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class TagSetUtils {
    private static TObjectIntHashMap posTagSetMap = new TObjectIntHashMap();
    private static TObjectIntHashMap ptb2SynTagSetMap = new TObjectIntHashMap();

    static {
        prepPOSTagSetMap();
        prepPTB2SynTagSetMap();
    }

    private TagSetUtils() {
    }

    static void prepPOSTagSetMap() {
        posTagSetMap.put("\"", POSTagSet.DBL_QUOTE);
        posTagSetMap.put("#", POSTagSet.HASH);
        posTagSetMap.put("$", POSTagSet.DOLLAR);
        posTagSetMap.put("'", POSTagSet.RC_SINGLE_QUOTE);
        posTagSetMap.put("(", POSTagSet.LEFT_BRACKET);
        posTagSetMap.put(")", POSTagSet.RIGHT_BRACKET);
        posTagSetMap.put(",", POSTagSet.COMMA);
        posTagSetMap.put(".", POSTagSet.PERIOD);
        posTagSetMap.put(":", POSTagSet.COLON);
        posTagSetMap.put("CC", POSTagSet.CC);
        posTagSetMap.put("CD", POSTagSet.CD);
        posTagSetMap.put("DT", POSTagSet.DT);
        posTagSetMap.put("EX", POSTagSet.EX);
        posTagSetMap.put("FW", POSTagSet.FW);
        posTagSetMap.put("IN", POSTagSet.IN);
        posTagSetMap.put("JJ", POSTagSet.JJ);
        posTagSetMap.put("JJR", POSTagSet.JJR);
        posTagSetMap.put("JJS", POSTagSet.JJS);
        posTagSetMap.put("LS", POSTagSet.LS);
        posTagSetMap.put("MD", POSTagSet.MD);
        posTagSetMap.put("NN", POSTagSet.NN);
        posTagSetMap.put("NNP", POSTagSet.NNP);
        posTagSetMap.put("NNPS", POSTagSet.NNPS);
        posTagSetMap.put("NNS", POSTagSet.NNS);
        posTagSetMap.put("PDT", POSTagSet.PDT);
        posTagSetMap.put("POS", POSTagSet.POS);
        posTagSetMap.put("PP", POSTagSet.PP);
        posTagSetMap.put("PRP", POSTagSet.PRP);
        // added 06/23/2011
        posTagSetMap.put("PRP$", POSTagSet.PP);
        posTagSetMap.put("RB", POSTagSet.RB);
        posTagSetMap.put("RBR", POSTagSet.RBR);
        posTagSetMap.put("RBS", POSTagSet.RBS);
        posTagSetMap.put("RP", POSTagSet.RP);
        posTagSetMap.put("SYM", POSTagSet.SYM);
        posTagSetMap.put("TO", POSTagSet.TO);
        posTagSetMap.put("UH", POSTagSet.UH);
        posTagSetMap.put("VB", POSTagSet.VB);
        posTagSetMap.put("VBD", POSTagSet.VBD);
        posTagSetMap.put("VBG", POSTagSet.VBG);
        posTagSetMap.put("VBN", POSTagSet.VBN);
        posTagSetMap.put("VBP", POSTagSet.VBP);
        posTagSetMap.put("VBZ", POSTagSet.VBZ);
        posTagSetMap.put("WDT", POSTagSet.WDT);
        posTagSetMap.put("WP", POSTagSet.WP);
        posTagSetMap.put("WRB", POSTagSet.WRB);
        posTagSetMap.put("`", POSTagSet.LO_SINGLE_QUOTE);
        posTagSetMap.put("AUX", POSTagSet.AUX);
    }

    static void prepPTB2SynTagSetMap() {
        ptb2SynTagSetMap.put("S", PTB2SyntacticLabelSet.S);
        ptb2SynTagSetMap.put("SBAR", PTB2SyntacticLabelSet.SBAR);
        ptb2SynTagSetMap.put("SBARQ", PTB2SyntacticLabelSet.SBARQ);
        ptb2SynTagSetMap.put("SINV", PTB2SyntacticLabelSet.SINV);
        ptb2SynTagSetMap.put("SQ", PTB2SyntacticLabelSet.SQ);

        ptb2SynTagSetMap.put("ADJP", PTB2SyntacticLabelSet.ADJP);
        ptb2SynTagSetMap.put("ADVP", PTB2SyntacticLabelSet.ADVP);
        ptb2SynTagSetMap.put("CONJP", PTB2SyntacticLabelSet.CONJP);
        ptb2SynTagSetMap.put("FRAG", PTB2SyntacticLabelSet.FRAG);
        ptb2SynTagSetMap.put("INTJ", PTB2SyntacticLabelSet.INTJ);
        ptb2SynTagSetMap.put("LST", PTB2SyntacticLabelSet.LST);
        ptb2SynTagSetMap.put("NAC", PTB2SyntacticLabelSet.NAC);
        ptb2SynTagSetMap.put("NP", PTB2SyntacticLabelSet.NP);
        ptb2SynTagSetMap.put("NX", PTB2SyntacticLabelSet.NX);
        ptb2SynTagSetMap.put("PP", PTB2SyntacticLabelSet.PP);
        ptb2SynTagSetMap.put("PRN", PTB2SyntacticLabelSet.PRN);
        ptb2SynTagSetMap.put("PRT", PTB2SyntacticLabelSet.PRT);
        ptb2SynTagSetMap.put("QP", PTB2SyntacticLabelSet.QP);
        ptb2SynTagSetMap.put("RRC", PTB2SyntacticLabelSet.RRC);
        ptb2SynTagSetMap.put("UCP", PTB2SyntacticLabelSet.UCP);
        ptb2SynTagSetMap.put("VP", PTB2SyntacticLabelSet.VP);
        ptb2SynTagSetMap.put("WHADJP", PTB2SyntacticLabelSet.WHADJP);
        ptb2SynTagSetMap.put("WHADVP", PTB2SyntacticLabelSet.WHADVP);
        ptb2SynTagSetMap.put("WHNP", PTB2SyntacticLabelSet.WHNP);
        ptb2SynTagSetMap.put("WHPP", PTB2SyntacticLabelSet.WHPP);
    }

    public static int getPOSTagCode(String tag) {
        return posTagSetMap.get(tag);
    }

    public static int getPTB2SynTagCode(String tag) {
        return ptb2SynTagSetMap.get(tag);
    }

    public static int getTagCode(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode <= 0) {
            tagCode = ptb2SynTagSetMap.get(tag);
        }
        return tagCode;
    }

    public static boolean isVerb(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.VB || tagCode == POSTagSet.VBD
                    || tagCode == POSTagSet.VBG || tagCode == POSTagSet.VBN
                    || tagCode == POSTagSet.VBP || tagCode == POSTagSet.VBZ);
        }
        return false;
    }

    // 12/30/2013
    public static boolean isParticiple(String tag) {
        int tagCode = ptb2SynTagSetMap.get(tag);
        if (tagCode > 0) {
            return tagCode == PTB2SyntacticLabelSet.PRT;
        }
        return false;
    }

    public static boolean isDeterminer(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.DT);
        }
        return false;
    }

    public static boolean isPronoun(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.PP || tagCode == POSTagSet.PRP);
        }
        return false;
    }

    public static boolean isPredeterminer(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.PDT);
        }
        return false;
    }

    public static boolean isComparativeOrSuperlativeAdjective(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.JJR || tagCode == POSTagSet.JJS);
        }
        return false;
    }

    public static boolean isCardinalNumber(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.CD);
        }
        return false;
    }

    public static boolean isAdverb(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.RB || tagCode == POSTagSet.RBR || tagCode == POSTagSet.RBS);
        }
        return false;
    }

    public static boolean isConjunction(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.CC);
        }
        return false;
    }

    public static boolean isAuxOrModal(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.AUX || tagCode == POSTagSet.MD);
        }
        return false;
    }

    public static boolean isProposition(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.IN);
        }
        return false;
    }

    public static boolean isNoun(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.NN || tagCode == POSTagSet.NNP
                    || tagCode == POSTagSet.NNPS || tagCode == POSTagSet.NNS);
        }
        return false;
    }

    public static boolean isAdjective(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.JJ || tagCode == POSTagSet.JJR || tagCode == POSTagSet.JJS);
        }
        return false;
    }

    public static boolean isLeftParen(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return tagCode == POSTagSet.LEFT_BRACKET;
        }
        return tag.equalsIgnoreCase("-lrb-") || tag.equalsIgnoreCase("lrb");
    }

    public static boolean isRightParen(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return tagCode == POSTagSet.RIGHT_BRACKET;
        }
        return tag.equalsIgnoreCase("-rrb-") || tag.equalsIgnoreCase("rrb");
    }

    public static boolean isPunctuation(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return tagCode == POSTagSet.COMMA || tagCode == POSTagSet.COLON;
        }
        return tag.equals(",") || tag.equals(";") || tag.equals(":");
    }

    public static boolean isNounOrNounPhrase(String tag) {
        int tagCode = posTagSetMap.get(tag);
        if (tagCode > 0) {
            return (tagCode == POSTagSet.NN || tagCode == POSTagSet.NNP
                    || tagCode == POSTagSet.NNPS || tagCode == POSTagSet.NNS);
        } else {
            tagCode = ptb2SynTagSetMap.get(tag);
            return (tagCode == PTB2SyntacticLabelSet.NP || tagCode == PTB2SyntacticLabelSet.NAC);
        }
    }

    public static boolean isNounPhrase(String tag) {
        int tagCode = ptb2SynTagSetMap.get(tag);
        return (tagCode == PTB2SyntacticLabelSet.NP || tagCode == PTB2SyntacticLabelSet.NAC);

    }

    public static boolean isVerbPhrase(String tag) {
        int tagCode = ptb2SynTagSetMap.get(tag);
        return tagCode == PTB2SyntacticLabelSet.VP;
    }

    public static boolean isClause(String tag) {
        int tagCode = ptb2SynTagSetMap.get(tag);
        return tagCode == PTB2SyntacticLabelSet.S
                || tagCode == PTB2SyntacticLabelSet.SBAR
                || tagCode == PTB2SyntacticLabelSet.SINV
                || tagCode == PTB2SyntacticLabelSet.SQ;
    }

    public static boolean isAdjectivePhrase(String tag) {
        int tagCode = ptb2SynTagSetMap.get(tag);
        return tagCode == PTB2SyntacticLabelSet.ADJP
                || tagCode == PTB2SyntacticLabelSet.WHADJP
                || tagCode == PTB2SyntacticLabelSet.WHADVP
                || tagCode == PTB2SyntacticLabelSet.WHNP
                || tagCode == PTB2SyntacticLabelSet.WHPP;
    }
}
