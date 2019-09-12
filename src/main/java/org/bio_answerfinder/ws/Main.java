package org.bio_answerfinder.ws;

import org.bio_answerfinder.ws.resources.QuestionResultRetrieverResource;
import org.bio_answerfinder.engine.QAParserRetrieverService;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

/**
 * Created by bozyurt on 4/17/19.
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:7080/baf/api/";
    static HttpServer server;

    public static HttpServer startServer() throws Exception {
        QAParserRetrieverService service = QAParserRetrieverService.getInstance();
        service.initialize();
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(QuestionResultRetrieverResource.class);
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI),
                resourceConfig, false);
        HttpServerProbe probe = new HttpServerProbe.Adapter() {
            public void onRequestReceiveEvent(HttpServerFilter filter, Connection connection, Request request) {
                System.out.println(request.getRequestURI());
            }
        };
        server.getServerConfiguration().getMonitoringConfig().getWebServerConfig().addProbes(probe);
        server.start();

        return server;
    }

    public static void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    public static void main(String[] args) throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Main.stopServer()));
        Main.startServer();
        Thread.currentThread().join();
    }
}
