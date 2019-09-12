package org.bio_answerfinder.engine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bio_answerfinder.services.*;
import org.bio_answerfinder.common.PubmedQueryConstructor2;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by bozyurt on 11/16/17.
 */
public class QAParserRetrieverHandler extends QAParserRetrieverHandlerBase {
    ElasticSearchService ess;
    SearchResultAnnotationCache cache;
    LookupUtils2 lookupUtils;


    public QAParserRetrieverHandler(LookupUtils2 lookupUtils) {
        super();
        this.lookupUtils = lookupUtils;
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        ess = new ElasticSearchService();
        ess.setUseCache(true);
    }

    public void enableAnnotationCache() throws IOException {
        cache = new SearchResultAnnotationCache();
    }

    @SuppressWarnings("Duplicates")
    @Override
    public List<PubMedDoc> retrieveResults(SearchQuery searchQuery, int maxNumOfDocs2Retrieve) throws Exception {

        PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(searchQuery, lemmanizer, lookupUtils);
        String keywordQuery = pqc.buildESQuery();
        System.out.println("ES Query:" + keywordQuery);
        List<PubMedDoc> pubMedDocs = ess.retrieveDocuments(keywordQuery, maxNumOfDocs2Retrieve);
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
                pubMedDocs = ess.retrieveDocuments(keywordQuery, maxNumOfDocs2Retrieve);
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
        return pubMedDocs;
    }

    public List<AnnotatedAbstract> annotateResults(List<PubMedDoc> results) throws Exception {
        List<AnnotatedAbstract> annotatedResults = new ArrayList<>(results.size());
        int i = 0;
        System.out.println("# results:" + results.size());
        for (PubMedDoc pmd : results) {
            AnnotatedAbstract annotatedAbstract = cache.getAnnnotatedAbstract(pmd.getPmid());

            if (annotatedAbstract == null) {
                List<String> sentences = extractSentences(pmd);
                annotatedAbstract = AnnotationService.annotateAbstract(pmd.getPmid(), sentences);
                annotatedResults.add(annotatedAbstract);
                cache.put(annotatedAbstract);
            }
            i++;
            System.out.print("\rAnnotated " + i);
        }
        System.out.println("\ndone.");

        return annotatedResults;
    }

    public List<AnnotatedAbstract> annotateResults(List<PubMedDoc> results, int batchSize) throws Exception {
        List<AnnotatedAbstract> annotatedResults = new ArrayList<>(results.size());
        int i = 0;
        System.out.println("# results:" + results.size());
        LinkedHashMap<String, List<String>> pmid2SentencesMap = new LinkedHashMap<>();
        while (i < results.size()) {
            int k = Math.min(i + batchSize, results.size());
            pmid2SentencesMap.clear();
            for (int j = i; j < k; j++) {
                PubMedDoc pmd = results.get(j);
                AnnotatedAbstract annotatedAbstract = cache.getAnnnotatedAbstract(pmd.getPmid());
                if (annotatedAbstract == null) {
                    List<String> sentences = extractSentences(pmd);
                    pmid2SentencesMap.put(pmd.getPmid(), sentences);
                } else {
                    annotatedResults.add(annotatedAbstract);
                }
            }
            if (!pmid2SentencesMap.isEmpty()) {
                List<AnnotatedAbstract> aaList = AnnotationService.annotateAbstracts(pmid2SentencesMap);
                annotatedResults.addAll(aaList);
                for (AnnotatedAbstract aa : aaList) {
                    cache.put(aa);
                }
            }
            i = k;

            System.out.print("\rAnnotated " + i);
        }
        System.out.println("\ndone.");

        return annotatedResults;
    }


    public static List<String> filterQuestionsWithKnownFocusEntityTypes() throws IOException {
        String HOME_DIR = System.getProperty("user.home");
        String csvFile = HOME_DIR + "/dev/java/bnerkit/data/bioasq/question_type_annot_data.csv";
        BufferedReader in = null;
        List<String> filteredQuestions = new ArrayList<>();
        try {
            in = FileUtils.newUTF8CharSetReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    continue;
                }
                String question = cr.get(1);
                String answerType = cr.get(8);
                String focusEntityTypes = cr.get(6);
                String focus = cr.get(3);
                String secondaryFocusEntity = cr.get(7);
                if (!GenUtils.isEmpty(focusEntityTypes)) {
                    filteredQuestions.add(question);
                } else if (!GenUtils.isEmpty(secondaryFocusEntity)) {
                    filteredQuestions.add(question);

                }

            }
        } finally {
            FileUtils.close(in);
        }
        return filteredQuestions;
    }


    public static void main(String[] args) throws Exception {
        LookupUtils2 lookupUtils = new LookupUtils2();
        lookupUtils.initialize();
        QAParserRetrieverHandler handler = new QAParserRetrieverHandler(lookupUtils);

        List<String> filteredQuestions = filterQuestionsWithKnownFocusEntityTypes();
        System.out.println("filteredQuestions:" + filteredQuestions.size());
        try {
            handler.enableAnnotationCache();
            handler.initialize();
            // String question = "Which is the protein that is encoded by the gene GLT8D1 ?";
            for (String question : filteredQuestions) {
                SearchQuery sq = handler.generateSearchQuery(question);
                List<PubMedDoc> pubMedDocs = handler.retrieveResults(sq, 2000);
                List<AnnotatedAbstract> annotatedAbstracts = handler.annotateResults(pubMedDocs, 10);
                for (AnnotatedAbstract aa : annotatedAbstracts) {
                    System.out.println(aa);
                    System.out.println("-----------------------");
                }
                System.out.println("Q:" + question);
                System.out.println("=======================================");
            }

        } finally {
            handler.shutdown();
        }

    }
}
