package org.bio_answerfinder.engine.query;

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
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.*;

/**
 * Created by bozyurt on 7/10/19.
 */
public class QuestionQueryRankingServiceClient {
    String serverURL = "http://127.0.0.1";
    int port = 5020;
    final static int maxNumTokens = 40;
    private GloveDBLookup gloveDBLookup;

    public QuestionQueryRankingServiceClient(GloveDBLookup gloveDBLookup) {
        this.gloveDBLookup = gloveDBLookup;
    }

    /**
     * @param terms
     * @param keywords Assumption: two or more keywords
     * @return ranked keywords
     * @throws Exception
     */
    public List<String> rankKeywords(List<String> terms, List<String> keywords) throws Exception {
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/qk_rank");
        URI uri = builder.build();
        List<String> rankedKeywords = new ArrayList<>(keywords.size());

        JSONArray jsonArray = prepPairwiseFVList(terms, keywords);
        JSONObject json = new JSONObject();
        json.put("noInputs", 100 * maxNumTokens);
        json.put("pairs", jsonArray);
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> params = new ArrayList<>(2);
            //System.out.println(json.getJSONArray("pairs").length());
            //System.out.println(json.getJSONArray("pairs").get(0));
            params.add(new BasicNameValuePair("data", json.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONArray jsArr = new JSONArray(responseBody);
                // Assertion.assertEquals(jsArr.length(), keywords.size());
                Set<String> seenSet = new HashSet<>();
                for (int i = 0; i < jsArr.length(); i++) {
                    String keyword = terms.get(jsArr.getInt(i));
                    rankedKeywords.add(keyword);
                    seenSet.add(keyword);
                }
                if (seenSet.size() < keywords.size()) {
                    for(String keyword : keywords) {
                        if (!seenSet.contains(keyword)) {
                            rankedKeywords.add(keyword);
                        }
                    }
                }
            }
        }

        return rankedKeywords;
    }


    JSONArray prepPairwiseFVList(List<String> terms, List<String> keywords) {
        JSONArray jsonArray = new JSONArray();
        Map<String, Integer> keyword2LocMap = new HashMap<>();
        for (String keyword : keywords) {
            int locIdx = terms.indexOf(keyword);
            Assertion.assertTrue(locIdx != -1);
            keyword2LocMap.put(keyword, locIdx);
        }
        for (String keyword1 : keyword2LocMap.keySet()) {
            int locIdx1 = keyword2LocMap.get(keyword1);
            float[] fv1 = prepareFeatureVector(terms, locIdx1, this.gloveDBLookup);
            for (String keyword2 : keyword2LocMap.keySet()) {
                if (keyword1.equals(keyword2)) {
                    continue;
                }
                int locIdx2 = keyword2LocMap.get(keyword2);
                float[] fv2 = prepareFeatureVector(terms, locIdx2, this.gloveDBLookup);
                JSONObject json = new JSONObject();
                json.put("qid", 1);
                json.put("label", -1); // label
                json.put("locIdx1", locIdx1);
                json.put("locIdx2", locIdx2);
                JSONArray arr = new JSONArray();
                for (int i = 0; i < fv1.length; i++) {
                    arr.put(fv1[i]);
                }
                json.put("fv1", arr);
                arr = new JSONArray();
                for (int i = 0; i < fv2.length; i++) {
                    arr.put(fv2[i]);
                }
                json.put("fv2", arr);
                jsonArray.put(json);
            }
        }
        return jsonArray;
    }


    public static float[] prepareFeatureVector(List<String> terms, int termLocIdx, GloveDBLookup gloveDBLookup) {
        float[] fv = new float[maxNumTokens * 100];
        int n = Math.min(terms.size(), maxNumTokens);
        int idx = 0;
        for (int i = 0; i < n; i++) {
            if (termLocIdx == i) {
                float[] sepVector = gloveDBLookup.getGloveVector("unk2");
                System.arraycopy(sepVector, 0, fv, idx * 100, 100);
                idx++;
            }
            String term = terms.get(i);
            float[] gloveVector = gloveDBLookup.getGloveVector(term);
            if (gloveVector == null) {
                gloveVector = gloveDBLookup.getGloveVector("unk1");
            }
            System.arraycopy(gloveVector, 0, fv, idx * 100, 100);
            idx++;
        }
        return fv;
    }


    public static void main(String[] args) throws Exception {
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile = FileUtils.adjustPath(props.getProperty("glove.db.file"));
        GloveDBLookup gloveDBLookup = GloveDBLookup.getInstance(dbFile);

        try {
            QuestionQueryRankingServiceClient client = new QuestionQueryRankingServiceClient(gloveDBLookup);

            String question = "Which database is available for the identification of chorion proteins in Lepidopteran proteomes ?";
            List<String> terms = Arrays.asList(question.split("\\s+"));
            List<String> keywords = Arrays.asList("database", "chorion", "proteins", "Lepidopteran", "proteomes");

            List<String> rankKeywords = client.rankKeywords(terms, keywords);
            System.out.println(rankKeywords);
        } finally {
            gloveDBLookup.shutdown();
        }
    }
}
