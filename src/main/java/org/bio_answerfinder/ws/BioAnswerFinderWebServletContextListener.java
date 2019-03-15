package org.bio_answerfinder.ws;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by bozyurt on 9/21/17.
 */
public class BioAnswerFinderWebServletContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent servletContextEvent) {

        try {
            BioAnswerFinderEngineService.getInstance().initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            BioAnswerFinderEngineService.getInstance().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
