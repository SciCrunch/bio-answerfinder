package org.bio_answerfinder.services;


import org.bio_answerfinder.util.SQLiteUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by bozyurt on 9/5/17.
 */
public class AcronymService {
    private Connection con;
    private static String dbFile = "/usr/local/lt/acronyms.db";

    public AcronymService() {
    }

    public void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        SQLiteUtils.optimizeWritePerformance(con);
    }

    public void shutdown() {
        SQLiteUtils.close(con);
    }

    public List<AcronymCluster> findExpansions(String acronym) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select expansion, freq, cluster_id from acronyms where acronym = ? order by cluster_id, freq desc");
            pst.setString(1, acronym);
            ResultSet rs = pst.executeQuery();
            LinkedHashMap<Integer, AcronymCluster> map = new LinkedHashMap<>();
            while (rs.next()) {
                String expansion = rs.getString(1);
                int freq = rs.getInt(2);
                int clusterId = rs.getInt(3);
                AcronymCluster ac = map.get(clusterId);
                if (ac == null) {
                    ac = new AcronymCluster(acronym);
                    map.put(clusterId, ac);
                }
                ac.addExpansion(new Expansion(expansion, freq));
            }
            rs.close();
            return new ArrayList<>(map.values());

        } catch (SQLException se) {
            System.err.println(se.getMessage());
            return Collections.emptyList();
        } finally {
            SQLiteUtils.close(pst);
        }
    }

    public String findAcronym(String expansion) {
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select acronym, freq from acronyms where expansion = ? order by freq desc");
            pst.setString(1, expansion);
            ResultSet rs = pst.executeQuery();
            String acronym = null;
            if (rs.next()) {
                acronym = rs.getString(1);
            }
            rs.close();
            return acronym;
        } catch (SQLException se) {
            System.err.println(se.getMessage());
            return null;
        } finally {
            SQLiteUtils.close(pst);
        }
    }

    public static class AcronymCluster {
        final String acronym;
        List<Expansion> expansions = new ArrayList<>(1);
        int totFreq = 0;

        public AcronymCluster(String acronym) {
            this.acronym = acronym;
        }

        void addExpansion(Expansion exp) {
            expansions.add(exp);
            totFreq += exp.freq;
        }

        public String getAcronym() {
            return acronym;
        }

        public List<Expansion> getExpansions() {
            return expansions;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("AcronymCluster{");
            sb.append("acronym='").append(acronym).append('\'');
            sb.append(", expansions=").append(expansions);
            sb.append(", totFreq=").append(totFreq);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class Expansion {
        final String expansion;
        final int freq;

        public Expansion(String expansion, int freq) {
            this.expansion = expansion;
            this.freq = freq;
        }

        public String getExpansion() {
            return expansion;
        }

        public int getFreq() {
            return freq;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Expansion{");
            sb.append("expansion='").append(expansion).append('\'');
            sb.append(", freq=").append(freq);
            sb.append('}');
            return sb.toString();
        }
    }


    public static void main(String[] args) throws Exception {
        AcronymService as = new AcronymService();
        try {
            as.initialize();
            List<AcronymCluster> expansions = as.findExpansions("FDA");
            for(AcronymCluster ac : expansions) {
                System.out.println(ac);
            }
        } finally {
            as.shutdown();
        }

    }
}
