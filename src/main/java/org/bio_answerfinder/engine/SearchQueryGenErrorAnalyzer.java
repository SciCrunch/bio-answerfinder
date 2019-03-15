package org.bio_answerfinder.engine;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.SQLiteUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

/**
 * Created by bozyurt on 8/30/17.
 */
public class SearchQueryGenErrorAnalyzer {
    public static String HOME_DIR = System.getProperty("user.home");

    public static TObjectFloatHashMap<String> loadVocabularyWithWeights() throws Exception {
        Class.forName("org.sqlite.JDBC");
        double N = 16820914;
        TObjectFloatHashMap<String> vocabulary = new TObjectFloatHashMap<>();
        //String indexSqliteFile = HOME_DIR + "/data/medline_index/tv.db";
        Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
        String dbFile =  FileUtils.adjustPath(props.getProperty("term_vectors.db.file"));
        Assertion.assertExistingPath(dbFile, dbFile);
        Connection con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        SQLiteUtils.optimizeWritePerformance(con);
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select term, doc_freq from terms");
            ResultSet rs = pst.executeQuery();
            int count = 0;
            float maxIDF = -1;
            while (rs.next()) {
                String term = rs.getString(1);
                int df = rs.getInt(2);
                float idf = (float) -Math.log(df / N);
                if (maxIDF < idf) {
                    maxIDF = idf;
                }
                vocabulary.put(term, idf);
                count++;
                if ((count % 1000) == 0) {
                    System.out.print("\rterms loaded so far:" + count);
                }
            }
            rs.close();
            for (Object k : vocabulary.keys()) {
                String term = (String) k;
                float weight = vocabulary.get(term);
                weight /= maxIDF;
                vocabulary.put(term, weight);

            }
            System.out.println("\n");
            return vocabulary;
        } finally {
            SQLiteUtils.close(pst);
            SQLiteUtils.close(con);
        }
    }


}
