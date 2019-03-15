package org.bio_answerfinder.ws;

import org.bio_answerfinder.engine.AnswerSentence;
import org.bio_answerfinder.engine.QAEngine1;
import org.bio_answerfinder.engine.QAEngineDL;

import java.util.List;

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

    public void initialize() throws Exception {
        engine.initialize();
    }

    public List<AnswerSentence> answerQuestion(String question, int topN) throws Exception {
        return engine.answerQuestion(question, topN);
    }


    public void shutdown()  {
        engine.shutdown();
    }
}
