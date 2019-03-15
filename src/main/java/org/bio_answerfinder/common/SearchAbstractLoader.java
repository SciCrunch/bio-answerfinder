package org.bio_answerfinder.common;


import org.bio_answerfinder.services.PubMedDoc;

import java.io.File;
import java.util.List;

/**
 * Created by bozyurt on 8/19/17.
 */
public class SearchAbstractLoader implements ISearchResultCollector {
    String runNo;

    public SearchAbstractLoader(String runNo) {
        this.runNo = runNo;
    }

    @Override
    public List<PubMedDoc> getSearchResults(String questionId) throws Exception {
        File rootDir = new File(QueryCoveragePerfUtil.abstractCacheRootDir, runNo);
        File questionAbstractsFile = new File(rootDir, questionId + ".json");
        return QueryCoveragePerfUtil.loadElasticAbstracts4Question(questionAbstractsFile.getAbsolutePath());
    }
}
