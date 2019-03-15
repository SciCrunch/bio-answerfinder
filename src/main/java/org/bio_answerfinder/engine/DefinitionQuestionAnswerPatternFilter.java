package org.bio_answerfinder.engine;


import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.services.*;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * pattern based recognition of answers for definition questions like 'What is <QP>'?
 * <p>
 * Created by bozyurt on 11/3/17.
 */
public class DefinitionQuestionAnswerPatternFilter {


    public List<String> filter(String queryPhrase, List<String> candidateAnswerSentences) {
        List<String> filtered = new ArrayList<>();
        Pattern suchAsPattern = Pattern.compile("\\s+such\\s+as\\s+" + Pattern.quote(queryPhrase));
        Pattern alsoCalledPattern = Pattern.compile("\\s+also\\s+called\\s+" + Pattern.quote(queryPhrase));
        Pattern appositivePattern = Pattern.compile(Pattern.quote(queryPhrase) + "\\s*,\\s*an?\\s+\\w+\\s+");
        Pattern arePattern = Pattern.compile(Pattern.quote(queryPhrase) + "\\s+are\\s+\\w+\\s+");

        List<Pattern> patterns = new ArrayList<>(3);
        patterns.add(suchAsPattern);
        patterns.add(alsoCalledPattern);
        patterns.add(appositivePattern);
        patterns.add(arePattern);
        for (String cas : candidateAnswerSentences) {
            int idx = cas.toLowerCase().indexOf(queryPhrase.toLowerCase());
            if (idx == -1) {
                continue;
            }
            boolean matched = false;
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(cas);
                if (matcher.find()) {
                    filtered.add(cas);
                    matched = true;
                    break;
                }
            }
            if (matched) {
                continue;
            }

            int offset = idx + queryPhrase.length();
            if (offset < cas.length()) {
                char[] carr = cas.toCharArray();
                offset = skipWSOrSpecial(carr, offset);
                if (offset < cas.length()) {
                    if (checkIsPattern(offset, carr)) {
                        filtered.add(cas);
                    }
                }
            }
        }
        return filtered;
    }

    /**
     * is a|an <AP>
     *
     * @param offset
     * @param carr
     * @return
     */
    boolean checkIsPattern(int offset, char[] carr) {
        int newOffset = startsWith(carr, offset, "is", true);
        if (newOffset == -1) {
            return false;
        }
        newOffset = skipWS(carr, newOffset);
        if (newOffset >= carr.length) {
            return false;
        }
        int newOffset2 = startsWith(carr, newOffset, "a", true);
        if (newOffset2 == -1) {
            newOffset2 = startsWith(carr, newOffset, "an", true);
        }
        if (newOffset2 == 1) {
            return false;
        }
        return true;
    }


    boolean checkAppositivePattern1(int offset, char[] carr) {
        int newOffset = startsWith(carr, offset, "(", true);
        if (newOffset == -1) {
            return false;
        }

        return true;
    }

    public static int skipWS(char[] carr, int offset) {
        for (int i = offset; i < carr.length; i++) {
            if (Character.isWhitespace(carr[i])) {
                offset++;
            } else {
                break;
            }
        }
        return offset;
    }

    public static int skipWSOrSpecial(char[] carr, int offset) {
        for (int i = offset; i < carr.length; i++) {
            if (Character.isWhitespace(carr[i]) || carr[i] == '\t' || !Character.isLetterOrDigit(carr[i])) {
                offset++;
            } else {
                break;
            }
        }
        return offset;
    }

    public static int startsWith(char[] carr, int offset, String refStr, boolean wordBoundary) {
        if ((offset + refStr.length()) >= carr.length) {
            return -1;
        }
        char[] refCarr = refStr.toCharArray();
        int i;
        for (i = offset; i < offset + refCarr.length; i++) {
            if (carr[i] != refCarr[i - offset]) {
                return -1;
            }
        }
        if (wordBoundary) {
            if (i < carr.length && Character.isWhitespace(carr[i]) || carr[i] == '.') {
                return i;
            }
        } else {
            return i;
        }

        return -1;
    }


    public static List<String> extractSentences(PubMedDoc pmd, ISentencePipeline sentencePipeline) throws Exception {
        return QAEngineBase.extractSentences(pmd, sentencePipeline);
    }

    public static void testFilter() throws Exception {
        QuestionParser questionParser;
        ElasticSearchService ess;
        NominalizationService nominalizationService;
        TObjectFloatHashMap<String> vocabulary;
        ISentencePipeline sentencePipeline = new StanfordSentencePipeline();

        nominalizationService = new NominalizationService();
        questionParser = new QuestionParser();
        questionParser.initialize();
        ess = new ElasticSearchService();
        ess.setUseCache(true);
        nominalizationService.initialize();
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();

        String query = "What is the Barr body?";
        query = "What is TOPAZ1?";
        query = "Which are the Yamanaka factors?";

        List<String> questions = FileUtils.loadSentences("/tmp/definition_questions.txt");

        for(String question : questions) {
            List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", question);
            List<String> qpList = DefinitionQuestionDetector.isDefinitionQuestion(dataRecords);
            Assertion.assertNotNull(qpList);
            SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
            SearchQuery sq = sqGen.generatePubmedQuery(vocabulary);
            System.out.println("Query:" + sq.build());
            List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(sq, 2000);
            DefinitionQuestionAnswerPatternFilter filter = new DefinitionQuestionAnswerPatternFilter();

            System.out.println("Q:" + question);
            FileUtils.appendLine("/tmp/definition_question_filter_results.txt","Q:" + question);
            for (PubMedDoc pmd : pubMedDocs) {
                List<String> candidateSentences = extractSentences(pmd, sentencePipeline);
                List<String> filtered = filter.filter(qpList.get(0), candidateSentences);
                if (filtered != null && !filtered.isEmpty()) {
                    for (String sentence : filtered) {
                        System.out.println("\t" + sentence);
                        FileUtils.appendLine("/tmp/definition_question_filter_results.txt","\t" + sentence);
                    }
                }
            }
            System.out.println("------------------------");
        }
    }

    public static void main(String[] args) throws Exception {
        testFilter();
    }

    static void testDriver() {
        DefinitionQuestionAnswerPatternFilter filter = new DefinitionQuestionAnswerPatternFilter();
        List<String> candidates = new ArrayList<>(2);
        candidates.add("Signal transducer and activator of transcription 3 (STAT3) is a transcription factor which in humans is encoded by the STAT3 gene.");
        candidates.add("Transcription factors such as STAT3 are noted .");
        candidates.add(" One of these, STAT3, a transcription factor, is used in this study.");
        List<String> filtered = filter.filter("STAT3", candidates);

        for (String f : filtered) {
            System.out.println(f);
        }
    }

}
