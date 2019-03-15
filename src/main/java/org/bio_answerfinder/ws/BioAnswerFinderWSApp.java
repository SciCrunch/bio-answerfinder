package org.bio_answerfinder.ws;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by bozyurt on 9/25/17.
 */
public class BioAnswerFinderWSApp extends ResourceConfig {
    public BioAnswerFinderWSApp() {
        packages("org.bio_answerfinder.ws.resources");
    }
}
