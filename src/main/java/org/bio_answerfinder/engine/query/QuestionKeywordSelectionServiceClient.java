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
import org.json.JSONArray;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 1/4/19.
 */
public class QuestionKeywordSelectionServiceClient {
    String serverURL = "http://127.0.0.1";
    int port = 5000;

    /**
     *
     * @param question a string containing space delimited question tokens
     * @param posTags a string of space delimited POS tags for the question
     * @return a list of tokens selected for the keyword search
     */
    public List<String> getSelectedQueryTerms(String question, String posTags) throws Exception {
        URIBuilder builder = new URIBuilder(serverURL);
        builder.setPort(port).setPath("/qks");
        URI uri = builder.build();
        List<String> keywords = new ArrayList<>(5);
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> params = new ArrayList<>(2);
            params.add(new BasicNameValuePair("question", question));
            params.add(new BasicNameValuePair("pos_tags", posTags));
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JSONArray jsArr = new JSONArray(responseBody);
                String [] tokens = question.split("\\s+");
                int len = Math.min(jsArr.length(), tokens.length);
                for(int i = 0; i < len; i++) {
                    double pred = jsArr.getDouble(i);
                    if (pred >= 0.5) {
                        keywords.add(tokens[i]);
                    }
                }
            }
        } finally {
            if (client != null) {
                client.close();
            }
        }
        return keywords;
    }

    public static void main(String[] args) throws Exception {
        String question = "List signaling molecules ( ligands ) that interact with the receptor EGFR ?";
        String posTags = "VB VBG NNS -LRB- NNS -RRB- IN NN IN DT NN NN .";
        question = "In which cells are gasdermins expressed ?";
        posTags = "IN WDT NNS VBP NNS VBN .";
        QuestionKeywordSelectionServiceClient client = new QuestionKeywordSelectionServiceClient();
        System.out.println(client.getSelectedQueryTerms(question, posTags));



    }
}
