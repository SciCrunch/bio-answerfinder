package org.bio_answerfinder.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.LRUCache;
import org.json.JSONArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 6/8/17.
 */
public class VocabularyService {
    final static String serviceURL = "http://cypher.neuinfo.org:9000";
    static Map<String, List<Concept>> lruCache = Collections.synchronizedMap(new LRUCache<String, List<Concept>>(8000));


    public static List<Concept> getConcepts4Term(String term) throws Exception {
        List<Concept> conceptList = lruCache.get(term);
        if (conceptList != null) {
            return conceptList;
        }
        //final HttpParams httpParams = new BasicHttpParams();
        //HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
        //HttpClient client = new DefaultHttpClient(httpParams);
        int timeout = 30000;
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout).setSocketTimeout(timeout);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfigBuilder.build()).build();
        URIBuilder builder = new URIBuilder(serviceURL);
        builder.setPath("/scigraph/vocabulary/term/" + term);
        // limit=20&searchSynonyms=true&searchAbbreviations=false&searchAcronyms=false
        builder.setParameter("searchSynonyms", "true");
        builder.setParameter("limit", "20");
        builder.setParameter("searchAbbreviations", "false");
        builder.setParameter("searchAcronyms", "false");
        try {
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);

            // System.out.println("uri:" + uri);
            httpGet.addHeader("Accept", "application/json");
            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String jsonStr = EntityUtils.toString(entity);
                JSONArray jsonArray = new JSONArray(jsonStr);
                List<Concept> concepts = new ArrayList<>(jsonArray.length());
                for (int i = 0; i < jsonArray.length(); i++) {
                    concepts.add(Concept.fromJSON(jsonArray.getJSONObject(i)));
                }
                lruCache.put(term, concepts);
                return concepts;
            } else if (response.getStatusLine().getStatusCode() == 404) {
                lruCache.put(term, Collections.EMPTY_LIST);
            }
        } finally {
            client.close();
            //client.getConnectionManager().shutdown();
        }
        return Collections.emptyList();
    }


    public static boolean hasOrganismConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (c.getCategories() != null && c.getCategories().contains("organism")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasProteinConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (!GenUtils.isEmpy(c.getCurie()) && c.getCurie().startsWith("PR:")) {
                return true;
            } else if (!GenUtils.isEmpty(c.getIri()) && c.getIri().indexOf("/PR_") != -1) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGeneConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (!GenUtils.isEmpy(c.getCurie()) && c.getCurie().indexOf("Gene:") != -1) {
                return true;
            } else if (!GenUtils.isEmpty(c.getIri()) && c.getIri().indexOf("gene") != -1) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasDiseaseConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (!GenUtils.isEmpy(c.getCurie()) && c.getCurie().startsWith("DOID:")) {
                return true;
            } else if (c.getCategories() != null && c.getCategories().contains("disease")) {
                return true;
            }

        }
        return false;
    }

    public static boolean hasMolecularEntityConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (c.getCategories() != null && c.getCategories().contains("molecular entity")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnatomicalEntity(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (c.getCategories() != null && c.getCategories().contains("anatomical entity")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSubcellularComponentConcept(List<Concept> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return false;
        }
        for (Concept c : concepts) {
            if (c.getCategories() != null && c.getCategories().contains("subcellular entity")) {
                return true;
            }
        }
        return false;
    }


    public static void main(String[] args) throws Exception {
        String term = "mouse";
        term = "TailorX";
        term = "COUGER";
        term = "gastrointestinal tract";
        term = "thyroid hormone resistance syndrome";
        term = "Food and Drug Administration";
        term = "TREM2";
        term = "Schizophrenia";
        term = "Parkinsons";
        term = "autism";
        term = "ADD";
        term = "Achondroplasia";
        term = "ghrelin";

        List<Concept> concepts = VocabularyService.getConcepts4Term(term);
        for (Concept concept : concepts) {
            System.out.println(concept);
        }
        System.out.println(hasDiseaseConcept(concepts));
        System.out.println(term + " - molecular entity:" + hasMolecularEntityConcept(concepts));

    }
}
