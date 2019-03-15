package org.bio_answerfinder.services;


import org.bio_answerfinder.util.SQLiteUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 9/5/17.
 */
public class NominalizationService {
    private Connection con;
    private static String dbFile = "/usr/local/lt/nominalization.db";

    public NominalizationService() {
    }

    public void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        SQLiteUtils.optimizeWritePerformance(con);
    }

    public void shutdown() {
        SQLiteUtils.close(con);
    }

    public List<String> getNominalizations(String verb) {
        PreparedStatement pst = null;
        List<String> nomList = new ArrayList<>(1);
        try {
            pst = con.prepareStatement("select nom from noms where verb = ?");
            pst.setString(1, verb.toLowerCase());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String nom = rs.getString(1);
                nomList.add(nom);
            }
            rs.close();
            return nomList;
        } catch (SQLException se) {
            System.err.println(se.getMessage());
            return nomList;
        } finally {
            SQLiteUtils.close(pst);
        }
    }

    public List<String> getAdjectiveNominalizations(String adjective) {
        PreparedStatement pst = null;
        List<String> nomList = new ArrayList<>(1);
        try {
            pst = con.prepareStatement("select nom from nomadjs where adj = ?");
            pst.setString(1, adjective.toLowerCase());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String nom = rs.getString(1);
                nomList.add(nom);
            }
            rs.close();
            return nomList;
        } catch (SQLException se) {
            System.err.println(se.getMessage());
            return nomList;
        } finally {
            SQLiteUtils.close(pst);
        }
    }


    public static void main(String[] args) throws Exception {
        NominalizationService ns = new NominalizationService();
        try {
            ns.initialize();

            List<String> nominalizations = ns.getNominalizations("interfere");
            System.out.println(nominalizations);

        } finally {
            ns.shutdown();
        }
    }
}
