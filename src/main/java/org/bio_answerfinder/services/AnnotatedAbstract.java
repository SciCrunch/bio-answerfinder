package org.bio_answerfinder.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 11/15/17.
 */
public class AnnotatedAbstract {
    String pmid;
    List<AnnotatedSentence> annotatedSentences = new ArrayList<>(5);

    public AnnotatedAbstract(String pmid) {
        this.pmid = pmid;
    }

    public void addSentence(AnnotatedSentence as) {
        annotatedSentences.add(as);
    }

    public String getPmid() {
        return pmid;
    }

    public List<AnnotatedSentence> getAnnotatedSentences() {
        return annotatedSentences;
    }


    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("pmid", pmid);
        JSONArray jsArr = new JSONArray();
        json.put("asList", jsArr);
        for(AnnotatedSentence as : annotatedSentences) {
            jsArr.put(as.toJSON());
        }
        return json;
    }

    public static AnnotatedAbstract fromJSON(JSONObject json) {
        String pmid = json.getString("pmid");
        AnnotatedAbstract aa = new AnnotatedAbstract(pmid);
        JSONArray asList = json.getJSONArray("asList");
        for(int i = 0; i < asList.length(); i++) {
            aa.addSentence( AnnotatedSentence.fromJSON(asList.getJSONObject(i)));
        }
        return aa;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AnnotatedAbstract{");
        sb.append("pmid='").append(pmid).append('\'');
        for(AnnotatedSentence as : annotatedSentences) {
            sb.append("\n\t").append(as);
        }
        sb.append('}');
        return sb.toString();
    }

    public static class AnnotatedSentence {
        final String annotatedSentence;
        final int sentIdx;

        public AnnotatedSentence(String annotatedSentence, int sentIdx) {
            this.annotatedSentence = annotatedSentence;
            this.sentIdx = sentIdx;
        }

        public String getAnnotatedSentence() {
            return annotatedSentence;
        }

        public int getSentIdx() {
            return sentIdx;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("AnnotatedSentence{");
            sb.append("sentIdx=").append(sentIdx);
            sb.append(", annotatedSentence='").append(annotatedSentence).append('\'');
            sb.append('}');
            return sb.toString();
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("s", annotatedSentence);
            json.put("id", sentIdx);
            return json;
        }

        public static AnnotatedSentence fromJSON(JSONObject json) {
            String s = json.getString("s");
            int sentIdx = json.getInt("id");
            return new AnnotatedSentence(s, sentIdx);
        }
    }
}
