package org.bio_answerfinder.engine;


import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.common.SearchQuery;

import java.util.List;

/**
 * Created by bozyurt on 11/6/17.
 */
public interface ISearchResultsInterceptor {

    public void handle(SearchQuery sq, List<PubMedDoc> pubMedDocs);
}
