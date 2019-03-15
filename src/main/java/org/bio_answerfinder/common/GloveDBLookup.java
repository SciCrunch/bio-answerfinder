package org.bio_answerfinder.common;


import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.LRUCache;
import org.bio_answerfinder.util.SQLiteUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;

/**
 * Created by bozyurt on 12/20/18.
 */
public class GloveDBLookup {
    private Connection con;
    private String dbFile;
    private Map<String, float[]> cache = Collections.synchronizedMap(new LRUCache<>(50000));
    private static GloveDBLookup instance = null;

    public synchronized static GloveDBLookup getInstance(String dbFile) throws Exception {
        if (instance == null) {
            instance = new GloveDBLookup(dbFile);
            instance.initialize();
        }
        return instance;
    }

    public synchronized static GloveDBLookup getInstance() {
        if (instance == null) {
            throw new RuntimeException("Not properly initialized!");
        }
        return instance;
    }

    private GloveDBLookup(String dbFile) {
        this.dbFile = dbFile;
    }

    private void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        SQLiteUtils.optimizeReadOnlyPerformance(con);
    }

    public void shutdown() {
        SQLiteUtils.close(con);
    }

    public float[] getGloveVector(String term) {
        float[] vec = cache.get(term);
        if (vec != null) {
            if (vec.length == 0) {
                return null;
            }
            return vec;
        }

        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select vector from glove_vecs where term = ?");
            pst.setString(1, term);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                vec = new float[100];
                LittleEndianDataInputStream in = null;
                try {
                    in =  new LittleEndianDataInputStream(rs.getBinaryStream(1));
                    for (int i = 0; i < 100; i++) {
                        vec[i] = in.readFloat();
                    }

                } finally {
                    FileUtils.close(in);
                }
                cache.put(term, vec);
                return vec;
            } else {
                cache.put(term, new float[0]);
            }
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            SQLiteUtils.close(pst);
        }
        return null;
    }


    public static void main(String[] args) throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String dbFile = HOME_DIR + "/medline_glove_v2.db";
        GloveDBLookup lookup = null;
        try {
            lookup = GloveDBLookup.getInstance(dbFile);
            String[] terms = "was is burak Alzheimer CVLT BRCA".split("\\s+");

            for(String term : terms) {
                float[] vec = lookup.getGloveVector(term);
                System.out.println(term + ": ");
                if (vec != null) {
                    for (float el : vec) {
                        System.out.print(el + ", ");
                    }
                }
                System.out.println();
            }
        } finally {
            if (lookup != null) {
                lookup.shutdown();
            }
        }

    }
}
