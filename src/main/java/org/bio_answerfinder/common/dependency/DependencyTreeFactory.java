package org.bio_answerfinder.common.dependency;

import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.nlp.morph.Lemmanizer;
import org.bio_answerfinder.nlp.morph.MorphException;
import org.bio_answerfinder.nlp.morph.TermMorphRecord;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.SRLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 2/25/19.
 */
public class DependencyTreeFactory {
    static Lemmanizer lemmanizer;

    static {
        try {
            lemmanizer = SRLUtils.prepLemmanizer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasVerbLemma(String verb) {
        try {
            TermMorphRecord infinitive = lemmanizer.getInfinitive(verb);
            return infinitive != null;
        } catch (MorphException e) {
            return false;
        }
    }

    public static String getLemma(String token, String posTag) {
        if (SRLUtils.isVerb(posTag)) {
            try {
                TermMorphRecord infinitive = lemmanizer.getInfinitive(token);
                if (infinitive != null) {
                    return infinitive.getBaseWord();
                }
            } catch (MorphException e) {
                e.printStackTrace();
            }
        } else if (SRLUtils.isCommonNoun(posTag)) {
            try {
                TermMorphRecord n = lemmanizer.getLemma(token, "N");
                if (n != null) {
                    return n.getBaseWord();
                }
            } catch (MorphException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public static String getLemmaOrToken(String token, String posTag) {
        String lemma = getLemma(token, posTag);
        return lemma != null ? lemma : token;
    }

    public static DependencyNode createFromList(List<String> dependencyList, String pt) {
        Map<String, List<String>> idDepMap = new HashMap<String, List<String>>();
        String rootStr = null;
        for (String dep : dependencyList) {
            if (dep.startsWith("root(")) {
                rootStr = dep;
            } else {
                Pair<String, String> idPair = extractIDPair(dep);
                List<String> depList;
                if (!idDepMap.containsKey(idPair.getFirst())) {
                    depList = new ArrayList<String>(1);
                    idDepMap.put(idPair.getFirst(), depList);
                } else {
                    depList = idDepMap.get(idPair.getFirst());
                }
                depList.add(dep);
            }
        }
        Pair<List<String>, List<String>> posTagsPair = extractPosTags(pt);
        List<Pair<String, String>> depTokens = new ArrayList<Pair<String, String>>(dependencyList.size());
        for (String dep : dependencyList) {
            Pair<String, String> tokenPair = extractTokenPair(dep);
            Pair<String, String> idPair = extractIDPair(dep);
            depTokens.add(new Pair<String, String>(idPair.getSecond(), tokenPair.getSecond()));
        }
        Map<String, String> id2PosTagMap = prepId2PosTagMap(posTagsPair, depTokens);


        Pair<String, String> idPair = extractIDPair(rootStr);
        Pair<String, String> tokenPair = extractTokenPair(rootStr);
        String depLabel = extractDepLabel(rootStr);
        NLPToken nlpToken = new NLPToken(tokenPair.getSecond());
        nlpToken.setPosTag(id2PosTagMap.get(idPair.getSecond()));
        DependencyNode root = new DependencyNode(idPair.getSecond(), depLabel, nlpToken, null);
        DependencyNode parent = root;
        Map<String, DependencyNode> nodeMap = new HashMap<String, DependencyNode>();
        nodeMap.put(idPair.getSecond(), root);
        build(idPair, parent, idDepMap, id2PosTagMap);
        return root;
    }

    static Map<String, String> prepId2PosTagMap(Pair<List<String>, List<String>> posTagsPair, List<Pair<String, String>> depTokens) {
        int i = 0;
        int prevIdx = 0;
        List<String> tokens = posTagsPair.getFirst();
        Map<String, String> posMap = new HashMap<String, String>();
        for (Pair<String, String> dtPair : depTokens) {
            String dt = normalizeToken(dtPair.getSecond());
            prevIdx = i;
            /*
            while (i < tokens.size() && !tokens.get(i).equals(dt)) {
                i++;
            }
            */
            i = indexOf(dt, tokens, prevIdx);
            if (i >= tokens.size()) {
                System.out.println();
                System.out.println(">> dt:" + dt + " prevIdx:" + prevIdx + " prevToken:" + tokens.get(prevIdx));
                System.out.println(">> tokens:" + tokens);
            }
            Assertion.assertTrue(tokens.get(i).equals(dt));
            posMap.put(dtPair.getFirst(), posTagsPair.getSecond().get(i));
        }
        return posMap;
    }

    static int indexOf(String dt, List<String> tokens, int prevIdx) {
        int i = prevIdx + 1;
        if (i < tokens.size()) {
            if (tokens.get(i).equals(dt)) {
                return i;
            }
        }
        for (i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).equals(dt)) {
                return i;
            }
        }

        return -1;
    }


    static void build(Pair<String, String> idPair, DependencyNode parent, Map<String, List<String>> idDepMap,
                      Map<String, String> id2PosTagMap) {
        if (idDepMap.isEmpty()) {
            return;
        }
        String firstID = idPair.getSecond();
        List<String> depList = idDepMap.get(firstID);
        if (depList != null) {
            for (String depStr : depList) {
                idPair = extractIDPair(depStr);
                Pair<String, String> tokenPair = extractTokenPair(depStr);
                String depLabel = extractDepLabel(depStr);
                NLPToken nlpToken = new NLPToken(tokenPair.getSecond());
                nlpToken.setPosTag(id2PosTagMap.get(idPair.getSecond()));
                if (SRLUtils.isVerb(nlpToken.getPosTag())) {
                    try {
                        TermMorphRecord infinitive = lemmanizer.getInfinitive(nlpToken.getToken());
                        if (infinitive != null) {
                            nlpToken.setLemma(infinitive.getBaseWord());
                        }
                    } catch (MorphException e) {
                        e.printStackTrace();
                    }
                } else if (SRLUtils.isCommonNoun(nlpToken.getPosTag())) {
                    try {
                        TermMorphRecord n = lemmanizer.getLemma(nlpToken.getToken(), "N");
                        if (n != null) {
                            nlpToken.setLemma(n.getBaseWord());
                        }
                    } catch (MorphException e) {
                        e.printStackTrace();
                    }
                }

                DependencyNode child = new DependencyNode(idPair.getSecond(), depLabel, nlpToken, parent);
                if (!DependencyNode.hasCycle(child, parent)) {
                    parent.addChild(child);
                    build(idPair, child, idDepMap, id2PosTagMap);
                }
            }
            idDepMap.remove(firstID);
        }
    }

    public static Pair<String, String> extractIDPair(String depStr) {
        int idx = depStr.indexOf('(');
        String s = depStr.substring(idx + 1, depStr.length() - 1);
        idx = s.indexOf(", ");
        // String[] tokens = s.split("\\s*,\\s*");
        String[] tokens = new String[2];
        tokens[0] = s.substring(0, idx);
        tokens[1] = s.substring(idx + 2);
        idx = tokens[0].lastIndexOf('-');
        String id1 = tokens[0].substring(idx + 1);
        idx = tokens[1].lastIndexOf('-');
        String id2 = tokens[1].substring(idx + 1);
        return new Pair<String, String>(id1, id2);
    }

    public static Pair<String, String> extractTokenPair(String depStr) {
        int idx = depStr.indexOf('(');
        String s = depStr.substring(idx + 1, depStr.length() - 1);
        // String[] tokens = s.split("\\s*,\\s*");
        idx = s.indexOf(", ");
        String[] tokens = new String[2];
        tokens[0] = s.substring(0, idx);
        tokens[1] = s.substring(idx + 2);
        idx = tokens[0].lastIndexOf('-');
        String token1 = tokens[0].substring(0, idx);
        idx = tokens[1].lastIndexOf('-');
        if (idx == -1) {
            System.out.println("***");
        }
        String token2 = tokens[1].substring(0, idx);
        return new Pair<String, String>(token1, token2);
    }

    public static Pair<List<String>, List<String>> extractPosTags(String ptSExpression) {
        List<String> tokens = new ArrayList<String>();
        List<String> posTags = new ArrayList<String>();
        Pair<List<String>, List<String>> pair = new Pair<List<String>, List<String>>(tokens, posTags);
        Pattern p = Pattern.compile("\\(([^\\s]+) ([^\\s\\)]+)\\)");
        Matcher matcher = p.matcher(ptSExpression);
        while (matcher.find()) {
            String posTag = matcher.group(1);
            String token = matcher.group(2);
            tokens.add(normalizeToken(token));
            posTags.add(posTag);
        }
        return pair;
    }

    public static String extractDepLabel(String depStr) {
        int idx = depStr.indexOf('(');
        return depStr.substring(0, idx);
    }

    public static String normalizeToken(String token) {
        if (token.equals("-LRB-")) {
            return "(";
        } else if (token.equals("-RRB-")) {
            return ")";
        } else if (token.equals("-LSB-")) {
            return "[";
        } else if (token.equals("-RSB-")) {
            return "]";
        } else if (token.equals("-RCB-")) {
            return "}";

        } else if (token.equals("-lCB-")) {
            return "{";
        }
        return token;
    }
}
