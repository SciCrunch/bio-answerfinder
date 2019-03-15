package org.bio_answerfinder.common;


import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.LRUCache;
import org.bio_answerfinder.util.SQLiteUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

/**
 * Created by bozyurt on 9/9/16.
 */
public class GloVeVectorManager implements Serializable {
    private static final long serialVersionUID = 1L;
    transient Connection con;
    transient LRUCache<String, List<Float>> cache = new LRUCache<String, List<Float>>(10000);
    transient Map<String, float[]> gloveMap;
    transient int gloveVecSize = -1;

    public GloVeVectorManager() {
    }

    public int getGloveVecSize() {
        return gloveVecSize;
    }

    public void initialize(String sqliteFile) throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        this.con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
        if (!SQLiteUtils.hasTable("glove_vecs", this.con)) {
            Statement st = null;
            try {
                st = this.con.createStatement();
                st.executeUpdate("create table glove_vecs (term varchar(200) primary key not null, " +
                        "vec varchar(1200) not null)");
            } finally {
                st.close();
            }
        }
        Statement st = null;
        try {
            st = this.con.createStatement();
            st.execute("PRAGMA synchronous = OFF");
            st.execute("PRAGMA journal_mode = MEMORY");
        } finally {
            st.close();
        }
    }


    public void populateDB(String gloveFile) throws Exception {
        BufferedReader in = null;
        int count = 0;
        try {
            in = FileUtils.newLatin1CharSetReader(gloveFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    int idx = line.indexOf(' ');
                    if (idx != -1) {
                        String term = line.substring(0, idx);
                        String vec = line.substring(idx + 1);
                        addTermGloveVec(term, vec);
                        count++;
                        if ((count % 1000) == 0) {
                            System.out.println(count + " vectors inserted so far");
                        }
                    }
                }
            }
            System.out.println("Total:" + count);
        } finally {
            FileUtils.close(in);
        }
    }

    public void cacheFromTextFile(String gloveTextFile) throws IOException {
        this.gloveMap = new HashMap<String, float[]>(600000);
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(gloveTextFile);
            String line;
            int count = 0;
            while ((line = in.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                String term = tokens[0];
                float[] vec = new float[tokens.length - 1];
                if (this.gloveVecSize < 0) {
                    this.gloveVecSize = tokens.length - 1;
                }
                for (int i = 1; i < tokens.length; i++) {
                    float value = Float.parseFloat(tokens[i]);
                    vec[i - 1] = value;
                }
                this.gloveMap.put(term, vec);
                count++;
                if ( (count % 1000) == 0) {
                    System.out.print("\rGlove vectors loaded so far:" + count);
                }
            }
            System.out.println("\ncached all Glove vectors");
        } finally {
            FileUtils.close(in);
        }
    }

    public float[] getTermGloveVector(String term) {
        return this.gloveMap.get(term);
    }

    public void cacheAllVectors() throws SQLException {
        PreparedStatement pst = null;
        this.gloveMap = new HashMap<String, float[]>(600000);
        try {
            pst = con.prepareStatement("select term, vec from glove_vecs");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String term = rs.getString(1);
                String vecStr = rs.getString(2);
                String[] tokens = vecStr.split("\\s+");
                float[] vec = new float[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    float value = Float.parseFloat(tokens[i]);
                    vec[i] = value;
                }
                this.gloveMap.put(term, vec);
            }
            System.out.println("cached all Glove vectors");
        } finally {
            if (pst != null) {
                pst.close();
            }
        }
    }


    public void findMostSimilars(String term) {
        float[] refVec = this.gloveMap.get(term);
        if (refVec != null) {
            List<Similarity> simList = new ArrayList<Similarity>(this.gloveMap.size() - 1);
            for (String aTerm : this.gloveMap.keySet()) {
                if (!aTerm.equals(term)) {
                    float[] vec = this.gloveMap.get(aTerm);
                    float sim = (float) cosSimilarity(refVec, vec);
                    simList.add(new Similarity(aTerm, sim));
                }
            }
            Collections.sort(simList, new Comparator<Similarity>() {
                @Override
                public int compare(Similarity o1, Similarity o2) {
                    return Float.compare(o2.sim, o1.sim);
                }
            });
            for (int i = 0; i < 10; i++) {
                Similarity similarity = simList.get(i);
                System.out.println(similarity.term + " (" + similarity.sim + ")");
            }
        }
    }

    public boolean hasVector(String term) {
        return this.gloveMap.containsKey(term.toLowerCase());
    }

    public double findCosSimilarity(String term1, String term2) {
        float[] vec1 = this.gloveMap.get(term1);
        float[] vec2 = this.gloveMap.get(term2);
        if (vec1 == null || vec2 == null) {
            if (term1.equals(term2)) {
                return 1.0;
            }
            return 0;
        }
        return cosSimilarity(vec1, vec2);
    }

    public void findMostSimilarsDiscrete(String term) {
        float[] refVec = this.gloveMap.get(term);
        int numBins = 8; // 8
        if (refVec != null) {
            float[] max_vals = new float[refVec.length];
            float[] min_vals = new float[refVec.length];
            for (int i = 0; i < refVec.length; i++) {
                min_vals[i] = Float.MAX_VALUE;
                max_vals[i] = Float.MIN_VALUE;
            }
            for (float[] vec : this.gloveMap.values()) {
                for (int i = 0; i < vec.length; i++) {
                    float value = vec[i];
                    if (value > max_vals[i]) {
                        max_vals[i] = value;
                    }
                    if (value < min_vals[i]) {
                        min_vals[i] = value;
                    }
                }
            }

            BitSet refBS = discretize(refVec, min_vals, max_vals, numBins);
            List<Similarity> simList = new ArrayList<Similarity>(this.gloveMap.size() - 1);
            for (String aTerm : this.gloveMap.keySet()) {
                if (!aTerm.equals(term)) {
                    float[] vec = this.gloveMap.get(aTerm);
                    BitSet bs = discretize(vec, min_vals, max_vals, numBins);
                    //float sim = (float) jaccardSim(refBS, bs);
                    float sim = (float) weightedJaccard(refBS, bs);
                    simList.add(new Similarity(aTerm, sim));
                }
            }
            Collections.sort(simList, new Comparator<Similarity>() {
                @Override
                public int compare(Similarity o1, Similarity o2) {
                    return Float.compare(o2.sim, o1.sim);
                }
            });
            for (int i = 0; i < 10; i++) {
                Similarity similarity = simList.get(i);
                System.out.println(similarity.term + " (" + similarity.sim + ")");
            }
        }
    }

    public static class Similarity implements Serializable {
        String term;
        float sim;

        public Similarity(String term, float sim) {
            this.term = term;
            this.sim = sim;
        }
    }

    public Ranges getRanges() throws SQLException {
        double[] min_vals;
        double[] max_vals;
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select idx, min_val, max_val from glove_vecs_range order by idx");
            ResultSet rs = pst.executeQuery();
            List<Double> minVals = new ArrayList<Double>(100);
            List<Double> maxVals = new ArrayList<Double>(100);
            while (rs.next()) {
                minVals.add(rs.getDouble(2));
                maxVals.add(rs.getDouble(3));
            }
            rs.close();
            min_vals = new double[minVals.size()];
            max_vals = new double[maxVals.size()];
            for (int i = 0; i < minVals.size(); i++) {
                min_vals[i] = minVals.get(i);
                max_vals[i] = maxVals.get(i);
            }
            return new Ranges(min_vals, max_vals);
        } finally {
            if (pst != null) {
                pst.close();
            }
        }

    }

    public static class Ranges implements Serializable {
        double[] min_vals;
        double[] max_vals;

        public Ranges(double[] min_vals, double[] max_vals) {
            this.min_vals = min_vals;
            this.max_vals = max_vals;
        }


        public List<Integer> discretize(List<Float> gloveVec, int numBins) {
            List<Integer> idxList = new ArrayList<Integer>(gloveVec.size());
            int len = gloveVec.size();
            int offset = 0;
            for (int i = 0; i < len; i++) {
                float value = gloveVec.get(i);
                double range = max_vals[i] - min_vals[i];
                double binSize = range / numBins;
                double curBinEnd = min_vals[i] + binSize;
                for (int j = 0; j < numBins; j++) {
                    if (value <= curBinEnd) {
                        idxList.add(offset + j);
                        break;
                    }
                    curBinEnd += binSize;
                }
                offset += numBins;
            }
            return idxList;
        }

    }

    public void prepMaxMinValues() throws SQLException {
        if (!SQLiteUtils.hasTable("glove_vecs_range", this.con)) {
            Statement st = null;
            try {
                st = this.con.createStatement();
                st.executeUpdate("create table glove_vecs_range (idx integer primary key not null, " +
                        "min_val real not null, max_val real not null)");
            } finally {
                st.close();
            }
        }
        double[] min_vals = null;
        double[] max_vals = null;
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select vec from glove_vecs");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String vecStr = rs.getString(1);
                String[] tokens = vecStr.split("\\s+");
                if (min_vals == null) {
                    min_vals = new double[tokens.length];
                    max_vals = new double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) {
                        min_vals[i] = Double.MAX_VALUE;
                        max_vals[i] = Double.MIN_VALUE;
                    }
                }
                for (int i = 0; i < tokens.length; i++) {
                    float value = Float.parseFloat(tokens[i]);
                    if (value > max_vals[i]) {
                        max_vals[i] = value;
                    }
                    if (value < min_vals[i]) {
                        min_vals[i] = value;
                    }
                }
            }
        } finally {
            if (pst != null) {
                pst.close();
            }
        }

        try {
            pst = this.con.prepareStatement("insert into glove_vecs_range(idx, min_val, max_val)  values (?,?,?)");
            for (int i = 0; i < min_vals.length; i++) {
                pst.setInt(1, i);
                pst.setDouble(2, min_vals[i]);
                pst.setDouble(3, max_vals[i]);
                pst.executeUpdate();
            }
        } finally {
            pst.close();
        }
    }


    public static double dotProduct(float[] vec1, float[] vec2) {
        double sum = 0;
        for (int i = 0; i < vec1.length; i++) {
            sum += vec1[i] * vec2[i];
        }
        return sum;
    }

    public static double norm2(float[] vec) {
        double sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i] * vec[i];
        }
        if (sum == 0) {
            return 0;
        }
        return Math.sqrt(sum);
    }

    public static double cosSimilarity(float[] vec1, float[] vec2) {
        double ab = dotProduct(vec1, vec2);
        if (ab == 0) {
            return ab;
        }
        return ab / (norm2(vec1) * norm2(vec2));
    }

    public static double jaccardSim(BitSet vec1, BitSet vec2) {
        BitSet andVec = (BitSet) vec1.clone();
        andVec.and(vec2);
        BitSet orVec = (BitSet) vec1.clone();
        orVec.or(vec2);
        int andCard = andVec.cardinality();
        if (andCard == 0) {
            return 0;
        }
        return andCard / (double) orVec.cardinality();
    }

    public static double weightedJaccard(BitSet vec1, BitSet vec2) {
        BitSet orVec = (BitSet) vec1.clone();
        orVec.or(vec2);
        int len = vec1.length();
        double sum = 0;
        for (int i = 0; i < len; i++) {
            if (vec1.get(i) && vec2.get(i)) {
                sum += 1.0;
            } else if (vec1.get(i)) {
                if ((i - 1 >= 0 && vec2.get(i - 1)) || (i + 1 < len && vec2.get(i + 1))) {
                    sum += 0.5;
                }
            }
        }
        return sum / (double) orVec.cardinality();
    }

    public static BitSet discretize(float[] vec, float[] min_vals, float[] max_vals, int numBins) {
        BitSet bs = new BitSet(numBins * vec.length);
        int len = vec.length;
        int offset = 0;
        for (int i = 0; i < len; i++) {
            float value = vec[i];
            double range = max_vals[i] - min_vals[i];
            double binSize = range / numBins;
            double curBinEnd = min_vals[i] + binSize;
            for (int j = 0; j < numBins; j++) {
                if (value <= curBinEnd) {
                    bs.set(offset + j);
                    break;
                }
                curBinEnd += binSize;
            }
            offset += numBins;
        }
        return bs;
    }


    public List<Float> getGloVeVector(String term) throws SQLException {
        if (cache.containsKey(term)) {
            return cache.get(term);
        }
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select vec from glove_vecs where term = ?");
            pst.setString(1, term);
            ResultSet rs = pst.executeQuery();
            List<Float> vec = null;
            if (rs.next()) {
                String vecStr = rs.getString(1);
                String[] tokens = vecStr.split("\\s+");
                vec = new ArrayList<Float>(tokens.length);
                for (String token : tokens) {
                    vec.add(Float.parseFloat(token));
                }
            }
            rs.close();
            if (vec == null) {
                cache.put(term, Collections.<Float>emptyList());
                return Collections.emptyList();
            } else {
                cache.put(term, vec);
                return vec;
            }

        } finally {
            if (pst != null) {
                pst.close();
            }
        }
    }

    void addTermGloveVec(String term, String gloveVecStr) throws SQLException {
        PreparedStatement st = null;
        try {
            st = this.con.prepareStatement("insert into glove_vecs(term, vec)  values (?,?)");
            st.setString(1, term);
            st.setString(2, gloveVecStr);
            st.executeUpdate();
        } finally {
            st.close();
        }
    }

    public void shutdown() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // no op
            }
        }
    }



    private static void doPrepMaxMinValues() throws Exception {
        GloVeVectorManager man = null;
        try {
            man = new GloVeVectorManager();
            man.initialize("/usr/local/lt/pmc_glove.db");

            man.prepMaxMinValues();
        } finally {
            if (man != null) {
                man.shutdown();
            }
        }
    }

    private static void testSimilarity() throws Exception {
        GloVeVectorManager man = null;
        try {
            man = new GloVeVectorManager();
            man.initialize("/usr/local/lt/pmc_glove.db");

            man.cacheAllVectors();

            String[] terms = {"antibody", "mouse", "gene"};

            // String[] terms = {"Ca", "CA"};

            for (String term : terms) {
                System.out.println("Term:" + term);
                System.out.println("===================");
                System.out.println("discrete\n--------");
                man.findMostSimilarsDiscrete(term);
                System.out.println("--------");
                man.findMostSimilars(term);

            }
        } finally {
            if (man != null) {
                man.shutdown();
            }
        }
    }

    private static void testSimilarity2() throws Exception {
        GloVeVectorManager man = null;
        try {
            man = new GloVeVectorManager();
            // man.cacheFromTextFile("${home}/tools/glove/glove.6B.100d.txt");
            man.cacheFromTextFile("${home}/tools/glove/glove.840B.300d.txt");


            Scanner sc = new Scanner(System.in);
            do {
                System.out.println("Enter two comma separated terms:");
                String ans = sc.nextLine();
                if (ans.equalsIgnoreCase("exit")) {
                    break;
                }
                String[] tokens = ans.split("\\s*,\\s*");
                if (tokens.length >= 2) {
                    double cos = man.findCosSimilarity(tokens[0], tokens[1]);
                    System.out.println(tokens[0] + " -- " + tokens[1] + ":" + cos);
                }
            } while (true);


        } finally {
            if (man != null) {
                man.shutdown();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // doPrepMaxMinValues();
        testSimilarity2();

    }

    private static void testIt() throws Exception {
        GloVeVectorManager man = null;
        try {
            String HOME_DIR = System.getProperty("user.home");
            man = new GloVeVectorManager();
            man.initialize(HOME_DIR + "/pmc_glove.db");

            // man.populateDB(HOME_DIR + "/data/pmc_glove_vectors.txt");
            //  man.populateDB(HOME_DIR + "/tools/glove/pmc_glove_vectors.txt");
            String sentence = "Primary antibodies used were mouse monoclonal anti–β-actin ( A5441 ; Sigma-Aldrich ) , " +
                    "rat purified anti–caspase 1 ( 645102 ; BioLegend , Fell , Germany ) , " +
                    "caspase 1 p10 antibody ( M-20 ) ( sc-514 ; Santa Cruz Biotechnology , Heidelberg , Germany )";
            String[] tokens = sentence.split("\\s+");
            for (String token : tokens) {
                List<Float> gloVeVector = man.getGloVeVector(token);
                System.out.println(token + " : " + gloVeVector);
            }

        } finally {
            if (man != null) {
                man.shutdown();
            }
        }
    }
}
