package org.bio_answerfinder;


import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.SpanPOS;
import org.bio_answerfinder.common.Utils;
import org.bio_answerfinder.common.types.ParseTreeManagerException;

import java.util.*;

/**
 * Created by bozyurt on 1/10/19.
 */
public class SimpleSearchQueryGenerator {
    List<DataRecord> questionRecords;

    public SimpleSearchQueryGenerator(List<DataRecord> questionRecords) {
        this.questionRecords = questionRecords;
    }

    public SearchQuery generatePubmedQuery(TObjectFloatHashMap<String> vocabulary, List<String> keywords) throws ParseTreeManagerException {
        SearchQuery sq = new SearchQuery();
        List<SearchQuery.QueryPart> queryParts = new ArrayList<>(1);
        Set<String> keywordSet = new HashSet<>(keywords);

        for (DataRecord dr : questionRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            queryParts.addAll( prepQueryParts(ps, keywordSet));
        }
        for (Iterator<SearchQuery.QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            SearchQuery.QueryPart qp = it.next();
            SearchQuery.Connective connective = it.hasNext() ? SearchQuery.Connective.OR : SearchQuery.Connective.NONE;
            sq.addQueryPart(qp, connective);
        }
        sq.rewriteByWeights(vocabulary);
        return sq;
    }

    List<SearchQuery.QueryPart> prepQueryParts(DataRecord.ParsedSentence ps, Set<String> keywordSet) throws ParseTreeManagerException {
        List<SearchQuery.QueryPart> queryParts = new ArrayList<>();
        List<String> posTags = ps.getPosTags();
        List<SpanPOS> spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);
        for (Iterator<SpanPOS> it = spList.iterator(); it.hasNext(); ) {
            SpanPOS sp = it.next();
            if (!keywordSet.contains(sp.getToken())) {
                it.remove();
            }
        }
        Utils.addLemmas(spList);
        for(SpanPOS sp : spList) {
            SearchQuery.SearchTerm st = new SearchQuery.SearchTerm(sp.getToken(), false);
            queryParts.add(new SearchQuery.QueryPart(st));
        }
        return queryParts;
    }
}
