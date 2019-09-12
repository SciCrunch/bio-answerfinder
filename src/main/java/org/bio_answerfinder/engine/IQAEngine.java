package org.bio_answerfinder.engine;

import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 10/3/17.
 */
public interface IQAEngine {
    public List<AnswerSentence> answerQuestion(String query, int topN, Map<String,String> options) throws Exception;
}
