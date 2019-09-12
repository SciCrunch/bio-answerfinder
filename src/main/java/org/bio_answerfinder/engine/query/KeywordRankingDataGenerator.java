package org.bio_answerfinder.engine.query;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 7/8/19.
 */
public class KeywordRankingDataGenerator {
    private GloveDBLookup gloveDBLookup;

    public KeywordRankingDataGenerator() throws Exception {
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        this.gloveDBLookup = GloveDBLookup.getInstance(dbFile);
    }

    public void shutdown() {
        if (this.gloveDBLookup != null) {
            this.gloveDBLookup.shutdown();
        }
    }

    public List<Pair<String[], int[]>> loadAnnotatedData(String rankingAnnotationFile) throws IOException {
        List<Pair<String[], int[]>> dataList = new ArrayList<>();
        try (BufferedReader bin = FileUtils.newUTF8CharSetReader(rankingAnnotationFile)) {
            String line;
            while ((line = bin.readLine()) != null) {
                if (line.startsWith("<")) {
                    line = bin.readLine();
                }
                String[] terms = line.split("\\s+");
                System.out.println(line);
                line = bin.readLine();
                String[] tokens = line.split("\\s+");
                int[] ranks = new int[tokens.length];
                ;
                for (int i = 0; i < tokens.length; i++) {
                    ranks[i] = Integer.parseInt(tokens[i]);
                }
                Assertion.assertEquals(terms.length, ranks.length);
                dataList.add(new Pair<>(terms, ranks));
            }
        }
        return dataList;
    }

    public void generatePairwiseRankings(List<Pair<String[], int[]>> dataList, int maxNumTokens, String outFile)
            throws Exception {
        List<RankingFeatureVector> list = new ArrayList<>(dataList.size() * 5);
        int qid = 0;
        for (Pair<String[], int[]> questionData : dataList) {

            List<RankingFeatureVector> rfvList = generatePairwiseRankings(questionData, maxNumTokens, qid);
            if (rfvList != null && !rfvList.isEmpty()) {
                list.addAll(rfvList);
            }
            qid++;
        }
        try (BufferedWriter out = FileUtils.getBufferedWriter(outFile, CharSetEncoding.UTF8)) {
            int fvLen = list.get(0).getFeatures1().length;
            out.write(String.valueOf(list.size()) + " " + fvLen);
            out.newLine();
            for (RankingFeatureVector rfv : list) {
                out.write(rfv.getQid() + " " + rfv.getLabel() + " " + rfv.getKeyword1LocIdx() + " "
                        + rfv.getKeyword2LocIdx() + " ");
                StringBuilder sb = new StringBuilder(fvLen * 20);
                for (float value : rfv.getFeatures1()) {
                    sb.append(value).append(' ');
                }
                for (float value : rfv.getFeatures2()) {
                    sb.append(value).append(' ');
                }
                out.write(sb.toString().trim());
                out.newLine();
            }
            System.out.println("wrote " + outFile);
        }
    }


    public List<RankingFeatureVector> generatePairwiseRankings(Pair<String[], int[]> questionData,
                                                               int maxNumTokens, int qid) {
        List<RankingFeatureVector> rfvList = new ArrayList<>(10);
        Set<Integer> uniqueRankings = new HashSet<>();
        Map<Integer, List<Integer>> rank2TermLocMap = new HashMap<>();
        int[] ranks = questionData.getSecond();
        String[] terms = questionData.getFirst();
        for (int i = 0; i < ranks.length; i++) {
            int rank = ranks[i];
            String term = terms[i];
            if (rank > 0) {
                uniqueRankings.add(rank);
                List<Integer> termLocs = rank2TermLocMap.get(rank);
                if (termLocs == null) {
                    termLocs = new ArrayList<>(1);
                    rank2TermLocMap.put(rank, termLocs);
                }
                termLocs.addAll(findLocations(terms, ranks, term));
            }
        }
        if (uniqueRankings.size() <= 1) {
            return null;
        }
        List<Integer> uniqueRanks = new ArrayList<>(uniqueRankings);
        for (int i = 0; i < uniqueRanks.size(); i++) {
            Integer rank1 = uniqueRanks.get(i);
            List<Integer> termLocs1 = rank2TermLocMap.get(rank1);
            float[] fv1 = prepareFeatureVector(terms, termLocs1.get(0), maxNumTokens);
            for (int j = 0; j < uniqueRanks.size(); j++) {
                if (i == j) {
                    continue;
                }
                Integer rank2 = uniqueRanks.get(j);
                List<Integer> termLocs2 = rank2TermLocMap.get(rank2);
                float[] fv2 = prepareFeatureVector(terms, termLocs2.get(0), maxNumTokens);
                int label = rank1 > rank2 ? 1 : 0;
                RankingFeatureVector rfv = new RankingFeatureVector(fv1, fv2, label, qid,
                        termLocs1.get(0), termLocs2.get(0));
                rfvList.add(rfv);
            }
        }
        return rfvList;
    }

    float[] prepareFeatureVector(String[] terms, int termLocIdx, int maxNumTokens) {
        float[] fv = new float[maxNumTokens * 100];
        int n = Math.min(terms.length, maxNumTokens);
        int idx = 0;
        for (int i = 0; i < n; i++) {
            if (termLocIdx == i) {
                float[] sepVector = this.gloveDBLookup.getGloveVector("unk2");
                System.arraycopy(sepVector, 0, fv, idx * 100, 100);
                idx++;
            }
            String term = terms[i];
            float[] gloveVector = this.gloveDBLookup.getGloveVector(term);
            if (gloveVector == null) {
                gloveVector = this.gloveDBLookup.getGloveVector("unk1");
            }
            System.arraycopy(gloveVector, 0, fv, idx * 100, 100);
            idx++;
        }
        return fv;
    }

    public static List<Integer> findLocations(String[] terms, int[] ranks, String refTerm) {
        List<Integer> locIdxList = new ArrayList<>(1);
        for (int i = 0; i < terms.length; i++) {
            if (terms[i].equalsIgnoreCase(refTerm) && ranks[i] > 0) {
                locIdxList.add(i);
            }
        }
        return locIdxList;
    }

    public static class RankingFeatureVector {
        int qid;
        float[] features1;
        float[] features2;
        int label;
        int keyword1LocIdx;
        int keyword2LocIdx;

        public RankingFeatureVector(float[] features1, float[] features2, int label,
                                    int qid, int keyword1LocIdx, int keyword2LocIdx) {
            this.features1 = features1;
            this.features2 = features2;
            this.label = label;
            this.qid = qid;
            this.keyword1LocIdx = keyword1LocIdx;
            this.keyword2LocIdx = keyword2LocIdx;
        }

        public float[] getFeatures1() {
            return features1;
        }

        public float[] getFeatures2() {
            return features2;
        }

        public int getLabel() {
            return label;
        }

        public int getQid() {
            return qid;
        }

        public int getKeyword1LocIdx() {
            return keyword1LocIdx;
        }

        public int getKeyword2LocIdx() {
            return keyword2LocIdx;
        }
    }

    public static final String homeDir = System.getProperty("user.home");


    public static void createTestData()  throws Exception {
        //String dataDir = homeDir + "/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc";
        String dataDir = homeDir + "/dev/java/bio-answerfinder/data/rank_test";
        KeywordRankingDataGenerator generator = null;
        try {
            generator = new KeywordRankingDataGenerator();
            List<Pair<String[], int[]>> dataList = generator.loadAnnotatedData(dataDir + "/rank_test_data.dat");
            generator.generatePairwiseRankings(dataList, 40, "/tmp/test_ranking.txt");

        } finally {
            if (generator != null) {
                generator.shutdown();
            }
        }
    }

    public static void main(String[] args) throws Exception {
       // createTrainData();
        createTestData();
    }

    static void createTrainData() throws Exception {
        //String dataDir = homeDir + "/dev/java/bio-answerfinder/data/bioasq/bioasq_manual_100/qsc";
        String dataDir = homeDir + "/dev/java/bio-answerfinder/data/rank_test";
        KeywordRankingDataGenerator generator = null;
        try {
            generator = new KeywordRankingDataGenerator();
            List<Pair<String[], int[]>> dataList = generator.loadAnnotatedData(dataDir + "/rank_train_data.dat");
            generator.generatePairwiseRankings(dataList, 40, "/tmp/train_ranking.txt");

        } finally {
            if (generator != null) {
                generator.shutdown();
            }
        }
    }
}
