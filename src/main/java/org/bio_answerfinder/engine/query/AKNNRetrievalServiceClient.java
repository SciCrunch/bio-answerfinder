package org.bio_answerfinder.engine.query;

import gnu.trove.map.hash.TObjectFloatHashMap;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.engine.SearchQueryGenErrorAnalyzer;
import org.bio_answerfinder.nlp.sentence.SentenceLexer2;
import org.bio_answerfinder.nlp.sentence.TokenInfo;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.FrequencyTable;
import org.bio_answerfinder.util.StopWords;
import org.bio_answerfinder.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 8/9/19.
 */
public class AKNNRetrievalServiceClient {
    String serverURL = "http://127.0.0.1";
    int port = 5551;

    public AKNNRetrievalServiceClient() {
    }

    public AKNNRetrievalServiceClient(String serverURL) {
        this.serverURL = serverURL;
    }

    public List<String> getSimilarAbstracts(String question, GloveDBLookup gloveMan,
                                            TObjectFloatHashMap<String> vocabulary, int k) throws Exception {
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/aknn");
        URI uri = builder.build();
        List<String> pmids = new ArrayList<>();
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> params = new ArrayList<>(2);
            JSONObject payload = preparePayload(question, gloveMan, vocabulary, k);
            params.add(new BasicNameValuePair("data", payload.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONArray jsArr = new JSONArray(responseBody);

                for (int i = 0; i < jsArr.length(); i++) {
                    pmids.add(jsArr.get(i).toString());
                }

            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return pmids;
    }


    JSONObject preparePayload(String question, GloveDBLookup gloveMan,
                              TObjectFloatHashMap<String> vocabulary,
                              int numNearestNeighbours) throws IOException {
        JSONObject json = new JSONObject();
        FrequencyTable<String> tfMap = new FrequencyTable<>();
        int maxNgramSize = 4;
        List<String> tokens = tokenize(question);
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (StopWords.isStopWord(token) || StringUtils.isAllSpecial(token)) {
                i++;
                continue;
            }
            int len = Math.min(i + maxNgramSize, tokens.size());
            String longestPhrase = null;
            int offset = 0;
            for (int j = i + 1; j < len; j++) {
                sb.setLength(0);
                for (int k = i; k <= j; k++) {
                    sb.append(tokens.get(k)).append(' ');
                }
                String p = sb.toString().trim();
                if (vocabulary.containsKey(p)) {
                    longestPhrase = p;
                    offset = j;
                } else {
                    break;
                }
            }
            if (longestPhrase != null) {
                i = offset + 1;
                tfMap.addValue(longestPhrase);
            } else {
                i++;
                tfMap.addValue(token);
            }
        }

        double[] centroid = null;
        for (Comparable<String> t : tfMap.getSortedKeys()) {
            String term = t.toString();
            int tf = tfMap.getFrequency(term);
            if (vocabulary.containsKey(term)) {
                float[] vec = gloveMan.getGloveVector(term);
                if (vec != null) {
                    float idf = vocabulary.get(term);
                    float tfidf = tf * idf;
                    if (centroid == null) {
                        centroid = new double[vec.length];
                        for (i = 0; i < vec.length; i++) {
                            centroid[i] = tfidf * vec[i];
                        }
                    } else {
                        for (i = 0; i < vec.length; i++) {
                            centroid[i] += tfidf * vec[i];
                        }
                    }
                }
            }
        }
        JSONArray jsArr = new JSONArray();
        for (i = 0; i < centroid.length; i++) {
            jsArr.put(centroid[i]);
        }
        json.put("query", jsArr);
        json.put("k", numNearestNeighbours);

        return json;
    }

    public List<String> tokenize(String content) throws IOException {
        SentenceLexer2 lexer = new SentenceLexer2(content);
        List<String> tokens = new ArrayList<>();
        TokenInfo ti;
        while ((ti = lexer.getNextTI()) != null) {
            tokens.add(ti.getTokValue());
        }
        return tokens;
    }


    public static void main(String[] args) throws Exception {
        GloveDBLookup gloveMan = null;
        try {
            Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
            String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
            gloveMan = GloveDBLookup.getInstance(dbFile);
            TObjectFloatHashMap<String> vocabulary = SearchQueryGenErrorAnalyzer.loadVocabularyWithWeights();

            String question = "Are genes symmetrically distributed between leading and lagging DNA strand in bacteria ?";

            AKNNRetrievalServiceClient client = new AKNNRetrievalServiceClient();

            List<String> similarAbstracts = client.getSimilarAbstracts(question, gloveMan, vocabulary, 100);

            System.out.println(similarAbstracts);

        } finally {
            gloveMan.shutdown();
        }
    }
}
