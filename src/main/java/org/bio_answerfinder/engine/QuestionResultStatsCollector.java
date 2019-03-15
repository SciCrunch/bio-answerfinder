package org.bio_answerfinder.engine;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 12/14/17.
 */
public class QuestionResultStatsCollector {
    QuestionResultStat current;
    List<QuestionResultStat> qrsList = new ArrayList<>();

    public QuestionResultStatsCollector() {
        current = new QuestionResultStat();
    }

    public QuestionResultStatsCollector setQid(String qid) {
        current.qid = qid;
        return this;
    }

    public QuestionResultStatsCollector setQuestion(String question) {
        current.question = question;
        return this;
    }

    public QuestionResultStatsCollector setQuery(String query) {
        current.query = query;
        return this;
    }

    public QuestionResultStatsCollector setNumCandidateResults(int numCandidateResults) {
        current.numCandidateResults = numCandidateResults;
        return this;
    }

    public void keep() {
        qrsList.add(current);
        current = new QuestionResultStat();
    }

    public void toss() {
        current = new QuestionResultStat();
    }


    public void save(String outJsonFile) throws IOException {
        JSONArray jsArr = new JSONArray();
        for(QuestionResultStat qrs : qrsList) {
            jsArr.put( qrs.toJSON());
        }
        String content = jsArr.toString(2);
        FileUtils.saveText(content, outJsonFile, CharSetEncoding.UTF8);
        System.out.println("saved " + outJsonFile);
    }




    public static class QuestionResultStat {
        String qid;
        String question;
        String query;
        int numCandidateResults;

        public QuestionResultStat() {
        }

        public String getQid() {
            return qid;
        }

        public String getQuestion() {
            return question;
        }

        public String getQuery() {
            return query;
        }

        public int getNumCandidateResults() {
            return numCandidateResults;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("qid",qid);
            json.put("question", question);
            json.put("query", query);
            json.put("numCandidateResults", numCandidateResults);
            return json;
        }

        public static QuestionResultStat fromJSON(JSONObject json) {
            QuestionResultStat qrs = new QuestionResultStat();
            qrs.qid = json.getString("qid");
            qrs.question = json.getString("question");
            qrs.query = json.getString("query");
            qrs.numCandidateResults = json.getInt("numCandidateResults");
            return qrs;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("QuestionResultStat{");
            sb.append("question='").append(question).append('\'');
            sb.append(", query='").append(query).append('\'');
            sb.append(", numCandidateResults=").append(numCandidateResults);
            sb.append('}');
            return sb.toString();
        }
    }
}
