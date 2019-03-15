package org.bio_answerfinder.services;

import java.util.List;

/**
 * Created by bozyurt on 7/7/17.
 */
public interface ISentencePipeline {

    public void extractSentences(String content, List<String> allSentences) throws Exception;
}