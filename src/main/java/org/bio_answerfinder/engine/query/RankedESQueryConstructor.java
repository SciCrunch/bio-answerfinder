package org.bio_answerfinder.engine.query;


import org.bio_answerfinder.common.PQCUtils;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.util.Assertion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by bozyurt on 7/14/19.
 */
public class RankedESQueryConstructor {
    SearchQuery searchQuery;
    List<QueryPart> currentList;

    public RankedESQueryConstructor(SearchQuery searchQuery) {
        this.searchQuery = searchQuery;
        this.currentList = new ArrayList<>(searchQuery.getQueryParts());
    }

    public String buildESQuery(BooleanQueryType queryType, boolean useWeights) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<QueryPart> it = currentList.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            if (qp.getSearchTerms().size() == 1) {
                SearchQuery.SearchTerm st = qp.getSearchTerms().get(0);
                sb.append(PQCUtils.prepESTerm(st, useWeights));
            }else {
                Assertion.assertTrue(qp.getSearchTerms().size() == 2);
                sb.append("(");
                sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(0), useWeights)).append(" OR ");
                sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(1), useWeights));
                sb.append(")");
            }
            if (it.hasNext()) {
                if (queryType == BooleanQueryType.AND) {
                    sb.append(" AND ");
                } else {
                    sb.append(" OR ");
                }
            }
        }
        return sb.toString().trim();
    }

    public boolean findAndRemove() {
        for (ListIterator<QueryPart> it = currentList.listIterator(currentList.size()); it.hasPrevious(); ) {
            it.previous();
            it.remove();
            return true;
        }
        return false;
    }
}


