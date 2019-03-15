package org.bio_answerfinder.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/5/17.
 */
public class OntologyRec {
    String matchedLabel;
    String conceptLabel;
    String conceptURI;
    String conceptTermId;
    double score;
    List<Range> ranges = new ArrayList<Range>(1);

    public OntologyRec() {
    }

    public String getMatchedLabel() {
        return matchedLabel;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public String getConceptTermId() {
        return conceptTermId;
    }

    public double getScore() {
        return score;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OntologyRec{");
        sb.append("matchedLabel='").append(matchedLabel).append("'\n");
        sb.append(", conceptLabel='").append(conceptLabel).append("'\n");
        sb.append(", conceptURI='").append(conceptURI).append("'\n");
        sb.append(", conceptTermId='").append(conceptTermId).append('\'');
        sb.append(", score=").append(score);
        sb.append("\n, ranges=").append(ranges);
        sb.append('}');
        return sb.toString();
    }

    public static OntologyRec fromJSON(JSONObject json) {
        OntologyRec ontologyRec = new OntologyRec();
        ontologyRec.matchedLabel = json.getString("matchedLabel");
        ontologyRec.score = json.getDouble("score");
        JSONObject concept = json.getJSONObject("concept");
        ontologyRec.conceptLabel = concept.getString("label");
        ontologyRec.conceptTermId = concept.getString("termId");
        ontologyRec.conceptURI = concept.getString("uri");
        JSONArray ranges = json.getJSONArray("ranges");
        for(int i = 0; i < ranges.length(); i++) {
            JSONObject ro = ranges.getJSONObject(i);
            int begin = ro.getInt("begin");
            int end = ro.getInt("end");
            ontologyRec.ranges.add( new Range(begin, end));
        }
        return ontologyRec;
    }

    public static class Range {
        int begin;
        int end;

        public Range(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Range::[");
            sb.append("begin=").append(begin);
            sb.append(", end=").append(end);
            sb.append(']');
            return sb.toString();
        }
    }
}
