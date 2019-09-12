package org.bio_answerfinder.engine.query;


import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.common.Acronym;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.SpanPOS;
import org.bio_answerfinder.common.Utils;
import org.bio_answerfinder.common.dependency.DTUtils;
import org.bio_answerfinder.common.dependency.DependencyNode;
import org.bio_answerfinder.common.dependency.DependencyTreeFactory;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.nlp.acronym.AcronymDetector;

import java.util.*;

/**
 * Created by bozyurt on 1/9/19.
 */
class Sentence2QueryHandler {
    private SearchQueryGenerator searchQueryGenerator;
    private DataRecord.ParsedSentence ps;
    private List<SpanPOS> spList;
    private List<SearchQuery.QueryPart> queryParts;
    private Set<String> keywordCandidateSet;
    /**
     * if set and keywordCandidateSet is provided make sure all the keywords are considered to be in the ES query
     */
    private boolean ensureInclusion = false;
    private boolean boosting = false;

    public Sentence2QueryHandler(SearchQueryGenerator searchQueryGenerator, DataRecord.ParsedSentence ps) {
        this.searchQueryGenerator = searchQueryGenerator;
        this.ps = ps;
    }

    public Sentence2QueryHandler keywordCandidates(List<String> keywords) {
        if (keywords != null) {
            keywordCandidateSet = new HashSet<>(keywords);
        }
        return this;
    }

    public Sentence2QueryHandler ensureInclusion(boolean ensureInclusion) {
        this.ensureInclusion = ensureInclusion;
        return this;
    }

    public Sentence2QueryHandler boosting(boolean boosting) {
        this.boosting = boosting;
        return this;
    }

    public List<SpanPOS> getSpList() {
        return spList;
    }


    public List<SearchQuery.QueryPart> getQueryParts() {
        return queryParts;
    }

    public static SpanPOS find(List<SpanPOS> spList, String token) {
        for (SpanPOS sp : spList) {
            if (sp.getToken().equals(token)) {
                return sp;
            }
        }
        return null;
    }


    public Sentence2QueryHandler invoke() throws ParseTreeManagerException {
        // String sentence = ps.getSentence();
        List<DataRecord.Chunk> chunks = ps.getChunks();
        List<String> posTags = ps.getPosTags();
        spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);

        if (!boosting && !ensureInclusion && keywordCandidateSet != null && !keywordCandidateSet.isEmpty()) {
            for (Iterator<SpanPOS> it = spList.iterator(); it.hasNext(); ) {
                SpanPOS sp = it.next();
                if (!keywordCandidateSet.contains(sp.getToken())) {
                    it.remove();
                }
            }
        }

        Utils.addLemmas(spList);
        List<SpanPOS> unknownVerbList = new ArrayList<>(1);
        Map<String, String> verbLemmaList = new HashMap<>(7);
        for (SpanPOS sp : spList) {
            if (sp.getPosTag().startsWith("VB")) {
                if (!DependencyTreeFactory.hasVerbLemma(sp.getToken().toLowerCase())) {
                    unknownVerbList.add(sp);
                } else {
                    String verbLemma = DependencyTreeFactory.getLemma(sp.getToken().toLowerCase(), sp.getPosTag());
                    if (!verbLemma.equals("be") && !verbLemma.equals("have") && !verbLemma.equals("do")) {
                        verbLemmaList.put(verbLemma, sp.getToken().toLowerCase());
                    }
                }
            }
        }
        DependencyNode depTree = DependencyTreeFactory.createFromList(ps.getDepList(), ps.getPt());
        List<DependencyNode> apposList = DTUtils.findNodesMatching(depTree, "appos");
        AcronymDetector ad = new AcronymDetector();
        if (apposList != null && !apposList.isEmpty()) {
            searchQueryGenerator.hasAppositive = true;
        }
        List<Acronym> acronyms = ad.findAcronyms(ps.getSentence(), ps.getPt());
        if (acronyms.isEmpty()) {
            acronyms = ad.findAcronyms(ps.getSentence(), posTags);
            if (!acronyms.isEmpty()) {
                System.out.println(acronyms);
            }
        }
        List<SpanPOS> nounSPList = new ArrayList<>(spList.size());
        Set<SpanPOS> seenNounSPSet = new HashSet<>();
        for (SpanPOS sp : spList) {
            if (sp.getPosTag().startsWith("NN") && !sp.getToken().equals("Does")) {
                nounSPList.add(sp);
            }
        }
        // treat all unknown verbs (by morphbank) as nouns
        for (SpanPOS sp : unknownVerbList) {
            nounSPList.add(sp);
        }
        if (ensureInclusion) {
            for (String keyword : keywordCandidateSet) {
                SpanPOS theSP = find(nounSPList, keyword);
                if (theSP == null) {
                    SpanPOS keywordSP = find(spList, keyword);
                    if (keywordSP == null) {
                        // could happen for multi sentence questions.
                        System.out.println("keyword:" + keyword);
                        System.out.println(spList);
                    } else {
                        //Assertion.assertNotNull(keywordSP);
                        nounSPList.add(keywordSP);
                    }
                }
            }
        }
        int count = 0;
        queryParts = new ArrayList<>(5);
        for (DataRecord.Chunk chunk : chunks) {
            if (chunk.getType().equals("NP")) {
                if (SearchQueryGenerator.isCoordinatedPhrase(chunk)) {
                    List<SearchQuery.QueryPart> ccQueryParts = searchQueryGenerator.handleCoordinatedPhrase(chunk, spList);
                    queryParts.addAll(ccQueryParts);
                    continue;
                }
                String np = SearchQueryGenerator.conditionNounPhrase(chunk, spList, seenNounSPSet);
                if (np.length() == 0) {
                    continue;
                }

                if (np.indexOf(" ") != -1) {
                    SearchQuery.SearchTerm st = new SearchQuery.SearchTerm(np, true, "NP");
                    queryParts.add(new SearchQuery.QueryPart(st));
                } else {
                    // handle terms containing / and -
                    queryParts.add(SearchQueryGenerator.token2QueryPart(np, "N"));

                    //queryParts.add(new QueryPart(new SearchTerm(np, false)));
                }
                count++;
            }
        }
        for (SpanPOS sp : nounSPList) {
            if (!seenNounSPSet.contains(sp)) {
                queryParts.add(new SearchQuery.QueryPart(new SearchQuery.SearchTerm(sp.getToken(), false, sp.getPosTag())));
            }
        }
        if (!verbLemmaList.isEmpty()) {
            for (String verbLemma : verbLemmaList.keySet()) {
                List<String> nominalizations = searchQueryGenerator.nomService.getNominalizations(verbLemma);
                if (!nominalizations.isEmpty()) {
                    SearchQuery.QueryPart qp = new SearchQuery.QueryPart();
                    if (searchQueryGenerator.verbUtils != null) {
                        List<String> verbConjugations = searchQueryGenerator.verbUtils.getAllVerbConjugations(verbLemma);
                        for (Iterator<String> it = verbConjugations.iterator(); it.hasNext(); ) {
                            String vc = it.next();
                            qp.addSearchTerm(new SearchQuery.SearchTerm(vc, false, "V"), SearchQuery.Connective.OR);
                        }
                    } else {
                        qp.addSearchTerm(new SearchQuery.SearchTerm(verbLemma, false, "VB"), SearchQuery.Connective.OR);
                        qp.addSearchTerm(new SearchQuery.SearchTerm(verbLemmaList.get(verbLemma), false, "VB"),
                                SearchQuery.Connective.OR);
                    }
                    for (Iterator<String> it = nominalizations.iterator(); it.hasNext(); ) {
                        String nom = it.next();
                        qp.addSearchTerm(new SearchQuery.SearchTerm(nom, false, "NOM"), it.hasNext() ? SearchQuery.Connective.OR : SearchQuery.Connective.NONE);
                    }
                    queryParts.add(qp);
                } else {
                    if (searchQueryGenerator.verbUtils != null) {
                        addVerbConjugations(verbLemma);
                    } else {
                        queryParts.add(new SearchQuery.QueryPart(new SearchQuery.SearchTerm(verbLemma, false, "VB")));
                    }
                }
            }
        }
        if ((searchQueryGenerator.hasAppositive && !apposList.isEmpty()) || !acronyms.isEmpty()) {
            // ASSUMPTION: Single appos per sentence
            queryParts = searchQueryGenerator.handleAppositive(searchQueryGenerator.hasAppositive ? apposList.get(0) : null,
                    queryParts, acronyms);
        }
        return this;
    }

    void addVerbConjugations(String verbLemma) {
        List<String> verbConjugations = searchQueryGenerator.verbUtils.getAllVerbConjugations(verbLemma);
        SearchQuery.QueryPart qp = new SearchQuery.QueryPart();
        for (Iterator<String> it = verbConjugations.iterator(); it.hasNext(); ) {
            String vc = it.next();
            qp.addSearchTerm(new SearchQuery.SearchTerm(vc, false, "V"), it.hasNext() ? SearchQuery.Connective.OR : SearchQuery.Connective.NONE);
        }
        queryParts.add(qp);
    }
}
