package org.bio_answerfinder.common;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.util.NumberUtils;

import java.io.Serializable;
import java.util.*;


/**
 * Created by bozyurt on 6/27/17.
 */


public class SearchQuery implements Serializable {
    static final long serialVersionUID = 1;
    List<QueryPart> queryParts = new ArrayList<>(2);

    public SearchQuery() {
    }

    @Deprecated
    public static SearchQuery buildFromQueryString(String queryStr) {
        char[] carr = queryStr.toCharArray();
        boolean inPhrase = false;
        StringBuilder buf = new StringBuilder();
        List<String> tokens = new ArrayList<>(5);
        for (int i = 0; i < carr.length; i++) {
            if (!inPhrase && Character.isWhitespace(carr[i])) {
                if (buf.length() > 0) {
                    tokens.add(buf.toString());
                    buf.setLength(0);
                }
                continue;
            } else if (carr[i] == '"') {
                if (inPhrase) {
                    if (buf.length() > 0) {
                        tokens.add(buf.toString());
                        buf.setLength(0);
                    }
                    inPhrase = false;
                } else {
                    inPhrase = true;
                }
            } else {
                buf.append(carr[i]);
            }
        }
        if (buf.length() > 0) {
            tokens.add(buf.toString());
        }
        SearchQuery sq = new SearchQuery();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            Connective con = Connective.OR;

            if (token.equals("OR") || token.equals("AND")) {
                continue;
            } else {
                if (i + 1 < tokens.size()) {
                    con = tokens.get(i + 1).equalsIgnoreCase("and") ? Connective.AND : Connective.OR;
                }
                sq.addQueryPart(new QueryPart(new SearchTerm(token, token.indexOf(' ') != -1, null)), con);
            }
        }
        return sq;
    }

    public void addQueryPart(QueryPart qp, Connective con) {
        if (!queryParts.isEmpty()) {
            QueryPart prev = queryParts.get(queryParts.size() - 1);
            prev.next = qp;
        }
        qp.connective = con;
        queryParts.add(qp);
    }

    public List<QueryPart> getQueryParts() {
        return queryParts;
    }

    public void rewriteByWeights(TObjectFloatHashMap<String> weightedVocabulary, Set<String> keywordSet,
                                 double boostFactor) {
        double minWeight = Double.MAX_VALUE;
        for (QueryPart qp : queryParts) {
            qp.calculateWeight(weightedVocabulary, keywordSet, boostFactor);
            minWeight = Math.min(qp.weight, minWeight);
        }
        for (QueryPart qp : queryParts) {
            qp.weight /= minWeight;
            for (SearchTerm st : qp.getSearchTerms()) {
                st.setWeight(qp.weight);
            }
        }
    }

    public void rewriteByWeights(TObjectFloatHashMap<String> weightedVocabulary) {
        double minWeight = Double.MAX_VALUE;
        for (QueryPart qp : queryParts) {
            qp.calculateWeight(weightedVocabulary);
            minWeight = Math.min(qp.weight, minWeight);
        }
        for (QueryPart qp : queryParts) {
            qp.weight /= minWeight;
            for (SearchTerm st : qp.getSearchTerms()) {
                st.setWeight(qp.weight);
            }
        }
    }

    public void rewriteByWeightsOld(TObjectFloatHashMap<String> weightedVocabulary) {
        if (queryParts.size() < 3) {
            return;
        }
        for (QueryPart qp : queryParts) {
            qp.calculateWeight(weightedVocabulary);
            qp.next = null;
        }
        Collections.sort(queryParts, new Comparator<QueryPart>() {
            @Override
            public int compare(QueryPart o1, QueryPart o2) {
                return Double.compare(o2.weight, o1.weight);
            }
        });
        int len = queryParts.size();
        for (int i = 0; i < len; i++) {
            QueryPart qp = queryParts.get(i);
            if (i == 0) {
                QueryPart nqp = queryParts.get(i + 1);
                if (qp.containsAny(nqp) || nqp.containsAny(qp)) {
                    qp.connective = Connective.OR;

                } else {
                    qp.connective = Connective.AND;
                }
                qp.next = nqp;
            } else {
                qp.connective = Connective.OR;
                if ((i + 1) < len) {
                    qp.next = queryParts.get(i + 1);
                }
            }
        }

    }

    public boolean hasTerm(String term, boolean caseSensitive) {
        for (QueryPart qp : queryParts) {
            for (SearchTerm st : qp.getSearchTerms()) {
                if (caseSensitive) {
                    if (st.getTerm().equals(term)) {
                        return true;
                    }
                } else {
                    if (st.getTerm().equalsIgnoreCase(term)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String build() {
        if (queryParts.size() == 1) {
            return queryParts.get(0).build();
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            sb.append(qp.build());
            if (qp.next != null) {
                sb.append(qp.connective == Connective.AND ? " AND " : " OR ");
            }
        }

        return sb.toString();
    }


    public String buildAndQuery() {
        if (queryParts.size() == 1) {
            return queryParts.get(0).build();
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            sb.append(qp.build());
            if (qp.next != null) {
                sb.append(" AND ");
            }
        }

        return sb.toString();
    }

    public String buildAfterFilter() {
        if (queryParts.size() == 1) {
            return queryParts.get(0).build();
        }
        StringBuilder sb = new StringBuilder();
        if (queryParts.size() > 2) {
            for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                if (qp.getSearchTerms().get(0).getWeight() < 1.1) {
                    continue;
                } else {
                    sb.append(qp.build());
                    if (qp.next != null) {
                        sb.append(qp.connective == Connective.AND ? " AND " : " OR ");
                    }
                }
            }
            String s = sb.toString().trim();
            if (s.endsWith(" OR")) {
                return s.substring(0, s.length() - 3);
            } else if (s.endsWith(" AND")) {
                return s.substring(0, s.length() - 4);
            }
            return s;
        } else {
            for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                sb.append(qp.build());
                if (qp.next != null) {
                    sb.append(qp.connective == Connective.AND ? " AND " : " OR ");
                }
            }
            return sb.toString();
        }

    }

    public static class QueryPart implements Serializable {
        static final long serialVersionUID = 1;
        List<SearchTerm> searchTerms = new ArrayList<>(1);
        Connective connective = Connective.NONE;
        QueryPart next;
        double weight = -1;

        public QueryPart() {
        }

        public QueryPart(SearchTerm st) {
            searchTerms.add(st);
        }

        public Connective getConnective() {
            return connective;
        }

        public double getWeight() {
            return weight;
        }

        public void unique() {
            Set<String> seenSet = new HashSet<>();
            unique(seenSet);
        }

        public void unique(Set<String> seenSet) {
            SearchTerm prevST = null;
            for (Iterator<SearchTerm> it = searchTerms.iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                if (!seenSet.contains(st.getTerm())) {
                    seenSet.add(st.getTerm());
                    prevST = st;
                } else {
                    if (prevST != null) {
                        prevST.next(st.next, st.getConnective());
                    }
                    it.remove();
                }
            }
        }

        public int textLength() {
            int tot = 0;
            for (SearchTerm st : searchTerms) {
                tot += st.getTerm().length();
            }
            return tot;
        }

        public int maxNumOfTokens() {
            int max = -1;
            for (SearchTerm st : searchTerms) {
                if (st.isPhrase()) {
                    String[] toks = st.getTerm().split("\\s+");
                    if (max < toks.length) {
                        max = toks.length;
                    }
                } else {
                    if (max < 1) {
                        max = 1;
                    }
                }
            }
            return max;
        }

        public boolean matches(String term) {
            if (searchTerms.size() > 1) {
                return false;
            }
            SearchTerm st = searchTerms.get(0);
            if (st.phrase) {
                return st.term.equals(term);
            } else {
                return st.term.equals(term);
            }
        }

        public boolean contains(String term) {
            if (searchTerms.size() > 1) {
                return false;
            }
            SearchTerm st = searchTerms.get(0);
            return st.term.indexOf(term) != -1;
        }

        public boolean matchesAny(String term) {
            for (SearchTerm st : searchTerms) {
                if (st.term.equals(term)) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsAny(QueryPart other) {
            for (SearchTerm st : searchTerms) {
                for (SearchTerm st2 : other.searchTerms) {
                    if (st.getTerm().contains(st2.getTerm())) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean same(QueryPart other) {
            if (searchTerms.size() != other.getSearchTerms().size()) {
                return false;
            }
            int foundCount = 0;
            for (SearchTerm st : searchTerms) {
                for (SearchTerm st2 : other.getSearchTerms()) {
                    if (st2.getTerm().equals(st.getTerm())) {
                        foundCount++;
                        break;
                    }
                }
            }
            return foundCount == searchTerms.size();
        }

        public void addSearchTerm(SearchTerm st, Connective con) {

            if (searchTerms.isEmpty()) {
                searchTerms.add(st);
            } else {
                SearchTerm prev = searchTerms.get(searchTerms.size() - 1);
                prev.next(st, con);
                searchTerms.add(st);
            }
        }

        public List<SearchTerm> getSearchTerms() {
            return searchTerms;
        }

        public void setNext(QueryPart next, Connective connective) {
            this.next = next;
            this.connective = connective;
        }

        public String build() {
            if (searchTerms.size() == 1) {
                return searchTerms.get(0).build();
            }
            StringBuilder sb = new StringBuilder(50);
            sb.append("( ");
            for (Iterator<SearchTerm> it = searchTerms.iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                sb.append(st.build());
                if (st.next != null) {
                    sb.append(st.connective == Connective.AND ? " AND " : " OR ");
                }
            }
            sb.append(")");
            return sb.toString();
        }

        public void replace(List<SearchTerm> newSearchTerms) {
            this.searchTerms = newSearchTerms;
        }

        public void calculateWeight(TObjectFloatHashMap<String> weightedVocabulary) {
            double sum = 0;
            int count = 0;
            for (Iterator<SearchTerm> it = searchTerms.iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                if (st.isPhrase()) {
                    if (weightedVocabulary.containsKey(st.getTerm())) {
                        sum += weightedVocabulary.get(st.getTerm());
                    } else {
                        if (st.isHyphenWord()) {
                            String hyphenWord = st.getTerm().replaceAll("\\s+", "-");
                            if (weightedVocabulary.containsKey(hyphenWord)) {
                                sum += weightedVocabulary.get(hyphenWord);
                            } else {
                                sum += 1.0;
                            }
                        } else {
                            String[] tokens = st.getTerm().split("\\s+");
                            double min = 1.0;
                            for (String token : tokens) {
                                if (weightedVocabulary.containsKey(token)) {
                                    double w = weightedVocabulary.get(token);
                                    if (min > w) {
                                        min = w;
                                    }
                                } else {
                                    // no-op
                                }
                            }
                            sum += min;
                        }
                    }
                    count++;
                } else {
                    if (weightedVocabulary.containsKey(st.getTerm())) {
                        sum += weightedVocabulary.get(st.getTerm());
                    } else {
                        sum += 1.0;
                    }
                    count++;
                }
            }
            this.weight = sum / count;
        }

        public static boolean containsAnyKeyword(String phrase, Set<String> keywordSet) {
            String[] tokens = phrase.split("\\s+");
            for (String token : tokens) {
                if (keywordSet.contains(token)) {
                    return true;
                }
            }
            return false;
        }

        public void calculateWeight(TObjectFloatHashMap<String> weightedVocabulary, Set<String> keywordSet,
                                    double boostFactor) {
            double sum = 0;
            int count = 0;
            for (Iterator<SearchTerm> it = searchTerms.iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                if (st.isPhrase()) {
                    if (weightedVocabulary.containsKey(st.getTerm())) {
                        double weight = weightedVocabulary.get(st.getTerm());
                        if (containsAnyKeyword(st.getTerm(), keywordSet)) {
                            weight *= boostFactor;
                        } else {
                            weight /= boostFactor;
                        }
                        sum += weight;
                    } else {
                        String[] tokens = st.getTerm().split("\\s+");
                        double min = 1.0;
                        for (String token : tokens) {
                            if (weightedVocabulary.containsKey(token)) {
                                double w = weightedVocabulary.get(token);
                                if (keywordSet.contains(token)) {
                                    w *= boostFactor;
                                }
                                if (min > w) {
                                    min = w;
                                }
                            }
                        }
                        sum += min;
                    }
                    count++;
                } else {
                    if (weightedVocabulary.containsKey(st.getTerm())) {
                        double weight = weightedVocabulary.get(st.getTerm());
                        if (keywordSet.contains(st.getTerm())) {
                            weight *= boostFactor;
                        } else {
                            weight /= boostFactor;
                        }
                        sum += weight;
                    } else {
                        if (keywordSet.contains(st.getTerm())) {
                            sum += 1.0;
                        } else {
                            sum += 0.2; // FIXME ??
                        }
                    }
                    count++;
                }
            }
            this.weight = sum / count;
        }

        public void removeAllMatching(String term) {
            for (Iterator<SearchTerm> it = searchTerms.iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                if (st.term.equals(term)) {
                    it.remove();
                }
            }
        }
    }

    public static class SearchTerm implements Serializable {
        static final long serialVersionUID = 1;
        String term;
        boolean phrase = false;
        boolean hyphenWord = false;
        Connective connective = Connective.NONE;
        SearchTerm next;
        double weight = -1;
        List<String> entityTypes;
        String posTag;

        public SearchTerm(String term, boolean phrase, String posTag, boolean hyphenWord) {
            this.term = term;
            this.phrase = phrase;
            this.posTag = posTag;
            this.hyphenWord = hyphenWord;
            // for PubMedServices
            if (this.term.indexOf('/') != -1) {
                this.phrase = true;
            }
        }

        public SearchTerm(String term, boolean phrase, String posTag) {
            this(term, phrase, posTag, false);
        }

        public SearchTerm(SearchTerm other) {
            this.term = other.term;
            this.phrase = other.phrase;
            this.connective = other.connective;
            this.weight = other.weight;
        }

        public double getWeight() {
            return weight;
        }

        void setWeight(double weight) {
            this.weight = weight;
        }

        public boolean isPhrase() {
            return phrase;
        }

        public boolean isHyphenWord() {
            return hyphenWord;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Connective getConnective() {
            return connective;
        }

        public void next(SearchTerm st, Connective connective) {
            this.next = st;
            this.connective = connective;
        }

        public List<String> getEntityTypes() {
            return entityTypes;
        }

        public void setEntityTypes(List<String> entityTypes) {
            this.entityTypes = entityTypes;
        }

        public void addEntityType(String entityType) {
            if (this.entityTypes == null) {
                entityTypes = new ArrayList<>(1);
            }
            if (!entityTypes.contains(entityType)) {
                entityTypes.add(entityType);
            }
        }

        public boolean hasEntityTypes() {
            return (entityTypes != null && !entityTypes.isEmpty());
        }

        public String getPosTag() {
            return posTag;
        }

        public void setPosTag(String posTag) {
            this.posTag = posTag;
        }

        public String build() {
            StringBuilder sb = new StringBuilder();
            if (!phrase) {

                sb.append(term);
            } else {
                sb.append('"').append(term).append('"');
            }
            if (weight > 0) {
                sb.append('^').append(NumberUtils.formatDecimal(weight, 1));
            }
            return sb.toString();
        }
    }

    public enum Connective {
        NONE, AND, OR
    }
}



