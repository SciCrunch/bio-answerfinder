package org.bio_answerfinder.evaluation;


import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.common.GloVeVectorManager;
import org.bio_answerfinder.common.WordVector;
import org.bio_answerfinder.engine.GloveDataPreparer;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.util.MathUtils;
import org.bio_answerfinder.util.SQLiteUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by bozyurt on 11/3/17.
 */
public class WMDEvaluator {
    static String HOME_DIR = System.getProperty("user.home");
    TObjectFloatHashMap<String> vocabulary;
    Set<String> ngramSet;
    GloVeVectorManager gloveMan;


    public void initialize() throws Exception {
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
        this.ngramSet = SQLiteUtils.loadPhraseSet(HOME_DIR + "/data/medline_index/tv.db");
        this.gloveMan = new GloVeVectorManager();
        String gloveTextFile = HOME_DIR + "/data/medline/pmc_2017_abstracts_glove_vectors.txt";
        this.gloveMan.cacheFromTextFile(gloveTextFile);
    }


    public void evaluate() throws Exception {
        String question = "Which kinases does baricitinib inhibit";
        String answer = "The effect of a high-fat meal on baricitinib pharmacokinetics was insignificant";

        question = GloveDataPreparer.prepSentence4Glove(question, ngramSet, 4);
        answer = GloveDataPreparer.prepSentence4Glove(answer, ngramSet, 4);
        Map<String, WordVector> questionMap = prepareWordVectorMap(question);
        Map<String, WordVector> sentenceMap = prepareWordVectorMap(answer);
        float score = MathUtils.relaxedWMD(questionMap, sentenceMap, false);


        System.out.println("score:" + score);


    }


    Map<String, WordVector> prepareWordVectorMap(String content4Glove) {
        Map<String, WordVector> map = new HashMap<>();
        String[] tokens = content4Glove.split("\\s+");
        for (String token : tokens) {
            boolean phrase = token.indexOf('_') != -1;
            float[] gloveVector = gloveMan.getTermGloveVector(token);
            String term = phrase ? token.replace('_', ' ') : token;
            if (!map.containsKey(term) && gloveVector != null && vocabulary.containsKey(term)) {
                float weight = vocabulary.get(term);
                map.put(term, new WordVector(weight, gloveVector, term));
            }
        }
        return map;
    }

    public static void main(String[] args) throws Exception {
        WMDEvaluator evaluator = new WMDEvaluator();
        evaluator.initialize();
        evaluator.evaluate();
    }
}
