package org.bio_answerfinder;


import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;
import org.bio_answerfinder.common.SpanPOS;
import org.bio_answerfinder.common.Utils;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.GenUtils;

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
        List<QueryPart> queryParts = new ArrayList<>(1);
        Set<String> keywordSet = new HashSet<>(keywords);

        for (DataRecord dr : questionRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            queryParts.addAll(prepQueryParts(ps, keywordSet));
        }
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            SearchQuery.Connective connective = it.hasNext() ? SearchQuery.Connective.OR : SearchQuery.Connective.NONE;
            sq.addQueryPart(qp, connective);
        }
        sq.rewriteByWeights(vocabulary);
        return sq;
    }

    List<QueryPart> prepQueryParts(DataRecord.ParsedSentence ps, Set<String> keywordSet) throws ParseTreeManagerException {
        List<QueryPart> queryParts = new ArrayList<>();
        List<String> posTags = ps.getPosTags();
        List<SpanPOS> spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);
        for (Iterator<SpanPOS> it = spList.iterator(); it.hasNext(); ) {
            SpanPOS sp = it.next();
            if (!keywordSet.contains(sp.getToken())) {
                it.remove();
            }
        }
        Utils.addLemmas(spList);
        for (SpanPOS sp : spList) {
            if (GenUtils.isEmpty(sp.getLemma()) || sp.getLemma().equals(sp.getToken())) {
                if (Utils.isCommonNoun(sp)) {
                    String pluralNoun = Utils.getPluralIfAny(sp);
                    if (pluralNoun != null) {
                        SearchTerm st = new SearchTerm(sp.getToken(), false, sp.getPosTag());
                        QueryPart qp = new QueryPart(st);
                        qp.addSearchTerm(new SearchTerm(pluralNoun, false, sp.getPosTag()), SearchQuery.Connective.OR);
                        queryParts.add(qp);
                        continue;
                    }

                }
                if (Utils.isHypenWord(sp)) {
                    String[] tokens = sp.getToken().split("-");
                    StringBuilder sb = new StringBuilder();
                    for (String token : tokens) {
                        sb.append(token).append(' ');
                    }
                    QueryPart qp = new QueryPart(new SearchTerm(sb.toString().trim(), true, sp.getPosTag(), true));
                    queryParts.add(qp);
                } else {
                    SearchTerm st = new SearchTerm(sp.getToken(), false, sp.getPosTag());
                    queryParts.add(new QueryPart(st));
                }
            } else {
                SearchTerm st = new SearchTerm(sp.getToken(), false, sp.getPosTag());
                QueryPart qp = new QueryPart(st);
                qp.addSearchTerm(new SearchTerm(sp.getLemma(), false, sp.getPosTag()), SearchQuery.Connective.OR);
                queryParts.add(qp);
            }
        }
        return queryParts;
    }
}
