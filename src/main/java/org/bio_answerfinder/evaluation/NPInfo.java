package org.bio_answerfinder.evaluation;

import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.TagSetUtils;

import java.util.*;

/**
 * Created by bozyurt on 9/15/19.
 */
public class NPInfo {
    String phrase;
    int startIdx;
    int endIdx;
    List<NPTokenInfo> tokens = new ArrayList<>(5);

    public static Set<String> domainStopWords = new HashSet<>(Arrays.asList("gene", "genes", "protein",
            "proteins", "enzyme", "enzymes", "drug", "drugs", "other", "others", "data",
            "mutation", "mutations", "chromosome", "chromosomes", "disease", "diseases"));

    public NPInfo(String phrase, int startIdx, int endIdx) {
        this.phrase = phrase;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
    }

    public void setTokens(List<NPTokenInfo> nptiList) {
        for (NPTokenInfo npti : nptiList) {
            if (npti.startIdx >= startIdx && npti.endIdx <= endIdx) {
                tokens.add(npti);
            }
        }
    }

    public String getPhrase() {
        return phrase;
    }

    public int getStartIdx() {
        return startIdx;
    }

    public int getEndIdx() {
        return endIdx;
    }

    public List<NPTokenInfo> getTokens() {
        return tokens;
    }

    public int length() {
        return phrase.length();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NPInfo{");
        sb.append("phrase='").append(phrase).append('\'');
        sb.append(", startIdx=").append(startIdx);
        sb.append(", endIdx=").append(endIdx);
        StringBuilder buf = new StringBuilder();
        tokens.forEach(t -> buf.append(t.posTag).append(' '));
        sb.append(", posTags='").append(buf.toString().trim()).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public void stripArticle() {
        if (tokens.isEmpty()) {
            return;
        }
        NPTokenInfo lastRemoved = null;
        for (Iterator<NPTokenInfo> it = tokens.iterator(); it.hasNext(); ) {
            NPTokenInfo npti = it.next();
            String posTag = npti.posTag;
            if (TagSetUtils.isDeterminer(posTag) || TagSetUtils.isPronoun(posTag)
                    || TagSetUtils.isPredeterminer(posTag)
                    || TagSetUtils.isCardinalNumber(posTag)
                    || TagSetUtils.isComparativeOrSuperlativeAdjective(posTag)
                    || TagSetUtils.isAdverb(posTag)
                    || domainStopWords.contains(npti.token.toLowerCase())) {
                lastRemoved = npti;
                it.remove();
            } else {
                break;
            }
        }
        if (lastRemoved != null) {
            int origLen = phrase.length();
            phrase = phrase.substring(lastRemoved.endIdx - startIdx).trim();
            int diff = origLen - phrase.length();
            startIdx += diff;
        }


    }

    public int firstConjunction() {
        for (int i = 0; i < tokens.size(); i++) {
            if (TagSetUtils.isConjunction(tokens.get(i).posTag)) {
                return i;
            }
        }
        return -1;
    }

    public List<NPInfo> split(int ccLocIdx) {
        List<NPInfo> npList = new ArrayList<>(2);
        NPTokenInfo ccNPTI = tokens.get(ccLocIdx);
        String first = phrase.substring(0, ccNPTI.startIdx - startIdx);
        int origLen = first.length();
        first = first.trim();
        int diff = origLen - first.length();
        NPInfo firstNP = new NPInfo(first, startIdx, ccNPTI.startIdx - diff);
        firstNP.tokens = new ArrayList<>(tokens.subList(0, ccLocIdx));
        npList.add(firstNP);
        String second = phrase.substring(ccNPTI.endIdx - startIdx);
        origLen = second.length();
        second = second.trim();
        diff = origLen - second.length();
        NPInfo secondNP = new NPInfo(second, ccNPTI.endIdx + diff, endIdx);
        secondNP.tokens = new ArrayList<>(tokens.subList(ccLocIdx + 1, tokens.size()));
        npList.add(secondNP);

        return npList;
    }

    public static class NPTokenInfo {
        String token;
        String posTag;
        int startIdx;
        int endIdx;

        public NPTokenInfo(String token, String posTag, int startIdx, int endIdx) {
            this.token = token;
            this.posTag = posTag;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }
    }


    public static List<NPInfo> extractSubPhrases(NPInfo npInfo, Set<String> uniqSet) {
        if (npInfo.getTokens().size() < 3) {
            if (!uniqSet.contains(npInfo.getPhrase())) {
                uniqSet.add(npInfo.getPhrase());
                return Arrays.asList(npInfo);
            } else {
                return Collections.emptyList();
            }
        }
        List<NPInfo> subPhrases = new ArrayList<>(5);
        if (npInfo.getTokens().size() < 5) {
            if (!uniqSet.contains(npInfo.getPhrase())) {
                subPhrases.add(npInfo);
                uniqSet.add(npInfo.getPhrase());
            }
            List<NPTokenInfo> tokens = npInfo.getTokens();
            for (int i = 1; i < tokens.size() - 1; i++) {
                int firstNounIdx = firstNounIdx(tokens, i);
                if (firstNounIdx != -1) {
                    NPTokenInfo npti = tokens.get(firstNounIdx);
                    String phrase = npInfo.getPhrase().substring(npti.startIdx - npInfo.getStartIdx());
                    if (!uniqSet.contains(phrase) && phrase.length() > 2) {
                        NPInfo sp = new NPInfo(phrase, npti.startIdx, npInfo.getEndIdx());
                        sp.tokens = new ArrayList<>(tokens.subList(firstNounIdx, tokens.size()));
                        subPhrases.add(sp);
                        uniqSet.add(phrase);
                    }
                }
            }
        } else {
            // most probably a coordinated list
            for (NPTokenInfo npti : npInfo.getTokens()) {
                String posTag = npti.posTag;
                if (TagSetUtils.isNoun(posTag) && !uniqSet.contains(npti.token) && npti.token.length() > 2) {
                    NPInfo sp = new NPInfo(npti.token, npti.startIdx, npti.endIdx);
                    sp.tokens = Arrays.asList(npti);
                    subPhrases.add(sp);
                    uniqSet.add(npti.token);
                }
            }
        }
        return subPhrases;
    }

    public static List<NPInfo> prepNPList(DataRecord.ParsedSentence ps) throws Exception {
        String sentence = ps.getSentence();
        List<NPTokenInfo> nptiList = toTokens(ps);

        List<NPInfo> npList = new ArrayList<>();
        Set<String> uniqSet = new HashSet<>();
        for (DataRecord.Chunk chunk : ps.getChunks()) {
            String phrase = sentence.substring(chunk.getStartIdx(), chunk.getEndIdx() + 1);
            if (phrase.length() < 3) {
                continue;
            }

            NPInfo npInfo = new NPInfo(phrase, chunk.getStartIdx(), chunk.getEndIdx() + 1);
            npInfo.setTokens(nptiList);

            int ccTokIdx = npInfo.firstConjunction();
            if (ccTokIdx != -1) {
                splitConjunction(npList, uniqSet, npInfo, ccTokIdx);

            } else {
                npInfo.stripArticle();
                npList.add(npInfo);
            }
        }
        return npList;
    }

    static int firstNounIdx(List<NPTokenInfo> nptiList, int startOffset) {
        for (int i = startOffset; i < nptiList.size(); i++) {
            if (TagSetUtils.isNoun(nptiList.get(i).posTag)) {
                return i;
            }
        }
        return -1;
    }

    public static void splitConjunction(List<NPInfo> npList, Set<String> uniqSet, NPInfo npInfo, int ccTokIdx) {
        List<NPInfo> split = npInfo.split(ccTokIdx);
        NPInfo first = split.get(0);
        NPInfo second = split.get(1);
        first.stripArticle();
        second.stripArticle();

        if (first.length() > 2 && !uniqSet.contains(first.getPhrase())) {
            npList.add(first);
            uniqSet.add(first.getPhrase());
        }
        if (second.length() < 3) {
            return;
        }
        int secCCLocIdx = second.firstConjunction();
        if (secCCLocIdx != -1) {
            splitConjunction(npList, uniqSet, second, secCCLocIdx);
            return;
        }
        if (!uniqSet.contains(second.getPhrase())) {
            npList.add(second);
            uniqSet.add(second.getPhrase());
        }
    }

    public static List<NPTokenInfo> toTokens(DataRecord.ParsedSentence ps) throws ParseTreeManagerException {
        String sentence = ps.getSentence();
        List<String> posTags = ps.getPosTags();
        List<String> tokens = ps.getTokens();
        List<NPTokenInfo> npTiList = new ArrayList<>(posTags.size());
        int offset = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String posTag = posTags.get(i);
            int idx = sentence.indexOf(token, offset);
            Assertion.assertTrue(idx != -1);
            int endIdx = idx + token.length();
            NPTokenInfo npti = new NPTokenInfo(token, posTag, idx, endIdx);
            npTiList.add(npti);
            offset = endIdx + 1;
        }
        return npTiList;
    }
}
