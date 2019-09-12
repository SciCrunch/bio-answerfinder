package org.bio_answerfinder.common;


import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;
import org.bio_answerfinder.engine.query.BooleanQueryType;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.util.Assertion;

import java.util.*;

public class SimpleESQueryConstructor {
	SearchQuery searchQuery;
	LookupUtils2 lookupUtils;
	List<QueryPart> currentList;

	public SimpleESQueryConstructor(SearchQuery searchQuery, LookupUtils2 lookupUtils) {
		super();
		this.searchQuery = searchQuery;
		this.lookupUtils = lookupUtils;

		if (lookupUtils != null) {
			PubmedQueryConstructor2.findEntities(searchQuery, this.lookupUtils);
		}
		for (Iterator<QueryPart> it = searchQuery.getQueryParts().iterator(); it.hasNext();) {
			QueryPart qp = it.next();
			for (SearchTerm searchTerm : qp.searchTerms) {
				// boost weights of the recognized entities in the question
				if (searchTerm.hasEntityTypes()) {
					searchTerm.setWeight(searchTerm.getWeight() * 2.0);
				}
			}
		}
		Collections.sort(searchQuery.queryParts, new Comparator<QueryPart>() {
			@Override
			public int compare(QueryPart o1, QueryPart o2) {
				return Double.compare(o2.getWeight(), o1.getWeight());
			}
		});
		this.currentList = new ArrayList<>(searchQuery.queryParts);
	}

	public boolean findAndRemove() {
		for (ListIterator<QueryPart> it = currentList.listIterator(currentList.size()); it.hasPrevious();) {
			it.previous();
			it.remove();
			return true;
		}
		return false;
	}

	public String buildESQuery(BooleanQueryType queryType) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<QueryPart> it = currentList.iterator(); it.hasNext();) {
			QueryPart qp = it.next();
			if (qp.getSearchTerms().size() == 1) {
				SearchTerm st = qp.getSearchTerms().get(0);
				sb.append(PQCUtils.prepESTerm(st));
			} else {
				Assertion.assertTrue(qp.getSearchTerms().size() == 2);
				sb.append("(");
				sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(0))).append(" OR ");
				sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(1)));
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


}
