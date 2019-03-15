package org.bio_answerfinder.engine;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bio_answerfinder.util.Assertion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bozyurt on 3/8/19.
 */
public class BERTRerankerServiceClient {
    String serverURL = "http://127.0.0.1";
    int port = 5001;

    public List<RankedSentence> rerank(String question, List<String> sentences) throws Exception {
        List<RankedSentence> rsList = new ArrayList<>(sentences.size());
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/rerank");
        URI uri = builder.build();
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(uri);
            JSONArray lines = new JSONArray();
            for(String sentence : sentences) {
                JSONArray jsArr = new JSONArray();
                jsArr.put("0");
                jsArr.put(question);
                jsArr.put(sentence);
                lines.put(jsArr);
            }
            List<NameValuePair> params = new ArrayList<>(2);

            params.add(new BasicNameValuePair("lines", lines.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(responseBody);
                JSONArray jsArr = json.getJSONArray("predictions");
                Assertion.assertEquals(jsArr.length(), sentences.size());
                for(int i = 0; i < jsArr.length(); i++) {
                    JSONArray arr = jsArr.getJSONArray(i);
                    String sentence = sentences.get(i);
                    RankedSentence rs = new RankedSentence(sentence, (float) arr.getDouble(1));
                    rsList.add(rs);
                }
                Collections.sort(rsList, new Comparator<RankedSentence>() {
                    @Override
                    public int compare(RankedSentence o1, RankedSentence o2) {
                        return Float.compare(o2.score, o1.score);
                    }
                });
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return rsList;
    }

    public static class RankedSentence {
        final String sentence;
        final float score;

        public RankedSentence(String sentence, float score) {
            this.sentence = sentence;
            this.score = score;
        }

        public String getSentence() {
            return sentence;
        }

        public float getScore() {
            return score;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("RankedSentence{");
            sb.append("score=").append(score);
            sb.append(", sentence='").append(sentence).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
