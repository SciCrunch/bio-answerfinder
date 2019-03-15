package org.bio_answerfinder.engine;


import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.common.*;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.engine.BERTRerankerServiceClient.RankedSentence;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.services.ElasticSearchService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.util.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 1/8/19.
 */
public class QAEngineDL extends QAEngineBase implements IQAEngine, Interceptor, Logger {
    int noDocs2Retrieve = 2000;
    GloveDBLookup gloveMan;
    ElasticSearchService ess;
    ILemmanizer lemmanizer;
    private LookupUtils2 lookupUtils;
    QuestionResultStatsCollector collector;
    BufferedWriter logOut;
    QAEngine1.QuestionResult current;


    public QAEngineDL() throws Exception {
        super();
        this.lemmanizer = SRLUtils.prepLemmanizer();
        lookupUtils = new LookupUtils2();
        collector = new QuestionResultStatsCollector();
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
        String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        // String HOME_DIR = System.getProperty("user.home");
        // String dbFile = HOME_DIR + "/medline_glove_v2.db";
        this.ess = new ElasticSearchService();
        ess.setUseCache(false);
        lookupUtils.initialize();
        gloveMan = GloveDBLookup.getInstance(dbFile);
    }

    @Override
    @SuppressWarnings("Duplicates")
    public List<AnswerSentence> answerQuestion(String query, int topN) throws Exception {
        List<AnswerSentence> answerSentences;
        List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", query);
        List<String> qpList = DefinitionQuestionDetector.isDefinitionQuestion(dataRecords);
        boolean definitionQuestion = qpList != null && !qpList.isEmpty();
        QuestionFocusDetector detector = new QuestionFocusDetector();
        QuestionFocusDetector.QuestionFocus questionFocus = detector.detectFocus(dataRecords);
        List<String> focusEntityTypes = getFocusEntityTypes(questionFocus);
        List<PubMedDoc> pubMedDocs = retrieveDocuments(query, dataRecords);
        String method = "wmd";
        if (definitionQuestion) {
            answerSentences = handleDefinitionAnswer(qpList, pubMedDocs);
            if (!answerSentences.isEmpty()) {
                showAnswers(query, answerSentences, topN);
                method = "definition";
            } else {
                answerSentences = handleViaWMD(dataRecords, pubMedDocs);
                showAnswers(query, answerSentences, topN);
            }
        } else if (!focusEntityTypes.isEmpty()) {
            answerSentences = handleQuestionWithFocusEntity(dataRecords, questionFocus, pubMedDocs);
            showAnswers(query, answerSentences, topN);
            method = "focus";
        } else {
            answerSentences = handleViaWMD(dataRecords, pubMedDocs);
            showAnswers(query, answerSentences, topN);
        }
        if (this.logOut != null) {
            this.current = prepareQuestionResult(query, answerSentences, topN, method);
        }

        normalizeAnswers(answerSentences);
        return answerSentences;
    }

    public static void normalizeAnswers(List<AnswerSentence> asList) {
        for(AnswerSentence as : asList) {
            String normalized =  as.getSentence().replaceAll("_", " ");
            if (!normalized.equals(as.getSentence())) {
               as.setSentence(normalized);
            }
        }
    }

    @SuppressWarnings("Duplicates")
    List<AnswerSentence> handleViaWMD(List<DataRecord> questionRecords, List<PubMedDoc> pubMedDocs) throws Exception {
        List<AnswerSentence> asList = new ArrayList<>();
        String questionContent = prepareQuestion4Glove(questionRecords);
        Map<String, WordVector> questionMap = prepareWordVectorMap(questionContent);
        for (PubMedDoc pmd : pubMedDocs) {
            List<String> sentences = prepareAbstract4Glove(pmd);
            int id = 1;
            for (String sentence : sentences) {
                Map<String, WordVector> sentenceMap = prepareWordVectorMap(sentence);
                float score = MathUtils.relaxedWMD(questionMap, sentenceMap, false);
                AnswerSentence as = new AnswerSentence(id, "user_question", pmd.getPmid(), score);
                as.setSentence(sentence);
                asList.add(as);
                id++;
            }
        }
        Collections.sort(asList, (o1, o2) -> Float.compare(o2.getScore(), o1.getScore()));

        // rerank
        asList = rerankViaBERT(extractQuestion(questionRecords), asList);
        return asList;
    }

    List<AnswerSentence> rerankViaBERT(String question, List<AnswerSentence> asList) {
        // only re-rank upto first 100 due to CPU cost
        BERTRerankerServiceClient client = new BERTRerankerServiceClient();
        try {
            Map<String, AnswerSentence> map = new LinkedHashMap<>();
            int len = Math.min(100, asList.size());
            List<String> candidates = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                AnswerSentence as = asList.get(i);
                map.put(as.getSentence(), as);
                candidates.add(as.getSentence());
            }
            List<RankedSentence> rerankedList = client.rerank(question, candidates);
            for (int i = 0; i < len; i++) {
                RankedSentence rs = rerankedList.get(i);
                AnswerSentence as = map.get(rs.getSentence());
                Assertion.assertNotNull(as);
                asList.set(i, as);
            }
            return asList;
        } catch (Throwable t) {
            t.printStackTrace();
            return asList;
        }
    }

    @SuppressWarnings("Duplicates")
    List<AnswerSentence> handleDefinitionAnswer(List<String> qpList, List<PubMedDoc> pubMedDocs) throws Exception {
        List<AnswerSentence> asList = new ArrayList<>();
        DefinitionQuestionAnswerPatternFilter filter = new DefinitionQuestionAnswerPatternFilter();
        int id = 1;
        for (PubMedDoc pmd : pubMedDocs) {
            List<String> candidateSentences = QAEngineBase.extractSentences(pmd, sentencePipeline);
            List<String> filtered = filter.filter(qpList.get(0), candidateSentences);
            if (filtered != null && !filtered.isEmpty()) {
                for (String sentence : filtered) {
                    AnswerSentence as = new AnswerSentence(id, "user_question", pmd.getPmid(), 1.0f);
                    as.setSentence(sentence);
                    asList.add(as);
                    id++;
                }
            }
        }
        return asList;
    }

    @SuppressWarnings("Duplicates")
    List<AnswerSentence> handleQuestionWithFocusEntity(List<DataRecord> questionRecords,
                                                       QuestionFocusDetector.QuestionFocus questionFocus,
                                                       List<PubMedDoc> pubMedDocs) throws Exception {
        FocusWordEntityTypeAnswerFilter filter = new FocusWordEntityTypeAnswerFilter(this.lookupUtils, this.sentencePipeline);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(questionRecords, nominalizationService, null, null);
        SearchQuery searchQuery = sqGen.generatePubmedQuery(vocabulary);
        List<ResultDoc> resultDocs = filter.filter(searchQuery, questionFocus, pubMedDocs);

        return rankSentences(questionRecords, resultDocs);
    }

    /**
     * @param query
     * @param dataRecords
     * @return
     * @throws Exception
     */
    @SuppressWarnings("Duplicates")
    List<PubMedDoc> retrieveDocuments(String query, List<DataRecord> dataRecords) throws Exception {
        SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
        SearchQuery sq;
        QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
        StringBuilder questionBuf = new StringBuilder();
        StringBuilder posTagsBuf = new StringBuilder();
        prepareQuestion4Prediction(dataRecords, questionBuf, posTagsBuf);
        List<String> selectedQueryTerms = client.getSelectedQueryTerms(questionBuf.toString().trim(),
                posTagsBuf.toString().trim());
        if (selectedQueryTerms != null && !selectedQueryTerms.isEmpty()) {
            sq = sqGen.generatePubmedQuery(vocabulary, selectedQueryTerms, SearchQueryOptions.ENSURE_INCLUSION);
        } else {
            sq = sqGen.generatePubmedQuery(vocabulary);
        }
        PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(sq, lemmanizer, lookupUtils);
        String keywordQuery = pqc.buildESQuery();
        System.out.println("ES Query:" + keywordQuery);
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, noDocs2Retrieve);
        List<PubMedDoc> prevPMDList = null;
        String prevKeywordQuery = null;
        if (pubMedDocs == null || pubMedDocs.isEmpty() || pubMedDocs.size() < 40) {
            if (pubMedDocs.size() > 0 && pubMedDocs.size() < 40) {
                prevPMDList = pubMedDocs;
                prevKeywordQuery = keywordQuery;
            }
            while (pqc.adjustQueryWithEntities()) {
                keywordQuery = pqc.buildESQuery();
                System.out.println("ES Query:" + keywordQuery);
                pubMedDocs = ess.retrieveDocuments(keywordQuery, noDocs2Retrieve);
                if (pubMedDocs != null && !pubMedDocs.isEmpty()) {
                    if (pubMedDocs.size() < 40) {
                        prevPMDList = pubMedDocs;
                        prevKeywordQuery = keywordQuery;
                    } else {
                        break;
                    }
                }
                if (keywordQuery.trim().length() == 0) {
                    break;
                }
            }
        }
        if (pubMedDocs == null || (pubMedDocs.size() > 1000 && prevPMDList != null && prevPMDList.size() > 10)) {
            if (prevKeywordQuery != null && !prevPMDList.isEmpty()) {
                System.out.println("using previous query with results " + prevKeywordQuery);
                pubMedDocs = prevPMDList;
                keywordQuery = prevKeywordQuery;
            } else {
                if (pubMedDocs == null) {
                    pubMedDocs = new ArrayList<>(0);
                }
            }
        }
        if (pubMedDocs.isEmpty()) {
            sq = sqGen.generatePubmedQuery(vocabulary);
            System.out.println("OR Query:" + sq.build());
            pubMedDocs = ess.retrieveDocuments(sq, noDocs2Retrieve);
            if (pubMedDocs == null) {
                pubMedDocs = new ArrayList<>(0);
            }
        }
        collector.setQid("").setQuestion(query).setQuery(keywordQuery).setNumCandidateResults(pubMedDocs.size());
        return pubMedDocs;
    }

    private void prepareQuestion4Prediction(List<DataRecord> dataRecords, StringBuilder questionBuf, StringBuilder posTagsBuf) throws ParseTreeManagerException {
        for (DataRecord dr : dataRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            List<String> posTags = ps.getPosTags();
            List<SpanPOS> spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);
            for (SpanPOS sp : spList) {
                questionBuf.append(sp.getToken()).append(' ');
                posTagsBuf.append(sp.getPosTag()).append(' ');
            }
        }
    }

    @SuppressWarnings("Duplicates")
    List<AnswerSentence> rankSentences(List<DataRecord> questionRecords, List<ResultDoc> filtered) throws Exception {
        List<AnswerSentence> asList = new ArrayList<>();
        String questionContent = prepareQuestion4Glove(questionRecords);
        Map<String, WordVector> questionMap = prepareWordVectorMap(questionContent);
        double maxRSScore = -1;
        for (ResultDoc rd : filtered) {
            for (ResultDoc.ResultSentence rs : rd.getSentences()) {
                if (rs.getScore() > maxRSScore) {
                    maxRSScore = rs.getScore();
                }
            }
        }

        int id = 1;
        for (ResultDoc rd : filtered) {
            for (ResultDoc.ResultSentence rs : rd.getSentences()) {
                String sentenceStr = GloveDataPreparer.prepSentence4Glove(rs.getSentence(), ngramSet, 4);
                if (sentenceStr.length() > 0) {
                    Map<String, WordVector> sentenceMap = prepareWordVectorMap(sentenceStr);
                    float score = MathUtils.relaxedWMD(questionMap, sentenceMap, false);
                    double combinedScore = 0.8 * score + (0.2 * (rs.getScore() / maxRSScore));
                    AnswerSentence as = new AnswerSentence(id, "user_question", rd.getPmid(), (float) combinedScore);
                    as.setSentence(rs.getSentence());
                    asList.add(as);
                    id++;
                }
            }
        }
        Collections.sort(asList, (o1, o2) -> Float.compare(o2.getScore(), o1.getScore()));
        // rerank
        asList = rerankViaBERT(extractQuestion(questionRecords), asList);
        return asList;
    }

    String prepareQuestion4Glove(List<DataRecord> questionRecords) {
        StringBuilder sb = new StringBuilder(100);
        for (DataRecord dr : questionRecords) {
            sb.append(dr.getSentences().get(0).getSentence()).append(' ');
        }
        String question = sb.toString().trim();
        return GloveDataPreparer.prepSentence4Glove(question, ngramSet, 4);
    }

    public static String extractQuestion(List<DataRecord> questionRecords) {
        StringBuilder sb = new StringBuilder(100);
        for (DataRecord dr : questionRecords) {
            sb.append(dr.getSentences().get(0).getSentence()).append(' ');
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("Duplicates")
    Map<String, WordVector> prepareWordVectorMap(String content4Glove) {
        Map<String, WordVector> map = new HashMap<>();
        String[] tokens = content4Glove.split("\\s+");
        for (String token : tokens) {
            boolean phrase = token.indexOf('_') != -1;
            float[] gloveVector = gloveMan.getGloveVector(token);
            String term = phrase ? token.replace('_', ' ') : token;
            if (!map.containsKey(term) && gloveVector != null && gloveVector.length != 0
                    && vocabulary.containsKey(term)) {
                float weight = vocabulary.get(term);
                map.put(term, new WordVector(weight, gloveVector, term));
            }
        }
        return map;
    }


    List<String> prepareAbstract4Glove(PubMedDoc pmd) throws Exception {
        List<String> sentences = extractSentences(pmd, sentencePipeline);
        List<String> gloveSentences = new ArrayList<>(sentences.size());
        for (String sentence : sentences) {
            String sentenceStr = GloveDataPreparer.prepSentence4Glove(sentence, ngramSet, 4);
            if (sentenceStr.length() > 0) {
                gloveSentences.add(sentenceStr);
            }
        }
        return gloveSentences;
    }

    @Override
    public QuestionResultStatsCollector getQuestionResultsCollector() {
        return this.collector;
    }

    @Override
    public void startLogging(String logFile) throws IOException {
        if (logFile != null) {
            this.logOut = FileUtils.newUTF8CharSetWriter(logFile);
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void logQuestionResults(String question, double MRR, int rank) throws IOException {
        if (this.logOut != null && current != null) {
            logOut.write("Question:" + current.getQuestion());
            logOut.newLine();
            logOut.write("Answer:");
            logOut.newLine();
            logOut.write(String.format("rank:%d MRR:%f method:%s", rank, MRR, current.getMethod()));
            logOut.newLine();
            int i = 1;
            for (AnswerSentence as : current.getTopNAnswers()) {
                logOut.write(GenUtils.formatText(i + ") " +
                        as.getSentence() + " [" + as.getScore() + "] PMID:" + as.getPmid(), 100));
                logOut.newLine();
                i++;
            }
            logOut.write("===========================");
            logOut.newLine();
            logOut.flush();
        }
    }

    QAEngine1.QuestionResult prepareQuestionResult(String question, List<AnswerSentence> answerSentences, int topN, String method) {
        int n = Math.min(topN, answerSentences.size());
        List<AnswerSentence> topNResults = answerSentences.subList(0, n);
        return new QAEngine1.QuestionResult(question, topNResults, method);
    }

    List<String> getFocusEntityTypes(QuestionFocusDetector.QuestionFocus questionFocus) {
        if (questionFocus == null) {
            return Collections.emptyList();
        }
        String focus = questionFocus.getFocusWord();
        return lookupUtils.getEntityType(focus);
    }

    @Override
    public void stopLogging() {
        FileUtils.close(this.logOut);
    }
}
