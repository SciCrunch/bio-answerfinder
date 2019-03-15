package org.bio_answerfinder.ws;


import org.bio_answerfinder.engine.AnswerSentence;
import org.bio_answerfinder.engine.QAEngine1;

import java.util.List;

/**
 * Created by bozyurt on 9/21/17.
 */
public class BaselineQAEngineService {
    private QAEngine1 engine;
    private static BaselineQAEngineService instance = null;

    private BaselineQAEngineService() throws Exception {
        this.engine = new QAEngine1();
    }

    public static synchronized BaselineQAEngineService getInstance() throws Exception {
        if (instance == null) {
            instance = new BaselineQAEngineService();
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
