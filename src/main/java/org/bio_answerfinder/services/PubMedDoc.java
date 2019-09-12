package org.bio_answerfinder.services;

import org.bio_answerfinder.util.GenUtils;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 6/5/17.
 */
public class PubMedDoc {
    String title;
    String documentAbstract;
    String journal;
    String pmid;
    String year;
    Object sections;
    double score;
    List<MeshHeading> meshHeadings = new ArrayList<MeshHeading>(5);
    boolean fullTextAvailable = false;


    public PubMedDoc() {
    }

    public PubMedDoc(String pmid) {
        this.pmid = pmid;
    }

    public PubMedDoc(String pmid, String title, String documentAbstract) {
        this.pmid = pmid;
        this.title = title;
        this.documentAbstract = documentAbstract;
    }

    public String getTitle() {
        return title;
    }

    public String getDocumentAbstract() {
        return documentAbstract;
    }

    public String getJournal() {
        return journal;
    }

    public String getPmid() {
        return pmid;
    }

    public String getYear() {
        return year;
    }

    public double getScore() {
        return score;
    }

    public List<MeshHeading> getMeshHeadings() {
        return meshHeadings;
    }

    public boolean isFullTextAvailable() {
        return fullTextAvailable;
    }

    public static String getString(JSONObject json, String fieldName) {
        if (json.has(fieldName)) {
            return json.get(fieldName).toString();
        }
        return null;
    }

    public static PubMedDoc fromPubmedFetchXML(Element pmaElement) {
        Element medlineCitationEl = pmaElement.getChild("MedlineCitation");
        PubMedDoc pmd = new PubMedDoc();
        pmd.pmid = medlineCitationEl.getChildTextTrim("PMID");
        Element articleEl = medlineCitationEl.getChild("Article");
        if (articleEl != null) {
            Element journalEl = articleEl.getChild("Journal");
            if (journalEl != null) {
                pmd.journal = journalEl.getChildTextTrim("Title");
                Element issueEl = journalEl.getChild("JournalIssue");
                if (issueEl != null && issueEl.getChild("PubDate") != null) {
                    pmd.year = issueEl.getChild("PubDate").getChildTextTrim("Year");
                }
            }
            String title = articleEl.getChildTextTrim("ArticleTitle");
            title = title.replace("^\\[","");
            title = title.replace("\\]\\.$",".");
            pmd.title = title;
            Element abstractEl = articleEl.getChild("Abstract");
            if (abstractEl != null) {
                List<Element> atList = abstractEl.getChildren("AbstractText");
                StringBuilder sb = new StringBuilder(atList.size() * 500);
                for(Element atEl : atList) {
                    sb.append(atEl.getTextTrim()).append(' ');
                }
                String abstractText = sb.toString().trim();
                pmd.documentAbstract = abstractText;
            } else {
                return null;
            }
        }

        return pmd;
    }

    public static PubMedDoc fromElasticJSON(JSONObject json) {
        PubMedDoc pmd = new PubMedDoc();
        JSONObject dc = json.getJSONObject("_source").getJSONObject("dc");
        if (dc.has("description")) {
            pmd.documentAbstract = dc.getString("description");
        }
        if (dc.has("title")) {
            pmd.title = dc.getString("title");
        }
        if (dc.has("publicationYear")) {
            pmd.year = dc.getString("publicationYear");
        }
        String identifier = dc.getString("identifier");
        int idx = identifier.lastIndexOf(':');
        pmd.pmid = identifier.substring(idx + 1);
        if (dc.has("publishers")) {
            String journal = dc.getJSONArray("publishers").getJSONObject(0).getString("name");
            pmd.journal = journal;
        }
        pmd.score = json.getDouble("_score");
        return pmd;
    }

    public JSONObject toJSON2() {
        JSONObject json = new JSONObject();
        json.put("documentAbstract", this.documentAbstract);
        if (title != null) {
            json.put("title", title);
        }
        if (year != null) {
            json.put("year", year);
        }
        json.put("pmid", pmid);
        json.put("score", this.score);
        if (journal != null) {
            json.put("journal", journal);
        }

        return json;
    }

    public static PubMedDoc fromJSON2(JSONObject json) {
        PubMedDoc pmd = new PubMedDoc();
        pmd.documentAbstract = json.getString("documentAbstract");
        if (json.has("title")) {
            pmd.title = json.getString("title");
        }
        pmd.pmid = json.getString("pmid");
        pmd.score = json.getDouble("score");
        if (json.has("year")) {
            pmd.year = json.getString("year");
        }
        if (json.has("journal")) {
            pmd.journal = json.getString("journal");
        }
        return pmd;
    }

    public static PubMedDoc fromJSON(JSONObject json) {
        PubMedDoc pmd = new PubMedDoc();
        pmd.documentAbstract = json.getString("documentAbstract");
        if (json.has("title")) {
            pmd.title = json.get("title").toString();
        }
        pmd.journal = json.getString("journal");
        pmd.fullTextAvailable = json.getBoolean("fulltextAvailable");
        pmd.pmid = json.getString("pmid");

        pmd.year = getString(json, "year");
        pmd.sections = json.get("sections");
        Map<String, MeshHeading> meshMap = new HashMap<>();
        Object meshAnnotations = json.get("meshAnnotations");
        if (json.has("meshAnnotations") && meshAnnotations != JSONObject.NULL) {

            JSONArray jsonArray = json.getJSONArray("meshAnnotations");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject ma = jsonArray.getJSONObject(i);
                String termLabel = ma.getString("termLabel");
                JSONObject uri = ma.getJSONObject("uri");
                String id = uri.getString("id");
                String namespace = uri.getString("namespace");
                MeshHeading mh = new MeshHeading(termLabel, id, namespace);
                meshMap.put(termLabel, mh);
            }
        }
        Object meshHeading = json.get("meshHeading");
        if (json.has("meshHeading") && meshHeading != JSONObject.NULL) {
            JSONArray jsonArray = json.getJSONArray("meshHeading");
            for (int i = 0; i < jsonArray.length(); i++) {
                String heading = jsonArray.getString(i);
                if (meshMap.containsKey(heading)) {
                    pmd.meshHeadings.add(meshMap.get(heading));
                } else {
                    pmd.meshHeadings.add(new MeshHeading(heading, null, null));
                }
            }
        }
        if (json.has("score")) {
            pmd.score = json.getDouble("score");
        }
        return pmd;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PubMedDoc{");
        sb.append("title='").append(title).append("'\n");
        sb.append("\nscore:").append(score);
        sb.append(", documentAbstract='").append(GenUtils.formatText(documentAbstract, 100)).append('\'');
        sb.append(", journal='").append(journal).append('\'');
        sb.append(", pmid='").append(pmid).append('\'');
        sb.append(", year='").append(year).append('\'');
        sb.append(", sections=").append(sections);
        sb.append(", meshHeadings=\n");
        for (MeshHeading mh : meshHeadings) {
            sb.append("\t").append(mh).append('\n');
        }
        sb.append(", fullTextAvailable=").append(fullTextAvailable);
        sb.append('}');
        return sb.toString();
    }

    public static class MeshHeading {
        String termLabel;
        String ID;
        String namespace;

        public MeshHeading(String termLabel, String ID, String namespace) {
            this.termLabel = termLabel;
            this.ID = ID;
            this.namespace = namespace;
        }

        public String getTermLabel() {
            return termLabel;
        }

        public String getID() {
            return ID;
        }

        public String getNamespace() {
            return namespace;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("MeshHeading{");
            sb.append("termLabel='").append(termLabel).append('\'');
            sb.append(", ID='").append(ID).append('\'');
            sb.append(", namespace='").append(namespace).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
