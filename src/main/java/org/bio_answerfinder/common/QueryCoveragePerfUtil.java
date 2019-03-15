package org.bio_answerfinder.common;

import org.apache.commons.cli.*;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.services.IRUtils;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Measures the coverage performance of the question generated PubMED query
 * in answer candidate generation.
 * <p/>
 * Created by bozyurt on 6/20/17.
 */
public class QueryCoveragePerfUtil {
    String runNo;
    static String abstractCacheRootDir = FileUtils.adjustPath("${home}/data/bioasq/abstracts");
    static String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
    List<QuestionRecord> questionRecords;
    Map<String, QuestionRecord> qrMap = new HashMap<>();
    ISearchResultCollector searchResultCollector;
    IRUtils irUtils;
    boolean saveBestResults = false;
    String knnResultsDir;

    public QueryCoveragePerfUtil(ISearchResultCollector searchResultCollector) {
        this.searchResultCollector = searchResultCollector;
    }


    public void initialize() throws IOException {
        //File rootDir = new File(abstractCacheRootDir, runNo);
        // Assertion.assertTrue(rootDir.isDirectory());
        this.questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            qrMap.put(qr.getId(), qr);
        }
        this.irUtils = new IRUtils();
    }

    public boolean isSaveBestResults() {
        return saveBestResults;
    }

    public void setSaveBestResults(boolean saveBestResults) {
        this.saveBestResults = saveBestResults;
    }

    public String getKnnResultsDir() {
        return knnResultsDir;
    }

    public void setKnnResultsDir(String knnResultsDir) {
        this.knnResultsDir = knnResultsDir;
    }

    public void showCoverage4Question(String questionId) throws Exception {
        File rootDir = new File(abstractCacheRootDir, runNo);
        QuestionRecord qr = qrMap.get(questionId);
        Assertion.assertNotNull(qr);
        File questionAbstractsFile = new File(rootDir, qr.getId() + ".json");
        // List<PubMedDoc> pubMedDocs = loadAbstracts4Question(questionAbstractsFile.getAbsolutePath());
        List<PubMedDoc> pubMedDocs = loadElasticAbstracts4Question(questionAbstractsFile.getAbsolutePath());
        showCoverage4Question(qr, pubMedDocs);
    }

    public void showCoverage(int maxQuestions) throws Exception {
        // File rootDir = new File(abstractCacheRootDir, runNo);
        initialize();
        int qid = 1;
        Map<String, Integer> qidMap = new HashMap<>();
        for (QuestionRecord qr : questionRecords) {
            if (!qidMap.containsKey(qr.getId())) {
                qidMap.put(qr.getId(), qid);
                qid++;
            }
        }

        List<Coverage> coverageList = new ArrayList<>(questionRecords.size());
        int i = 0;
        for (QuestionRecord qr : questionRecords) {

            // File questionAbstractsFile = new File(rootDir, qr.getId() + ".json");
            //List<PubMedDoc> pubMedDocs = loadAbstracts4Question(questionAbstractsFile.getAbsolutePath());
            //List<PubMedDoc> pubMedDocs = loadElasticAbstracts4Question(questionAbstractsFile.getAbsolutePath());
            List<PubMedDoc> pubMedDocs = this.searchResultCollector.getSearchResults(qr.getId());
            if (knnResultsDir != null && new File(knnResultsDir).isDirectory()) {
                addKNNResults(qr, knnResultsDir, pubMedDocs);
            }

            Coverage coverage = showCoverage4Question(qr, pubMedDocs);
            irUtils.showPrecisionAtK4Question(qr, pubMedDocs, 20);
            if (saveBestResults) {
                irUtils.saveTopNResults(qr, pubMedDocs, 10);
            }
            coverageList.add(coverage);
            i++;
            if (maxQuestions > 0 && i > maxQuestions) {
                break;
            }
        }
        generateSummary(coverageList);
    }

    void generateSummary(List<Coverage> coverageList) throws IOException {
        List<Coverage> noMatchList = new ArrayList<>();
        int gsTotal = 0;
        int foundTotal = 0;
        int fullMatchTotal = 0;
        int noMatchTotal = 0;
        for (Coverage coverage : coverageList) {
            gsTotal += coverage.getGsCount();
            foundTotal += coverage.getCount();
            if (coverage.getCount() == 0) {
                noMatchList.add(coverage);
                noMatchTotal++;
            } else if (coverage.getCount() == coverage.getGsCount()) {
                fullMatchTotal++;
            }
        }
        System.out.println("No match questions");
        List<String> noMatchQidList = new ArrayList<>(noMatchList.size());
        for (Coverage coverage : noMatchList) {
            System.out.println(coverage.getId() + ":" + coverage.getQuestion());
            System.out.println("coverage:" + coverage.getCount() + " # gs count:" + coverage.getGsCount());
            noMatchQidList.add(coverage.getId());
        }
        FileUtils.saveList(noMatchQidList, "/tmp/no_match_qid.lst", CharSetEncoding.UTF8);
        System.out.println("=================");
        double percentCoverage = foundTotal / (double) gsTotal * 100.0;
        System.out.println("% coverage:" + percentCoverage + " # full match:" + fullMatchTotal + " # no match:" + noMatchTotal);
    }

    public static void addKNNResults(QuestionRecord qr, String knnDir, List<PubMedDoc> pubMedDocs) {
        File knnResultFile = new File(knnDir, qr.getId() + "_ir.csv");
        if (!knnResultFile.isFile()) {
            System.err.println("Cannot find " + knnResultFile);
            return;
        }
        Set<String> seenSet = new HashSet<>();
        for(PubMedDoc pmd : pubMedDocs) {
            seenSet.add(pmd.getPmid());
        }
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(knnResultFile.getAbsolutePath());
            String line;
            int commonCount = 0;
            while((line = in.readLine()) != null) {
                String[] tokens = line.split("\\s*,\\s*");
                String pmid = tokens[0];
                if (!seenSet.contains(pmid)) {
                    pubMedDocs.add(new PubMedDoc(pmid));
                    seenSet.add(pmid);
                } else {
                    commonCount++;
                }
            }
            if (commonCount > 0) {
                System.out.println(qr.getId() + " commonCount:" + commonCount);
            }
        } catch(IOException iox) {
            System.out.println(iox);
        } finally {
            FileUtils.close(in);
        }


    }
    public static Coverage showCoverage4Question(QuestionRecord qr, List<PubMedDoc> pubMedDocs) {
        Set<String> gsPMIDSet = new HashSet<>();
        for (String docURL : qr.getAnswer().getDocuments()) {
            String pmid = QuestionRecord.Answer.getPMID(docURL);
            gsPMIDSet.add(pmid);
        }
        int count = 0;
        for (PubMedDoc pmd : pubMedDocs) {
            if (gsPMIDSet.contains(pmd.getPmid())) {
                count++;
            }
        }
        System.out.println(qr.getQuestion());

        System.out.println("Q " + qr.getId() + " coverage:" + count + " # gs count:"
                + gsPMIDSet.size() + " # docs retrieved:" + pubMedDocs.size());
        System.out.println("---------------------");
        try {
            FileUtils.appendLine("/tmp/coverage.txt", "Q " + qr.getId() + " coverage:" + count + " # gs count:"
                    + gsPMIDSet.size() + " # docs retrieved:" + pubMedDocs.size());
        } catch (Exception x) {
            // ignore
        }
        return new Coverage(qr.getQuestion(), qr.getId(), count, gsPMIDSet.size());
    }

    public static class Coverage {
        String question;
        String id;
        int count;
        int gsCount;

        public Coverage(String question, String id, int count, int gsCount) {
            this.question = question;
            this.id = id;
            this.count = count;
            this.gsCount = gsCount;
        }

        public String getQuestion() {
            return question;
        }

        public String getId() {
            return id;
        }

        public int getCount() {
            return count;
        }

        public int getGsCount() {
            return gsCount;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("question", question);
            json.put("count", count);
            json.put("gsCount", gsCount);
            json.put("coverageFrac", count / (double) gsCount);
            return json;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Coverage{");
            sb.append("count=").append(count);
            sb.append(", gsCount=").append(gsCount);
            sb.append(", question='").append(question).append('\'');
            sb.append(", id='").append(id).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public static List<PubMedDoc> loadElasticAbstracts4Question(String questionAbstractsFile) throws IOException {
        if (!new File(questionAbstractsFile).isFile()) {
            System.err.println("Cannot find:" + questionAbstractsFile);
            return new ArrayList<>(1);
        }
        String content = FileUtils.loadAsString(questionAbstractsFile, CharSetEncoding.UTF8);
        JSONObject json = new JSONObject(content);
        if (!json.has("hits")) {
            return Collections.emptyList();
        }
        JSONArray jsArr = json.getJSONObject("hits").getJSONArray("hits");
        List<PubMedDoc> pmdList = new ArrayList<>(jsArr.length());
        for (int i = 0; i < jsArr.length(); i++) {
            PubMedDoc pmd = PubMedDoc.fromElasticJSON(jsArr.getJSONObject(i));
            if (pmd.getDocumentAbstract() != null) {
                pmdList.add(pmd);
            }
        }
        return pmdList;
    }

    public static List<PubMedDoc> loadAbstracts4Question(String questionAbstractsFile) throws IOException {
        String content = FileUtils.loadAsString(questionAbstractsFile, CharSetEncoding.UTF8);
        JSONObject json = new JSONObject(content);
        if (!json.has("result")) {
            return Collections.emptyList();
        }
        System.out.println("keywords:" + json.getJSONObject("result").getString("keywords"));
        JSONArray jsonArray = json.getJSONObject("result").getJSONArray("documents");
        List<PubMedDoc> pmdList = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            pmdList.add(PubMedDoc.fromJSON(jsonArray.getJSONObject(i)));
        }
        return pmdList;
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SearchQueryGenerator", options);
        System.exit(1);
    }

    public static void cli(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option runNoOption = Option.builder("r").hasArg().required().desc("runNo").build();
        Options options = new Options();
        options.addOption(help);
        options.addOption(runNoOption);

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
        String runNo = line.getOptionValue('r');
        ISearchResultCollector searchResultCollector = new SearchAbstractLoader(runNo); // run6 baseline

        QueryCoveragePerfUtil pu = new QueryCoveragePerfUtil(searchResultCollector);

        String HOME_DIR = System.getProperty("user.home");

        pu.setKnnResultsDir(HOME_DIR+ "/data/bioasq/rawmd");

        pu.showCoverage(-1);
    }

    public static void main(String[] args) throws Exception {
        cli(args);
    }
}
