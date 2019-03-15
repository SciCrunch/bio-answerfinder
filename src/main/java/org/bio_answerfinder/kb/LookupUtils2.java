package org.bio_answerfinder.kb;


import org.bio_answerfinder.services.OntologyLookupManager;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.SQLiteUtils;
import org.bio_answerfinder.util.StringUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by bozyurt on 12/19/17.
 */
public class LookupUtils2 extends LookupUtils {
    protected OntologyLookupManager ontologyLookupManager;
    protected Set<String> geneSet = new HashSet<>();
    protected Set<String> diseaseSet = new HashSet<>();
    static String HOME_DIR = System.getProperty("user.home");
    public static Set<String> diseaseStopWords = new HashSet<>(Arrays.asList("sodium", "colon",
            "salt", "child", "rodents", "type 2", "ticks", "type b"));

    public static Set<String> anatomicalEntityStopWords = new HashSet<>(Arrays.asList("processes","adults","male","female",
            "wall","axis","juvenile","segment","crop","juveniles","back","extension","opening","sensor",
            "sole","calf","animal","vessel","radius","valve","margin","angular","front","helix",
            "spot","spots","bill","palm","duct","stripes","olive","atlas","siphon","digit","extremity",
            "adult","layer","posterior","surface"));

    public LookupUtils2() {
        super();
        ontologyLookupManager = new OntologyLookupManager();
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        List<File> ontologyLTFileList = new ArrayList<File>(2);
        ontologyLTFileList.add(new File(HOME_DIR + "/data/scigraph_lt/ontology-classes-with-labels-synonyms-parents.json"));
        ontologyLTFileList.add(new File(HOME_DIR + "/data/scigraph_lt/scr-classes-with-labels-synonyms-parents.json"));
        ontologyLookupManager.load(ontologyLTFileList);
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:/usr/local/lt/hugo.db");
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select symbol, gene_name  from hugo_genes");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String symbol = rs.getString(1);
                String geneName = rs.getString(2);
                geneSet.add(symbol);
                geneSet.add(geneName);
            }
            System.out.println("# gene names and symbols:" + geneSet.size());
            rs.close();
        } finally {
            SQLiteUtils.close(pst);
            SQLiteUtils.close(con);
        }
        loadDiseases();
    }

    void loadDiseases() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:/usr/local/lt/diseases.db");
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select disease,synonym, alt_name from disease_names");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String disease = rs.getString(1);
                String synonym = rs.getString(2);
                String altName = rs.getString(3);
                addTerm(disease, diseaseSet);
                addTerm(synonym, diseaseSet);
                addTerm(altName, diseaseSet);
            }
            System.out.println("# diseases:" + diseaseSet.size());
            rs.close();
        } finally {
            SQLiteUtils.close(pst);
            SQLiteUtils.close(con);
        }
    }

    static void addTerm(String term, Set<String> set) {
        if (GenUtils.isEmpty(term)) {
            return;
        }
        set.add(term);
        if (!StringUtils.isAllCapital(term)) {
            set.add(term.toLowerCase());
        }
    }

    @Override
    public boolean isDisease(String term) {
        if (diseaseStopWords.contains(term.toLowerCase())) {
            return false;
        }
        if (term.equalsIgnoreCase("disease") || term.equalsIgnoreCase("disorder")) {
            return true;
        }
        boolean ok = ontologyLookupManager.isA(term, "disease");
        if (ok) {
            return true;
        }
        return diseaseSet.contains(term);
    }

    @Override
    public boolean isProtein(String term) {
        if (term.equals("FDA")) {
            return false;
        }
        if (term.equalsIgnoreCase("histone") || term.equalsIgnoreCase("kinase") || term.equalsIgnoreCase("ligase")
                || term.equals("enzyme") || term.equalsIgnoreCase("protein")) {
            return true;
        }
        return ontologyLookupManager.isA(term, "protein");
    }

    @Override
    public boolean isGene(String term) {
        if (term.equalsIgnoreCase("gene") || term.equalsIgnoreCase("oncogene") || term.equalsIgnoreCase("oncogenes")) {
            return true;
        }
        return geneSet.contains(term);
    }

    @Override
    public boolean isMolecularEntity(String term) {
        if (term.equals("FDA")) {
            return false;
        }
        return ontologyLookupManager.isA(term, "molecular entity");
    }

    public boolean isCellularComponent(String term) {
        if (term.equalsIgnoreCase("triad") || term.equalsIgnoreCase("antibody")) {
            return false;
        }
        return ontologyLookupManager.isA(term, "cellular_component");
    }

    @Override
    public boolean isAnatomicalEntity(String term) {
        if (StringUtils.isAllCapital(term) || term.equals("web") || term.equals("element") || term.equals("role")
                || term.equals("system") || term.equals("depression") || term.equals("system") || term.equals("test")
                || term.equals("body") || term.equals("scale") || term.equals("fluid") || term.equals("organism")
                || term.equalsIgnoreCase("matrix") || term.equals("process") || term.equals("field")
                || term.equals("secretion")) {
            return false;
        }
        if (anatomicalEntityStopWords.contains(term.toLowerCase())) {
            return false;
        }
        return ontologyLookupManager.isA(term, "anatomical structure");
    }

    @Override
    public boolean isOrganism(String term) {
        if (term.equalsIgnoreCase("cancer")) {
            return false;
        }
        return ontologyLookupManager.isA(term, "organism");
    }

    @Override
    public List<String> getEntityType(String term) {
        if (term == null || term.trim().length() == 0 || term.equalsIgnoreCase("finding") || term.equalsIgnoreCase("top")) {
            return Collections.emptyList();
        }
        List<String> entityTypes = new ArrayList<>(1);
        if (isGene(term)) {
            entityTypes.add("gene");
        }
        if (isEnyzme(term)) {
            entityTypes.add("enzyme");
        }
        if (isProtein(term)) {
            entityTypes.add("protein");
        }
        if (isDisease(term)) {
            entityTypes.add("disease");
        }
        if (isDrug(term)) {
            entityTypes.add("drug");
        }
        if (isMolecularEntity(term)) {
            entityTypes.add("molecular entity");
        }
        if (isOrganism(term)) {
            entityTypes.add("organism");
        }
        if (isAnatomicalEntity(term)) {
            entityTypes.add("anatomical entity");
        }
        if (isCellularComponent(term)) {
            entityTypes.add("cellular component");
        }
        return entityTypes;
    }

    public static void main(String[] args) throws Exception {
        LookupUtils2 lookupUtils = new LookupUtils2();
        lookupUtils.initialize();

        System.out.println(lookupUtils.getEntityType("TREM2"));
        System.out.println(lookupUtils.getEntityType("diabetes"));
        System.out.println(lookupUtils.getEntityType("ADHD"));
        System.out.println(lookupUtils.getEntityType("Alzheimer"));
        System.out.println(lookupUtils.getEntityType("Hirschsprung"));
    }
}
