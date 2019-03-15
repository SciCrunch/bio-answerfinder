package org.bio_answerfinder.services;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.common.SearchQuery;
import org.bio_answerfinder.common.SearchQuery.Connective;
import org.bio_answerfinder.common.SearchQuery.QueryPart;
import org.bio_answerfinder.common.SearchQuery.SearchTerm;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 7/18/17.
 */
public class ElasticSearchService {
    String serverURL = "https://5f86098ac2b28a982cebf64e82db4ea2.us-west-2.aws.found.io";
    int port = 9243;
    Properties props;
    boolean verbose = false;
    boolean useCache = false;
    SearchResultCache cache;

    public ElasticSearchService() throws IOException {
        this.props = FileUtils.loadProperties("/bio-answerfinder.properties");
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) throws IOException {
        this.useCache = useCache;
        if (this.useCache && cache == null) {
            cache = new SearchResultCache();
        }
    }

    public List<PubMedDoc> retrieveDocuments(SearchQuery searchQuery, int size) throws Exception {
        return retrieveDocuments(searchQuery, size, null);
    }

    public PubMedDoc retrieveDocumentbyPMID(String pmid) throws Exception {
        String username = props.getProperty("elasticsearch.username");
        String pwd = props.getProperty("elasticsearch.pwd");
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/pubmed/literature/_search");
        URI uri = builder.build();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, pwd));
        // DefaultHttpClient client = new DefaultHttpClient();
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider).build();
        String body = buildPMIDQuery(pmid).toString(2);
        try {
            // client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            //         new UsernamePasswordCredentials(username, pwd));
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", "application/octet-stream");
            StringEntity entity = new StringEntity(body, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                if (!json.has("hits")) {
                    return null;
                }
                JSONArray jsArr = json.getJSONObject("hits").getJSONArray("hits");
                if (jsArr.length() > 0) {
                    return PubMedDoc.fromElasticJSON(jsArr.getJSONObject(0));
                }
            }
        } finally {
            client.close();
            // client.getConnectionManager().shutdown();
        }
        return null;
    }

    public List<PubMedDoc> retrieveDocuments(String keywordQuery, int size) throws Exception {
        if (useCache) {
            List<PubMedDoc> results = cache.getResults(keywordQuery);
            if (results != null) {
                return results;
            }
        }
        String username = props.getProperty("elasticsearch.username");
        String pwd = props.getProperty("elasticsearch.pwd");
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/pubmed/literature/_search");

        URI uri = builder.build();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, pwd));
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider).build();
        // DefaultHttpClient client = new DefaultHttpClient();
        // JSONObject bodyJson = buildQueryBody(searchQuery, size);
        JSONObject bodyJson = buildQueryStringBody(keywordQuery, size);
        String body = bodyJson.toString(2);
        System.out.println(body);
        try {
            //client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            //        new UsernamePasswordCredentials(username, pwd));
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", "application/octet-stream");
            StringEntity entity = new StringEntity(body, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                if (verbose) {
                    System.out.println(json.toString(2));
                }
                if (!json.has("hits")) {
                    return Collections.emptyList();
                }
                JSONArray jsArr = json.getJSONObject("hits").getJSONArray("hits");
                List<PubMedDoc> pmdList = new ArrayList<>(jsArr.length());
                for (int i = 0; i < jsArr.length(); i++) {
                    PubMedDoc pmd = PubMedDoc.fromElasticJSON(jsArr.getJSONObject(i));
                    if (pmd.documentAbstract != null) {
                        pmdList.add(pmd);
                    }
                }
                if (useCache) {
                    cache.put(keywordQuery, pmdList);
                }
                return pmdList;
            }
        } finally {
            //client.getConnectionManager().shutdown();
            client.close();
        }
        return Collections.emptyList();
    }

    public List<PubMedDoc> retrieveDocuments(SearchQuery searchQuery, int size, String cacheFile) throws Exception {
        String key = searchQuery.build();
        if (useCache) {
            List<PubMedDoc> results = cache.getResults(key);
            if (results != null) {
                return results;
            }
        }

        String username = props.getProperty("elasticsearch.username");
        String pwd = props.getProperty("elasticsearch.pwd");
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/pubmed/literature/_search");

        URI uri = builder.build();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, pwd));
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider).build();
        // DefaultHttpClient client = new DefaultHttpClient();
        // JSONObject bodyJson = buildQueryBody(searchQuery, size);
        JSONObject bodyJson = buildQueryStringBody(searchQuery, size);
        String body = bodyJson.toString(2);
        System.out.println(body);
        try {
            // client.getCredentialsProvider().setCredentials(AuthScope.ANY,
            //         new UsernamePasswordCredentials(username, pwd));
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", "application/octet-stream");
            StringEntity entity = new StringEntity(body, "UTF-8");
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                if (cacheFile != null) {
                    FileUtils.saveText(responseBody, cacheFile, CharSetEncoding.UTF8);
                    System.out.println("saved result to " + cacheFile);
                }
                if (verbose) {
                    System.out.println(json.toString(2));
                }
                if (!json.has("hits")) {
                    return Collections.emptyList();
                }
                JSONArray jsArr = json.getJSONObject("hits").getJSONArray("hits");
                List<PubMedDoc> pmdList = new ArrayList<>(jsArr.length());
                for (int i = 0; i < jsArr.length(); i++) {
                    PubMedDoc pmd = PubMedDoc.fromElasticJSON(jsArr.getJSONObject(i));
                    if (pmd.documentAbstract != null) {
                        pmdList.add(pmd);
                    }
                }
                if (useCache) {
                    cache.put(key, pmdList);
                }
                return pmdList;
            }
        } finally {
            client.close();
            //client.getConnectionManager().shutdown();
        }
        return Collections.emptyList();
    }

    public static List<PubMedDoc> loadResults(String esResultsJsonFile) throws IOException {
        String jsonStr = FileUtils.loadAsString(esResultsJsonFile, CharSetEncoding.UTF8);
        JSONObject json = new JSONObject(jsonStr);
        jsonStr = null;
        JSONArray jsArr = json.getJSONObject("hits").getJSONArray("hits");
        List<PubMedDoc> pmdList = new ArrayList<>(jsArr.length());
        for (int i = 0; i < jsArr.length(); i++) {
            PubMedDoc pmd = PubMedDoc.fromElasticJSON(jsArr.getJSONObject(i));
            if (pmd.documentAbstract != null) {
                pmdList.add(pmd);
            }
        }
        return pmdList;
    }

    JSONObject buildPMIDQuery(String pmid) {
        JSONObject json = new JSONObject();
        JSONObject query = new JSONObject();
        JSONObject term = new JSONObject();
        term.put("dc.identifier", pmid);
        query.put("term", term);
        json.put("query", query);
        return json;
    }

    JSONObject buildQueryBody(String keywordQuery, int size) {
        JSONObject json = new JSONObject();
        json.put("from", 0);
        json.put("size", size);
        JSONObject query = new JSONObject();
        json.put("query", query);
        JSONObject boolJson = new JSONObject();
        query.put("bool", boolJson);
        JSONArray shouldArr = new JSONArray();
        boolJson.put("should", shouldArr);
        JSONObject termJson = new JSONObject();
        termJson.put("_all", keywordQuery);
        shouldArr.put(new JSONObject().put("match", termJson));
        return json;
    }

    JSONObject buildQueryStringBody(String keywordQuery, int size) {
        JSONObject json = new JSONObject();
        json.put("from", 0);
        json.put("size", size);
        JSONObject query = new JSONObject();
        json.put("query", query);
        JSONObject qsJSON = new JSONObject();
        qsJSON.put("query", keywordQuery);
        JSONArray jsArr = new JSONArray();
        jsArr.put("dc.description");
        jsArr.put("dc.title");
        qsJSON.put("fields", jsArr);
        query.put("query_string", qsJSON);

        return json;
    }

    JSONObject buildQueryStringBody(SearchQuery searchQuery, int size) {
        JSONObject json = new JSONObject();
        json.put("from", 0);
        json.put("size", size);
        JSONObject query = new JSONObject();
        json.put("query", query);
        JSONObject qsJSON = new JSONObject();
        qsJSON.put("query", searchQuery.build());
        // qsJSON.put("default_field","dc.description");
        // to use _all field remove fields field
        JSONArray jsArr = new JSONArray();
        jsArr.put("dc.description");
        jsArr.put("dc.title");
        qsJSON.put("fields", jsArr);
        // qsJSON.put("use_dis_max",true);
        query.put("query_string", qsJSON);

        return json;
    }

    JSONObject buildQueryBody(SearchQuery searchQuery, int size) {
        JSONObject json = new JSONObject();
        json.put("from", 0);
        json.put("size", size);
        JSONObject query = new JSONObject();
        json.put("query", query);
        JSONObject boolJson = new JSONObject();
        query.put("bool", boolJson);
        JSONArray shouldArr = new JSONArray();
        boolJson.put("should", shouldArr);
        JSONArray mustArr = new JSONArray();

        // Assumption all query parts are ORed together. Within each query part there can be binary AND.
        for (QueryPart qp : searchQuery.getQueryParts()) {
            if (qp.getSearchTerms().size() == 2 && qp.getSearchTerms().get(0).getConnective() == Connective.AND) {
                for (SearchTerm st : qp.getSearchTerms()) {
                    JSONObject fieldJson = new JSONObject().put("_all", st.getTerm());
                    mustArr.put(new JSONObject().put("match", fieldJson));
                }
            } else {
                if (qp.getSearchTerms().size() == 1) {
                    JSONObject fieldJson = new JSONObject().put("_all", qp.getSearchTerms().get(0).getTerm());
                    if (qp.getConnective() != Connective.AND) {
                        shouldArr.put(new JSONObject().put("match", fieldJson));
                    } else {
                        mustArr.put(new JSONObject().put("match", fieldJson));
                    }
                } else {
                    // assumption all OR
                    for (SearchTerm st : qp.getSearchTerms()) {
                        JSONObject fieldJson = new JSONObject().put("_all", st.getTerm());
                        shouldArr.put(new JSONObject().put("match", fieldJson));
                    }
                }
            }
        }
        if (mustArr.length() > 0) {
            boolJson.put("must", mustArr);
        }

        return json;
    }


    public static void testPMIDBasedRetrieval() throws Exception {
        ElasticSearchService ess = new ElasticSearchService();
        PubMedDoc pubMedDoc = ess.retrieveDocumentbyPMID("19255803");
        Assertion.assertNotNull(pubMedDoc);
        System.out.println(pubMedDoc);
    }


    static void testDriver() throws Exception {
        ElasticSearchService ess = new ElasticSearchService();

        SearchQuery sq = new SearchQuery();
        QueryPart qp1 = new QueryPart(new SearchTerm("Hirschsprung", false));
        qp1.addSearchTerm(new SearchTerm("mendelian", false), Connective.OR);
        qp1.addSearchTerm(new SearchTerm("multifactorial", false), Connective.OR);

        sq.addQueryPart(qp1, Connective.NONE);

        List<PubMedDoc> pmdList = ess.retrieveDocuments(sq, 10);
        for (PubMedDoc pmd : pmdList) {
            System.out.println(pmd);
        }
    }

    public static void main(String[] args) throws Exception {
        // testDriver();
        testPMIDBasedRetrieval();

    }

}
