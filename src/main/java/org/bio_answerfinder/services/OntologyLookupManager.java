package org.bio_answerfinder.services;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 12/7/17.
 */
public class OntologyLookupManager {
    Map<String, List<OntologyRec>> lookupTable = new HashMap<String, List<OntologyRec>>();
    Map<String, LabelParentRec> curieTable = new HashMap<>();

    public void load(List<File> ontologyLTFileList) throws Exception {
        for (File file : ontologyLTFileList) {
            String jsonStr = FileUtils.loadAsString(file.getAbsolutePath(), CharSetEncoding.UTF8);
            JSONObject json = new JSONObject(jsonStr);
            jsonStr = null;
            for (String curie : json.keySet()) {
                JSONArray jsArr = json.getJSONArray(curie);
                JSONArray labels = jsArr.getJSONArray(0);
                JSONArray synonyms = jsArr.getJSONArray(1);
                JSONArray parents = jsArr.getJSONArray(2);
                prepOntologyRecs(curie, labels, parents);
                prepOntologyRecs(curie, synonyms, parents);
            }
        }
    }


    public List<OntologyRec> lookup(String term) {
        return lookupTable.get(term);
    }

    public LabelParentRec lookupByCurie(String curie) {
        return curieTable.get(curie);
    }

    public boolean isA(String term, String type) {
        List<OntologyRec> orList = lookupTable.get(term);
        if (orList == null || orList.isEmpty()) {
            return false;
        }
        OntologyRec ontologyRec = orList.get(0);
        String curie = ontologyRec.getCurie();
        do {
            LabelParentRec lpr = curieTable.get(curie);
            if (lpr == null) {
                break;
            }
            if (lpr.matches(type, true)) {
                return true;
            }
            if (lpr.parentCurieList.isEmpty()) {
                break;
            }
            curie = lpr.parentCurieList.get(0);
        } while (curie != null);

        return false;
    }

    public void showPath(String term) {
        List<OntologyRec> orList = lookupTable.get(term);
        if (orList == null || orList.isEmpty()) {
            return;
        }
        // System.out.println(orList);
        for(OntologyRec ontologyRec : orList) {
            System.out.println(ontologyRec);
            String curie = ontologyRec.getCurie();
            String indent = "\t";
            do {
                LabelParentRec lpr = curieTable.get(curie);
                if (lpr == null) {
                    break;
                }
                System.out.println(indent + lpr);
                if (lpr.parentCurieList.isEmpty()) {
                    break;
                }
                curie = lpr.parentCurieList.get(0);
                indent += "\t";
            } while (curie != null);
            System.out.println("-----------");
        }
    }

    public List<OntologyRec> getLongestMatchingOntologyMappings(String phrase) {
        String[] tiList = phrase.split("\\s+");
        if (tiList.length == 1) {
            return lookupTable.get(phrase);
        }
        // first try full match
        List<OntologyRec> ontologyRecs = lookupTable.get(phrase);
        if (ontologyRecs != null) {
            return ontologyRecs;
        }
        // try partial matches. The head word in NPs is usually the last word so try dropping terms
        // from the start of the phrase
        StringBuilder sb = new StringBuilder(phrase.length() + 5);
        int noTokens = tiList.length;
        for (int i = 1; i < noTokens; i++) {
            sb.setLength(0);
            for (int j = i; j < noTokens; j++) {
                sb.append(tiList[j]).append(' ');
            }
            String subPhrase = sb.toString().trim();
            // any terms less than 2 characters are too ambiguous
            if (subPhrase.length() > 2) {
                ontologyRecs = lookupTable.get(subPhrase);
                if (ontologyRecs != null) {
                    return ontologyRecs;
                }
            }
        }
        for (int i = 0; i < noTokens - 1; i++) {
            sb.setLength(0);
            for (int j = 0; j <= i; j++) {
                sb.append(tiList[j]).append(' ');
            }
            String subPhrase = sb.toString().trim();
            // any terms less than 2 characters are too ambiguous
            if (subPhrase.length() > 2) {
                ontologyRecs = lookupTable.get(subPhrase);
                if (ontologyRecs != null) {
                    return ontologyRecs;
                }
            }
        }
        return null;
    }

    private void prepOntologyRecs(String curie, JSONArray labels, JSONArray parents) {
        for (int i = 0; i < labels.length(); i++) {
            String label = labels.getString(i);
            OntologyRec ontologyRec = new OntologyRec(curie);
            for (int j = 0; j < parents.length(); j++) {
                ontologyRec.addParentCurie(parents.getString(j));
            }
            List<OntologyRec> list = lookupTable.get(label);
            if (list == null) {
                list = new ArrayList<OntologyRec>(1);
                lookupTable.put(label, list);
            }
            list.add(ontologyRec);
        }
        if (!curieTable.containsKey(curie)) {
            LabelParentRec lpr = new LabelParentRec();
            curieTable.put(curie, lpr);
            for (int i = 0; i < labels.length(); i++) {
                lpr.addLabel(labels.getString(i));
            }
            for (int j = 0; j < parents.length(); j++) {
                lpr.addParentCurie(parents.getString(j));
            }
        }
    }


    public static class LabelParentRec {
        final List<String> labels = new ArrayList<>(1);
        final List<String> parentCurieList = new ArrayList<String>(1);

        public LabelParentRec() {
        }

        public void addParentCurie(String parentCurie) {
            parentCurieList.add(parentCurie);
        }

        public void addLabel(String label) {
            labels.add(label);
        }

        public boolean matches(String type, boolean caseInsensitive) {
            for (String label : labels) {
                if (caseInsensitive) {
                    if (label.equalsIgnoreCase(type)) {
                        return true;
                    }
                } else {
                    if (label.equals(type)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LabelParentRec)) return false;

            LabelParentRec that = (LabelParentRec) o;

            if (!labels.equals(that.labels)) return false;
            return parentCurieList.equals(that.parentCurieList);

        }

        @Override
        public int hashCode() {
            int result = labels.hashCode();
            result = 31 * result + parentCurieList.hashCode();
            return result;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("LabelParentRec{");
            sb.append("labels=").append(labels);
            sb.append(", parentCurieList=").append(parentCurieList);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class OntologyRec {
        final String curie;
        final List<String> parentCurieList = new ArrayList<String>(1);

        public OntologyRec(String curie) {
            this.curie = curie;
        }

        public void addParentCurie(String parentCurie) {
            parentCurieList.add(parentCurie);
        }

        public String getCurie() {
            return curie;
        }

        public List<String> getParentCurieList() {
            return parentCurieList;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OntologyRec that = (OntologyRec) o;

            if (!curie.equals(that.curie)) return false;
            return parentCurieList.equals(that.parentCurieList);
        }

        @Override
        public int hashCode() {
            int result = curie.hashCode();
            result = 31 * result + parentCurieList.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("OntologyRec{");
            sb.append("curie='").append(curie).append('\'');
            sb.append(", parentCurieList=").append(parentCurieList);
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        List<File> ontologyLTFileList = new ArrayList<File>(2);
        String HOME_DIR = System.getProperty("user.home");
        ontologyLTFileList.add(new File(HOME_DIR + "/data/scigraph_lt/ontology-classes-with-labels-synonyms-parents.json"));
        ontologyLTFileList.add(new File(HOME_DIR + "/data/scigraph_lt/scr-classes-with-labels-synonyms-parents.json"));

        OntologyLookupManager olm = new OntologyLookupManager();
        olm.load(ontologyLTFileList);
        System.out.println(olm.lookup("mouse"));
        System.out.println(olm.lookup("ghrelin"));
        System.out.println(olm.lookup("TREM2"));

        System.out.println(olm.isA("ghrelin", "protein"));
        System.out.println(olm.lookupByCurie("NIFMOL:nifext_5012"));
        System.out.println(olm.lookup("schizophrenia"));
        System.out.println(olm.lookupByCurie("DOID:2468"));
        System.out.println(olm.lookupByCurie("DOID:1561"));
        System.out.println(olm.lookupByCurie("DOID:150"));
        System.out.println(olm.lookupByCurie("DOID:4"));
        System.out.println(olm.isA("schizophrenia", "disease"));
        olm.showPath("BRCA1");
        olm.showPath("POGZ");
        olm.showPath("ghrelin");
        System.out.println(olm.isA("ghrelin","molecular entity"));

        System.out.println( "subcellular: " + olm.isA("mitochondria","cellular_component"));
        olm.showPath("mitochondria");

        olm.showPath("muscle");
        olm.showPath("mouse");
        System.out.println( olm.isA("mouse","organism"));
    }
}

