package org.bio_answerfinder.ws.resources;

import org.bio_answerfinder.engine.AnswerSentence;
import org.bio_answerfinder.util.NumberUtils;
import org.bio_answerfinder.ws.BioAnswerFinderEngineService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 9/21/17.
 */

@Path("bioqa")
public class QAEngineResource {

    @POST
    @Path("/ask")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response answerQuestion(@FormParam("query") String queryString, @FormParam("resultSize") String resultSizeStr,
                                   @FormParam("useReranking") boolean useReranking) {
        try {
            JSONObject json = new JSONObject();
            JSONArray jsArr = new JSONArray();
            json.put("answerSentences", jsArr);

            Map<String, String> options = new HashMap<>();
            options.put("useReranking", String.valueOf(useReranking));
            int resultSize = 50;
            if (!resultSizeStr.equalsIgnoreCase("all") && !resultSizeStr.equalsIgnoreCase("max")) {
                resultSize = NumberUtils.toInt(resultSizeStr, 50);
            }
            List<AnswerSentence> answerSentences = BioAnswerFinderEngineService
                    .getInstance().answerQuestion(queryString, resultSize, options);
            if (!resultSizeStr.equalsIgnoreCase("all") && !resultSizeStr.equalsIgnoreCase("max")) {
                if (answerSentences.size() > resultSize) {
                    answerSentences = answerSentences.subList(0, resultSize);
                }
            }
            System.out.println("# answerSentences: " + answerSentences.size());
            for (AnswerSentence as : answerSentences) {
                JSONObject asJson = new JSONObject();
                asJson.put("s", as.getSentence());
                asJson.put("pmid", as.getPmid());
                asJson.put("score", as.getScore());
                jsArr.put(asJson);
            }
            return Response.ok(json.toString(2)).build();
        } catch (Throwable t) {
            t.printStackTrace();
            return Response.serverError().build();
        }
    }
}
