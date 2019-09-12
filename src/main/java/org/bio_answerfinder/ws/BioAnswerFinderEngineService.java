package org.bio_answerfinder.ws;

import java.util.List;
import java.util.Map;

import org.bio_answerfinder.engine.AnswerSentence;
import org.bio_answerfinder.engine.QAEngineDL;

/**
 * Created by bozyurt on 3/12/19.
 */
public class BioAnswerFinderEngineService {
    private QAEngineDL engine;
    private static BioAnswerFinderEngineService instance = null;

    private BioAnswerFinderEngineService() throws Exception {
        this.engine = new QAEngineDL();
    }

    public static synchronized BioAnswerFinderEngineService getInstance() throws Exception {
        if (instance == null) {
            instance = new BioAnswerFinderEngineService();
        }
        return instance;
    }

    public QAEngineDL getEngine() {
        return engine;
    }

    public void initialize() throws Exception {
        engine.initialize();
    }

    public List<AnswerSentence> answerQuestion(String question, int topN,
                                               Map<String, String> options) throws Exception {
        return engine.answerQuestion(question, topN, options);
    }


    public void shutdown() {
        engine.shutdown();
    }
}
