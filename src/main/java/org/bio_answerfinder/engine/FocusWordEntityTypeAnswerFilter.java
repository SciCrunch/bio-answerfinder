package org.bio_answerfinder.engine;


import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.services.ISentencePipeline;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.services.SearchResultAnnotationCache;
import org.bio_answerfinder.services.StanfordSentencePipeline;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.common.PubmedQueryConstructor2;
import org.bio_answerfinder.common.ResultDoc;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.sentence.TokenInfo;
import org.bio_answerfinder.util.*;

import java.io.IOException;
import java.util.*;


/**
 * Created by bozyurt on 11/22/17.
 */
public class FocusWordEntityTypeAnswerFilter {
    StopWordsHandler stopWordsHandler;
    SearchResultAnnotationCache cache;
    LookupUtils2 lookupUtils;
    ISentencePipeline sentencePipeline;


    public FocusWordEntityTypeAnswerFilter() throws Exception {
        stopWordsHandler = new StopWordsHandler();
        stopWordsHandler.initialize();
        this.cache = null;
        lookupUtils = new LookupUtils2();
        lookupUtils.initialize();
        sentencePipeline = new StanfordSentencePipeline();
    }

    public FocusWordEntityTypeAnswerFilter(LookupUtils2 lookupUtils, ISentencePipeline sentencePipeline) throws Exception {
        this.lookupUtils = lookupUtils;
        this.sentencePipeline = sentencePipeline;
    }

    public LookupUtils2 getLookupUtils() {
        return lookupUtils;
    }

    public List<String> getFocusEntityTypes(QuestionFocusDetector.QuestionFocus questionFocus) {
        String focus = questionFocus.getFocusWord();
        return lookupUtils.getEntityType(focus);
    }

    public List<ResultDoc> filter(QuestionFocusDetector.QuestionFocus questionFocus, List<PubMedDoc> results) throws Exception {
        List<ResultDoc> filtered = new ArrayList<>();
        String focus = questionFocus.getFocusWord();
        List<String> entityTypes = lookupUtils.getFocusEntityType(focus);
        Assertion.assertTrue(!entityTypes.isEmpty());
        Set<String> refEntityTypeSet = new HashSet<>(entityTypes);
        if (refEntityTypeSet.size() > 1) {
            refEntityTypeSet.remove("molecular entity");
        }
        for (PubMedDoc pmd : results) {
            List<String> sentences = QAEngineBase.extractSentences(pmd, sentencePipeline);
            // AnnotatedAbstract aa = cache.getAnnnotatedAbstract(pmd.getPmid());
            ResultDoc rd = new ResultDoc(pmd.getPmid(), (float) pmd.getScore());
            for (String sentence : sentences) {
                String[] tokens = sentence.split("\\s+");
                for (String token : tokens) {
                    if (StringUtils.isAllSpecial(token) || StopWords.isStopWord(token.toLowerCase()) || stopWordsHandler.isStopWord(token)) {
                        continue;
                    }
                    entityTypes = lookupUtils.getEntityType(token);
                    if (!entityTypes.isEmpty() && containsAny(entityTypes, refEntityTypeSet)) {
                        rd.addSentence(new ResultDoc.ResultSentence(sentence, 0, 1));
                        break;
                    }
                }
            }
            if (!rd.getSentences().isEmpty()) {
                filtered.add(rd);
            }
        }

        return filtered;
    }

    public List<ResultDoc> filter(SearchQuery sq, QuestionFocusDetector.QuestionFocus questionFocus, List<PubMedDoc> results) throws Exception {
        Map<String, List<String>> queryEntitiesMap = findEntities(sq);
        List<ResultDoc> filtered = new ArrayList<>();
        String focus = questionFocus.getFocusWord();
        List<String> entityTypes = lookupUtils.getFocusEntityType(focus);
        Assertion.assertTrue(!entityTypes.isEmpty());
        Set<String> refEntityTypeSet = new HashSet<>(entityTypes);
        for (PubMedDoc pmd : results) {
            List<String> sentences = QAEngineBase.extractSentences(pmd, sentencePipeline);
            ResultDoc rd = new ResultDoc(pmd.getPmid(), (float) pmd.getScore());
            for (String sentence : sentences) {
                Map<String, List<String>> entitiesInSentence = findEntitiesInSentence(sentence);
                int numOfQuestionEntities = 0;
                int numOfFocusEntities = 0;
                for (String entity : entitiesInSentence.keySet()) {
                    if (queryEntitiesMap.containsKey(entity)) {
                        numOfQuestionEntities++;
                    } else {
                        entityTypes = entitiesInSentence.get(entity);
                        if (containsAny(entityTypes, refEntityTypeSet)) {
                            numOfFocusEntities++;
                        }
                    }
                }
                double score = 0;
                if (numOfFocusEntities > 0) {
                    if (queryEntitiesMap.isEmpty()) {
                        score = 1.0;
                    } else {
                        score = numOfQuestionEntities / (double) queryEntitiesMap.size() + 1.0;
                    }
                    rd.addSentence(new ResultDoc.ResultSentence(sentence, (float) score, numOfFocusEntities));
                }

            }
            if (!rd.getSentences().isEmpty()) {
                filtered.add(rd);
            }

        }
        return filtered;
    }

    public Map<String, List<String>> findEntitiesInSentence(String sentence) throws IOException {
        Map<String, List<String>> map = new HashMap<>(11);
        List<TokenInfo> tiList = NERUtils.toTokens(sentence);
        int len = tiList.size();
        int i = 0;
        StringBuilder sb = new StringBuilder(100);
        while (i < len) {
            String token = tiList.get(i).getTokValue();
            if (StringUtils.isAllSpecial(token) || StopWords.isStopWord(token.toLowerCase())) {
                i++;
                continue;
            }
            sb.setLength(0);
            int theIdx = -1;
            String thePhrase = null;
            List<String> entityTypes = null;
            for (int j = i; j < len; j++) {
                TokenInfo ti = tiList.get(j);
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(ti.getTokValue());
                String phrase = sb.toString();
                List<String> etList = check4Entities(phrase);
                if (etList == null || etList.isEmpty()) {
                    break;
                } else {
                    thePhrase = phrase;
                    entityTypes = etList;
                    theIdx = j;
                }
            }
            if (thePhrase != null) {
                map.put(thePhrase, entityTypes);
                i = theIdx + 1;
            } else {
                i++;
            }
        }
        List<String> keys = new ArrayList<>(map.keySet());
        for (String key : keys) {
            if (PubmedQueryConstructor2.entityStopWords.contains(key.toLowerCase())) {
                map.remove(key);
            }
        }

        return map;
    }

    List<String> check4Entities(String phrase) {
        if (phrase.startsWith("protein")) {
            int idx = phrase.indexOf(' ');
            if (idx != -1) {
                String suffix = phrase.substring(idx + 1).trim();
                List<String> entityTypes = lookupUtils.getEntityType(suffix);
                if (!entityTypes.isEmpty()) {
                    return entityTypes;
                } else if (suffix.indexOf(' ') == -1) {
                    entityTypes.add("protein");
                    return entityTypes;
                }
            }
        }
        List<String> entityTypes = lookupUtils.getEntityType(phrase);
        if (entityTypes == null || entityTypes.isEmpty()) {
            entityTypes = lookupUtils.getEntityType(phrase.toLowerCase());
        }
        return entityTypes;
    }


    public Map<String, List<String>> findEntities(SearchQuery searchQuery) {
        Map<String, List<String>> map = new HashMap<>();
        Set<String> diseaseSet = new HashSet<>();
        Set<String> proteinSet = new HashSet<>();
        Set<String> geneSet = new HashSet<>();
        for (SearchQuery.QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
                if (PubmedQueryConstructor2.entityStopWords.contains(st.getTerm())) {
                    continue;
                }
                List<String> entityTypes = lookupUtils.getEntityType(st.getTerm());
                if (!entityTypes.isEmpty()) {
                    if (!map.containsKey(st.getTerm())) {
                        map.put(st.getTerm(), entityTypes);
                    }
                }

                if (st.isPhrase()) {
                    int idx = st.getTerm().lastIndexOf(' ');
                    if (idx != -1) {
                        String prefix = st.getTerm().substring(0, idx).trim();
                        String term = st.getTerm();
                        if (term.endsWith(" disease") || term.endsWith(" disorder") || term.endsWith("syndrome")) {
                            diseaseSet.add(term);
                            diseaseSet.add(prefix);
                            if (prefix.endsWith("'s")) {
                                prefix = prefix.substring(0, prefix.length() - 2).trim();
                                diseaseSet.add(prefix);
                            }
                        } else if (term.endsWith("protein") || term.endsWith("proteins")) {
                            proteinSet.add(term);
                            proteinSet.add(prefix);
                        } else if (term.startsWith("protein")) {
                            String suffix = term.substring(term.indexOf(' ') + 1).trim();
                            proteinSet.add(term);
                            proteinSet.add(suffix);
                        } else if (term.endsWith("gene") || term.endsWith("genes")) {
                            geneSet.add(term);
                            geneSet.add(prefix);
                        }
                    }
                }
            }
        }
        for (SearchQuery.QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
                if (st.getEntityTypes() == null) {
                    if (geneSet.contains(st.getTerm())) {
                        addEntityType(st.getTerm(), map, "gene");
                    } else if (proteinSet.contains(st.getTerm())) {
                        addEntityType(st.getTerm(), map, "protein");
                    } else if (diseaseSet.contains(st.getTerm())) {
                        addEntityType(st.getTerm(), map, "disease");
                    }
                }
            }
        }
        return map;
    }

    public static void addEntityType(String key, Map<String, List<String>> map, String entityType) {
        List<String> entityTypes = map.get(key);
        if (entityTypes == null) {
            entityTypes = new ArrayList<>(1);
            map.put(key, entityTypes);
        }
        entityTypes.add(entityType);
    }

    public static boolean containsAny(List<String> list, Set<String> refSet) {
        for (String s : list) {
            if (refSet.contains(s)) {
                return true;
            }
        }
        return false;
    }


    public static class StopWordsHandler {
        Set<String> stopwordSet = new HashSet<>();

        public StopWordsHandler() {
        }

        public void initialize() throws IOException {
            Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
            String stopwordsFile = props.getProperty("annotation.stopwords.file");
            if (stopwordsFile == null) {
                System.err.println("no property annotation.stopwords.file");
                return;
            }
            String[] lines = FileUtils.readLines(stopwordsFile, true, CharSetEncoding.UTF8);
            for (String line : lines) {
                String[] tokens = line.split("\\s+");
                tokens = Arrays.copyOf(tokens, tokens.length - 1);
                String term = GenUtils.join(tokens, " ");
                stopwordSet.add(term);
            }
        }

        public boolean isStopWord(String phrase) {
            return stopwordSet.contains(phrase);
        }

    }

    public static void testQuestionEntities() throws Exception {
        QAParserRetrieverHandler handler = null;
        try {
            FocusWordEntityTypeAnswerFilter filter = new FocusWordEntityTypeAnswerFilter();
            handler = new QAParserRetrieverHandler(filter.getLookupUtils());
            handler.initialize();
            String question = "Which is the protein that is encoded by the gene GLT8D1 ?";
            List<DataRecord> dataRecords = handler.parseQuestion(question);
            SearchQuery sq = handler.generateSearchQuery(dataRecords);

            Map<String, List<String>> map = filter.findEntities(sq);
            System.out.println(map);
        } finally {
            if (handler != null) {
                handler.shutdown();
            }
        }

    }

    public static void testAnswerSentences() throws Exception {
        String aSentence = "The GLT8D1 gene codes for the protein named glycosyltransferase 8 domain containing 1.";
        FocusWordEntityTypeAnswerFilter filter = new FocusWordEntityTypeAnswerFilter();
        Map<String, List<String>> map = filter.findEntitiesInSentence(aSentence);
        System.out.println(map);
    }

    public static void main(String[] args) throws Exception {
        // testQuestionEntities();
        // testAnswerSentences();
        testDriver();
    }

    public static void testDriver() throws Exception {
        QAParserRetrieverHandler handler = null;

        List<String> filteredQuestions = QAParserRetrieverHandler.filterQuestionsWithKnownFocusEntityTypes();
        System.out.println("# of questions with known focus entity types:" + filteredQuestions.size());
        FocusWordEntityTypeAnswerFilter filter;
        QuestionFocusDetector detector = new QuestionFocusDetector();
        try {
            filter = new FocusWordEntityTypeAnswerFilter();
            handler = new QAParserRetrieverHandler(filter.getLookupUtils());
            handler.initialize();
            // String question = "Which is the protein that is encoded by the gene GLT8D1 ?";
            for (String question : filteredQuestions) {
                List<DataRecord> dataRecords = handler.parseQuestion(question);
                QuestionFocusDetector.QuestionFocus questionFocus = detector.detectFocus(dataRecords);
                List<String> focusEntityTypes = filter.getFocusEntityTypes(questionFocus);
                if (focusEntityTypes.isEmpty()) {
                    continue;
                }

                System.out.println("Q:" + question);
                String ans = GenUtils.askQuestion("Filter (y/[n])?");
                if (ans.equalsIgnoreCase("y")) {
                    SearchQuery sq = handler.generateSearchQuery(dataRecords);
                    List<PubMedDoc> pubMedDocs = handler.retrieveResults(sq, 2000);
                    sq = handler.generateSearchQuery(dataRecords);
                    List<ResultDoc> filtered = filter.filter(sq, questionFocus, pubMedDocs);
                    int count = 0;
                    for (ResultDoc rd : filtered) {
                        for (ResultDoc.ResultSentence rs : rd.getSentences()) {
                            System.out.println(rs.getSentence() + " (" + rd.getPmid() + ")" + " [" + rs.getScore() + "]");
                            count++;
                        }
                        if (count > 10) {
                            System.out.println("...");
                            break;
                        }
                        System.out.println();
                    }
                } else if (ans.equalsIgnoreCase("q")) {
                    break;
                }
            }
        } finally {
            handler.shutdown();

        }
    }
}
