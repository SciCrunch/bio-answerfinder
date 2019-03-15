package org.bio_answerfinder.common;

import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;

import java.util.*;

/**
 * iteratively builds a keyword query for PubMED search engine.
 * Created by bozyurt on 12/13/17.
 */
public class PubmedQueryConstructor {
    SearchQuery searchQuery;
    List<QueryPart> qpList;
    QueryPart max;
    List<QueryPart> currentList = new ArrayList<>(5);


    public PubmedQueryConstructor(SearchQuery searchQuery) {
        this.searchQuery = searchQuery;
        qpList = new ArrayList<>(searchQuery.getQueryParts());
        Collections.sort(qpList, (o1, o2) -> Double.compare(o2.getWeight(), o1.getWeight()));
        max = Collections.max(qpList, (o1, o2) -> Double.compare(o1.getWeight(), o2.getWeight()));
        double maxWeight = max.getWeight();
        for (QueryPart qp : qpList) {
            if (qp.getSearchTerms().isEmpty()) { //  || (maxWeight > 3 && qp.getWeight() < 1.2)) {
                continue;
            }
            if (maxWeight > 1.5 &&
                    qp.getSearchTerms().size() == 1 && !qp.getSearchTerms().get(0).isPhrase() && qp.getWeight() <= 1.0) {
                continue;
            }
            currentList.add(qp);
        }
        Collections.sort(currentList, new Comparator<QueryPart>() {
            @Override
            public int compare(QueryPart o1, QueryPart o2) {
                boolean found1 = false, found2 = false;
                if (o1.getSearchTerms().size() > 2 && PQCUtils.allNonPhrase(o1)) {
                    found1 = true;
                }
                if (o2.getSearchTerms().size() > 2 && PQCUtils.allNonPhrase(o2)) {
                    found2 = true;
                }
                if (found1 && !found2) {
                    return 1;
                } else if (!found1 && found2) {
                    return -1;
                }
                return 0;
            }
        });
    }


    public boolean adjustQuery() {
        if (currentList.size() > 1) {
            currentList = currentList.subList(0, currentList.size() - 1);
            return true;
        }
        return false;
    }

    public String buildQuery() {
        return buildQuery(currentList);
    }


    public String buildQuery(List<QueryPart> queryParts) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();

            if (qp.getSearchTerms().size() == 2 && qp.getSearchTerms().get(0).getConnective() == SearchQuery.Connective.AND) {
                sb.append("(");
                sb.append(PQCUtils.prepTerm(qp.getSearchTerms().get(0)))
                        .append(" AND ").append(PQCUtils.prepTerm(qp.getSearchTerms().get(1)));
                sb.append(")");
            } else {
                if (qp.getSearchTerms().size() == 1) {
                    if (PQCUtils.isEligible(qp.getSearchTerms().get(0))) {
                        sb.append(PQCUtils.prepTerm(qp.getSearchTerms().get(0)));
                    } else {
                        continue;
                    }
                } else {
                    String commonPrefix;
                    if (PQCUtils.allNonPhrase(qp) && !PQCUtils.allCapital(qp) && (commonPrefix = PQCUtils.getLongestCommonPrefix(qp)) != null) {
                        if (qp.getSearchTerms().size() == 2) {
                            sb.append(commonPrefix).append('*');
                        } else {
                            continue;
                        }
                    } else {
                        List<SearchTerm> stList = new ArrayList<>(qp.getSearchTerms().size());
                        for (SearchTerm st : qp.getSearchTerms()) {
                            if (PQCUtils.isEligible(st)) {
                                stList.add(st);
                            }
                        }
                        if (!stList.isEmpty()) {
                            sb.append('(');
                            for (Iterator<SearchTerm> it2 = stList.iterator(); it2.hasNext(); ) {
                                SearchTerm searchTerm = it2.next();
                                sb.append(PQCUtils.prepTerm(searchTerm));
                                if (it2.hasNext()) {
                                    sb.append(" OR ");
                                }
                            }
                            sb.append(')');
                        } else {
                            continue;
                        }
                    }
                }
            }
            if (it.hasNext()) {
                sb.append(" AND ");
            }
        }
        String q = sb.toString().trim();
        if (q.endsWith(" AND")) {
            q = q.substring(0, q.length() - 4).trim();
        }
        return q;
    }





    public static void main(String[] args) {
        List<String> terms = new ArrayList<>(2);
        terms.add("Mutation");
        terms.add("mutation");
        System.out.println(PQCUtils.getLongestCommonPrefix(terms));

        System.out.println(PQCUtils.prepTerm(new SearchTerm("most likely protein", true)));

    }
}
