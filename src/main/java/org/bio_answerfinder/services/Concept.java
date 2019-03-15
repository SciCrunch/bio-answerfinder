package org.bio_answerfinder.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/8/17.
 */
public class Concept {
    boolean deprecated = false;
    String iri;
    List<String> labels = new ArrayList<>(1);
    String curie;
    List<String> categories = new ArrayList<>(1);
    List<String> synonyms = new ArrayList<>(2);
    List<String> acronyms = new ArrayList<>(1);
    List<String> abbreviations = new ArrayList<>(1);

    public Concept() {
    }


    public Concept(String label, String curie, String category) {
        this.curie = curie;
        this.labels.add(label);
        if (category != null) {
            categories.add(category);
        }
    }

    public static Concept fromJSON(JSONObject json) {
        Concept concept = new Concept();
        concept.iri = json.getString("iri");
        if (json.has("curie")) {
            concept.curie = json.get("curie").toString();
        }
        concept.deprecated = json.getBoolean("deprecated");
        JSONArray jsonArray = json.getJSONArray("labels");
        for (int i = 0; i < jsonArray.length(); i++) {
            concept.labels.add(jsonArray.getString(i));
        }
        jsonArray = json.getJSONArray("categories");
        for (int i = 0; i < jsonArray.length(); i++) {
            concept.categories.add(jsonArray.getString(i));
        }
        jsonArray = json.getJSONArray("synonyms");
        for (int i = 0; i < jsonArray.length(); i++) {
            concept.synonyms.add(jsonArray.getString(i));
        }
        jsonArray = json.getJSONArray("acronyms");
        for (int i = 0; i < jsonArray.length(); i++) {
            concept.acronyms.add(jsonArray.getString(i));
        }
        jsonArray = json.getJSONArray("abbreviations");
        for (int i = 0; i < jsonArray.length(); i++) {
            concept.abbreviations.add(jsonArray.getString(i));
        }
        return concept;
    }

    public String getIri() {
        return iri;
    }

    public List<String> getLabels() {
        return labels;
    }

    public String getCurie() {
        return curie;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getSynonyms() {
        return synonyms;
    }

    public List<String> getAcronyms() {
        return acronyms;
    }

    public List<String> getAbbreviations() {
        return abbreviations;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Concept{");
        sb.append("iri='").append(iri).append('\'');
        sb.append(", synonyms=").append(synonyms);
        sb.append(", categories=").append(categories);
        sb.append(", acronyms=").append(acronyms);
        sb.append(", abbreviations=").append(abbreviations);
        sb.append(", labels=").append(labels);
        sb.append(", curie='").append(curie).append('\'');
        sb.append(", deprecated=").append(deprecated);
        sb.append('}');
        return sb.toString();
    }
}
