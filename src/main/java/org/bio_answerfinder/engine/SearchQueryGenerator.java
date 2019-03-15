package org.bio_answerfinder.engine;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.Chunker;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.DataRecordReader;
import org.bio_answerfinder.common.*;
import org.bio_answerfinder.common.SearchQuery.Connective;
import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;
import org.apache.commons.cli.*;
import org.bio_answerfinder.services.AcronymService;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.common.dependency.DependencyNode;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 6/12/17.
 */
public class SearchQueryGenerator {
    List<DataRecord> questionRecords;
    boolean hasAppositive = false;
    NominalizationService nomService;
    AcronymService acronymService;
    VerbUtils verbUtils;


    public SearchQueryGenerator(List<DataRecord> questionRecords) {
        this(questionRecords, null, null, null);
    }

    public SearchQueryGenerator(List<DataRecord> questionRecords, NominalizationService nomService,
                                AcronymService acronymService, VerbUtils verbUtils) {
        this.questionRecords = questionRecords;
        this.nomService = nomService;
        this.acronymService = acronymService;
        this.verbUtils = verbUtils;
    }


    public boolean isHasAppositive() {
        return hasAppositive;
    }


    public static List<SpanPOS> getSpanPosListWithLemmas(DataRecord.ParsedSentence ps) throws ParseTreeManagerException {
        List<String> posTags = ps.getPosTags();
        List<SpanPOS> spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);
        Utils.addLemmas(spList);
        return spList;
    }

    public SearchQuery generatePubmedQuery() throws ParseTreeManagerException {
        return generatePubmedQuery(null);
    }

    public SearchQuery generatePubmedQuery(TObjectFloatHashMap<String> vocabulary) throws ParseTreeManagerException {
        return generatePubmedQuery(vocabulary, null, SearchQueryOptions.NONE);
    }

    public SearchQuery generatePubmedQuery(TObjectFloatHashMap<String> vocabulary, List<String> keywords,
                                           SearchQueryOptions searchQueryOptions) throws ParseTreeManagerException {
        SearchQuery sq = new SearchQuery();
        List<QueryPart> queryParts = new ArrayList<>(1);

        for (DataRecord dr : questionRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            boolean ensureInclusion = searchQueryOptions == SearchQueryOptions.ENSURE_INCLUSION;
            boolean boosting = searchQueryOptions == SearchQueryOptions.BOOST;
            Sentence2QueryHandler sentence2QueryHandler = new Sentence2QueryHandler(this, ps)
                    .keywordCandidates(keywords).ensureInclusion(ensureInclusion).boosting(boosting).invoke();
            List<QueryPart> newQpList = sentence2QueryHandler.getQueryParts();
            queryParts.addAll(newQpList);
            List<SpanPOS> spList = sentence2QueryHandler.getSpList();
            queryParts = unique(queryParts);
            if (vocabulary != null) {
                expandByVocabulary(queryParts, vocabulary);
                queryParts = unique(queryParts);
            }
            if (acronymService != null) {
                findAcronyms(spList, queryParts);
            }

            for (Iterator<QueryPart> it = newQpList.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                handleLemmatization(qp, spList);
            }
        }
        queryParts = unique(queryParts);
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            if (qp.matchesAny("List") || qp.matchesAny("list")) {
                it.remove();
            }
        }
        for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
            QueryPart qp = it.next();
            Connective connective = it.hasNext() ? Connective.OR : Connective.NONE;
            sq.addQueryPart(qp, connective);
        }
        if (searchQueryOptions == SearchQueryOptions.BOOST) {
            sq.rewriteByWeights(vocabulary, new HashSet<>(keywords), 2.0);
        } else {
            sq.rewriteByWeights(vocabulary);
        }
        return sq;
    }


    void findAcronyms(List<SpanPOS> spList, List<QueryPart> queryParts) {
        if (acronymService == null) {
            return;
        }
        int n = 6; // upto 6-grams
        int len = spList.size();
        int upper_limit = (len > n) ? len - n : 1;
        StringBuilder sb = new StringBuilder(100);
        for (int i = 0; i < upper_limit; i++) {
            int m = Math.min(len, n + i);
            sb.setLength(0);
            String theAcronym = null;
            for (int j = i; j < m; j++) {
                sb.append(spList.get(j).getToken()).append(' ');
                if (j == i) {
                    // at least bigram
                    continue;
                }
                String ngram = sb.toString().trim();
                String acronym = acronymService.findAcronym(ngram);
                if (acronym != null) {
                    theAcronym = acronym;
                }
            }
            if (theAcronym != null) {
                queryParts.add(new QueryPart(new SearchTerm(theAcronym, false)));
            }
        }
    }

    static void handleLemmatization(QueryPart qp, List<SpanPOS> spList) {
        if (qp.getSearchTerms().size() > 1) {
            return;
        }
        List<SearchTerm> newSearchTerms = new ArrayList<>();
        boolean found = false;
        Connective prevCon = null;

        for (SearchTerm st : qp.getSearchTerms()) {
            String term = st.getTerm();
            List<SpanPOS> posList = Utils.locateSpansInSentence(term, spList);
            if (!posList.isEmpty()) {
                SpanPOS lastSP = posList.get(posList.size() - 1);
                if (!lastSP.getLemma().equals(lastSP.getToken())) {
                    if (!newSearchTerms.isEmpty()) {
                        SearchTerm lst = newSearchTerms.get(newSearchTerms.size() - 1);
                        lst.next(st, prevCon);
                    }
                    newSearchTerms.add(st);
                    StringBuilder sb = new StringBuilder();
                    for (Iterator<SpanPOS> it = posList.iterator(); it.hasNext(); ) {
                        SpanPOS sp = it.next();
                        if (it.hasNext()) {
                            sb.append(sp.getToken()).append(' ');
                        } else {
                            sb.append(sp.getLemma());
                        }
                    }
                    SearchTerm lemmaST = new SearchTerm(sb.toString(), sb.toString().indexOf(' ') != -1);
                    st.next(lemmaST, Connective.OR);
                    newSearchTerms.add(lemmaST);
                    found = true;
                    // st.setTerm(sb.toString());
                } else {
                    if (!newSearchTerms.isEmpty()) {
                        SearchTerm lst = newSearchTerms.get(newSearchTerms.size() - 1);
                        lst.next(st, prevCon);
                    }
                    newSearchTerms.add(st);
                }
            }
            prevCon = st.getConnective();
        }
        if (found) {
            qp.replace(newSearchTerms);
        }

    }

    static boolean contains(Set<QueryPart> queryPartSet, QueryPart aqp) {
        for (QueryPart qp : queryPartSet) {
            if (qp.same(aqp)) {
                return true;
            }
        }
        return false;
    }

    public static void expandByVocabulary(List<QueryPart> queryParts, TObjectFloatHashMap<String> vocabulary) {
        List<String> newTerms = new ArrayList<>(1);
        for (QueryPart qp : queryParts) {
            for (SearchTerm st : qp.getSearchTerms()) {
                if (st.isPhrase()) {
                    List<String> allSubMatches = findAllSubMatches(st.getTerm(), vocabulary);
                    if (!allSubMatches.isEmpty()) {
                        newTerms.addAll(allSubMatches);
                    }
                }
            }
        }
        for (String newTerm : newTerms) {
            boolean phrase = newTerm.indexOf(' ') != -1;
            if (phrase) {
                queryParts.add(new QueryPart(new SearchTerm(newTerm, true)));
            } else {
                queryParts.add(token2QueryPart(newTerm));
            }
        }
    }

    public static List<String> findAllSubMatches(String term, TObjectFloatHashMap<String> vocabulary) {
        String[] tokens = term.split("\\s+");
        List<String> allSubMatches = new ArrayList<>(1);
        boolean fullMatch = vocabulary.contains(term);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < tokens.length - 1; i++) {
            sb.setLength(0);
            for (int j = i; j < tokens.length; j++) {
                sb.append(tokens[j]).append(' ');
            }
            String subTerm = sb.toString().trim();
            if (vocabulary.containsKey(subTerm)) {
                allSubMatches.add(subTerm);
            }
        }
        for (int i = 1; i < tokens.length - 1; i++) {
            sb.setLength(0);
            for (int j = 0; j <= i; j++) {
                sb.append(tokens[j]).append(' ');
            }
            String subTerm = sb.toString().trim();
            if (vocabulary.containsKey(subTerm)) {
                allSubMatches.add(subTerm);
            }
        }
        if (allSubMatches.isEmpty() && !fullMatch) {
            for (String token : tokens) {
                if (vocabulary.containsKey(token)) {
                    allSubMatches.add(token);
                }
            }
        }
        return allSubMatches;
    }

    public static List<QueryPart> unique(List<QueryPart> queryParts) {
        if (queryParts.isEmpty() || queryParts.size() == 1) {
            return queryParts;
        }
        // remove any enclosing parenthesis
        for (QueryPart qp : queryParts) {
            if (qp.getSearchTerms().size() == 1) {
                SearchTerm st = qp.getSearchTerms().get(0);
                if (st.getTerm().startsWith("(") && st.getTerm().endsWith(")")) {
                    String term = st.getTerm().substring(1);
                    term = term.substring(0, term.length() - 1);
                    st.setTerm(term.trim());
                }
            }

        }
        Set<QueryPart> seenSet = new HashSet<>(7);
        for (QueryPart qp1 : queryParts) {
            for (QueryPart qp2 : queryParts) {
                if (qp1 == qp2 || seenSet.contains(qp2) || qp2.getSearchTerms().size() > 1) {
                    continue;
                }
                if (qp1.matchesAny(qp2.getSearchTerms().get(0).getTerm())) {
                    if (!contains(seenSet, qp2)) {
                        seenSet.add(qp2);
                    }
                }
            }
        }
        if (!seenSet.isEmpty()) {
            queryParts.removeAll(seenSet);
        }
        // remove any numeric terms  (Also single letter terms (12/25/17))
        for (Iterator<QueryPart> iter = queryParts.iterator(); iter.hasNext(); ) {
            QueryPart qp = iter.next();
            for (Iterator<SearchTerm> it = qp.getSearchTerms().iterator(); it.hasNext(); ) {
                SearchTerm st = it.next();
                if (!st.isPhrase() && (NumberUtils.isNumber(st.getTerm()) || st.getTerm().length() == 1)) {
                    it.remove();
                }
            }
            if (qp.getSearchTerms().isEmpty()) {
                iter.remove();
            }
        }
        return queryParts;
    }


    List<QueryPart> handleCoordinatedPhrase(DataRecord.Chunk chunk, List<SpanPOS> spList) {
        List<SpanPOS> locatedList = Utils.locateSpansInSentence(chunk.getStartIdx(), chunk.getEndIdx(), spList);

        // clean up any bad tokens in the front of the chunk
        for (Iterator<SpanPOS> it = locatedList.iterator(); it.hasNext(); ) {
            SpanPOS sp = it.next();
            String posTag = sp.getPosTag();
            if (posTag.equals("DT") || posTag.startsWith("W") ||
                    (posTag.startsWith("VB") && !posTag.equals("VBG"))) {
                it.remove();
            } else {
                break;
            }
        }

        List<List<SpanPOS>> subPhrases = new ArrayList<>(2);
        List<SpanPOS> list = new ArrayList<>();
        String cc = null;
        for (SpanPOS sp : locatedList) {
            if (!sp.getPosTag().equals("CC")) {
                list.add(sp);
            } else {
                subPhrases.add(new ArrayList<>(list));
                list.clear();
                cc = sp.getToken();
            }
        }
        if (!list.isEmpty()) {
            subPhrases.add(list);
        }
        List<QueryPart> queryParts = new ArrayList<>(2);
        StringBuilder sb;
        // int lastNumTokens = subPhrases.get(subPhrases.size() - 1).size();
        /*
        if (lastNumTokens > 1) {
            sb.append("( ");
            List<SpanPOS> lastPhrase = subPhrases.get(subPhrases.size() - 1);
            String lastHead = lastPhrase.get(lastPhrase.size() - 1).getToken();
            for (Iterator<List<SpanPOS>> it = subPhrases.iterator(); it.hasNext(); ) {
                List<SpanPOS> subPhrase = it.next();
                if (expandSubPhrase && subPhrase.size() == 1) {
                    // ASSUMPTION
                    sb.append('"');
                    sb.append(subPhrase.get(0).getToken()).append(' ').append(lastHead).append('"');
                } else {
                    sb.append('"');
                    for (Iterator<SpanPOS> it2 = subPhrase.iterator(); it2.hasNext(); ) {
                        sb.append(it2.next().getToken());
                        if (it2.hasNext()) {
                            sb.append(' ');
                        }
                    }
                    sb.append("\" ");
                }
                if (it.hasNext()) {
                    sb.append(" ").append(cc.toUpperCase()).append(' ');
                }
            }
            sb.append(" )");
        } else {
        */
        QueryPart qp = null;
        for (Iterator<List<SpanPOS>> it = subPhrases.iterator(); it.hasNext(); ) {
            List<SpanPOS> subPhrase = it.next();
            if (subPhrase.size() == 1) {
                if (qp == null) {
                    qp = new QueryPart();
                    qp.addSearchTerm(new SearchTerm(subPhrase.get(0).getToken(), false), toConnective(cc));
                    queryParts.add(qp);
                } else {
                    qp.addSearchTerm(new SearchTerm(subPhrase.get(0).getToken(), false), toConnective(cc));
                }
            } else {
                sb = new StringBuilder();
                for (Iterator<SpanPOS> it2 = subPhrase.iterator(); it2.hasNext(); ) {
                    SpanPOS spanPOS = it2.next();
                    int code = TagSetUtils.getPOSTagCode(spanPOS.getPosTag());
                    if (code == POSTagSet.DT) {
                        continue;
                    }
                    sb.append(spanPOS.getToken());
                    if (it2.hasNext()) {
                        sb.append(' ');
                    }
                }
                String phrase = sb.toString().trim();
                SearchTerm st = new SearchTerm(phrase, phrase.indexOf(' ') != -1);
                if (qp == null) {
                    qp = new QueryPart();
                    queryParts.add(qp);
                }
                qp.addSearchTerm(st, toConnective(cc));
            }
        }
        return queryParts;
    }

    Connective toConnective(String cc) {
        //  return cc.equalsIgnoreCase("or") ? Connective.OR : Connective.AND;
        //TODO always OR
        return Connective.OR;
    }

    List<QueryPart> handleAppositive(DependencyNode apposNode, List<QueryPart> queryParts, List<Acronym> acronymList) {
        String expansion = null;
        if (apposNode == null || !acronymList.isEmpty()) {
            // ASSUMPTION: single acronym per sentence
            Acronym acr = acronymList.get(0);
            for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                if (qp.matches(acr.getAcronym())) {
                    it.remove();
                } else if (qp.contains(acr.getExpansion(0).getExpansion())) {
                    // Assertion.assertTrue(expansion == null);
                    if (expansion == null) {
                        expansion = qp.getSearchTerms().get(0).getTerm();
                        it.remove();
                    }
                }
            }
            QueryPart qp = new QueryPart(new SearchTerm(acr.getExpansion(0).getExpansion(), true));
            qp.addSearchTerm(new SearchTerm(acr.getAcronym(), false), Connective.OR);
            queryParts.add(qp);

            return queryParts;
        }
        // remove appos from the queryParts first
        DependencyNode parent = apposNode.getParent();


        QueryPart longestMatch = findLongestMatch(parent, queryParts);
        if (longestMatch != null) {
            queryParts.remove(longestMatch);
            for (Iterator<QueryPart> it = queryParts.iterator(); it.hasNext(); ) {
                QueryPart qp = it.next();
                if (qp.matches(apposNode.getToken())) {
                    it.remove();
                }
            }
            QueryPart qp = null;
            if (longestMatch.matches(apposNode.getToken())) {
                qp = new QueryPart(new SearchTerm(apposNode.getToken(), false));
                if (!apposNode.getToken().equalsIgnoreCase(apposNode.getLemma())) {
                    SearchTerm st = new SearchTerm(apposNode.getLemma(), false);
                    qp.addSearchTerm(st, Connective.OR);
                } else {
                }
            } else {
                qp = conditionAppositiveExp(longestMatch, apposNode.getToken());
                qp.addSearchTerm(new SearchTerm(apposNode.getToken(), false), Connective.OR);
                if (!apposNode.getToken().equalsIgnoreCase(apposNode.getLemma())) {
                    qp.addSearchTerm(new SearchTerm(apposNode.getLemma(), false), Connective.OR);
                }
            }
            queryParts.add(qp);
        }
        return queryParts;
    }

    static QueryPart conditionAppositiveExp(QueryPart qp, String apposToken) {
        Assertion.assertTrue(qp.getSearchTerms().size() == 1);
        if (qp.contains(apposToken)) {
            String term = qp.getSearchTerms().get(0).getTerm();
            Pattern p = Pattern.compile("\\(\\s+([^\\s]+)\\s+\\)");
            Matcher matcher = p.matcher(term);
            if (matcher.find()) {
                if (matcher.group(1).equals(apposToken)) {
                    StringBuilder sb = new StringBuilder(term.length());
                    sb.append(term.substring(0, matcher.start()));
                    sb.append(term.substring(matcher.end(), term.length()));
                    term = sb.toString().trim();
                    QueryPart qp1 = new QueryPart(new SearchTerm(term, term.indexOf(' ') != -1));
                    return qp1;
                }
            }
        }
        return qp;

    }

    public static QueryPart findLongestMatch(DependencyNode dn, List<QueryPart> queryParts) {
        QueryPart matched = null;
        int matchLen = 0;
        for (QueryPart qp : queryParts) {
            if (qp.contains(dn.getToken())) {
                if (dn.getChildren().isEmpty()) {
                    if (matchLen <= 0) {
                        matched = qp;
                        matchLen = 1;
                    }
                } else {
                    int count = 1;
                    for (DependencyNode child : dn.getChildren()) {
                        if (qp.contains(child.getToken())) {
                            count++;
                        }
                    }
                    if (count > matchLen) {
                        matched = qp;
                        matchLen = count;
                    }
                }
            }
        }
        return matched;
    }

    public static boolean isCoordinatedPhrase(DataRecord.Chunk chunk) {
        return chunk.getText().indexOf(" and ") != -1 || chunk.getText().indexOf(" or ") != -1;
    }

    public static String conditionNounPhrase(DataRecord.Chunk chunk, List<SpanPOS> spList, Set<SpanPOS> seenNounSPSet) {
        List<SpanPOS> locatedList = Utils.locateSpansInSentence(chunk.getStartIdx(), chunk.getEndIdx(), spList);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        boolean hasPossesive = false;
        for (Iterator<SpanPOS> it = locatedList.iterator(); it.hasNext(); ) {
            SpanPOS sp = it.next();
            if (first && sp.getPosTag().equals("POS")) {
                hasPossesive = true;
            }
            if (sp.getPosTag().equals("DT") || sp.getPosTag().startsWith("PRP")) {
                continue;
            } else if (sp.getPosTag().startsWith("W")) {
                continue;
            } else if (first && sp.getPosTag().startsWith("VB") && !sp.getPosTag().equals("VBG")) {
                continue;
            } else if (sp.getPosTag().equals("``") || sp.getPosTag().equals("''")) {
                continue;
            } else if (sp.getToken().equals("List")) {
                // Stanford parser cannot detect List as verb at the beginning of a sentence.
                continue;
            }
            first = false;
            seenNounSPSet.add(sp);
            sb.append(sp.getToken()).append(' ');
        }
        String s = sb.toString().trim();
        if (s.length() == 1) {
            return "";
        }
        if (hasPossesive) {
            int idx = Utils.spanPOSIndexOf(locatedList.get(0).getStartIdx(), spList);
            Assertion.assertTrue(idx != -1);
            if (idx > 0) {
                s = spList.get(idx - 1).getToken() + s;
            }
        }
        return s;
    }

    public static boolean isSlashWord(String token) {
        int idx = token.indexOf('/');
        if (idx > 0) {
            return (token.indexOf('/', idx + 1) == -1);
        }
        return false;
    }

    /**
     * handle terms with a '/'  in it for Elastic search
     *
     * @param token
     * @return
     */
    public static QueryPart token2QueryPart(String token) {
        if (isSlashWord(token)) {
            int idx = token.indexOf('/');
            String token1 = token.substring(0, idx);
            String token2 = token.substring(idx + 1);
            QueryPart qp = new QueryPart();
            if (token2.length() == 1 && NumberUtils.isNumber(token2) &&
                    Character.isDigit(token1.charAt(token1.length() - 1))) {
                qp.addSearchTerm(new SearchTerm(token1, false), Connective.OR);
                qp.addSearchTerm(new SearchTerm(token1.substring(0, token1.length() - 1) + token2,
                        false), Connective.NONE);

            } else {
                if (token2.length() == 1 && NumberUtils.isNumber(token2)) {
                    qp.addSearchTerm(new SearchTerm(token1, false), Connective.NONE);
                } else {
                    qp.addSearchTerm(new SearchTerm(token1, false), Connective.OR);
                    qp.addSearchTerm(new SearchTerm(token2, false), Connective.NONE);
                }
            }
            return qp;
/*
        } else if (token.indexOf('-') != -1) {
            String[] tokens = token.split("\\-");
            List<String> tokenList = new ArrayList<>(tokens.length);
            for (String tok : tokens) {
                if (StopWords.isStopWord(tok) || tok.length() == 1) {
                    continue;
                } else {
                    tokenList.add(tok);
                }
            }
            QueryPart qp = new QueryPart();
            for (Iterator<String> it = tokenList.iterator(); it.hasNext(); ) {
                String tok = it.next();
                qp.addSearchTerm(new SearchTerm(tok, false), it.hasNext() ? Connective.AND : Connective.NONE);
            }
            return qp;
        */
        } else {
            return new QueryPart(new SearchTerm(token, false));
        }
    }



    public static void prepQueries() throws Exception {
        TObjectFloatHashMap<String> vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        List<DataRecord> drList = DataRecordReader.loadRecords(dataRecordXmlFile);
        // use parse tree based chunks
        Chunker.integrateNPChunks(drList);

        Map<String, List<DataRecord>> questionMap = prepQuestionMap(drList);

        NominalizationService nominalizationService = new NominalizationService();
        nominalizationService.initialize();
        AcronymService acronymService = new AcronymService();
        acronymService.initialize();
        try {
            for (List<DataRecord> dataRecords : questionMap.values()) {
                SearchQueryGenerator sqGen = new SearchQueryGenerator(dataRecords, nominalizationService, acronymService, null);

                SearchQuery sq = sqGen.generatePubmedQuery(vocabulary);
                String query = sq.build();
                if (query.trim().length() == 0) {
                    System.err.println("Empty query! Skipping...");
                    continue;
                }
                System.out.println("Question:" + dataRecords.get(0).getText());
                System.out.println("Query:" + sq.build());
                System.out.println("===================================");
            }
        } finally {
            nominalizationService.shutdown();
            acronymService.shutdown();
        }
    }



    static Map<String, List<DataRecord>> prepQuestionMap(List<DataRecord> drList) {
        Map<String, List<DataRecord>> questionMap = new LinkedHashMap<>();
        for (DataRecord dr : drList) {
            List<DataRecord> dataRecords = questionMap.get(dr.getDocumentId());
            if (dataRecords == null) {
                dataRecords = new ArrayList<>(1);
                questionMap.put(dr.getDocumentId(), dataRecords);
            }
            dataRecords.add(dr);
        }
        return questionMap;
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SearchQueryGenerator", options);
        System.exit(1);
    }

    public static void cli(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option runNoOption = Option.builder("n").hasArg().required().desc("runNo").build();
        Option skipOption = Option.builder("s").hasArg().desc("question id to start").build();
        Option relevanceFeedbackOption = Option.builder("r").desc("if true use blind relevance feedback").build();
        Option numDocsOption = Option.builder("d").hasArg().desc("max # of docs to retrieve").build();
        Options options = new Options();
        options.addOption(help);
        options.addOption(runNoOption);
        options.addOption(skipOption);
        options.addOption(numDocsOption);
        options.addOption(relevanceFeedbackOption);
        CommandLineParser cli = new DefaultParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }
        if (line.hasOption("h")) {
            usage(options);
        }
        String skip = null;
        if (line.hasOption("s")) {
            skip = line.getOptionValue("s");
        }
        boolean useRelevanceFeedback = false;
        if (line.hasOption("r")) {
            useRelevanceFeedback = true;
        }
        String runNo = line.getOptionValue('n');
        System.out.println("skip:" + skip);
        // downloadQueryResults(skip);
        // String runNo = "run10";
        int numDocs2Retrieve = 1000;
        if (line.hasOption('d')) {
            numDocs2Retrieve = NumberUtils.toInt(line.getOptionValue('d'), 1000);
        }
        System.out.println("numDocs2Retrieve:" + numDocs2Retrieve);
        // downloadQueryResultsFromElastic(skip, runNo, numDocs2Retrieve, useRelevanceFeedback);

        //  prepQueries();
    }

    static void testDriver() throws Exception {
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        List<DataRecord> drList = DataRecordReader.loadRecords(dataRecordXmlFile);
        int appositiveCount = 0;
        for (DataRecord dr : drList) {
            SearchQueryGenerator sqGen = new SearchQueryGenerator(Arrays.asList(dr));
            SearchQuery query = sqGen.generatePubmedQuery();

            if (sqGen.isHasAppositive()) {
                appositiveCount++;
                System.out.println("*********************>");
            }
            System.out.println("Question:" + dr.getText());
            System.out.println("Query:" + query.build());
            System.out.println("===================================");
        }
        System.out.println("appositiveCount:" + appositiveCount);
    }

    static void prepSearchQueries(String outFile, boolean useChunker) throws Exception {
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        List<DataRecord> drList = DataRecordReader.loadRecords(dataRecordXmlFile);
        if (useChunker) {
            Chunker.integrateNPChunks(drList);
        }
        File of = new File(outFile);
        if (of.isFile()) {
            of.delete();
        }
        for (DataRecord dr : drList) {
            SearchQueryGenerator sqGen = new SearchQueryGenerator(Arrays.asList(dr));
            SearchQuery query = sqGen.generatePubmedQuery();
            FileUtils.appendLine(outFile, "Q:(" + dr.getId() + ") " + GenUtils.formatText(dr.getText(), 100));
            FileUtils.appendLine(outFile, "Query:" + query.build());
            FileUtils.appendLine(outFile, "------------------------------");
        }
    }


    public static void main(String[] args) throws Exception {
        // testDriver();
        cli(args);
        //prepSearchQueries("/tmp/search_queries_senna_chunks.txt", false);
        // prepSearchQueries("/tmp/search_queries_pt_chunks.txt", true);
    }

}
