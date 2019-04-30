package org.bio_answerfinder.common;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;
import org.bio_answerfinder.engine.QuestionParser;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.engine.SearchQueryGenerator;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.nlp.morph.MorphException;
import org.bio_answerfinder.nlp.morph.TermMorphRecord;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.SRLUtils;

import java.util.*;

/**
 * Created by bozyurt on 12/16/17.
 */
public class PubmedQueryConstructor2 {
    SearchQuery searchQuery;
    ILemmanizer lemmanizer;
    List<List<QueryPart>> qpList;
    List<List<QueryPart>> currentList = new ArrayList<>(5);
    LookupUtils2 lookupUtils;
    public static Set<String> entityStopWords = new HashSet<>(Arrays.asList("gene", "genes", "disease", "disorder", "enzyme", "protein", "proteins",
            "syndrome", "syndromes", "disorders", "enzymes"));


    public PubmedQueryConstructor2(SearchQuery searchQuery, ILemmanizer lemmanizer, LookupUtils2 lookupUtils) {
        this.searchQuery = searchQuery;
        this.lemmanizer = lemmanizer;
        this.lookupUtils = lookupUtils;
        qpList = new ArrayList<>();

        if (lookupUtils != null) {
            findEntities(searchQuery);
        }
        Map<QueryPart, String> verbMap = new HashMap<>();
        QueryPart max = Collections.max(searchQuery.getQueryParts(), (o1, o2) -> Double.compare(o1.getWeight(), o2.getWeight()));
        double maxWeight = max.getWeight();

        for (Iterator<QueryPart> it = searchQuery.getQueryParts().iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            if (maxWeight > 1.5 &&
                    qp.getSearchTerms().size() == 1 && !qp.getSearchTerms().get(0).isPhrase() && qp.getWeight() <= 1.0) {
                it.remove();
                continue;
            }
            if (qp.getSearchTerms().size() == 1 && !qp.getSearchTerms().get(0).isPhrase()) {
                SearchTerm st = qp.getSearchTerms().get(0);
                try {
                    TermMorphRecord infinitive = lemmanizer.getInfinitive(st.getTerm());
                    if (infinitive != null) {
                        String stem = infinitive.getBaseWord();
                        /*
                        if (stem.endsWith("e")) {
                            stem = stem.substring(0, stem.length() - 1);
                        }
                        stem = stem + "*";
                        */
                        st.setTerm(stem);
                        verbMap.put(qp, stem);
                    } else if (st.getTerm().endsWith("ed") && st.getTerm().length() > 5) {
                        // most probably a verb
                        //String stem = st.getTerm().substring(0, st.getTerm().length() - 2) + "*";
                        String stem = st.getTerm().substring(0, st.getTerm().length() - 1);
                        st.setTerm(stem);
                        verbMap.put(qp, stem);
                    }
                } catch (MorphException e) {
                }
            }
        }
        List<QueryPart> workingList = new ArrayList<>(searchQuery.getQueryParts());
        Collections.sort(workingList, (o1, o2) -> Integer.compare(o2.maxNumOfTokens(), o1.maxNumOfTokens()));
        while (!workingList.isEmpty()) {
            List<QueryPart> eqList = new ArrayList<>(2);
            boolean first = true;
            QueryPart ref = null;
            for (Iterator<QueryPart> it = workingList.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                if (first) {
                    first = false;
                    eqList.add(qp);
                    ref = qp;
                    it.remove();
                } else {
                    if (ref.containsAny(qp) || qp.containsAny(ref)) {
                        eqList.add(qp);
                        it.remove();
                    }
                }
            }
            if (!eqList.isEmpty()) {
                Collections.sort(eqList, new Comparator<QueryPart>() {
                    @Override
                    public int compare(QueryPart o1, QueryPart o2) {
                        int c = Double.compare(o2.getWeight(), o1.getWeight());
                        if (c == 0) {
                            return Integer.compare(o1.maxNumOfTokens(), o2.maxNumOfTokens());
                        }
                        return c;
                    }
                });
                qpList.add(eqList);
            }

        }

        Collections.sort(qpList, (o1, o2) -> Double.compare(getWeight(o2), getWeight(o1)));
        currentList = new ArrayList<>(qpList);
        Collections.sort(currentList, new Comparator<List<QueryPart>>() {
            @Override
            public int compare(List<QueryPart> o1, List<QueryPart> o2) {
                boolean found1 = false, found2 = false;
                if (o1.size() == 1) {
                    QueryPart qp = o1.get(0);
                    if (qp.getSearchTerms().size() > 2 && PQCUtils.allNonPhrase(qp)) {
                        found1 = true;
                    } else if (verbMap.containsKey(qp)) {
                        found1 = true;
                    }
                }
                if (o2.size() == 1) {
                    QueryPart qp = o2.get(0);
                    if (qp.getSearchTerms().size() > 2 && PQCUtils.allNonPhrase(qp)) {
                        found2 = true;
                    } else if (verbMap.containsKey(qp)) {
                        found2 = true;
                    }
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

    static double getWeight(List<QueryPart> eqList) {
        double maxWeight = -1;
        for (QueryPart qp : eqList) {
            if (maxWeight < qp.getWeight()) {
                maxWeight = qp.getWeight();
            }
        }
        return maxWeight;
    }

    void findEntities(SearchQuery searchQuery) {
        Set<String> diseaseSet = new HashSet<>();
        Set<String> proteinSet = new HashSet<>();
        Set<String> geneSet = new HashSet<>();
        for (QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchTerm st : qp.getSearchTerms()) {
                if (entityStopWords.contains(st.getTerm())) {
                    continue;
                }
                List<String> entityTypes = lookupUtils.getEntityType(st.getTerm());
                if (!entityTypes.isEmpty()) {
                    st.setEntityTypes(entityTypes);
                }

                if (st.isPhrase()) {
                    int idx = st.getTerm().lastIndexOf(' ');
                    if (idx != -1) {
                        String prefix = st.getTerm().substring(0, idx).trim();
                        String term = st.getTerm();
                        if (term.endsWith(" disease") || term.endsWith(" disorder") || term.endsWith("syndrome")) {
                            diseaseSet.add(term);
                            diseaseSet.add(prefix);
                            if (prefix.endsWith("'s")) {
                                prefix = prefix.substring(0, prefix.length() - 2).trim();
                                diseaseSet.add(prefix);
                            }
                        } else if (term.endsWith("protein") || term.endsWith("proteins")) {
                            proteinSet.add(term);
                            proteinSet.add(prefix);
                        } else if (term.endsWith("gene") || term.endsWith("genes")) {
                            geneSet.add(term);
                            geneSet.add(prefix);
                        }
                    }
                }
            }
        }
        for (QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchTerm st : qp.getSearchTerms()) {
                if (st.getEntityTypes() == null) {
                    if (geneSet.contains(st.getTerm())) {
                        st.addEntityType("gene");
                    } else if (proteinSet.contains(st.getTerm())) {
                        st.addEntityType("protein");
                    } else if (diseaseSet.contains(st.getTerm())) {
                        st.addEntityType("disease");
                    }
                }
            }
        }
    }

    boolean findAndRemove(List<QueryPart> eqList) {
        for (ListIterator<QueryPart> it = eqList.listIterator(eqList.size()); it.hasPrevious(); ) {
            QueryPart qp = it.previous();
            int len = qp.getSearchTerms().size();
            for (int i = len - 1; i >= 0; i--) {
                SearchTerm st = qp.getSearchTerms().get(i);
                if (st.getEntityTypes() == null) {
                    qp.getSearchTerms().remove(i);
                    if (qp.getSearchTerms().isEmpty()) {
                        it.remove();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean adjustQueryWithEntities() {
        boolean found = false;
        for (ListIterator<List<QueryPart>> it = currentList.listIterator(currentList.size()); it.hasPrevious(); ) {
            List<QueryPart> eqList = it.previous();
            boolean removed = findAndRemove(eqList);
            if (removed) {
                return true;
            }
        }
        int totSize = 0;
        for (List<QueryPart> eqList : currentList) {
            for (QueryPart qp : eqList) {
                totSize += qp.getSearchTerms().size();
            }
        }
        if (totSize > 1) {
            for (ListIterator<List<QueryPart>> it = currentList.listIterator(currentList.size()); it.hasPrevious(); ) {
                List<QueryPart> eqList = it.previous();
                if (eqList.size() > 1) {
                    int lastIdx = eqList.size() - 1;
                    QueryPart qp = eqList.get(lastIdx);
                    if (qp.getSearchTerms().size() == 1) {
                        eqList.remove(lastIdx);
                    } else {
                        qp.getSearchTerms().remove(qp.getSearchTerms().size() - 1);
                        if (qp.getSearchTerms().isEmpty()) {
                            eqList.remove(lastIdx);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }


    public boolean adjustQuery() {
        boolean found = false;
        for (ListIterator<List<QueryPart>> it = currentList.listIterator(currentList.size()); it.hasPrevious(); ) {
            List<QueryPart> eqList = it.previous();
            if (eqList.size() > 1) {
                found = true;
                int lastIdx = eqList.size() - 1;
                QueryPart qp = eqList.get(lastIdx);
                if (qp.getSearchTerms().size() == 1) {
                    eqList.remove(lastIdx);
                } else {
                    qp.getSearchTerms().remove(qp.getSearchTerms().size() - 1);
                }
                break;
            }
        }
        if (found) {
            return true;
        }
        if (currentList.size() > 1) {
            currentList.remove(currentList.size() - 1);
            return true;
        }
        return false;
    }

    public boolean adjustQuery2() {
        for (ListIterator<List<QueryPart>> it = currentList.listIterator(currentList.size()); it.hasPrevious(); ) {
            List<QueryPart> eqList = it.previous();
            QueryPart last = eqList.get(eqList.size() - 1);
            if (last.getSearchTerms().size() == 1) {
                eqList.remove(last);
            } else {
                last.getSearchTerms().remove(last.getSearchTerms().size() - 1);
            }
            if (eqList.isEmpty()) {
                it.remove();
            }
            break;
        }
        if (currentList.size() == 1 && currentList.get(0).size() == 1) {
            return false;
        }

        return true;
    }


    public String buildQuery() {
        return buildQuery(currentList);
    }


    public String buildESQuery() {
        return buildESQuery(currentList);
    }

    public String buildESQuery(List<List<QueryPart>> queryParts) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<List<QueryPart>> it1 = queryParts.iterator(); it1.hasNext(); ) {
            List<QueryPart> eqList = it1.next();
            boolean added = false;
            for (Iterator<QueryPart> it = eqList.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();

                if (qp.getSearchTerms().size() == 2 && qp.getSearchTerms().get(0).getConnective() == SearchQuery.Connective.AND) {
                    sb.append("(");
                    sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(0))).append(" AND ")
                            .append(PQCUtils.prepESTerm(qp.getSearchTerms().get(1)));
                    sb.append(")");
                    added = true;
                } else {
                    if (qp.getSearchTerms().size() == 1) {
                        if (PQCUtils.isEligible(qp.getSearchTerms().get(0))) {
                            sb.append(PQCUtils.prepESTerm(qp.getSearchTerms().get(0)));
                            added = true;
                        } else {
                            continue;
                        }
                    } else {
                        /*
                        String commonPrefix;
                        if (PQCUtils.allNonPhrase(qp) && !PQCUtils.allCapital(qp) && (commonPrefix = PQCUtils.getLongestCommonPrefix(qp)) != null) {
                            sb.append(commonPrefix).append('*');
                            added = true;
                        } else {
                        */
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
                                sb.append(PQCUtils.prepESTerm(searchTerm));
                                if (it2.hasNext()) {
                                    sb.append(" OR ");
                                }
                            }
                            sb.append(')');
                            added = true;
                        } else {
                            continue;
                        }

                    }
                }
                if (added && it.hasNext() && !sb.toString().trim().endsWith("AND")) {
                    sb.append(" AND ");
                }
            }
            if (added && it1.hasNext() && !sb.toString().trim().endsWith("AND")) {
                sb.append(" AND ");
            }
        }
        String q = sb.toString().trim();
        if (q.endsWith(" AND")) {
            q = q.substring(0, q.length() - 4).trim();
        }
        return q;
    }


    public String buildQuery(List<List<QueryPart>> queryParts) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<List<QueryPart>> it1 = queryParts.iterator(); it1.hasNext(); ) {
            List<QueryPart> eqList = it1.next();
            for (Iterator<QueryPart> it = eqList.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();

                if (qp.getSearchTerms().size() == 2 && qp.getSearchTerms().get(0).getConnective() == SearchQuery.Connective.AND) {
                    sb.append("(");
                    sb.append(PQCUtils.prepTerm(qp.getSearchTerms().get(0))).append(" AND ").append(PQCUtils.prepTerm(qp.getSearchTerms().get(1)));
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
                            sb.append(commonPrefix).append('*');
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
            if (it1.hasNext()) {
                sb.append(" AND ");
            }
        }
        String q = sb.toString().trim();
        if (q.endsWith(" AND")) {
            q = q.substring(0, q.length() - 4).trim();
        }
        return q;
    }


    public static void saveSearchQueries(List<String> questions, String serFile) throws Exception {
        NominalizationService nominalizationService = new NominalizationService();
        QuestionParser questionParser = new QuestionParser();
        TObjectFloatHashMap<String> vocabulary;
        try {
            questionParser.initialize();
            nominalizationService.initialize();
            vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
            Map<String, SearchQuery> sqMap = new HashMap<>();
            for (String question : questions) {
                List<DataRecord> dataRecords = questionParser.parseQuestion("user_query", question);
                SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, null, null);
                SearchQuery sq = sqGen.generatePubmedQuery(vocabulary);
                sqMap.put(question, sq);
            }
            GenUtils.serialize(sqMap, serFile);

        } finally {
            nominalizationService.shutdown();
        }
    }

    public static void main(String[] args) throws Exception {
        String serFile = "/tmp/sq_map.ser";
        List<String> questions = Arrays.asList(
                "Which E3 ubiquitin ligase mediates the ubiquitination and degradation of the interferon receptor type 1 (IFNAR1)?",
                "Is the Histidine-Rich Calcium Binding protein (HRC) related to arrhythmias and cardiac disease?",
                "Is there any functional association during viral replication between flaviviridae viral RNA depended RNA polymerase and viral helicase?",
                "Which autophagy pathway is trigered by the KFERQ motif of cytosolic proteins?",
                "Which residue of alpha-synuclein was found to be phosphorylated in Lewy bodies?",
                "Is Hirschsprung disease a mendelian or a multifactorial disorder?",
                "What is the role of extracellular signal-related kinases 1 and 2 (ERK1/2) proteins in craniosynostosis?",
                "What are the pyknons?",
                "What is the function of TALENs?");
        // saveSearchQueries(questions, serFile);

        Map<String, SearchQuery> sqMap = (Map<String, SearchQuery>) GenUtils.deserialize(serFile);
        ILemmanizer lemmanizer = SRLUtils.prepLemmanizer();
        LookupUtils2 lookupUtils = new LookupUtils2();
        lookupUtils.initialize();
        for (String question : questions) {
            System.out.println("Q:" + question);
            SearchQuery sq = sqMap.get(question);
            System.out.println("Query:" + sq.build());
            PubmedQueryConstructor2 pqc = new PubmedQueryConstructor2(sq, lemmanizer, lookupUtils);
            do {
                String query = pqc.buildESQuery();
                System.out.println("ES query:" + query);
            } while (pqc.adjustQueryWithEntities());
            System.out.println("--------------------------");
        }

    }
}
