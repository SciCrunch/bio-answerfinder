package org.bio_answerfinder.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.GenUtils;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 11/15/17.
 *
 * @see {http://cypher.neuinfo.org:9000/scigraph/docs/#!/annotations/annotate}
 */
public class AnnotationService {
    final static String serviceURL = "http://cypher.neuinfo.org:9000";


    public static List<AnnotatedAbstract> annotateAbstracts(LinkedHashMap<String, List<String>> pmid2SentencesMap) throws Exception {
        StringBuilder buffer = new StringBuilder(100000);
        boolean first = true;
        for (String pmid : pmid2SentencesMap.keySet()) {
            List<String> sentences = pmid2SentencesMap.get(pmid);
            String docContent = GenUtils.join(sentences, " || ");
            if (!first) {
                buffer.append(" ~!!~ ");
            }
            buffer.append(docContent);
            first = false;
        }

        //final HttpParams httpParams = new BasicHttpParams();
        //HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
        //HttpClient client = new DefaultHttpClient(httpParams);
        int timeout = 30000;
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout).setSocketTimeout(timeout);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfigBuilder.build()).build();
        URIBuilder builder = new URIBuilder(serviceURL);
        builder.setPath("/scigraph/annotations");
        try {
            URI uri = builder.build();
            HttpPost httpPost = new HttpPost(uri);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("minLength", "4"));
            nvps.add(new BasicNameValuePair("longestOnly", "true"));
            nvps.add(new BasicNameValuePair("content", buffer.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                String[] resultDocs = responseBody.split("\\s~!!~\\s");
                List<String> pmids = new ArrayList<>(pmid2SentencesMap.keySet());
                List<AnnotatedAbstract> annotatedAbstracts = new ArrayList<>(resultDocs.length);
                for (int i = 0; i < resultDocs.length; i++) {
                    String pmid = pmids.get(i);
                    String[] resultSentences = resultDocs[i].split("\\s\\|\\|\\s");
                    AnnotatedAbstract annotatedAbstract = new AnnotatedAbstract(pmid);
                    for (int j = 0; j < resultSentences.length; j++) {
                        String rs = resultSentences[j];
                        handleAnnotatedSentence(annotatedAbstract, j, rs);
                    }
                    annotatedAbstracts.add(annotatedAbstract);
                }
                return annotatedAbstracts;
            }

        } finally {
            client.close();
            // client.getConnectionManager().shutdown();
        }
        return null;
    }

    static void handleAnnotatedSentence(AnnotatedAbstract annotatedAbstract, int j, String rs) {
        if (rs.indexOf("</span>") != -1) {
            AnnotatedAbstract.AnnotatedSentence as = new AnnotatedAbstract.AnnotatedSentence(rs.trim(), j);
            annotatedAbstract.addSentence(as);
        }
    }

    public static AnnotatedAbstract annotateAbstract(String pmid, List<String> sentences) throws Exception {
        String content = GenUtils.join(sentences, " || ");

        //final HttpParams httpParams = new BasicHttpParams();
        //HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
        //HttpClient client = new DefaultHttpClient(httpParams);

        int timeout = 30000;
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom().setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout).setSocketTimeout(timeout);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfigBuilder.build()).build();
        URIBuilder builder = new URIBuilder(serviceURL);
        builder.setPath("/scigraph/annotations");
        builder.setParameter("minLength", "4");
        builder.setParameter("longestOnly", "true");
        builder.setParameter("content", content);
        try {
            URI uri = builder.build();
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (response.getStatusLine().getStatusCode() == 200 && entity != null) {
                String resultStr = EntityUtils.toString(entity);
                String[] resultSentences = resultStr.split("\\s\\|\\|\\s");
                Assertion.assertEquals(resultSentences.length, sentences.size());
                AnnotatedAbstract annotatedAbstract = new AnnotatedAbstract(pmid);
                for (int i = 0; i < resultSentences.length; i++) {
                    String rs = resultSentences[i];
                    handleAnnotatedSentence(annotatedAbstract, i, rs);
                }
                return annotatedAbstract;
            }
        } finally {
            client.close();
            // client.getConnectionManager().shutdown();
        }
        return null;
    }


    public static Map<String, List<Concept>> getAnnotations(AnnotatedAbstract.AnnotatedSentence annotatedSentence) {
        Pattern p = Pattern.compile("<span class=\"sciCrunchAnnotation\"\\s+data-sciGraph=\"([^\"]+)\">([^>]+)<");
        Matcher m = p.matcher(annotatedSentence.getAnnotatedSentence());
        Map<String, List<Concept>> map = null;
        while (m.find()) {
            if (map == null) {
                map = new HashMap<>(11);
            }
            String scigraphData = m.group(1);
            String term = m.group(2);
            String[] conceptStrArr = scigraphData.split("\\|");
            List<Concept> concepts = new ArrayList<>(conceptStrArr.length);
            for (String conceptStr : conceptStrArr) {
                String[] parts = conceptStr.split(",");
                if (parts.length == 3) {
                    String label = null;
                    String curie = null;
                    String category = null;
                    label = parts[0].trim();
                    if (!GenUtils.isEmpty(parts[1])) {
                        curie = parts[1].trim();
                    }
                    if (!GenUtils.isEmpty(parts[2])) {
                        category = parts[2].trim();
                    }
                    if (curie != null) {
                        Concept concept = new Concept(label, curie, category);
                        concepts.add(concept);
                    }
                }
            }
            if (!concepts.isEmpty()) {
                map.put(term, concepts);
            }
        }
        return map;
    }


    public static void main(String[] args) throws Exception {
        String[] sentences = new String[]{
                "In response to cytokines and growth factors, STAT3 is phosphorylated by receptor-associated Janus kinases (JAK), form homo- or heterodimers, and translocate to the cell nucleus where they act as transcription activators.",
                "Specifically, STAT3 becomes activated after phosphorylation of tyrosine 705 in response to such ligands as interferons, epidermal growth factor (EGF), Interleukin (IL-)5 and IL-6.",
                "Additionally, activation of STAT3 may occur via phosphorylation of serine 727 by Mitogen-activated protein kinases (MAPK)[6] and through c-src non-receptor tyrosine kinase.[7][8]",
                "STAT3 mediates the expression of a variety of genes in response to cell stimuli, and thus plays a key role in many cellular processes such as cell growth and apoptosis.[9]"
        };
        AnnotatedAbstract aa = AnnotationService.annotateAbstract("12345", Arrays.asList(sentences));
        System.out.println(aa);

        for (AnnotatedAbstract.AnnotatedSentence as : aa.getAnnotatedSentences()) {
            Map<String, List<Concept>> map = getAnnotations(as);
            for (String term : map.keySet()) {
                System.out.println(term + " -> ");
                List<Concept> concepts = map.get(term);
                for (Concept concept : concepts) {
                    System.out.println("\t" + concept);
                }
            }
        }

    }
}