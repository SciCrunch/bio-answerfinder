package org.bio_answerfinder.common;


import org.bio_answerfinder.services.PubMedDoc;

import java.util.List;

/**
 * Created by bozyurt on 8/11/17.
 */
public interface ISearchResultCollector {

    public List<PubMedDoc> getSearchResults(String questionId) throws Exception;
}
