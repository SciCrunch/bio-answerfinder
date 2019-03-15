package org.bio_answerfinder.util;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bozyurt on 7/31/17.
 */
public class SQLiteUtils {

    public static boolean hasTable(String tableName, Connection con) throws SQLException {
        PreparedStatement st = null;
        try {
            st = con.prepareStatement("select name from sqlite_master where type='table' and name = ?");
            st.setString(1, tableName);
            ResultSet rs = st.executeQuery();
            boolean ok = rs.next();
            rs.close();
            return ok;
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    public static void close(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                // no op
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                // no op
            }
        }
    }


    public static void optimizeWritePerformance(Connection con) throws SQLException {
        Statement st = null;
        try {
            st = con.createStatement();
            st.execute("PRAGMA synchronous = OFF");
            st.execute("PRAGMA journal_mode = MEMORY");
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    public static void optimizeReadOnlyPerformance(Connection con) throws SQLException {
        Statement st = null;
        try {
            st = con.createStatement();
            st.execute("PRAGMA synchronous = OFF");
            st.execute("PRAGMA journal_mode = OFF");
            st.execute("PRAGMA query_only = ON");
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }
    public static Set<String> loadPhraseSet(String indexSqliteFile) throws Exception {
        Set<String> phraseSet = new HashSet<>();
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:" + indexSqliteFile);
        SQLiteUtils.optimizeWritePerformance(con);
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select term from terms");
            ResultSet rs = pst.executeQuery();
            int count = 0;
            while (rs.next()) {
                String term = rs.getString(1);
                if (term.indexOf(' ') != -1) {
                    phraseSet.add(term);
                }

                count++;
                if ((count % 1000) == 0) {
                    System.out.print("\rterms loaded so far:" + count);
                }
            }
            rs.close();
            System.out.println("\n");

        } finally {
            close(pst);
            close(con);
        }
        return phraseSet;
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // no op
            }
        }
    }
}
