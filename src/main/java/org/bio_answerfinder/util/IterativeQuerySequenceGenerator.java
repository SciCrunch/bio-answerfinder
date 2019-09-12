package org.bio_answerfinder.util;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.engine.QAParserRetrieverService;
import org.bio_answerfinder.engine.QAParserRetrieverService.QuestionQueryRec;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.DoubleStream;

/**
 * Created by bozyurt on 4/25/19.
 */
public class IterativeQuerySequenceGenerator {
    QAParserRetrieverService retrieverService;
    Random rnd;


    public IterativeQuerySequenceGenerator(long seed) throws Exception {
        retrieverService = QAParserRetrieverService.getInstance();
        retrieverService.initialize();
        rnd = new Random(seed);
    }


    public Policy generateQueryInteractively4Question(String question) throws Exception {
        retrieverService.buildSearchQuery(question);
        QuestionQueryRec qrc = retrieverService.retrieveResults4CurrentQuery(question);
        JSONObject json = qrc.toJSON();
        List<WeightedTerm> refWTList = new ArrayList<>(getAllSearchTerms(json.getJSONObject("query")));
        Set<WeightedTerm> usedSet = new HashSet<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Policy policy = new Policy(question);
        while (true) {
            usedSet.clear();
            System.out.println(json.toString(2));
            System.out.println("Question:" + question);
            System.out.print("Term to remove (start|exit): ");
            String ans = reader.readLine().trim();
            if (ans.equalsIgnoreCase("exit")) {
                return policy;
            } else if (ans.equalsIgnoreCase("start")) {
                policy = new Policy(question);
                retrieverService.buildSearchQuery(question);
                qrc = retrieverService.retrieveResults4CurrentQuery(question);
                json = qrc.toJSON();
            } else {
                WeightedTerm wt = findByTerm(ans, refWTList);
                if (wt != null) {
                    System.out.println("selected:" + wt);
                    QuestionQueryRec questionQueryRec;
                    if (wt.hasMore()) {
                        questionQueryRec = retrieverService.removeTermsFromQuery(question, new HashSet<>(wt.getTerms()));
                    } else {
                        questionQueryRec = retrieverService.removeTermFromQuery(question, wt.getTerm());
                    }
                    json = questionQueryRec.toJSON();
                    policy.add(wt);
                    int numReturnedDocs = json.getInt("numReturnedDocs");
                    if (numReturnedDocs > 0) {
                        questionQueryRec = retrieverService.getCoverage4CurrentQuery(question);
                        json = questionQueryRec.toJSON();
                        int gsCount = json.getInt("gsCount");
                        int numCovered = json.getInt("numCovered");
                        policy.setGsCount(gsCount);
                        policy.setNumCovered(numCovered);
                        policy.setNumReturned(numReturnedDocs);
                    }
                }
            }
        }
    }

    public Policy generateQuerySequences4Question(String question) throws Exception {
        retrieverService.buildSearchQuery(question);
        QuestionQueryRec qrc = retrieverService.retrieveResults4CurrentQuery(question);
        JSONObject json = qrc.toJSON();
        List<WeightedTerm> refWTList = new ArrayList<>(getAllSearchTerms(json.getJSONObject("query")));
        Set<WeightedTerm> usedSet = new HashSet<>();
        List<Policy> policies = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            usedSet.clear();
            Policy policy = new Policy(question);
            policies.add(policy);
            List<WeightedTerm> wtList = new ArrayList<>(refWTList);
            int tempCount = 1;
            while (usedSet.size() < refWTList.size() - 1) {
                //double temperature = 1.0 / Math.log((tempCount + 1 + 0.0000001));
                //double temperature = 1.0 / (tempCount  + 0.0000001);
                // double temperature = 1.0 / ((refWTList.size() - tempCount)  + 0.0000001);
                // WeightedTerm wt = select(wtList, usedSet);
                WeightedTerm wt = selectByRW(wtList, usedSet);
                // WeightedTerm wt = selectBySA(wtList, usedSet, temperature);
                System.out.println("selected:" + wt);
                QuestionQueryRec questionQueryRec;
                if (wt.hasMore()) {
                    questionQueryRec = retrieverService.removeTermsFromQuery(question, new HashSet<>(wt.getTerms()));
                } else {
                    questionQueryRec = retrieverService.removeTermFromQuery(question, wt.getTerm());
                }
                json = questionQueryRec.toJSON();
                policy.add(wt);
                int numReturnedDocs = json.getInt("numReturnedDocs");
                tempCount++;
                if (numReturnedDocs > 5) {
                    questionQueryRec = retrieverService.getCoverage4CurrentQuery(question);
                    json = questionQueryRec.toJSON();
                    int gsCount = json.getInt("gsCount");
                    int numCovered = json.getInt("numCovered");
                    policy.setGsCount(gsCount);
                    policy.setNumCovered(numCovered);
                    policy.setNumReturned(numReturnedDocs);
                    break;
                }
            }
            retrieverService.buildSearchQuery(question);
            retrieverService.retrieveResults4CurrentQuery(question);
        }
        Policy bestPolicy = policies.stream().max(Comparator.comparing(Policy::getNumCovered)).get();
        //Policy bestPolicy = policies.stream().max(Comparator.comparing(Policy::calcScore)).get();
        System.out.println(bestPolicy);
        return bestPolicy;
    }


    public WeightedTerm findByTerm(String term, List<WeightedTerm> wtList) {
        for (WeightedTerm wt : wtList) {
            if (wt.getTerm().equals(term)) {
                return wt;
            }
        }
        return null;
    }


    public WeightedTerm select(List<WeightedTerm> wtList, Set<WeightedTerm> usedSet) {
        WeightedTerm selected;
        do {
            int idx = rnd.nextInt(wtList.size());
            selected = wtList.get(idx);
        } while (usedSet.contains(selected));
        if (selected != null) {
            usedSet.add(selected);
        }
        return selected;
    }

    public WeightedTerm selectBySA(List<WeightedTerm> wtList, Set<WeightedTerm> usedSet, double temperature) {
        TObjectDoubleHashMap<WeightedTerm> rwWeightMap = prepRWWeightMap(wtList);
        double z = DoubleStream.of(rwWeightMap.values()).map(v -> Math.exp(v / temperature)).sum();
        TObjectDoubleHashMap<WeightedTerm> probsMap = new TObjectDoubleHashMap<>();
        wtList.forEach(wt -> probsMap.put(wt, Math.exp(rwWeightMap.get(wt) / temperature) / z));
        WeightedTerm selected;
        do {
            selected = selectByRW(wtList, probsMap);
        } while (usedSet.contains(selected));
        if (selected != null) {
            usedSet.add(selected);
        }
        return selected;
    }


    public WeightedTerm selectByRW(List<WeightedTerm> wtList, Set<WeightedTerm> usedSet) {
        TObjectDoubleHashMap<WeightedTerm> rwWeightMap = prepRWWeightMap(wtList);
        WeightedTerm selected;
        do {
            selected = selectByRW(wtList, rwWeightMap);
        } while (usedSet.contains(selected));
        if (selected != null) {
            usedSet.add(selected);
        }
        return selected;
    }

    private TObjectDoubleHashMap<WeightedTerm> prepRWWeightMap(List<WeightedTerm> wtList) {
        TObjectDoubleHashMap<WeightedTerm> rwWeightMap = new TObjectDoubleHashMap<>();
        wtList.forEach(w -> rwWeightMap.put(w, 1.0 / w.getWeight()));
        double sum = DoubleStream.of(rwWeightMap.values()).sum();
        rwWeightMap.keySet().forEach(w -> rwWeightMap.put(w, rwWeightMap.get(w) / sum));
        return rwWeightMap;
    }

    WeightedTerm selectByRW(List<WeightedTerm> wtList, TObjectDoubleHashMap<WeightedTerm> rwWeightMap) {
        double partSum = 0, r = this.rnd.nextDouble();
        for (int i = 0; i < wtList.size(); i++) {
            WeightedTerm wt = wtList.get(i);
            partSum += rwWeightMap.get(wt);
            if (partSum >= r) {
                return wt;
            }
        }
        return null;
    }

    Set<WeightedTerm> getAllSearchTerms(JSONObject query) {
        Set<WeightedTerm> wtSet = new HashSet<>();
        JSONArray qpArr = query.getJSONArray("queryParts");
        for (int i = 0; i < qpArr.length(); i++) {
            JSONObject qpJson = qpArr.getJSONObject(i);
            boolean orOP = false;
            if (qpJson.has("op") && qpJson.getString("op").equals("OR")) {
                orOP = true;
            }
            JSONArray stArr = qpJson.getJSONArray("stList");
            if (orOP) {
                WeightedTerm wt = null;
                for (int j = 0; j < stArr.length(); j++) {
                    JSONObject stJson = stArr.getJSONObject(j);
                    if (j == 0) {
                        wt = new WeightedTerm(stJson.getString("term"), stJson.getDouble("weight"));
                        wtSet.add(wt);
                    } else {
                        wt.addOrTerm(stJson.getString("term"));
                    }
                }
            } else {
                for (int j = 0; j < stArr.length(); j++) {
                    JSONObject stJson = stArr.getJSONObject(j);
                    WeightedTerm wt = new WeightedTerm(stJson.getString("term"), stJson.getDouble("weight"));
                    wtSet.add(wt);
                }
            }
        }

        return wtSet;
    }

    public static class Policy {
        String question;
        List<WeightedTerm> removalSequence = new ArrayList<>(10);
        int numReturned = 0;
        int gsCount = 0;
        int numCovered = 0;

        public Policy(String question) {
            this.question = question;
        }

        public List<WeightedTerm> getRemovalSequence() {
            return removalSequence;
        }

        public void add(WeightedTerm wt) {
            removalSequence.add(wt);
        }

        public int getNumReturned() {
            return numReturned;
        }

        public void setNumReturned(int numReturned) {
            this.numReturned = numReturned;
        }

        public int getGsCount() {
            return gsCount;
        }

        public void setGsCount(int gsCount) {
            this.gsCount = gsCount;
        }

        public int getNumCovered() {
            return numCovered;
        }

        public void setNumCovered(int numCovered) {
            this.numCovered = numCovered;
        }

        public double calcScore() {
            if (this.getGsCount() == 0) {
                return 0;
            }
            double score = numCovered / (double) gsCount;
            score = 0.5 * score + 0.5 * numCovered / numReturned;
            return score;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("question", question);
            json.put("numReturned", numReturned);
            json.put("numCovered", numCovered);
            json.put("gsCount", gsCount);
            JSONArray jsArr = new JSONArray();
            json.put("removalSequence", jsArr);
            removalSequence.forEach(wt -> jsArr.put(wt.toJSON()));
            return json;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Policy{");
            sb.append("question='").append(question).append('\'');
            sb.append(", removalSequence=").append(removalSequence);
            sb.append(", numReturned=").append(numReturned);
            sb.append(", gsCount=").append(gsCount);
            sb.append(", numCovered=").append(numCovered);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class WeightedTerm {
        final String term;
        final double weight;
        final List orList = new ArrayList<>(1);

        public WeightedTerm(String term, double weight) {
            this.term = term;
            this.weight = weight;
        }

        public String getTerm() {
            return term;
        }

        public double getWeight() {
            return weight;
        }

        public void addOrTerm(String orTerm) {
            orList.add(orTerm);
        }

        public boolean hasMore() {
            return !orList.isEmpty();
        }

        public List<String> getTerms() {
            List<String> terms = new ArrayList<>(orList.size() + 1);
            terms.add(term);
            terms.addAll(orList);
            return terms;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("term", term);
            json.put("weight", weight);
            if (hasMore()) {
                JSONArray jsArr = new JSONArray();
                orList.forEach(t -> jsArr.put(t));
                json.put("orList", jsArr);
            }
            return json;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WeightedTerm)) return false;
            WeightedTerm that = (WeightedTerm) o;
            return term.equals(that.term);
        }

        @Override
        public int hashCode() {
            return term.hashCode();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("WeightedTerm{");
            sb.append("term='").append(term).append('\'');
            sb.append(", weight=").append(weight);
            if (hasMore()) {
                sb.append(", orList=").append(orList);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    public static List<String> loadQuestions(String qscSetFile) throws IOException {
        List<String> questions = new ArrayList<>();
        try (BufferedReader in = FileUtils.newUTF8CharSetReader(qscSetFile)) {
            String line;
            int i = 0;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if ((i % 2) == 0) {
                    questions.add(line);
                }
                i++;
            }
        }
        return questions;
    }

    public static void savePolicies(List<Policy> policies, String outFile) throws IOException {
        JSONArray jsArr = new JSONArray();
        policies.forEach(p -> jsArr.put(p.toJSON()));
        FileUtils.saveText(jsArr.toString(2), outFile, CharSetEncoding.UTF8);
        System.out.println("wrote " + outFile);
    }

    private static void findBestPolicies(List<String> questions) throws Exception {
        IterativeQuerySequenceGenerator generator = new IterativeQuerySequenceGenerator(23847484L);
        //List<String> testQuestions = questions.subList(100, questions.size());
        //List<String> testQuestions = questions.subList(30, 100);
        List<String> testQuestions = questions.subList(0, 30);
        List<Policy> bestPolicies = new ArrayList<>();
        for (String testQuestion : testQuestions) {
            Policy policy = generator.generateQuerySequences4Question(testQuestion);
            bestPolicies.add(policy);
        }

        DoubleSummaryStatistics statistics = bestPolicies.stream()
                .mapToDouble(p -> p.getGsCount() > 0 ? p.getNumCovered() / p.getGsCount() : 0).summaryStatistics();

        System.out.println("Average coverage:" + statistics.getAverage());
        System.out.println("Min coverage:" + statistics.getMin());
        System.out.println("Max coverage:" + statistics.getMax());
        System.out.println("# of questions:" + statistics.getCount());

        savePolicies(bestPolicies, "/tmp/best_policies.json");
    }

    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String qscSetFile = HOME_DIR + "/dev/java/bio-answerfinder/scripts/rlbr/no_results_qsc_set.txt";

        List<String> questions = loadQuestions(qscSetFile);

        // findBestPolicies(questions);
        interactive();
    }

    private static void interactive() throws Exception {
        IterativeQuerySequenceGenerator generator = new IterativeQuerySequenceGenerator(23847484L);
        String question = "Which is the most well-accepted method for Down syndrome non-invasive prenatal diagnosis ?";
        question = "Which variables are included in the SPAN-100 score ?";
        question = "What is needed for MMP proteins to be functional ?";
        question = "Which is the main regulatory molecule of SERCA2A function in the cardiac muscle ?";
        question = "Does thyroid hormone receptor beta1 affect insulin secretion ?";
        question = "Does fibronectin constitute a serum biomarker for Duchenne muscular dystrophy ?";
        question = "What is the association between GERD and gluten ?";
        question = "What is the Drosophila melanogaster Groucho protein ?";

        Policy policy = generator.generateQueryInteractively4Question(question);
        System.out.println("========================");
        System.out.println(policy.toJSON().toString(2));
    }


}
