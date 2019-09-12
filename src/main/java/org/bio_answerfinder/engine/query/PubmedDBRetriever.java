package org.bio_answerfinder.engine.query;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.services.PubMedDoc;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.SQLiteUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 8/9/19.
 */
public class PubmedDBRetriever {
    Connection con;
    String dbFile;

    public PubmedDBRetriever(String dbFile) throws Exception {
        this.dbFile = dbFile;
    }

    public void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        SQLiteUtils.optimizeReadOnlyPerformance(con);
    }

    public List<PubMedDoc> retrieve(String question, TObjectFloatHashMap<String> vocabulary,
                                    GloveDBLookup gloveMan, int maxNumDocs) throws Exception {

        AKNNRetrievalServiceClient client = new AKNNRetrievalServiceClient("http://137.110.119.165");
        List<String> pmids = client.getSimilarAbstracts(question, gloveMan, vocabulary, maxNumDocs);
        return retrieve(pmids);
    }

    public List<PubMedDoc> retrieve(List<String> pmids) throws SQLException {
        if (pmids.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("select pmid, title, abstract from pubmed_abstracts where pmid in (");
        sb.append(GenUtils.join(pmids, ",")).append(")");
        Statement st = null;
        List<PubMedDoc> pmdList = new ArrayList<>(pmids.size());
        try {
            st = con.createStatement();
            ResultSet rs = st.executeQuery(sb.toString());
            while (rs.next()) {
                String pmid = String.valueOf(rs.getInt(1));
                String title = rs.getString(2);
                String description = rs.getString(3);
                PubMedDoc pmd = new PubMedDoc(pmid, title, description);
                pmdList.add(pmd);
            }
            rs.close();
            return pmdList;
        } finally {
            SQLiteUtils.close(st);
        }
    }

    public void shutdown() {
        SQLiteUtils.close(con);
    }

    public static void main(String[] args) throws Exception {
        GloveDBLookup gloveMan = null;
        PubmedDBRetriever retriever = null;
        try {
            Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
            String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
            gloveMan = GloveDBLookup.getInstance(dbFile);
            TObjectFloatHashMap<String> vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();
            String abstractsDbFile = props.getProperty("pubmed.abstracts.db");
            retriever = new PubmedDBRetriever(abstractsDbFile);
            retriever.initialize();
            String question = "Are genes symmetrically distributed between leading and lagging DNA strand in bacteria ?";
            List<PubMedDoc> pmdList = retriever.retrieve(question, vocabulary, gloveMan, 10);
            System.out.println("Q:" + question);
            pmdList.forEach(System.out::println);

        } finally {
            gloveMan.shutdown();
            retriever.shutdown();
        }
    }
}
