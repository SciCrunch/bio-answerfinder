package org.bio_answerfinder.evaluation;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.DataRecord.Chunk;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.common.GloVeVectorManager;
import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.common.WordVector;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.engine.QuestionFocusDetector;
import org.bio_answerfinder.engine.QuestionFocusDetector.QuestionFocus;
import org.bio_answerfinder.engine.QuestionParser;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.evaluation.ClusterUtils.Cluster;
import org.bio_answerfinder.evaluation.FactoidExtractionEvaluation.QuestionInfo;
import org.bio_answerfinder.evaluation.SimilarityUtils.MatchingResult;
import org.bio_answerfinder.util.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by bozyurt on 9/14/19.
 */
public class FactoidUtils {
    QuestionParser questionParser;
    GloveDBLookup gloveMan;
    TObjectFloatHashMap<String> vocabulary;

    public FactoidUtils() throws Exception {
        questionParser = new QuestionParser();
        questionParser.initialize();
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        gloveMan = GloveDBLookup.getInstance(dbFile);
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
    }

    public FactoidUtils(TObjectFloatHashMap<String> vocabulary) throws Exception {
        this.vocabulary = vocabulary;
        questionParser = new QuestionParser();
        questionParser.initialize();
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        gloveMan = GloveDBLookup.getInstance(dbFile);
    }

    public QuestionFocus findFocus(String question) throws Exception {
        List<DataRecord> dataRecords = questionParser.parseQuestion("", question);
        QuestionFocusDetector detector = new QuestionFocusDetector();
        QuestionFocus questionFocus = detector.detectFocus(dataRecords);

        System.out.println(questionFocus);
        return questionFocus;
    }

    public Set<String> getQuestionTokens(String question) throws Exception {
        List<DataRecord> dataRecords = questionParser.parseQuestion("", question);
        return getQuestionTokens(dataRecords);

    }


    public static Set<String> getQuestionTokens(List<DataRecord> dataRecords) throws ParseTreeManagerException {
        Set<String> tokenSet = new HashSet<>();
        for (DataRecord dr : dataRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            List<String> tokens = ps.getTokens();
            tokens.forEach(t -> tokenSet.add(t.toLowerCase()));
        }
        return tokenSet;
    }

    public MatchingResult generateCandidatePhrases2(QuestionInfo qi, int topN, QuestionFocus qf, Set<String> questionTokenSet) throws Exception {
        int len = Math.min(topN, qi.getCandidateSentences().size());
        List<String> sentences = qi.getCandidateSentences().subList(0, len);
        List<ScoredNPInfo> phrases = new ArrayList<>();
        Set<String> seenSet = new HashSet<>();
        List<String> normalizedSentences = new ArrayList<>();
        for (String sentence : sentences) {
            sentence = sentence.replaceAll("_+", " ");
            normalizedSentences.add(sentence);
            List<DataRecord> dataRecords = questionParser.parseQuestion("", sentence);
            // Assertion.assertEquals(1, dataRecords.size());
            DataRecord dr = dataRecords.get(0);
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            String s = ps.getSentence();
            System.out.println(GenUtils.formatText(s, 100));
            List<NPInfo> npList = NPInfo.prepNPList(ps);
            for (NPInfo np : npList) {
                String focusWord = qf.getFocusWord();
                if (qf.getPhrase().indexOf(" in ") != -1 || qf.getPhrase().indexOf(" for ") != -1 ||
                        qf.getPhrase().indexOf("  of ") != -1) {
                    if (!GenUtils.isEmpty(qf.getModifier())) {
                        focusWord = qf.getModifier();
                    }
                }
                ScoredNPInfo sp = scoreAndFilter(focusWord, np, seenSet, false);
                if (sp != null && sp.getScore() > 0.1 &&
                        isEligible(sp.getPhrase().getPhrase(), questionTokenSet, gloveMan)) {
                    phrases.add(sp);
                }
            }
            System.out.println("------------");
        }
        // weightByIDF(phrases);
        weightByTFIDF(phrases, normalizedSentences);
        //weightByTFIDFOnly(phrases, normalizedSentences);
       // ClusterUtils clusterUtils = new ClusterUtils(phrases, gloveMan);

       // List<Cluster> clusters = clusterUtils.handle();
       // Collections.sort(clusters, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

        // phrases = getClusterBests(clusters);
        Collections.sort(phrases, (o1, o2) -> Double.compare(o2.score, o1.score));
        //System.out.println();
        //clusters.forEach(System.out::println);
        System.out.println();
        System.out.println(qi.getQuestion());
        System.out.println("---------------------------------");
        phrases.forEach(System.out::println);
        System.out.println(qi.getQr().getAnswer().getExactAnswer());
        System.out.println("============================");

        return getRank(phrases, qi);
    }


    List<ScoredNPInfo> getClusterBests(List<Cluster> clusters) {
        List<ScoredNPInfo> phrases = new ArrayList<>(clusters.size());
        for (Cluster cluster : clusters) {
            phrases.add(cluster.getMaxScoringMember());
        }
        return phrases;
    }

    public Pair<String, Integer> getRank4EntityFilter(List<String> answers, QuestionInfo qi) {
        List<String> gsAnswers = new ArrayList<>();
        for (QuestionRecord.AnswerItem ai : qi.getQr().getAnswer().getExactAnswer()) {
            gsAnswers.addAll(ai.getAnswers());
        }

        List<Double> scores = new ArrayList<>(answers.size());
        for (String phrase : answers) {
            Map<String, WordVector> phraseMap = prepareWordVectorMap(phrase);
            double max = -1;
            for (String gsAnswer : gsAnswers) {
                Map<String, WordVector> gsaMap = prepareWordVectorMap(gsAnswer);
                float wmd = MathUtils.relaxedWMD(gsaMap, phraseMap, false);
                if (wmd > max) {
                    max = wmd;
                }
            }
            scores.add(max);
        }
        double maxScore = -1;
        int rank = -1;
        String theAnswer = null;
        for (int i = 0; i < answers.size(); i++) {
            String phrase = answers.get(i);
            double score = scores.get(i);
            if (score > maxScore) {
                rank = i + 1;
                maxScore = score;
                theAnswer = phrase;
            }
        }
        return new Pair<>(theAnswer, rank);
    }


    public MatchingResult getRank(List<ScoredNPInfo> phrases, QuestionInfo qi) {
        List<String> gsAnswers = new ArrayList<>();
        for (QuestionRecord.AnswerItem ai : qi.getQr().getAnswer().getExactAnswer()) {
            gsAnswers.addAll(ai.getAnswers());
        }

        List<Double> scores = new ArrayList<>(phrases.size());
        for (ScoredNPInfo sp : phrases) {
            Map<String, WordVector> phraseMap = prepareWordVectorMap(sp.getPhrase().getPhrase());
            double max = -1;
            for (String gsAnswer : gsAnswers) {
                Map<String, WordVector> gsaMap = prepareWordVectorMap(gsAnswer);
                float wmd = MathUtils.relaxedWMD(gsaMap, phraseMap, false);
                if (wmd > max) {
                    max = wmd;
                }
            }
            scores.add(max);
        }
        double maxScore = -1;
        int rank = -1;
        ScoredNPInfo theAnswer = null;
        for (int i = 0; i < phrases.size(); i++) {
            ScoredNPInfo sp = phrases.get(i);
            double score = scores.get(i);
            if (score > maxScore) {
                rank = i + 1;
                maxScore = score;
                theAnswer = sp;
            }
        }
        return new MatchingResult(qi, theAnswer, rank);
    }


    public static boolean isEligible(String phrase, Set<String> refTokenSet, GloveDBLookup gloveMan) {
        if (NPInfo.domainStopWords.contains(phrase.toLowerCase())) {
            return false;
        }

        return !SimilarityUtils.fuzzyMatches(phrase, refTokenSet, gloveMan, 0.8);

        /*
        String[] tokens = phrase.split("\\s+");
        if (tokens.length == 1) {
            return !refTokenSet.contains(phrase.toLowerCase());
        } else {
            int matchCount = 0;
            for (String token : tokens) {
                if (refTokenSet.contains(token.toLowerCase())) {
                    matchCount++;
                }
            }
            double score = matchCount / (double) tokens.length;
            return score <= 0.5;
        }
        */
    }

    void rerankByWRWMD(Set<String> questionTokenSet, List<ScoredNPInfo> phrases) {
        String q = GenUtils.join(new ArrayList<Object>(questionTokenSet), " ");
        Map<String, WordVector> questionMap = prepareWordVectorMap(q);
        for (ScoredNPInfo sp : phrases) {
            Map<String, WordVector> phraseMap = prepareWordVectorMap(sp.getPhrase().getPhrase());
            float wmd = MathUtils.relaxedWMD(questionMap, phraseMap, false);
            sp.score = wmd;
        }
    }

    Map<String, WordVector> prepareWordVectorMap(String content4Glove) {
        Map<String, WordVector> map = new HashMap<>();
        String[] tokens = content4Glove.split("\\s+");
        for (String token : tokens) {
            boolean phrase = token.indexOf('_') != -1;
            float[] gloveVector = gloveMan.getGloveVector(token);
            String term = phrase ? token.replace('_', ' ') : token;
            if (!map.containsKey(term) && gloveVector != null && vocabulary.containsKey(term)) {
                float weight = vocabulary.get(term);
                map.put(term, new WordVector(weight, gloveVector, term));
            }
        }
        return map;
    }

    @Deprecated
    public void generateCandidatePhrases(QuestionInfo qi, int topN, QuestionFocus qf) throws Exception {
        int len = Math.min(topN, qi.getCandidateSentences().size());
        List<String> sentences = qi.getCandidateSentences().subList(0, len);

        List<ScoredPhrase> phrases = new ArrayList<>();
        Set<String> seenSet = new HashSet<>();

        for (String sentence : sentences) {
            sentence = sentence.replaceAll("_+", " ");

            List<DataRecord> dataRecords = questionParser.parseQuestion("", sentence);
            Assertion.assertEquals(1, dataRecords.size());
            DataRecord dr = dataRecords.get(0);
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            String s = ps.getSentence();
            System.out.println(GenUtils.formatText(s, 100));
            List<String> npList = prepNPList(sentence, ps.getChunks());
            System.out.println();
            // npList.forEach(System.out::println);
            for (String np : npList) {
                System.out.println(np);
                List<String> subPhrases = extractSubPhrases(np);
                for (String subPhrase : subPhrases) {
                    System.out.println("\t" + subPhrase);
                }
                ScoredPhrase sp = filter(qf.getFocusWord(), np);
                if (sp != null && !seenSet.contains(sp.getPhrase())) {
                    phrases.add(sp);
                    seenSet.add(sp.getPhrase());
                }
            }
            System.out.println("------------");
        }
        weight(phrases);
        Collections.sort(phrases, (o1, o2) -> Double.compare(o2.score, o1.score));


        phrases.forEach(System.out::println);
        System.out.println(qi.getQr().getAnswer().getExactAnswer());
        System.out.println("============================");
    }


    void weightByIDF(List<ScoredNPInfo> phrases) {
        for (ScoredNPInfo sp : phrases) {
            float idf = vocabulary.get(sp.getPhrase().getPhrase());
            if (idf > 0) {
                sp.score *= idf;
            }
        }
    }

    void weightByTFIDF(List<ScoredNPInfo> phrases, List<String> sentences) {
        FrequencyTable<String> ft = new FrequencyTable<>();
        for (ScoredNPInfo sp : phrases) {
            for (String sentence : sentences) {
                String phrase = sp.getPhrase().getPhrase();

                Pattern p = Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b");
                Matcher matcher = p.matcher(sentence);
                // int idx = sentence.indexOf(phrase);
                while (matcher.find()) {
                    ft.addValue(phrase);
                    //idx = sentence.indexOf(phrase, idx + phrase.length());
                }
            }
        }
        for (ScoredNPInfo sp : phrases) {
            String phrase = sp.getPhrase().getPhrase();
            int tf = ft.getFrequency(phrase);
            float idf = vocabulary.get(phrase);
            if (idf == 0) {
                String[] tokens = phrase.split("\\s+");
                double sum = 0;
                for (String token : tokens) {
                    sum += vocabulary.get(token);
                }
                idf = (float) (sum / tokens.length);
            }

            if (idf > 0) {
                sp.score *= Math.log(1 + tf) * idf;
            } else {
                sp.score *= Math.log(1 + tf);
            }
        }
    }

    void weightByTFIDFOnly(List<ScoredNPInfo> phrases, List<String> sentences) {
        FrequencyTable<String> ft = new FrequencyTable<>();
        for (ScoredNPInfo sp : phrases) {
            for (String sentence : sentences) {
                String phrase = sp.getPhrase().getPhrase();
                int idx = sentence.indexOf(phrase);
                if (idx != -1) {
                    ft.addValue(phrase);
                }
            }
        }
        for (ScoredNPInfo sp : phrases) {
            String phrase = sp.getPhrase().getPhrase();
            int tf = ft.getFrequency(phrase);
            float idf = vocabulary.get(phrase);
            if (idf > 0) {
                sp.score = tf * idf;
            } else {
                sp.score = tf;
            }
        }
    }

    void weight(List<ScoredPhrase> phrases) {
        for (ScoredPhrase sp : phrases) {
            float idf = vocabulary.get(sp.getPhrase());
            if (idf > 0) {
                sp.score *= idf;
            }
        }
    }

    ScoredNPInfo scoreAndFilter(String focusWord, NPInfo np, Set<String> uniqSet, boolean verbose) {
        float[] fv = gloveMan.getGloveVector(focusWord);
        if (fv == null) {
            return null;
        }
        List<NPInfo> subPhrases = NPInfo.extractSubPhrases(np, uniqSet);
        double fvNorm = MathUtils.euclidianNorm(fv);
        NPInfo thePhrase = null;
        double maxSim = -1;
        for (NPInfo subPhrase : subPhrases) {
            if (subPhrase.getPhrase().equalsIgnoreCase(focusWord)) {
                continue;
            }
            String sp = subPhrase.getPhrase().replaceAll("\\s+", "_");
            float[] sv = gloveMan.getGloveVector(sp);
            double similarity = -1;
            if (sv == null) {
                if (subPhrase.getTokens().size() > 1) {
                    similarity = getPhraseSimilarity(fv, fvNorm, subPhrase.getPhrase());
                } else {
                    continue;
                }
            } else {
                similarity = MathUtils.cosSimilarity(fv, sv, fvNorm);
            }
            if (similarity > maxSim) {
                maxSim = similarity;
                thePhrase = subPhrase;
            }
        }
        if (thePhrase == null) {
            return null;
        }
        if (verbose) {
            System.out.println("\t* " + focusWord + "  -> " + thePhrase.getPhrase() + " (" + maxSim + ")");
        }
        return new ScoredNPInfo(thePhrase, maxSim);
    }


    ScoredPhrase filter(String focusWord, String np) {
        float[] fv = gloveMan.getGloveVector(focusWord);
        List<String> subPhrases = extractSubPhrases(np);
        double fvNorm = MathUtils.euclidianNorm(fv);
        double maxSim = -1;
        String thePhrase = null;
        for (String subPhrase : subPhrases) {
            if (subPhrase.equals(focusWord)) {
                //TODO
                continue;
            }
            String sp = subPhrase.replaceAll("\\s+", "_");
            float[] sv = gloveMan.getGloveVector(sp);
            double similarity = -1;
            if (sv == null) {
                if (subPhrase.indexOf(' ') != -1) {
                    similarity = getPhraseSimilarity(fv, fvNorm, subPhrase);
                } else {
                    continue;
                }
            } else {
                similarity = MathUtils.cosSimilarity(fv, sv, fvNorm);
            }
            if (similarity > maxSim) {
                maxSim = similarity;
                thePhrase = subPhrase;
            }
        }
        if (thePhrase == null) {
            return null;
        }
        System.out.println("\t* " + focusWord + "  -> " + thePhrase + " (" + maxSim + ")");
        return new ScoredPhrase(thePhrase, maxSim);
    }

    double getPhraseSimilarity(float[] fv, double fvNorm, String phrase) {
        String[] tokens = phrase.split("\\s+");
        double sum = 0;
        int count = 0;
        for (String token : tokens) {
            float[] sv = gloveMan.getGloveVector(token);
            if (sv != null) {
                double similarity = MathUtils.cosSimilarity(fv, sv, fvNorm);
                sum += similarity;
                count++;
            }
        }
        double avg = -1;
        if (count > 0) {
            avg = sum / count;
        }
        return avg;

    }

    public static class ScoredNPInfo {
        NPInfo phrase;
        double score;

        public ScoredNPInfo(NPInfo phrase, double score) {
            this.phrase = phrase;
            this.score = score;
        }

        public NPInfo getPhrase() {
            return phrase;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ScoredNPInfo{");
            sb.append("phrase=").append(phrase);
            sb.append(", score=").append(score);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class ScoredPhrase {
        String phrase;
        double score;

        public ScoredPhrase(String phrase, double score) {
            this.phrase = phrase;
            this.score = score;
        }

        public String getPhrase() {
            return phrase;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ScoredPhrase{");
            sb.append("phrase='").append(phrase).append('\'');
            sb.append(", score=").append(score);
            sb.append('}');
            return sb.toString();
        }
    }

    public static List<String> extractSubPhrases(String np) {
        String[] tokens = np.split("\\s+");
        if (tokens.length == 1) {
            return Arrays.asList(np);
        }
        List<String> subPhrases = new ArrayList<>(10);
        List<String> filteredTokens = new ArrayList<>();
        boolean hasStopWords = false;
        for (String token : tokens) {
            if (!StopWords.isStopWord(token)) {
                filteredTokens.add(token);
            } else {
                hasStopWords = true;
            }
        }
        subPhrases.addAll(filteredTokens);
        if (!hasStopWords) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < tokens.length; i++) {
                sb.setLength(0);
                for (int j = i; j < tokens.length; j++) {
                    sb.append(tokens[j]).append(' ');
                }
                String subPhrase = sb.toString().trim();
                if (!subPhrases.contains(subPhrase)) {
                    subPhrases.add(subPhrase);
                }
            }

            subPhrases.add(np);
        }
        subPhrases = subPhrases.stream().filter(s -> s.length() > 2 && isEligible(s)).collect(Collectors.toList());
        return subPhrases;
    }

    public static boolean isEligible(String subPhrase) {
        if (!Character.isLetter(subPhrase.charAt(0))) {
            return false;
        }
        if (isNumber(subPhrase)) {
            return false;
        }
        if (StringUtils.isAllSpecial(subPhrase)) {
            return false;
        }
        return true;
    }

    public static boolean isNumber(String subPhrase) {
        char[] carr = subPhrase.toCharArray();
        for (char c : carr) {
            if (!(Character.isDigit(c) || Character.isSpaceChar(c) || c == '.' || c == '-' || c == ',')) {
                return false;
            }
        }
        return true;
    }

    public static List<String> prepNPList(String sentence, List<Chunk> chunks) {
        List<String> npList = new ArrayList<>();
        Set<String> uniqSet = new HashSet<>();
        for (Chunk chunk : chunks) {
            String phrase = sentence.substring(chunk.getStartIdx(), chunk.getEndIdx() + 1);
            if (phrase.length() < 3) {
                continue;
            }
            int idx = phrase.indexOf(" and ");
            boolean done = false;
            if (idx == -1) {
                idx = phrase.indexOf(" or ");
                if (idx != -1) {
                    splitConjunction(npList, uniqSet, phrase, idx, 4);
                    done = true;
                }
            } else {
                splitConjunction(npList, uniqSet, phrase, idx, 5);
                done = true;
            }
            if (!done) {
                phrase = stripArticle(phrase);
                if (phrase.length() > 2 && !uniqSet.contains(phrase)) {
                    npList.add(phrase);
                    uniqSet.add(phrase);
                }
            }
        }
        return npList;
    }

    private static void splitConjunction(List<String> npList, Set<String> uniqSet, String phrase, int idx, int offset) {
        String first = stripArticle(phrase.substring(0, idx).trim());
        String second = stripArticle(phrase.substring(idx + offset).trim());
        if (first.length() > 2 && !uniqSet.contains(first)) {
            npList.add(first);
            uniqSet.add(first);
        }
        if (second.length() < 3) {
            return;
        }
        int secIdx = second.indexOf(" and ");
        int secOffset = 5;
        if (secIdx != -1) {
            splitConjunction(npList, uniqSet, second, secIdx, secOffset);
            return;
        } else {
            secIdx = second.indexOf(" or ");
            secOffset = 4;
            if (secIdx != -1) {
                splitConjunction(npList, uniqSet, second, secIdx, secOffset);
                return;
            }
        }

        if (second.length() > 2 && !uniqSet.contains(second)) {
            npList.add(second);
            uniqSet.add(second);
        }
    }


    public static String stripArticle(String phrase) {
        String stripped = stripPrefix(phrase, "the");
        if (stripped != null) {
            return stripped;
        }
        stripped = stripPrefix(phrase, "a");
        if (stripped != null) {
            return stripped;
        }
        stripped = stripPrefix(phrase, "an");
        if (stripped != null) {
            return stripped;
        }
        return phrase;
    }

    public static String stripPrefix(String phrase, String prefix) {
        if (phrase.toLowerCase().startsWith(prefix + " ")) {
            return phrase.substring(prefix.length()).trim();
        }
        return null;
    }

}
