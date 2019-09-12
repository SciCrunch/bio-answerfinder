package org.bio_answerfinder.engine;


import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.engine.query.SearchQueryGenerator;
import org.bio_answerfinder.services.ISentencePipeline;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.services.StanfordSentencePipeline;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.util.SRLUtils;
import org.bio_answerfinder.util.VerbUtils;

import java.util.List;

/**
 * Created by bozyurt on 12/12/17.
 */
public abstract class QAParserRetrieverHandlerBase {
    protected QuestionParser questionParser;
    protected NominalizationService nominalizationService;
    protected TObjectFloatHashMap<String> vocabulary;
    protected ISentencePipeline sentencePipeline;
    protected VerbUtils verbUtils;
    protected ILemmanizer lemmanizer;

    public QAParserRetrieverHandlerBase() {
        nominalizationService = new NominalizationService();
        questionParser = new QuestionParser();
    }

    public void initialize() throws Exception {
        lemmanizer = SRLUtils.prepLemmanizer();
        verbUtils = new VerbUtils(lemmanizer);
        questionParser.initialize();
        sentencePipeline = new StanfordSentencePipeline();

        nominalizationService.initialize();
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
        verbUtils.initialize();
    }

    public void shutdown() {
        nominalizationService.shutdown();
        lemmanizer.shutdown();
    }

    public List<DataRecord> parseQuestion(String question) throws Exception {
        return questionParser.parseQuestion("user_query", question);
    }

    public SearchQuery generateSearchQuery(List<DataRecord> questionRecords) throws ParseTreeManagerException {
        SearchQueryGenerator sqGen = new SearchQueryGenerator(questionRecords, nominalizationService, null, verbUtils);
        return sqGen.generatePubmedQuery(vocabulary);
    }

    public SearchQuery generateSearchQuery(String question) throws Exception {
        List<DataRecord> questionRecords = questionParser.parseQuestion("user_query", question);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(questionRecords, nominalizationService, null, verbUtils);
        return sqGen.generatePubmedQuery(vocabulary);
    }
    public abstract List<PubMedDoc> retrieveResults(SearchQuery searchQuery, int maxNumOfDocs2Retrieve) throws Exception;

    public List<String> extractSentences(PubMedDoc pmd) throws Exception {
        return QAEngineBase.extractSentences(pmd, sentencePipeline);
    }
}
