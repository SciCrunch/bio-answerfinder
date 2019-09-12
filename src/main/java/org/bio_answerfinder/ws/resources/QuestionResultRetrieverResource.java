package org.bio_answerfinder.ws.resources;

import org.bio_answerfinder.engine.QAParserRetrieverService;
import org.bio_answerfinder.engine.QAParserRetrieverService.QuestionQueryRec;
import org.bio_answerfinder.util.GenUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by bozyurt on 4/17/19.
 */

@Path("qrr")
public class QuestionResultRetrieverResource {

    @GET
    @Path("/retrieve")
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildSearchQuery(@QueryParam("question") String question) {
        System.out.println("question:" + question);
        try {
            if (GenUtils.isEmpty(question)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            QAParserRetrieverService retrieverService = QAParserRetrieverService.getInstance();

            retrieverService.buildSearchQuery(question);
            QuestionQueryRec questionQueryRec = retrieverService.retrieveResults4CurrentQuery(question);
            return Response.ok(questionQueryRec.toJSON().toString(2)).build();
        } catch (Throwable t) {
            t.printStackTrace();
            return Response.serverError().build();
        }
    }


    @GET
    @Path("/removeTerm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTermFromQuery(@QueryParam("question") String question, @QueryParam("term") String term) throws Exception {
        try {
             if (GenUtils.isEmpty(question) || GenUtils.isEmpty(term)) {
                 return Response.status(Response.Status.BAD_REQUEST).build();
             }
            QAParserRetrieverService retrieverService = QAParserRetrieverService.getInstance();
            QuestionQueryRec questionQueryRec = retrieverService.removeTermFromQuery(question, term);
            return Response.ok(questionQueryRec.toJSON().toString(2)).build();
        } catch (Throwable t) {
            t.printStackTrace();
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/coverage")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCoverage4CurrentQuery(@QueryParam("question") String question) throws Exception {
        try {
            if (GenUtils.isEmpty(question)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            QAParserRetrieverService retrieverService = QAParserRetrieverService.getInstance();
            QuestionQueryRec questionQueryRec = retrieverService.getCoverage4CurrentQuery(question);
            return Response.ok(questionQueryRec.toJSON().toString(2)).build();
        } catch(Throwable t) {
            t.printStackTrace();
            return Response.serverError().build();
        }
    }

}
