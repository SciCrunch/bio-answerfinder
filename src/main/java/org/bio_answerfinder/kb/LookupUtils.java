package org.bio_answerfinder.kb;


import org.bio_answerfinder.services.Concept;
import org.bio_answerfinder.services.VocabularyService;
import org.bio_answerfinder.util.SQLiteUtils;
import org.bio_answerfinder.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class LookupUtils {
    protected Set<String> drugSet = new HashSet<>();
    protected Set<String> enzymeSet = new HashSet<>();
    public static Set<String> drugStopswords = new HashSet<>(Arrays.asList("fast", "complete", "same", "release",
            "solution", "water", "spectrum", "control", "the", "rabbit", "body", "hair", "muscle",
            "sleep", "first", "recovery", "care", "hand", "balance", "contact", "original", "mucosa",
            "cold", "horse", "sinus", "purpose", "maintain", "regimen", "regular", "fiber", "beef",
            "wheat", "soybean", "advanced", "healing", "nasal", "health", "orange", "easy",
            "arthritis", "iris", "green", "date", "attitude", "barley", "lobster", "purple",
            "potato", "turkey", "perform", "soma", "allergy", "burn", "primer", "lamb",
            "trout", "eagle", "duck", "egg", "white", "snail", "baby", "crab", "or serum",
            "salmon", "grape", "nose", "care", "protection", "coral", "patch", "clean", "carbon",
            "medicated", "as", "oyster", "chicken", "hygenie", "tomato", "beet", "goose",
            "2-10", "cough", "pain relief", "eggplant", "yolk", "bean", "pear", "cucumber",
            "sweet potato", "screen", "therapeutic", "corn", "wash", "rinse", "egg yolk",
            "a and d", "rubbing", "apple", "carrot", "poppy seed", "luxury", "sensible",
            "acne", "rapid action", "asparagus", "radish", "mist", "tuna", "alert", "37.4",
            "after burn", "clam", "lemon", "secure", "mask", "day", "turnip", "reform",
            "enema", "crest", "swabs","feminine", "tandem", "comfort", "assured", "rice",
            "prolong","the first", "prevent","beam","relief","react","shrimp","pacific",
            "spinach","scrub","horseradish","lettuce","enhancer","acacia","micro","palm"));

    public LookupUtils() {
    }

    public void initialize() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Connection con = DriverManager.getConnection("jdbc:sqlite:/usr/local/lt/drug_bank.db");
        PreparedStatement pst = null;
        try {
            pst = con.prepareStatement("select drug_name, other_names,enzyme  from drug_bank");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String drug = rs.getString(1);
                String otherNames = rs.getString(2);
                String enzyme = rs.getString(3);
                drugSet.add(drug.toLowerCase());
                if (otherNames != null) {
                    String[] synonyms = otherNames.split("\\s*;\\s*");
                    for (String synonym : synonyms) {
                        drugSet.add(synonym.toLowerCase());
                    }
                }
                if (enzyme != null) {
                    enzymeSet.add(enzyme);
                }
            }
            rs.close();
            enzymeSet.add("kinase");
            enzymeSet.add("ligase");
            enzymeSet.add("enzyme");
            System.out.println("# of enzymes:" + enzymeSet.size());
            System.out.println("# of drugs:" + drugSet.size());
        } finally {
            SQLiteUtils.close(pst);
            SQLiteUtils.close(con);
        }
    }


    public boolean isDrug(String drugTerm) {
        if (drugStopswords.contains(drugTerm.toLowerCase())) {
            return false;
        }
        return drugSet.contains(drugTerm.toLowerCase());
    }

    public boolean isEnyzme(String term) {
        return enzymeSet.contains(term.toLowerCase());
    }

    public boolean isDisease(String term) {
        if (term.equalsIgnoreCase("disease") || term.equalsIgnoreCase("disorder")) {
            return true;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasDiseaseConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isGene(String term) {
        if (term.equalsIgnoreCase("gene") || term.equalsIgnoreCase("oncogene") || term.equalsIgnoreCase("oncogenes")) {
            return true;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasGeneConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isProtein(String term) {
        if (term.equals("FDA")) {
            return false;
        }
        if (term.equalsIgnoreCase("histone") || term.equalsIgnoreCase("kinase") || term.equalsIgnoreCase("ligase")
                || term.equals("enzyme") || term.equalsIgnoreCase("protein")) {
            return true;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasProteinConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isMolecularEntity(String term) {
        if (term.equals("FDA")) {
            return false;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasMolecularEntityConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isSubcellularComponent(String term) {
        if (term.equalsIgnoreCase("triad") || term.equalsIgnoreCase("antibody")) {
            return false;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasSubcellularComponentConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isAnatomicalEntity(String term) {
        if (StringUtils.isAllCapital(term) || term.equals("web") || term.equals("element") || term.equals("role")
                || term.equals("system") || term.equals("depression") || term.equals("system") || term.equals("test")
                || term.equals("body") || term.equals("scale") || term.equals("fluid") || term.equals("organism")
                || term.equalsIgnoreCase("matrix") || term.equals("process") || term.equals("field")
                || term.equals("secretion")) {
            return false;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasAnatomicalEntity(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

    public boolean isOrganism(String term) {
        if (term.equalsIgnoreCase("cancer")) {
            return false;
        }
        try {
            List<Concept> concepts = VocabularyService.getConcepts4Term(term);
            return VocabularyService.hasOrganismConcept(concepts);
        } catch (Exception x) {
            System.err.println(x.getMessage());
        }
        return false;
    }

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
        if (isSubcellularComponent(term)) {
            entityTypes.add("subcellular component");
        }
        if (isAnatomicalEntity(term)) {
            entityTypes.add("anatomical entity");
        }

        return entityTypes;
    }

    public static void main(String[] args) throws Exception {
        LookupUtils lookupUtils = new LookupUtils();
        lookupUtils.initialize();
        System.out.println(lookupUtils.isDrug("ibuprofen"));


    }

}