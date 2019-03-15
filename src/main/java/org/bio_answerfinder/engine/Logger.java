package org.bio_answerfinder.engine;

import java.io.IOException;

/**
 * Created by bozyurt on 1/4/18.
 */
public interface Logger {
    void startLogging(String logFile) throws IOException;

    void logQuestionResults(String question, double MRR, int rank) throws IOException;

    void stopLogging();
}
