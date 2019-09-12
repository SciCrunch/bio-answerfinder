package org.bio_answerfinder.engine;

import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.PubmedQueryConstructor2;
import org.bio_answerfinder.common.QueryCoveragePerfUtil;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.engine.query.QuestionKeywordSelectionServiceClient;
import org.bio_answerfinder.engine.query.SearchQueryGenerator;
import org.bio_answerfinder.engine.query.SearchQueryOptions;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.services.ElasticSearchService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.StringUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Not thread safe
 * <p>
 * Created by bozyurt on 4/16/19.
 */
public class QAParserRetrieverService extends QAParserRetrieverHandlerBase {
    ElasticSearchService ess;
    LookupUtils2 lookupUtils;
    SearchQueryOptions searchQueryOptions = SearchQueryOptions.NONE;
    Map<String, QuestionQueryRec> cache = new HashMap<>();
    static String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
    Map<String, QuestionRecord> qrMap = new HashMap<>();
    private static QAParserRetrieverService instance;

    public synchronized static QAParserRetrieverService getInstance() {
        if (instance == null) {
            instance = new QAParserRetrieverService();
        }
        return instance;
    }


    private QAParserRetrieverService() {
        super();
        this.lookupUtils = new LookupUtils2();
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        lookupUtils.initialize();
        ess = new ElasticSearchService();
        // FIXME for test
        ess.setUseCache(false);
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
    }

    @SuppressWarnings("Duplicates")
    public SearchQuery buildSearchQuery(String question) throws Exception {
        List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", question);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
        SearchQuery sq;
        if (searchQueryOptions != SearchQueryOptions.NONE) {
            QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
            StringBuilder questionBuf = new StringBuilder();
            StringBuilder posTagsBuf = new StringBuilder();
            EngineUtils.prepareQuestion4Prediction(dataRecords, questionBuf, posTagsBuf);
            List<String> selectedQueryTerms = client.getSelectedQueryTerms(questionBuf.toString().trim(), posTagsBuf.toString().trim());
            if (selectedQueryTerms != null && !selectedQueryTerms.isEmpty()) {
                sq = sqGen.generatePubmedQuery(vocabulary, selectedQueryTerms, searchQueryOptions);
            } else {
                sq = sqGen.generatePubmedQuery(vocabulary);
            }
        } else {
            sq = sqGen.generatePubmedQuery(vocabulary);
        }
        cache.put(question, new QuestionQueryRec(question, sq));
        return sq;
    }


    public QuestionQueryRec retrieveResults4CurrentQuery(String question) throws Exception {
        QuestionQueryRec questionQueryRec = cache.get(question);
        Assertion.assertNotNull(questionQueryRec);
        PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(questionQueryRec.getSearchQuery(),
                lemmanizer, lookupUtils);
        String keywordQuery = pqc.buildESQuery();

        questionQueryRec.setPqc(pqc);
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, 2000);
        questionQueryRec.setNumReturnedDocs(pubMedDocs.size());

        return questionQueryRec;
    }

    public QuestionQueryRec getCoverage4CurrentQuery(String question) throws Exception {
        QuestionQueryRec questionQueryRec = cache.get(question);
        Assertion.assertNotNull(questionQueryRec);
        String keywordQuery = questionQueryRec.getPqc().buildESQuery();
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, 2000);
        questionQueryRec.setNumReturnedDocs(pubMedDocs.size());
        QuestionRecord qr = find(question);
        Assertion.assertNotNull(qr);
        QueryCoveragePerfUtil.Coverage coverage = QueryCoveragePerfUtil.showCoverage4Question(qr, pubMedDocs);
        questionQueryRec.setNumCovered(coverage.getCount());
        questionQueryRec.setGsCount(coverage.getGsCount());
        return questionQueryRec;
    }

    public QuestionQueryRec removeTermFromQuery(String question, String term) throws Exception {
        QuestionQueryRec questionQueryRec = cache.get(question);
        Assertion.assertNotNull(questionQueryRec);
        boolean removed = questionQueryRec.removeTermFromQuery(term);
        Assertion.assertTrue(removed);
        String keywordQuery = questionQueryRec.getPqc().buildESQuery();
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, 2000);
        questionQueryRec.setNumReturnedDocs(pubMedDocs.size());
        return questionQueryRec;
    }

    public QuestionQueryRec removeTermsFromQuery(String question, Set<String> termSet) throws Exception {
        QuestionQueryRec questionQueryRec = cache.get(question);
        Assertion.assertNotNull(questionQueryRec);
        termSet.forEach(questionQueryRec::removeTermFromQuery);
        String keywordQuery = questionQueryRec.getPqc().buildESQuery();
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, 2000);
        questionQueryRec.setNumReturnedDocs(pubMedDocs.size());
        return questionQueryRec;
    }

    @Override
    public List<PubMedDoc> retrieveResults(SearchQuery searchQuery, int maxNumOfDocs2Retrieve) throws Exception {

        PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(searchQuery, lemmanizer, lookupUtils);
        String keywordQuery = pqc.buildESQuery();

        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, maxNumOfDocs2Retrieve);

        return pubMedDocs;
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


    public static class QuestionQueryRec {
        String question;
        SearchQuery searchQuery;
        PubmedQueryConstructor2 pqc;
        int numCovered = 0;
        int gsCount = 0;
        int numReturnedDocs = 0;

        public QuestionQueryRec(String question, SearchQuery searchQuery) {
            this.question = question;
            this.searchQuery = searchQuery;
        }

        public String getQuestion() {
            return question;
        }

        public PubmedQueryConstructor2 getPqc() {
            return pqc;
        }

        public boolean removeTermFromQuery(String term) {
            return pqc.removeTermFromQuery(term);
        }

        public void setPqc(PubmedQueryConstructor2 pqc) {
            this.pqc = pqc;
        }

        public int getNumCovered() {
            return numCovered;
        }

        public void setNumCovered(int numCovered) {
            this.numCovered = numCovered;
        }

        public int getNumReturnedDocs() {
            return numReturnedDocs;
        }

        public void setNumReturnedDocs(int numReturnedDocs) {
            this.numReturnedDocs = numReturnedDocs;
        }

        public SearchQuery getSearchQuery() {
            return searchQuery;
        }

        public int getGsCount() {
            return gsCount;
        }

        public void setGsCount(int gsCount) {
            this.gsCount = gsCount;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("question", question);
            json.put("numReturnedDocs", numReturnedDocs);
            json.put("gsCount", gsCount);
            json.put("numCovered", numCovered);
            json.put("query", pqc.buildJSON());
            return json;
        }
    }
}
