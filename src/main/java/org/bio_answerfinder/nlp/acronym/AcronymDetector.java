package org.bio_answerfinder.nlp.acronym;


import org.bio_answerfinder.common.*;
import org.bio_answerfinder.common.Acronym.AcrExpansion;
import org.bio_answerfinder.common.types.Node;
import org.bio_answerfinder.common.types.ParseTreeManager;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.SRLUtils;
import org.bio_answerfinder.util.TagSetUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 6/12/17.
 */
public class AcronymDetector {
    protected Pattern pattern = Pattern
            .compile("\\(\\s*([A-Z][a-zA-Z\\-]*[A-Z1-9]?(?:/[1-9])?s?)\\s*\\)");
    protected Map<String, Acronym> acrSet = new LinkedHashMap<String, Acronym>();

    public static String prepKey(Acronym acr) {
        StringBuilder sb = new StringBuilder(100);
        sb.append(acr.getAcronym()).append(':');
        sb.append(acr.getExpansion(0).getExpansion());
        return sb.toString();
    }

    public boolean hasAcronymDefinition(String sentence) {
        Matcher m = pattern.matcher(sentence);
        return m.find();
    }

    /**
     * checks if the parse tree is a valid constituent parse tree (not a one level token - POS tag pairs)
     *
     * @param pt
     * @return
     */
    public static boolean isAValidParseTree(String pt) {
        try {
            Node rootNode = ParseTreeManager.asParseTree(pt);
            for (Node child : rootNode.getChildren()) {
                if (child.hasChildren()) {
                    return true;
                }
            }
        } catch (ParseTreeManagerException e) {
            e.printStackTrace();
        }
        return false;

    }

    public List<Acronym> findAcronyms(String sentence, String pt) throws ParseTreeManagerException {

        Matcher m = pattern.matcher(sentence);
        if (!m.find()) {
            return Collections.emptyList();
        }
        List<Acronym> acronymList = new ArrayList<>(1);
        List<AcronymPTLocWrapper> aptwList = new ArrayList<AcronymPTLocWrapper>(2);
        Node rootNode = ParseTreeManager.asParseTree(pt);
        List<Node> npList = new ArrayList<Node>(2);
        SRLUtils.findNounPhrases(rootNode, npList);
        for (Node npNode : npList) {
            String phraseStr = ParseTreeManager.parseTree2Sentence(npNode)
                    .trim();
            m = pattern.matcher(phraseStr);
            int count = 0;
            while (m.find()) {
                if (count == 0) {
                    System.out.println(phraseStr);
                }
                AcronymPTLocWrapper aptw = findAcronym(npNode, count, "ch");
                if (aptw != null) {
                    acronymList.add(aptw.getAcronym());
                }
                count++;
            }
        }
        return acronymList;
    }


    public List<Acronym> findAcronyms(String sentence, List<String> posTags) throws ParseTreeManagerException {
        Matcher m = pattern.matcher(sentence);
        if (!m.find()) {
            return Collections.emptyList();
        }

        List<Span> spanList = Utils.tokenize(sentence);

        int spanStartIdx = Utils.spanIndexOf(m.start(), spanList);
        int spanEndIdx = Utils.spanIndexOf(m.end() - 1, spanList);
        String acronym = m.group(1);
        ListIterator<Span> spanListIterator = spanList.listIterator(spanStartIdx);

        List<Acronym> acronymList = new ArrayList<>(1);
        int idx = spanStartIdx;
        List<Span> expList = new ArrayList<>(4);
        while (spanListIterator.hasPrevious()) {
            Span span = spanListIterator.previous();
            idx--;
            String posTag = posTags.get(idx);
            int tagCode = TagSetUtils.getPOSTagCode(posTag);
            if (TagSetUtils.isNoun(posTag)
                    || tagCode == POSTagSet.JJ
                    || tagCode == POSTagSet.VBN
                    || tagCode == POSTagSet.VBP
                    || tagCode == POSTagSet.VBG
                    || tagCode == POSTagSet.VB
                    || tagCode == POSTagSet.FW
                    || (tagCode == POSTagSet.IN && isEligibleProposition(span.getToken()))
                    || tagCode == POSTagSet.POS
                    || tagCode == POSTagSet.TO
                    || (tagCode == POSTagSet.CC && span.getToken()
                    .equals("and"))) {
                if (tagCode == POSTagSet.CC) {
                    if (idx > 0) {
                        // if an conjunction is encountered both conjoined
                        // words
                        // must start with upper case letters to the an
                        // acronym.
                        Span prev = spanList.get(idx - 1);
                        if (!Character.isUpperCase(prev.getToken()
                                .charAt(0))
                                || !Character.isUpperCase(spanList
                                .get(idx + 1).getToken().charAt(0))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                expList.add(span);
            } else {
                System.out.println("Stopping at " + span.getToken());
                break;
            }
        }
        Collections.reverse(expList);
        StringBuilder sb = new StringBuilder();
        for (Span span : expList) {
            sb.append(span.getToken()).append(' ');
        }
        String expansion = sb.toString().trim();
        System.out.println("Acronym:" + acronym);
        System.out.println("Expansion: " + expansion);
        String revExpansion = checkValidity(acronym, expansion, true);
        if (revExpansion != null) {
            // System.out.println("Rev Expansion:" + revExpansion);
            acronymList.add(prepAcronymRecord(expansion, acronym, revExpansion));
        } else {
            revExpansion = checkValidity(acronym, expansion, false);
            if (revExpansion != null) {
                acronymList.add(prepAcronymRecord(expansion, acronym, revExpansion));
            }
        }
        return acronymList;
    }

    protected AcronymPTLocWrapper findAcronym(Node npNode,
                                              int leftParenSkipCount, String source) {
        StringBuilder acronymSB = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        List<Node> acronymSpanningNodes = new ArrayList<Node>(1);
        List<Node> sList = new ArrayList<Node>(10);
        createSurfaceList(npNode, sList);
        Acronym acr = null;

        int i = 0;
        int startIdx = -1;
        int count = 0;
        for (Node leafNode : sList) {
            if (isLeftParen(leafNode)) {
                count++;
                if (count > leftParenSkipCount) {
                    startIdx = i;
                    break;
                }
            }
            i++;
        }
        if (startIdx >= 0) {
            // get acronym
            ListIterator<Node> iter = sList.listIterator(startIdx + 1);
            while (iter.hasNext()) {
                Node leafNode = iter.next();
                if (isRightParen(leafNode)) {
                    break;
                } else {
                    acronymSB.append(leafNode.getToken());
                    acronymSpanningNodes.add(leafNode);
                }
            }
            iter = sList.listIterator(startIdx);
            List<Node> expList = new ArrayList<Node>(4);
            int idx = startIdx;
            while (iter.hasPrevious()) {
                Node leafNode = iter.previous();
                idx--;
                int tagCode = TagSetUtils.getPOSTagCode(leafNode.getTag());
                if (TagSetUtils.isNoun(leafNode.getTag())
                        || tagCode == POSTagSet.JJ
                        || tagCode == POSTagSet.VBN
                        || tagCode == POSTagSet.VBP
                        || tagCode == POSTagSet.VBG
                        || tagCode == POSTagSet.VB
                        || tagCode == POSTagSet.FW
                        || tagCode == POSTagSet.CD
                        || (tagCode == POSTagSet.IN && isEligibleProposition(leafNode
                        .getToken()))
                        || tagCode == POSTagSet.POS
                        || tagCode == POSTagSet.TO
                        || (tagCode == POSTagSet.CC && leafNode.getToken()
                        .equals("and"))) {
                    if (tagCode == POSTagSet.CC) {
                        if (idx > 0) {
                            // if an conjunction is encountered both conjoined
                            // words
                            // must start with upper case letters to the an
                            // acronym.
                            Node prev = sList.get(idx - 1);
                            boolean bothNumber = Character.isDigit(prev.getToken().charAt(0)) &&
                                    Character.isDigit(sList
                                            .get(idx + 1).getToken().charAt(0));
                            boolean bothUpperCase = Character.isUpperCase(prev.getToken()
                                    .charAt(0)) && Character.isUpperCase(sList
                                    .get(idx + 1).getToken().charAt(0));
                            if (!bothNumber && !bothUpperCase) {
                                break;
                            }
                            /*
                            if (!Character.isUpperCase(prev.getToken()
                                    .charAt(0))
                                    || !Character.isUpperCase(sList
                                    .get(idx + 1).getToken().charAt(0))) {
                                break;
                            }
                            */
                        } else {
                            break;
                        }
                    }
                    expList.add(leafNode);
                } else {
                    System.out.println("Stopping at " + leafNode.getTag());
                    break;
                }
            }
            Collections.reverse(expList);
            for (Node n : expList) {
                sb.append(ParseTreeManager.charniak2SpecialChar(n.getToken())).append(' ');
            }

            String expansion = sb.toString().trim();
            String acronym = acronymSB.toString();
            System.out.println("Acronym:" + acronym);
            System.out.println("Expansion: " + expansion);
            String revExpansion = checkValidity(acronym, expansion, true);
            if (revExpansion == null) {
                // check also without dash expansion
                revExpansion = checkValidity(acronym, expansion, false);
                if (revExpansion != null) {
                    acr = getAcronym(source, expList, expansion, acronym, revExpansion);
                } else {
                    System.out.println("*** INVALID ***");
                    return null;
                }
            } else {
                acr = getAcronym(source, expList, expansion, acronym, revExpansion);
            }
        }
        return new AcronymPTLocWrapper(acronymSpanningNodes, acr);
    }

    private Acronym prepAcronymRecord(String expansion, String acronym, String revExpansion) {
        Acronym acr;
        if (!revExpansion.equals(expansion)) {
            System.out.println(">> Revised expansion:"
                    + revExpansion);
        }
        acr = new Acronym(acronym);
        AcrExpansion acrExp = new AcrExpansion(revExpansion, "", "ch", "1");
        acr.addExpansion(acrExp);
        String key = prepKey(acr);
        adjustFreq(acr, key);
        return acr;
    }

    private void adjustFreq(Acronym acr, String key) {
        if (!acrSet.containsKey(key)) {
            acrSet.put(key, acr);
            acr.getExpansion(0).setFreq(1);
        } else {
            Acronym a = acrSet.get(key);
            AcrExpansion exp = a.getExpansion(0);
            exp.setFreq(exp.getFreq() + 1);
        }
    }

    private Acronym getAcronym(String source, List<Node> expList, String expansion,
                               String acronym, String revExpansion) {
        Acronym acr;
        if (!revExpansion.equals(expansion)) {
            System.out.println(">> Revised expansion:"
                    + revExpansion);
        }
        acr = new Acronym(acronym);
        String expansionPt = toLispRep(expList, revExpansion);

        AcrExpansion acrExp = new AcrExpansion(revExpansion,
                expansionPt, source, "1");
        acr.addExpansion(acrExp);
        String key = prepKey(acr);
        adjustFreq(acr, key);
        return acr;
    }

    protected String toLispRep(List<Node> expList, String revExpansion) {
        String[] toks = revExpansion.split("\\s+");
        if (toks.length == expList.size()) {
            return toLispRep(expList);
        } else {
            List<Node> revExpList = new ArrayList<Node>(expList.size());
            int startIdx = findMatchingNodeIdx(toks[0], expList, 0);
            if (startIdx != -1) {
                int endIdx = findMatchingNodeIdx(toks[toks.length - 1],
                        expList, startIdx);
                if (endIdx != -1) {
                    for (int i = startIdx; i <= endIdx; i++) {
                        revExpList.add(expList.get(i));
                    }
                    return toLispRep(revExpList);
                }
            }
            boolean found = false;
            int startOffset = 0;
            while (!found) {
                revExpList.clear();
                boolean ok = true;
                int lastIdx = -1;
                int i = 0;
                while (i < toks.length) {
                    String tok = toks[i];
                    int idx = findMatchingNodeIdx(tok, expList, startOffset);
                    if (lastIdx == -1) {
                        startOffset = idx;
                    }

                    if (idx == -1 || (lastIdx >= 0 && idx != (lastIdx + 1))) {
                        if (lastIdx >= 0
                                && idx != -1
                                && isEligibleProposition(expList.get(
                                lastIdx + 1).getToken())) {
                            idx = lastIdx + 1;
                            --i;
                        } else {
                            ok = false;
                            break;
                        }
                    }
                    revExpList.add(expList.get(idx));
                    lastIdx = idx;
                    i++;
                }
                if (!ok) {
                    if (startOffset < 0) {
                        System.err
                                .println("No alignment with parse tree. Returning empty parse tree");
                        return "";
                    }
                    startOffset++;
                    if (startOffset >= toks.length) {
                        throw new RuntimeException(
                                "Should not happen! But did!");
                    }
                } else {
                    found = true;
                }
            }
            return toLispRep(revExpList);
        }
    }

    protected String toLispRep(List<Node> expList) {
        StringBuilder sb = new StringBuilder();
        sb.append("(FRAG");
        for (Node n : expList) {
            sb.append(' ').append(n.toLispString());
        }
        sb.append(')');
        return sb.toString();
    }

    protected int findMatchingNodeIdx(String tok, List<Node> nodeList,
                                      int startOffset) {
        for (int i = startOffset; i < nodeList.size(); i++) {
            Node n = nodeList.get(i);
            if (tok.equals(n.getToken()) || n.getToken().endsWith(tok)
                    || n.getToken().startsWith(tok)) {
                return i;
            }

        }
        return -1;
    }

    protected String removeTillPivot(String expansion, int pivotIdx) {
        char[] carr = expansion.toCharArray();
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (char aCarr : carr) {
            if (idx < pivotIdx
                    && (aCarr == '-' || Character.isWhitespace(aCarr))) {
                idx++;
                continue;
            }
            if (idx == pivotIdx) {
                sb.append(aCarr);
            }
        }
        return sb.toString();
    }

    protected String conditionAcronym(String acronym) {
        acronym = acronym.replaceAll("-", "");
        if (acronym.endsWith("s"))
            acronym = acronym.substring(0, acronym.length() - 1);
        return acronym;
    }

    protected String checkValidity(String acronym, String expansion,
                                   boolean withDashExpansion) {
        acronym = conditionAcronym(acronym);
        String[] toks;
        if (withDashExpansion)
            toks = expansion.split("[\\s|-]+");
        else
            toks = expansion.split("\\s+");
        int pivotIdxBeforeStrip = findFirstPivotIdx(acronym, toks);
        // remove any occurrence of eligible propositions and conjunctions;
        toks = stripIneligibleToks(toks);
        StringBuilder buffer = new StringBuilder();
        if (toks.length == acronym.length()) {
            // check if acronym letter is the first letter of each tok
            int pivotIdx = findFirstPivotIdx(acronym, toks);
            if (pivotIdx == 0) {

                if (checkIfFLBAcronym(toks, acronym, buffer, 0)) {
                    return expansion;
                } else {
                    if (checkValidity2(acronym, 0, toks, pivotIdx)) {
                        return expansion;
                    }
                    return null;
                }
            } else {
                if (pivotIdx > 0) {
                    if (checkValidity2(acronym, 0, toks, pivotIdx)) {
                        return removeTillPivot(expansion, pivotIdxBeforeStrip);
                    }
                } else
                    return null;
            }
        } else {
            // check if there is any token starting with the first letter of
            // potential acronym
            int pivotIdx = findFirstPivotIdx(acronym, toks);
            if (pivotIdx >= 0) {
                if (toks.length - pivotIdx == acronym.length()) {

                    if (checkIfFLBAcronym(toks, acronym, buffer, pivotIdx)) {
                        return buffer.toString().trim();
                    }
                    if (checkValidity2(acronym, 1, toks, pivotIdx)) {
                        return removeTillPivot(expansion, pivotIdxBeforeStrip);
                    }
                } else {
                    if (checkIfFLBAcronym(toks, acronym, buffer, pivotIdx)) {
                        return buffer.toString().trim();
                    }
                    if (checkValidity2(acronym, 0, toks, pivotIdx)) {
                        return removeTillPivot(expansion, pivotIdxBeforeStrip);
                    }
                }
            }
        }
        return null;
    }

    protected boolean checkIfFLBAcronym(String[] toks, String acronym,
                                        StringBuilder sb, int startOffset) {
        char[] acrArr = acronym.toLowerCase().toCharArray();
        if (toks.length < acrArr.length)
            return false;
        int diff = toks.length - acrArr.length;
        int offset = startOffset;
        while (offset <= diff) {
            boolean ok = true;
            int idx = offset;
            int endLoc = toks.length;
            for (int i = offset; i < toks.length; i++) {
                if (toks[i].length() == 0 || (idx - offset) >= acrArr.length) {
                    if ((idx - offset) == acrArr.length) {
                        endLoc = idx;
                        break;
                    }
                    ok = false;
                    break;
                }
                char c = Character.toLowerCase(toks[i].charAt(0));
                if (!Character.isLetter(c)) {
                    continue;
                }
                if (c != acrArr[idx - offset]) {
                    ok = false;
                    break;
                } else {
                    idx++;
                }
            }
            if (ok) {
                for (int i = offset; i < endLoc; i++) {
                    sb.append(toks[i]).append(' ');
                }
                return true;
            }
            offset++;
        }
        return false;
    }

    protected String[] stripIneligibleToks(String[] toks) {
        List<String> stripped = new ArrayList<String>(toks.length);
        for (String tok : toks) {
            if (tok.equals("of") || tok.equals("in") || tok.equals("and")
                    || tok.equals("to") || tok.equals("for"))
                continue;
            stripped.add(tok);
        }
        String[] strippedArr = new String[stripped.size()];
        return stripped.toArray(strippedArr);
    }

    protected int findFirstPivotIdx(String acronym, String[] toks) {
        if (acronym.length() == 0)
            return -1;
        char firstChar = acronym.toLowerCase().charAt(0);
        int pivotIdx = -1;
        for (int i = 0; i < toks.length; i++) {
            if (toks[i].length() == 0)
                continue;
            char c = Character.toLowerCase(toks[i].charAt(0));
            if (firstChar == c) {
                pivotIdx = i;
                break;
            }
        }
        return pivotIdx;
    }

    boolean checkValidity2(String acronym, int acrStartIdx, String[] toks,
                           int pivotIdx) {
        acronym = conditionAcronym(acronym);
        char[] acrArr = acronym.toLowerCase().toCharArray();
        String[] remToks = new String[toks.length - pivotIdx];
        for (int i = pivotIdx; i < toks.length; i++)
            remToks[i - pivotIdx] = toks[i].toLowerCase();
        int k = 0;
        int acrIdx = acrStartIdx;
        int len = remToks.length;
        while (k < len) {
            if (acrIdx >= acrArr.length) {
                return true;
            }
            boolean hasNext = k + 1 < len;

            char c = remToks[k].charAt(0);
            if (c == acrArr[acrIdx]) {
                if (hasNext && acrIdx + 1 < acrArr.length
                        && remToks[k + 1].charAt(0) == acrArr[acrIdx + 1]) {
                    k++;
                }
                acrIdx++;
            } else {
                int idx = remToks[k].indexOf(acrArr[acrIdx]);
                if (idx == -1) {

                    return false;
                }
                acrIdx++;
                if (acrIdx >= acrArr.length) {
                    return true;
                }
                if (!hasNext) {
                    int i = acrIdx;
                    while (i < acrArr.length) {
                        idx = remToks[k].indexOf(acrArr[i], idx + 1);
                        if (idx == -1)
                            return false;
                        i++;
                    }
                    return true;
                } else {
                    // there are more tokens
                    AcrTokIdx pivot = findFirstLetterMatch(remToks, k + 1, acrArr,
                            acrIdx);
                    if (pivot == null) {
                        //if any other words match by first letter (allowing skipping)
                        if (findFirstLetterMatchAllWithSkip(remToks, k + 1, acrArr, acrIdx)) {
                            return true;
                        }
                        return false;
                    }
                    int i = acrIdx;
                    while (i < pivot.acrIdx) {
                        idx = remToks[k].indexOf(acrArr[i], idx + 1);
                        if (idx == -1)
                            return false;
                        i++;
                    }
                    acrIdx = pivot.acrIdx;
                    k++;
                }

            }
        }
        return false;
    }

    protected boolean findFirstLetterMatchAllWithSkip(String[] toks, int startIdx,
                                                      char[] acr, int acrStartIdx) {
        int tokIdx = startIdx;
        for (int i = acrStartIdx; i < acr.length; i++) {
            if (!Character.isLetterOrDigit(acr[i])) {
                continue;
            }
            boolean found = false;
            for (int j = tokIdx; j < toks.length; j++) {
                if (isSame(toks[j].charAt(0), acr[i])) {
                    tokIdx = j + 1;
                    found = true;
                    break;
                }
            }
            if (!found || (tokIdx >= toks.length && (i + 1) < acr.length)) {
                return false;
            }
        }
        return true;
    }

    static boolean isSame(char c1, char c2) {
        return Character.toLowerCase(c1) == Character.toLowerCase(c2);
    }

    protected AcrTokIdx findFirstLetterMatch(String[] toks, int startIdx,
                                             char[] acr, int acrStartIdx) {
        for (int i = acrStartIdx; i < acr.length; i++) {
            if (toks[startIdx].charAt(0) == acr[i])
                return new AcrTokIdx(i, startIdx);
        }
        return null;
    }

    class AcrTokIdx {
        int acrIdx;
        int tokIdx;

        public AcrTokIdx(int acrIdx, int tokIdx) {
            this.acrIdx = acrIdx;
            this.tokIdx = tokIdx;
        }
    }// ;

    public boolean isLeftParen(Node nd) {
        return nd.getToken().equals("-LRB-");
    }

    public boolean isRightParen(Node nd) {
        return nd.getToken().equals("-RRB-");
    }

    public static boolean isEligibleProposition(String tok) {
        return tok.equals("in") || tok.equals("of") || tok.equals("for")
                || tok.equals("to");
    }

    public void createSurfaceList(Node node, List<Node> sList) {
        if (node == null)
            return;
        if (!node.hasChildren()) {
            sList.add(node);
        }
        for (Node child : node.getChildren()) {
            createSurfaceList(child, sList);
        }
    }

    public static class AcronymPTLocWrapper {
        List<Node> acronymSpanningNodes;
        Acronym acronym;

        public AcronymPTLocWrapper(List<Node> acronymSpanningNodes,
                                   Acronym acronym) {
            super();
            this.acronymSpanningNodes = acronymSpanningNodes;
            this.acronym = acronym;
        }

        public List<Node> getAcronymSpanningNodes() {
            return acronymSpanningNodes;
        }

        public Node getLastAcronymNode() {
            return acronymSpanningNodes.get(acronymSpanningNodes.size() - 1);
        }

        public Acronym getAcronym() {
            return acronym;
        }
    }// ;


    public static void main(String[] args) throws Exception {
        Pair<String, String> p1 = new Pair<String, String>(
                "Is Crohn 's disease ( CD ) linked to the consumption of refrigerated food ?",
                "(ROOT (S (NP (NP (NP (NNP Is) (NNP Crohn) (POS 's)) (NN disease)) (PRN (-LRB- -LRB-) (NN CD) (-RRB- -RRB-))) (VP (VBN linked) (PP (TO to) (NP (NP (DT the) (NN consumption)) (PP (IN of) (NP (JJ refrigerated) (NN food)))))) (. ?)))");

        Pair<String, String> p2 = new Pair<String, String>(
                "Are there Conserved Noncoding Elements ( CNEs ) in plant genomes ?",
                "(ROOT (SQ (VBP Are) (NP (EX there)) (PP (NP (NP (NNP Conserved) (NNP Noncoding) (NNP Elements)) (PRN (-LRB- -LRB-) (NP (NNP CNEs)) (-RRB- -RRB-))) (IN in) (NP (NN plant) (NNS genomes))) (. ?)))");
        Pair<String, String> p3 = new Pair<String, String>(
                "Could Catecholaminergic Polymorphic Ventricular Tachycardia ( CPVT ) cause sudden cardiac death ?",
                "(ROOT (SQ (MD Could) (NP (NP (NNP Catecholaminergic) (NNP Polymorphic) (NNP Ventricular) (NNP Tachycardia)) (NP (-LRB- -LRB-) (NNP CPVT) (-RRB- -RRB-))) (VP (VB cause) (NP (JJ sudden) (JJ cardiac) (NN death))) (. ?)))"
        );

        Pair<String, String> p4 = new Pair<>(
                "What is the role of extracellular signal-related kinases 1 and 2 ( ERK1/2 ) proteins in craniosynostosis ?",
                "(ROOT (SBARQ (WHNP (WP What)) (SQ (VBZ is) (NP (NP (DT the) (NN role)) (PP (IN of) (NP (NP (NP (JJ extracellular) (JJ signal-related) (NNS kinases)) (NP (CD 1))) (CC and) (NP (NP (CD 2) (-LRB- -LRB-) (NN ERK1/2) (-RRB- -RRB-)) (NP (NP (NNS proteins)) (PP (IN in) (NP (NN craniosynostosis))))))))) (. ?)))"
        );

        Pair<String, String> p5 = new Pair<>(
                "Is Calcium/Calmodulin dependent protein kinase II ( CaMKII ) involved in cardiac arrhythmias and heart failure ?",
                "(ROOT (SQ (VBZ Is) (NP (NNP Calcium/Calmodulin)) (NP (NP (NP (JJ dependent) (NN protein) (NN kinase)) (NP (NP (NNP II)) (PRN (-LRB- -LRB-) (NP (NNP CaMKII)) (-RRB- -RRB-)))) (VP (VBN involved) (PP (IN in) (NP (NP (JJ cardiac) (NNS arrhythmias)) (CC and) (NP (NN heart) (NN failure)))))) (. ?)))"
        );

        Pair<String, String> p6 = new Pair<>(
                "Which diseases are caused by mutations in Calsequestrin 2 ( CASQ2 ) gene ?",
                "(ROOT (SBARQ (WHNP (WDT Which) (NN diseases)) (SQ (VBP are) (VP (VBN caused) (PP (IN by) (NP (NP (NNS mutations)) (PP (IN in) (NP (NN Calsequestrin) (CD 2) (-LRB- -LRB-) (NNP CASQ2) (-RRB- -RRB-) (NN gene))))))) (. ?)))"
        );

        Pair<String, String> p7 = new Pair<>(
                "How are CRM ( cis-regulatory modules ) defined ?",
                "(ROOT (SBARQ (WHADVP (WRB How)) (SQ (VBP are) (NP (NNP CRM)) (VP (PRN (-LRB- -LRB-) (NP (JJ cis-regulatory) (NNS modules)) (-RRB- -RRB-)) (VBN defined))) (. ?)))"
        );

        Pair<String, String> p8 = new Pair<>(
                "Is RIP1 ( RIP-1 ) part of the necrosome ?",
                "(ROOT (SQ (VBZ Is) (NP (NNP RIP1)) (NP (NP (-LRB- -LRB-) (JJ RIP-1) (-RRB- -RRB-) (NN part)) (PP (IN of) (NP (DT the) (NN necrosome)))) (. ?)))"
        );
        Pair<String, String> p9 = new Pair<>(
                "Which is the most known bacterium responsible for botulism ( sausage-poisoning ) ?",
                "(ROOT (SBARQ (WHNP (WDT Which)) (SQ (VBZ is) (NP (NP (NP (DT the) (ADJP (RBS most) (JJ known)) (NN bacterium)) (ADJP (JJ responsible) (PP (IN for) (NP (NN botulism))))) (PRN (-LRB- -LRB-) (NP (NN sausage-poisoning)) (-RRB- -RRB-)))) (. ?)))"
        );
        AcronymDetector ad = new AcronymDetector();

        List<Pair<String, String>> pairs = Arrays.asList(p4, p9, p8, p7, p6, p5, p1, p2, p3);

        for (Pair<String, String> pair : pairs) {
            List<Acronym> acronyms = ad.findAcronyms(pair.getFirst(), pair.getSecond());
            System.out.println(acronyms);
        }

    }
}
