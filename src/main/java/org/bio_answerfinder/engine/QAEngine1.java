package org.bio_answerfinder.engine;


import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.engine.query.SearchQueryGenerator;
import org.bio_answerfinder.services.ElasticSearchService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.common.*;
import org.bio_answerfinder.engine.QuestionFocusDetector.QuestionFocus;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.util.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 1/3/18.
 */
public class QAEngine1 extends QAEngineBase implements IQAEngine, Interceptor, Logger {
    int noDocs2Retrieve = 2000;
    //GloVeVectorManager gloveMan;
    GloveDBLookup gloveMan;
    ElasticSearchService ess;
    ILemmanizer lemmanizer;
    private LookupUtils2 lookupUtils;
    QuestionResultStatsCollector collector;
    BufferedWriter logOut;
    QuestionResult current;


    public QAEngine1() throws Exception {
        super();
        this.lemmanizer = SRLUtils.prepLemmanizer();
        lookupUtils = new LookupUtils2();
        collector = new QuestionResultStatsCollector();
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        this.ess = new ElasticSearchService();
        ess.setUseCache(true);
        lookupUtils.initialize();
        /*
        this.gloveMan = new GloVeVectorManager();
        String gloveTextFile = HOME_DIR + "/data/medline/pmc_2017_abstracts_glove_vectors.txt";
        this.gloveMan.cacheFromTextFile(gloveTextFile);
        */
        Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
        String dbFile =  FileUtils.adjustPath(props.getProperty("glove.db.file"));
        Assertion.assertExistingPath(dbFile, dbFile);
        gloveMan = GloveDBLookup.getInstance(dbFile);
    }


    @Override
    public QuestionResultStatsCollector getQuestionResultsCollector() {
        return this.collector;
    }

    @Override
    public List<AnswerSentence> answerQuestion(String query, int topN, Map<String, String> options) throws Exception {
        List<AnswerSentence> answerSentences;
        List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", query);
        List<String> qpList = DefinitionQuestionDetector.isDefinitionQuestion(dataRecords);
        boolean definitionQuestion = qpList != null && !qpList.isEmpty();
        QuestionFocusDetector detector = new QuestionFocusDetector();
        QuestionFocus questionFocus = detector.detectFocus(dataRecords);
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

        return answerSentences;
    }

    QuestionResult prepareQuestionResult(String question, List<AnswerSentence> answerSentences, int topN, String method) {
        int n = Math.min(topN, answerSentences.size());
        List<AnswerSentence> topNResults = answerSentences.subList(0, n);
        return new QuestionResult(question, topNResults, method);
    }

    List<String> getFocusEntityTypes(QuestionFocus questionFocus) {
        if (questionFocus == null) {
            return Collections.emptyList();
        }
        String focus = questionFocus.getFocusWord();
        return lookupUtils.getEntityType(focus);
    }

    @SuppressWarnings("Duplicates")
    List<AnswerSentence> handleQuestionWithFocusEntity(List<DataRecord> questionRecords,
                                                       QuestionFocus questionFocus,
                                                       List<PubMedDoc> pubMedDocs) throws Exception {
        FocusWordEntityTypeAnswerFilter filter = new FocusWordEntityTypeAnswerFilter(this.lookupUtils, this.sentencePipeline);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(questionRecords, nominalizationService, null, null);
        SearchQuery searchQuery = sqGen.generatePubmedQuery(vocabulary);
        List<ResultDoc> resultDocs = filter.filter(searchQuery, questionFocus, pubMedDocs);

        return rankSentences(questionRecords, resultDocs);
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
        return asList;
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
                    double combinedScore = 0.8 * score + (0.2 *(rs.getScore() / maxRSScore));
                    AnswerSentence as = new AnswerSentence(id, "user_question", rd.getPmid(), (float) combinedScore);
                    as.setSentence(rs.getSentence());
                    asList.add(as);
                    id++;
                }
            }
        }
        Collections.sort(asList, (o1, o2) -> Float.compare(o2.getScore(), o1.getScore()));
        return asList;
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
    Map<String, WordVector> prepareWordVectorMap(String content4Glove) {
        Map<String, WordVector> map = new HashMap<>();
        String[] tokens = content4Glove.split("\\s+");
        for (String token : tokens) {

            boolean phrase = token.indexOf('_') != -1;
            //float[] gloveVector = gloveMan.getTermGloveVector(token);
            float[] gloveVector = gloveMan.getGloveVector(token);
            String term = phrase ? token.replace('_', ' ') : token;
            if (!map.containsKey(term) && gloveVector != null && vocabulary.containsKey(term)) {
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

    String prepareQuestion4Glove(List<DataRecord> questionRecords) {
        StringBuilder sb = new StringBuilder(100);
        for (DataRecord dr : questionRecords) {
            sb.append(dr.getSentences().get(0).getSentence()).append(' ');
        }
        String question = sb.toString().trim();
        return GloveDataPreparer.prepSentence4Glove(question, ngramSet, 4);
    }

    @SuppressWarnings("Duplicates")
    List<PubMedDoc> retrieveDocuments(String query, List<DataRecord> dataRecords) throws Exception {
        SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
        SearchQuery sq = sqGen.generatePubmedQuery(vocabulary);
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

    @Override
    public void startLogging(String logFile) throws IOException {
        if (logFile != null) {
            this.logOut = FileUtils.newUTF8CharSetWriter(logFile);
        }
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void logQuestionResults(String question, double MRR, int rank) throws IOException {
        if (this.logOut != null && current != null) {
            logOut.write("Question:" + current.getQuestion());
            logOut.newLine();
            logOut.write("Answer:");
            logOut.newLine();
            logOut.write(String.format("rank:%d MRR:%f method:%s", rank, MRR, current.getMethod()));
            logOut.newLine();
            int i = 1;
            for(AnswerSentence as : current.getTopNAnswers()) {
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

    @Override
    public void stopLogging() {
        FileUtils.close(this.logOut);
    }


    public static class QuestionResult {
        String question;
        String method;
        List<AnswerSentence> topNAnswers;

        public QuestionResult(String question, List<AnswerSentence> topNAnswers, String method) {
            this.question = question;
            this.topNAnswers = topNAnswers;
            this.method = method;
        }

        public String getQuestion() {
            return question;
        }

        public List<AnswerSentence> getTopNAnswers() {
            return topNAnswers;
        }

        public String getMethod() {
            return method;
        }
    }
}
