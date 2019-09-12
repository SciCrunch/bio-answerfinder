package org.bio_answerfinder.engine;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.services.ISentencePipeline;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.services.StanfordSentencePipeline;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.SQLiteUtils;

import java.util.*;

/**
 * Created by bozyurt on 9/19/17.
 */
public class QAEngineBase {
    QuestionParser questionParser;

    NominalizationService nominalizationService;
    TObjectFloatHashMap<String> vocabulary;
    ISentencePipeline sentencePipeline;
    Set<String> ngramSet;
    ISearchResultsInterceptor searchResultsInterceptor;

    public QAEngineBase() {
        nominalizationService = new NominalizationService();
        questionParser = new QuestionParser();
    }

    public static List<String> extractSentences(PubMedDoc pmd, ISentencePipeline sentencePipeline) throws Exception {
        List<String> allSentences = new ArrayList<>(10);
        if (!GenUtils.isEmpty(pmd.getTitle())) {
            sentencePipeline.extractSentences(pmd.getTitle(), allSentences);
        }
        if (!GenUtils.isEmpty(pmd.getDocumentAbstract())) {
            sentencePipeline.extractSentences(pmd.getDocumentAbstract(), allSentences);
        }
        for (Iterator<String> it = allSentences.iterator(); it.hasNext(); ) {
            String sentence = it.next().trim();
            if (sentence.length() == 0) {
                System.out.println("removed empty sentence.");
                it.remove();
            }
        }

        return allSentences;
    }

    public ISearchResultsInterceptor getSearchResultsInterceptor() {
        return searchResultsInterceptor;
    }

    public void setSearchResultsInterceptor(ISearchResultsInterceptor searchResultsInterceptor) {
        this.searchResultsInterceptor = searchResultsInterceptor;
    }

    public void initialize() throws Exception {
        questionParser.initialize();

        nominalizationService.initialize();
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
        sentencePipeline = new StanfordSentencePipeline();
        // this.ngramSet = SQLiteUtils.loadPhraseSet(HOME_DIR + "/data/medline_index/tv.db");
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile =  FileUtils.adjustPath(props.getProperty("term_vectors.db.file"));
        Assertion.assertExistingPath(dbFile, dbFile);
        this.ngramSet = SQLiteUtils.loadPhraseSet(dbFile);

    }

    public void shutdown() {
        nominalizationService.shutdown();
    }

    protected void showAnswers(String question, List<AnswerSentence> answerSentences, int topN) {
        System.out.println("Question:" + question);
        System.out.println("Answer:");
        int n = Math.min(topN, answerSentences.size());
        for (int i = 0; i < n; i++) {
            AnswerSentence as = answerSentences.get(i);
            System.out.println(GenUtils.formatText((i + 1) + ") " +
                    as.getSentence() + " [" + as.getScore() + "] PMID:" + as.getPmid(), 100));
        }
    }
}
