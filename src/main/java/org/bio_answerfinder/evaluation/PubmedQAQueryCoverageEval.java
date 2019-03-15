package org.bio_answerfinder.evaluation;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.SimpleSearchQueryGenerator;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.*;
import org.bio_answerfinder.common.QueryCoveragePerfUtil.Coverage;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.engine.*;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.services.ElasticSearchService;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.util.*;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 1/8/19.
 */
public class PubmedQAQueryCoverageEval {
    int noDocs2Retrieve = 2000;
    static String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
    ElasticSearchService ess;
    Map<String, QuestionRecord> qrMap = new HashMap<>();
    ILemmanizer lemmanizer;
    private LookupUtils2 lookupUtils;
    NominalizationService nominalizationService;
    TObjectFloatHashMap<String> vocabulary;
    QuestionParser questionParser;


    public PubmedQAQueryCoverageEval() throws Exception {
        this.ess = new ElasticSearchService();
        this.ess.setUseCache(true);
        this.lemmanizer = SRLUtils.prepLemmanizer();
        lookupUtils = new LookupUtils2();
        nominalizationService = new NominalizationService();
        questionParser = new QuestionParser();
    }

    public void initialize() throws Exception {
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
        lookupUtils.initialize();
        nominalizationService.initialize();
        questionParser.initialize();
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();

    }

    public void checkESSCoverage(String qscTestFile) throws Exception {
        List<String> questions = loadQSCAnnotFile(qscTestFile);
        List<Coverage> coverageList = new ArrayList<>();
        int noCoverageCount = 0;
        for (String question : questions) {
            QuestionRecord qr = find(question);
            Assertion.assertNotNull(qr);
            Set<String> gsPMIDSet = new HashSet<>();
            for (String docURL : qr.getAnswer().getDocuments()) {
                String pmid = QuestionRecord.Answer.getPMID(docURL);
                gsPMIDSet.add(pmid);
            }
            int count = 0;
            for (String pmid : gsPMIDSet) {
                PubMedDoc pubMedDoc = ess.retrieveDocumentbyPMID(pmid);
                if (pubMedDoc != null) {
                    count++;
                }
            }

            Coverage coverage = new Coverage(qr.getQuestion(), qr.getId(), count, gsPMIDSet.size());
            coverageList.add(coverage);
            if (coverage.getCount() == 0) {
                noCoverageCount++;
            }
            System.out.println(coverage.toString());
        }
        System.out.println("noCoverageCount:" + noCoverageCount);
        double[] coverageFracArr = new double[coverageList.size()];
        for (int i = 0; i < coverageList.size(); i++) {
            Coverage coverage = coverageList.get(i);
            coverageFracArr[i] = coverage.getCount() / (double) coverage.getGsCount();
        }
        System.out.println("Coverage Fraction (mean): " + MathUtils.mean(coverageFracArr));
        System.out.println("Coverage Fraction (std) : " + MathUtils.std(coverageFracArr));
        saveCoverageDetails(coverageList, "/tmp/ess_coverage_details.json");
    }

    public void evaluate(String qscTestFile, boolean dryRun, SearchQueryOptions searchQueryOptions, boolean simple) throws Exception {
        List<String> questions = loadQSCAnnotFile(qscTestFile);
        List<Coverage> coverageList = new ArrayList<>();
        int noCoverageCount = 0;
        int noZeroCandidates = 0;
        for (String question : questions) {
            QuestionRecord qr = find(question);
            Assertion.assertNotNull(qr);
            if (!dryRun) {
                List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", question);
                List<PubMedDoc> pubMedDocs;
                if (simple) {
                    pubMedDocs = retrieveDocumentsSimple(dataRecords);
                } else if (searchQueryOptions == SearchQueryOptions.HYBRID_FILTER) {
                    pubMedDocs = retrieveDocumentsHybrid(dataRecords, searchQueryOptions);
                } else {
                    pubMedDocs = retrieveDocuments(question, dataRecords, searchQueryOptions);
                }
                Coverage coverage = QueryCoveragePerfUtil.showCoverage4Question(qr, pubMedDocs);
                coverageList.add(coverage);
                if (coverage.getCount() == 0) {
                    noCoverageCount++;
                    if (pubMedDocs == null || pubMedDocs.isEmpty()) {
                        noZeroCandidates++;
                    }
                }
            }
        }
        System.out.println("noCoverageCount:" + noCoverageCount);
        System.out.println("noZeroCandidates:" + noZeroCandidates);
        double[] coverageFracArr = new double[coverageList.size()];
        for (int i = 0; i < coverageList.size(); i++) {
            Coverage coverage = coverageList.get(i);
            coverageFracArr[i] = coverage.getCount() / (double) coverage.getGsCount();
        }
        System.out.println("Coverage Fraction (mean): " + MathUtils.mean(coverageFracArr));
        System.out.println("Coverage Fraction (std) : " + MathUtils.std(coverageFracArr));
        saveCoverageDetails(coverageList, "/tmp/coverage_details.json");

    }

    public static void saveCoverageDetails(List<Coverage> coverageList, String outJsonFile) throws IOException {
        JSONArray jsArr = new JSONArray();
        for (Coverage coverage : coverageList) {
            jsArr.put(coverage.toJSON());
        }
        String jsonStr = jsArr.toString(2);
        FileUtils.saveText(jsonStr, outJsonFile, CharSetEncoding.UTF8);
        System.out.println("saved " + outJsonFile);

    }

    List<PubMedDoc> retrieveDocumentsSimple(List<DataRecord> dataRecords) throws Exception {
        SimpleSearchQueryGenerator sqGen = new SimpleSearchQueryGenerator(dataRecords);
        QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
        StringBuilder questionBuf = new StringBuilder();
        StringBuilder posTagsBuf = new StringBuilder();
        prepareQuestion4Prediction(dataRecords, questionBuf, posTagsBuf);
        List<String> selectedQueryTerms = client.getSelectedQueryTerms(questionBuf.toString().trim(), posTagsBuf.toString().trim());
        SearchQuery sq = sqGen.generatePubmedQuery(vocabulary, selectedQueryTerms);
        PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(sq, lemmanizer, lookupUtils);
        String keywordQuery = pqc.buildESQuery();
        System.out.println("ES Query:" + keywordQuery);
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, noDocs2Retrieve);
        return pubMedDocs;
    }


    @SuppressWarnings("Duplicates")
    List<PubMedDoc> retrieveDocuments(String query, List<DataRecord> dataRecords, SearchQueryOptions searchQueryOptions) throws Exception {
        SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
        SearchQuery sq;
        if (searchQueryOptions != SearchQueryOptions.NONE) {
            QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
            StringBuilder questionBuf = new StringBuilder();
            StringBuilder posTagsBuf = new StringBuilder();
            prepareQuestion4Prediction(dataRecords, questionBuf, posTagsBuf);
            List<String> selectedQueryTerms = client.getSelectedQueryTerms(questionBuf.toString().trim(), posTagsBuf.toString().trim());
            if (selectedQueryTerms != null && !selectedQueryTerms.isEmpty()) {
                sq = sqGen.generatePubmedQuery(vocabulary, selectedQueryTerms, searchQueryOptions);
            } else {
                sq = sqGen.generatePubmedQuery(vocabulary);
            }
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
        return pubMedDocs;
    }

    @SuppressWarnings("Duplicates")
    List<PubMedDoc> retrieveDocumentsHybrid(List<DataRecord> dataRecords, SearchQueryOptions searchQueryOptions) throws Exception {
        Assertion.assertTrue(searchQueryOptions == SearchQueryOptions.HYBRID_FILTER);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
        SimpleSearchQueryGenerator sqGen2 = new SimpleSearchQueryGenerator(dataRecords);
        SearchQuery sq;
        SearchQuery sq2 = null;
        QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
        StringBuilder questionBuf = new StringBuilder();
        StringBuilder posTagsBuf = new StringBuilder();
        prepareQuestion4Prediction(dataRecords, questionBuf, posTagsBuf);
        List<String> selectedQueryTerms = client.getSelectedQueryTerms(questionBuf.toString().trim(), posTagsBuf.toString().trim());
        if (selectedQueryTerms != null && !selectedQueryTerms.isEmpty()) {
            sq = sqGen.generatePubmedQuery(vocabulary, selectedQueryTerms, SearchQueryOptions.FILTER);
            sq2 = sqGen2.generatePubmedQuery(vocabulary, selectedQueryTerms);
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
        if (sq2 != null) {
            PubmedQueryConstructor2 pqc2 = new PubmedQueryConstructor2(sq2, lemmanizer, lookupUtils);
            String keywordQuery2 = pqc2.buildESQuery();
            System.out.println("ES Query:" + keywordQuery2);
            List<PubMedDoc> pmdList = ess.retrieveDocuments(keywordQuery, noDocs2Retrieve);
            // add all the new abstract retrieved by the DNN based keyword selector
            if (pmdList != null && !pmdList.isEmpty()) {
                Map<String, PubMedDoc> pmidMap = new HashMap<>();
                for (PubMedDoc pmd : pubMedDocs) {
                    pmidMap.put(pmd.getPmid(), pmd);
                }
                int count = 0;
                for (PubMedDoc pmd : pubMedDocs) {
                    if (!pmidMap.containsKey(pmd.getPmid())) {
                        pubMedDocs.add(pmd);
                        count++;
                    }
                }
                if (count > 0) {
                    System.out.println(String.format("*** Hybrid added %d new abstracts", count));
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

    /**
     * read questions from question keyword selector classifier training/testing file
     *
     * @param qscAnnotFile
     * @return
     */
    public static List<String> loadQSCAnnotFile(String qscAnnotFile) throws IOException {
        List<String> questions = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(qscAnnotFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                questions.add(line);
                in.readLine(); // skip labels
            }
        } finally {
            FileUtils.close(in);
        }
        return questions;
    }


    QuestionRecord find(String question) {
        QuestionRecord theQR = qrMap.get(StringUtils.stripWS(question));
        if (theQR != null) {
            return theQR;
        }
        for (QuestionRecord qr : qrMap.values()) {
            String q = normalizeQuestion(qr.getQuestion());
            if (q.startsWith(question)) {
                return qr;
            }
        }
        return null;
    }

    static String normalizeQuestion(String question) {
        String[] tokens = question.split("\\s+");
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];
            if (StringUtils.isAllSpecial(token)) {
                i++;
                continue;
            }
            buf.append(token).append(' ');
            i++;
        }
        return buf.toString().trim();
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String qscTestFile = HOME_DIR + "/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc/qsc_set_test.txt";
        PubmedQAQueryCoverageEval eval = new PubmedQAQueryCoverageEval();

        eval.initialize();

        //eval.evaluate(qscTestFile, false, SearchQueryOptions.HYBRID_FILTER, false);
        eval.evaluate(qscTestFile, false, SearchQueryOptions.NONE, false);

        //eval.checkESSCoverage(qscTestFile);
    }

}
