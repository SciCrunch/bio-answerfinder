package org.bio_answerfinder.evaluation;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.*;
import org.apache.commons.lang.math.NumberUtils;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.common.PubmedQueryConstructor2;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.engine.FocusWordEntityTypeAnswerFilter;
import org.bio_answerfinder.engine.QuestionFocusDetector;
import org.bio_answerfinder.engine.QuestionParser;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.engine.query.SearchQueryGenerator;
import org.bio_answerfinder.kb.LookupUtils2;
import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.nlp.sentence.TokenInfo;
import org.bio_answerfinder.services.ISentencePipeline;
import org.bio_answerfinder.services.NominalizationService;
import org.bio_answerfinder.services.StanfordSentencePipeline;
import org.bio_answerfinder.util.*;
import org.bio_answerfinder.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by bozyurt on 9/13/19.
 */
public class FactoidExtractionEvaluation {
    QuestionParser questionParser;
    TObjectFloatHashMap<String> vocabulary;
    ISentencePipeline sentencePipeline;
    Set<String> ngramSet;
    GloveDBLookup gloveMan;
    ILemmanizer lemmanizer;
    LookupUtils2 lookupUtils;
    NominalizationService nominalizationService;
    FactoidUtils factoidUtils;
    public static String HOME_DIR = System.getProperty("user.home");


    public FactoidExtractionEvaluation() throws Exception {
        questionParser = new QuestionParser();
        this.lemmanizer = SRLUtils.prepLemmanizer();
        lookupUtils = new LookupUtils2();
        nominalizationService = new NominalizationService();
    }

    public void initialize() throws Exception {
        nominalizationService.initialize();
        questionParser.initialize();
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
        sentencePipeline = new StanfordSentencePipeline();
        String tvDbFile = FileUtils.adjustPath(props.getProperty("term_vectors.db.file"));
        Assertion.assertExistingPath(tvDbFile, tvDbFile);
        this.ngramSet = SQLiteUtils.loadPhraseSet(tvDbFile);
        // String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        lookupUtils.initialize();
        factoidUtils = new FactoidUtils(vocabulary);
    }

    public void check(List<QuestionInfo> qiList) throws Exception {
        int count = 0;
        List<String> nonFocusQuestions = new ArrayList<>();
        for (QuestionInfo qi : qiList) {
            List<String> candidateSentences = qi.getCandidateSentences().subList(0,
                    Math.min(10, qi.getCandidateSentences().size()));

            List<String> exactAnswers = extractExactAnswers(qi.getQuestion(), candidateSentences);
            if (!exactAnswers.isEmpty()) {
                System.out.println("Q:" + qi.getQuestion());
                System.out.println(exactAnswers);
                System.out.println("GS:");
                System.out.println(qi.getQr().getAnswer().getExactAnswer());
                System.out.println("------------------------");
                Pair<String, Integer> rankedAnswer = factoidUtils.getRank4EntityFilter(exactAnswers, qi);
                System.out.println(rankedAnswer.getFirst() + " rank:" + rankedAnswer.getSecond());
                count++;
            } else {
                nonFocusQuestions.add(qi.getQuestion());
                QuestionFocusDetector.QuestionFocus qf = factoidUtils.findFocus(qi.getQuestion());
                if (qf != null) {
                    Set<String> questionTokens = factoidUtils.getQuestionTokens(qi.getQuestion());
                    SimilarityUtils.MatchingResult matchingResult = factoidUtils.generateCandidatePhrases2(qi, 10, qf, questionTokens);
                    System.out.println(matchingResult);
                }
            }
        }
        System.out.println("count:" + count);
        nonFocusQuestions.forEach(System.out::println);
    }

    public List<String> extractExactAnswers(String question, List<String> candidateSentences) throws Exception {
        List<DataRecord> questionRecords = questionParser.parseQuestion("user_query", question);
        SearchQueryGenerator sqGen = new SearchQueryGenerator(questionRecords, nominalizationService, null, null);
        SearchQuery searchQuery = sqGen.generatePubmedQuery(vocabulary);
        QuestionFocusDetector detector = new QuestionFocusDetector();
        QuestionFocusDetector.QuestionFocus questionFocus = detector.detectFocus(questionRecords);
        List<String> focusEntityTypes = getFocusEntityTypes(questionFocus);
        if (focusEntityTypes.size() > 1) {
            focusEntityTypes.remove("molecular entity");
        }
        Set<String> uniqSet = new HashSet<>();
        List<String> answers = new ArrayList<>();
        if (!focusEntityTypes.isEmpty()) {
            for (String candidateSentence : candidateSentences) {
                List<String> sentenceEntities = filter(searchQuery, candidateSentence, focusEntityTypes);
                if (!sentenceEntities.isEmpty()) {
                    for (String entity : sentenceEntities) {
                        if (!uniqSet.contains(entity.toLowerCase())) {
                            answers.add(entity);
                            uniqSet.add(entity.toLowerCase());
                        }
                    }
                }
            }
        }
        if (!focusEntityTypes.isEmpty() && questionFocus.getSecondFocus() != null) {
            System.out.println("***********************  " + focusEntityTypes + " Focus:" + questionFocus);
        }

        Set<String> propositionSet = new HashSet<>(Arrays.asList("in", "for", "on", "at", "of",
                "by", "up", "over", "with", "off", "that"));
        for (Iterator<String> it = answers.iterator(); it.hasNext(); ) {
            String answer = it.next();
            String[] tokens = answer.split("\\s+");
            if (tokens.length > 1) {
                List<String> remaining = new ArrayList<>(tokens.length);
                for (String token : tokens) {
                    if (!NPInfo.domainStopWords.contains(token.toLowerCase()) && !propositionSet.contains(token.toLowerCase())) {
                        remaining.add(token);
                    }
                }
                if (remaining.size() < tokens.length) {
                    answer = GenUtils.join(remaining, " ");
                }
            }

            if (NPInfo.domainStopWords.contains(answer.toLowerCase())) {
                it.remove();
            } else if (answer.length() < 2) {
                it.remove();
            } else if (NumberUtils.isNumber(answer)) {
                it.remove();
            }
        }


        return answers;
    }


    public List<String> filter(SearchQuery sq,
                               String sentence, List<String> focusEntityTypes) throws Exception {
        Map<String, List<String>> queryEntitiesMap = findEntities(sq);
        Map<String, List<String>> map = findEntitiesInSentence(sentence);
        Set<String> refEntityTypeSet = new HashSet<>(focusEntityTypes);
        List<String> factoidCandidates = new ArrayList<>();
        int numOfQuestionEntities = 0;
        for (String entity : map.keySet()) {
            if (queryEntitiesMap.containsKey(entity)) {
                numOfQuestionEntities++;
            } else {
                List<String> entityTypes = map.get(entity);
                if (FocusWordEntityTypeAnswerFilter.containsAny(entityTypes, refEntityTypeSet)) {
                    factoidCandidates.add(entity);
                }
            }
        }

        return factoidCandidates;

    }

    List<String> getFocusEntityTypes(QuestionFocusDetector.QuestionFocus questionFocus) {
        if (questionFocus == null) {
            return Collections.emptyList();
        }
        String focus = questionFocus.getFocusWord();
        return lookupUtils.getFocusEntityType(focus);
    }


    @SuppressWarnings("Duplicates")
    public Map<String, List<String>> findEntitiesInSentence(String sentence) throws IOException {
        Map<String, List<String>> map = new HashMap<>(11);
        List<TokenInfo> tiList = NERUtils.toTokens(sentence);
        int len = tiList.size();
        int i = 0;
        StringBuilder sb = new StringBuilder(100);
        while (i < len) {
            String token = tiList.get(i).getTokValue();
            if (StringUtils.isAllSpecial(token) || StopWords.isStopWord(token.toLowerCase())) {
                i++;
                continue;
            }
            sb.setLength(0);
            int theIdx = -1;
            String thePhrase = null;
            List<String> entityTypes = null;
            for (int j = i; j < len; j++) {
                TokenInfo ti = tiList.get(j);
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(ti.getTokValue());
                String phrase = sb.toString();
                List<String> etList = check4Entities(phrase);
                if (etList == null || etList.isEmpty()) {
                    break;
                } else {
                    thePhrase = phrase;
                    entityTypes = etList;
                    theIdx = j;
                }
            }
            if (thePhrase != null) {
                map.put(thePhrase, entityTypes);
                i = theIdx + 1;
            } else {
                i++;
            }
        }
        List<String> keys = new ArrayList<>(map.keySet());
        for (String key : keys) {
            if (PubmedQueryConstructor2.entityStopWords.contains(key.toLowerCase())) {
                map.remove(key);
            }
        }

        return map;
    }

    @SuppressWarnings("Duplicates")
    List<String> check4Entities(String phrase) {
        if (phrase.startsWith("protein")) {
            int idx = phrase.indexOf(' ');
            if (idx != -1) {
                String suffix = phrase.substring(idx + 1).trim();
                List<String> entityTypes = lookupUtils.getEntityType(suffix);
                if (!entityTypes.isEmpty()) {
                    return entityTypes;
                } else if (suffix.indexOf(' ') == -1) {
                    entityTypes.add("protein");
                    return entityTypes;
                }
            }
        }
        List<String> entityTypes = lookupUtils.getEntityType(phrase);
        if (entityTypes == null || entityTypes.isEmpty()) {
            entityTypes = lookupUtils.getEntityType(phrase.toLowerCase());
        }
        return entityTypes;
    }


    @SuppressWarnings("Duplicates")
    public Map<String, List<String>> findEntities(SearchQuery searchQuery) {
        Map<String, List<String>> map = new HashMap<>();
        Set<String> diseaseSet = new HashSet<>();
        Set<String> proteinSet = new HashSet<>();
        Set<String> geneSet = new HashSet<>();
        for (SearchQuery.QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
                if (PubmedQueryConstructor2.entityStopWords.contains(st.getTerm())) {
                    continue;
                }
                List<String> entityTypes = lookupUtils.getEntityType(st.getTerm());
                if (!entityTypes.isEmpty()) {
                    if (!map.containsKey(st.getTerm())) {
                        map.put(st.getTerm(), entityTypes);
                    }
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
                        } else if (term.startsWith("protein")) {
                            String suffix = term.substring(term.indexOf(' ') + 1).trim();
                            proteinSet.add(term);
                            proteinSet.add(suffix);
                        } else if (term.endsWith("gene") || term.endsWith("genes")) {
                            geneSet.add(term);
                            geneSet.add(prefix);
                        }
                    }
                }
            }
        }
        for (SearchQuery.QueryPart qp : searchQuery.getQueryParts()) {
            for (SearchQuery.SearchTerm st : qp.getSearchTerms()) {
                if (st.getEntityTypes() == null) {
                    if (geneSet.contains(st.getTerm())) {
                        FocusWordEntityTypeAnswerFilter.addEntityType(st.getTerm(), map, "gene");
                    } else if (proteinSet.contains(st.getTerm())) {
                        FocusWordEntityTypeAnswerFilter.addEntityType(st.getTerm(), map, "protein");
                    } else if (diseaseSet.contains(st.getTerm())) {
                        FocusWordEntityTypeAnswerFilter.addEntityType(st.getTerm(), map, "disease");
                    }
                }
            }
        }
        return map;
    }

    public static Set<String> prepExclusionSet() throws IOException {
        String questionAnswerCandidatesCSVFile = HOME_DIR +
                "/dev/java/bio-answerfinder/data/evaluation/bert_question_answer_candidates.csv";
        Set<String> exclusionSet = new HashSet<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(questionAnswerCandidatesCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    continue;
                }
                String question = cr.get(2);
                String key = StringUtils.stripWS(question);
                if (!exclusionSet.contains(key)) {
                    exclusionSet.add(key);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        String rankTestQuestions = HOME_DIR +
                "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
        try {
            in = FileUtils.newUTF8CharSetReader(rankTestQuestions);
            String line;
            while ((line = in.readLine()) != null) {
                String key = StringUtils.stripWS(line);
                if (!exclusionSet.contains(key)) {
                    exclusionSet.add(key);
                }
            }

        } finally {
            FileUtils.close(in);
        }
        return exclusionSet;
    }


    public static List<QuestionInfo> getRankTestQuestionInfos() throws Exception {
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        Map<String, QuestionRecord> qrMap = new HashMap<>();
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
        List<QuestionInfo> qiList = new ArrayList<>();
        String resultsTextFile = HOME_DIR +
                "/dev/java/bio-answerfinder/data/rank_annotations/method3_results.txt";
        BufferedReader in = null;
        int qid = 1;
        Pattern p = Pattern.compile("^\\d\\)");
        try {
            in = FileUtils.newUTF8CharSetReader(resultsTextFile);
            String line;
            QuestionInfo curQI = null;
            Set<String> questionSet = new HashSet<>();
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("---------")) {
                    continue;
                }
                Matcher matcher = p.matcher(line);
                if (!matcher.find()) {
                    String question = line;
                    String key = StringUtils.stripWS(question);
                    QuestionRecord qr = qrMap.get(key);
                    if (qr == null) {
                        System.out.println(question);
                    }
                    Assertion.assertNotNull(qr);
                    String questionType = qr.getType();
                    QuestionInfo qi = new QuestionInfo(qid,
                            question, questionType, qr);
                    curQI = qi;
                    qiList.add(qi);
                    questionSet.add(question);
                    qid++;
                } else {
                    String candidateSentence = line.substring(matcher.end()).trim();
                    curQI.addSentence(candidateSentence);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        qiList = qiList.stream().filter((QuestionInfo qi) -> qi.getType().equals("factoid"))
                .collect(Collectors.toList());
        return qiList;
    }


    public static List<QuestionInfo> getDevQuestionInfos(Set<String> exclusionSet) throws IOException {
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        Map<String, QuestionRecord> qrMap = new HashMap<>();
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
        List<QuestionInfo> qiList = new ArrayList<>();
        String bertTrainQuestionsTSVFile = HOME_DIR +
                "/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/train.tsv";
        BufferedReader in = null;
        int qid = 1;
        try {
            in = FileUtils.newUTF8CharSetReader(bertTrainQuestionsTSVFile);
            String line;
            QuestionInfo curQI = null;
            Set<String> questionSet = new HashSet<>();
            boolean first = true;
            while ((line = in.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] tokens = line.split("\t");
                Assertion.assertTrue(tokens.length == 3);
                String question = tokens[1];
                String candidateSentence = tokens[2];
                String key = StringUtils.stripWS(question);
                if (exclusionSet.contains(key)) {
                    continue;
                }
                if (!questionSet.contains(question)) {
                    QuestionRecord qr = qrMap.get(key);
                    if (qr == null) {
                        System.out.println(question);
                    }
                    Assertion.assertNotNull(qr);
                    String questionType = qr.getType();
                    QuestionInfo qi = new QuestionInfo(qid,
                            question, questionType, qr);
                    qi.addSentence(candidateSentence);
                    curQI = qi;
                    qiList.add(qi);
                    questionSet.add(question);
                    qid++;
                } else {
                    curQI.addSentence(candidateSentence);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        qiList = qiList.stream().filter((QuestionInfo qi) -> qi.getType().equals("factoid"))
                .collect(Collectors.toList());
        return qiList;
    }

    public static List<QuestionInfo> getQuestionInfos(String questionAnswerCandidatesCSVFile) throws IOException {
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        Map<String, QuestionRecord> qrMap = new HashMap<>();
        for (QuestionRecord qr : questionRecords) {
            String key = StringUtils.stripWS(qr.getQuestion());
            qrMap.put(key, qr);
        }
        List<QuestionInfo> qiList = new ArrayList<>();
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(questionAnswerCandidatesCSVFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            Set<String> questionSet = new HashSet<>();
            QuestionInfo curQI = null;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    // System.out.println(cr.get(2));
                    continue;
                }
                int qid = org.apache.commons.lang.math.NumberUtils.toInt(cr.get(0));
                String question = cr.get(2);
                String candidateSentence = cr.get(3);
                if (!questionSet.contains(question)) {
                    String key = StringUtils.stripWS(question);
                    QuestionRecord qr = qrMap.get(key);
                    if (qr == null) {
                        System.out.println(question);

                    }
                    Assertion.assertNotNull(qr);
                    String questionType = qr.getType();
                    QuestionInfo qi = new QuestionInfo(qid, question, questionType, qr);
                    qi.addSentence(candidateSentence);
                    curQI = qi;
                    qiList.add(qi);
                    questionSet.add(question);
                } else {
                    curQI.addSentence(candidateSentence);
                }
            }
        } finally {
            FileUtils.close(in);
        }
        qiList = qiList.stream().filter((QuestionInfo qi) -> qi.getType().equals("factoid"))
                .collect(Collectors.toList());
        return qiList;
    }

    public static class QuestionInfo {
        final int qid;
        final String question;
        final String type;
        final QuestionRecord qr;
        List<String> candidateSentences = new ArrayList<>(10);

        public QuestionInfo(int qid, String question, String type, QuestionRecord qr) {
            this.qid = qid;
            this.question = question;
            this.type = type;
            this.qr = qr;
        }

        public int getQid() {
            return qid;
        }

        public String getQuestion() {
            return question;
        }

        public String getType() {
            return type;
        }


        public List<String> getCandidateSentences() {
            return candidateSentences;
        }

        public void setCandidateSentences(List<String> candidateSentences) {
            this.candidateSentences = candidateSentences;
        }

        public void addSentence(String sentence) {
            candidateSentences.add(sentence);
        }

        public QuestionRecord getQr() {
            return qr;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("QuestionInfo{");
            sb.append("qid=").append(qid);
            sb.append(", type='").append(type).append('\'');
            sb.append(", question='").append(question).append('\'');
            int i = 1;
            for (String c : candidateSentences) {
                sb.append("\n\t" + i + ") " + c);
                i++;
            }
            sb.append('}');
            return sb.toString();
        }
    }


    public static void main(String[] args) throws Exception {
        String rankTestFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_annotations/method3_results.txt";
        List<QuestionInfo> rankQIList = getRankTestQuestionInfos();
        System.out.println("rankQIList:" + rankQIList.size());
        FactoidExtractionEvaluation eval = new FactoidExtractionEvaluation();
        eval.initialize();
        eval.check(rankQIList);
    }

    static void evaluateBERTTestSet() throws Exception {
        Set<String> exclusionSet = prepExclusionSet();
        System.out.println(exclusionSet.size());
        List<QuestionInfo> devQuestionInfos = getDevQuestionInfos(exclusionSet);
        System.out.println("devQuestionInfos:" + devQuestionInfos.size());
        // System.in.read();


        System.out.println(devQuestionInfos.size());


        String questionAnswerCandidatesCSVFile = HOME_DIR + "/dev/java/bio-answerfinder/data/evaluation/bert_question_answer_candidates.csv";
        List<QuestionInfo> qiList = getQuestionInfos(questionAnswerCandidatesCSVFile);

        System.out.println("qiList:" + qiList.size());
        FactoidExtractionEvaluation eval = new FactoidExtractionEvaluation();
        eval.initialize();
        //eval.check(remaining);
        eval.check(qiList);
    }
}
