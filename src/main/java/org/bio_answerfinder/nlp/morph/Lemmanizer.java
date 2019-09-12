package org.bio_answerfinder.nlp.morph;

import org.bio_answerfinder.common.types.NullObject;
import org.bio_answerfinder.util.LRUCache;
import org.bio_answerfinder.util.PersistentUtils;
import org.sqlite.SQLiteConfig;

import java.sql.*;
import java.util.StringTokenizer;

/**
 * Given a word (lexeme) determines no inflection form (lemma) of it.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Lemmanizer implements ILemmanizer {
    protected Connection con;
    // Profiler profiler;
    LRUCache lru;

    public Lemmanizer(String dbURL, String user, String pwd) throws Exception {
        if (dbURL.startsWith("jdbc:postgresql:")) {
            Class.forName("org.postgresql.Driver").newInstance();
            con = DriverManager.getConnection(dbURL, user, pwd);
        } else if (dbURL.startsWith("jdbc:sqlite:")) {
            Class.forName("org.sqlite.JDBC").newInstance();
            SQLiteConfig config = new SQLiteConfig();
            config.setCacheSize(10000);
            con = DriverManager.getConnection(dbURL, config.toProperties());
        } else {
            throw new Exception("Unsupported DB type:" + dbURL);
        }
        lru = new LRUCache(50000);
        // profiler = Profiler.getInstance("VotingWSD");
    }

    public void shutdown() {
        if (con != null) {
            try {
                con.close();
            } catch (Exception x) {
            }
        }
    }

    public TermMorphRecord getInfinitive(String verb) throws MorphException {

        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select content from morph_terms "
                    + "where key_term = ?");
            pst.setString(1, verb);
            rs = pst.executeQuery();
            if (rs.next()) {
                String content = rs.getString(1);
                TermMorphRecord[] tmr = extract(content);
                for (TermMorphRecord aTmr : tmr) {
                    if (aTmr.getPOS().equals("V")) {
                        // in XTAG morphology db hypenated forms of some verbs
                        // are
                        // before unhypenated ones
                        if (aTmr.baseWord.indexOf("-") == -1) {
                            return aTmr;
                        } else {
                            if (verb.startsWith("co-")
                                    || verb.startsWith("vitro-")
                                    || verb.startsWith("down-")
                                    || verb.startsWith("up-")) {
                                return aTmr;
                            }
                        }
                    }
                }
            }
            return null;

        } catch (SQLException se) {
            throw new MorphException(se);
        } finally {
            PersistentUtils.closeResultSet(rs);
            PersistentUtils.closeStatetement(pst);
        }
    }

    /**
     * @param term
     * @param posType 'V' for verb , 'A' for adjective and 'N' for noun
     * @return
     * @throws MorphException
     */
    public TermMorphRecord getLemma(String term, String posType)
            throws MorphException {
        // profiler.entryPoint("getLemma");
        StringBuilder sb = new StringBuilder();
        sb.append(term).append(':').append(posType);
        long key = sb.toString().hashCode();

        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            Object o = lru.get(key);
            if (o != null) {
                if (o == NullObject.nil) {
                    return null;
                } else {
                    return (TermMorphRecord) o;
                }
            }
            pst = con.prepareStatement("select content from morph_terms "
                    + "where key_term = ?");
            pst.setString(1, term);
            rs = pst.executeQuery();
            if (rs.next()) {
                String content = rs.getString(1);
                TermMorphRecord[] tmr = extract(content);
                for (TermMorphRecord aTmr : tmr) {
                    if (aTmr.getPOS().equals(posType)) {
                        lru.put(key, aTmr);
                        return aTmr;
                    }
                }
            }
            lru.put(key, NullObject.nil);
            return null;

        } catch (SQLException se) {
            throw new MorphException(se);
        } finally {
            PersistentUtils.closeResultSet(rs);
            PersistentUtils.closeStatetement(pst);
            // profiler.exitPoint("getLemma");
        }
    }

    protected TermMorphRecord[] extract(String content) {
        StringTokenizer stok = new StringTokenizer(content, "#");
        int numForms = stok.countTokens();
        TermMorphRecord[] tmrArr = new TermMorphRecord[numForms];
        int idx = 0;
        while (stok.hasMoreTokens()) {
            String recStr = stok.nextToken();
            StringTokenizer st = new StringTokenizer(recStr, "\t");
            String baseWord = st.nextToken();
            if (!st.hasMoreTokens()) {
                System.err.println(recStr);
            }
            String s = st.nextToken();
            int placeIdx = s.indexOf(' ');
            String POS;
            String inflection = null;
            if (placeIdx != -1) {
                POS = s.substring(0, placeIdx);
                inflection = s.substring(placeIdx + 1);
            } else {
                POS = s;
            }
            TermMorphRecord tmr = new TermMorphRecord(baseWord, inflection, POS);
            tmrArr[idx] = tmr;
            idx++;
        }
        return tmrArr;
    }

    public static void main(String[] args) throws Exception {
        Lemmanizer lemmanizer = null;
        try {
            lemmanizer = new Lemmanizer(
                    "jdbc:postgresql://localhost/bnlpkit", "postgres", "");
            TermMorphRecord tmr = lemmanizer.getInfinitive("was");
            System.out.println(tmr);

        } finally {
            if (lemmanizer != null) {
                lemmanizer.shutdown();
            }
        }
    }
}// ;
