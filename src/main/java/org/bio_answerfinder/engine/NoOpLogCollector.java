package org.bio_answerfinder.engine;

/**
 * Created by bozyurt on 7/17/19.
 */
public class NoOpLogCollector implements ILogCollector {
    @Override
    public void log(String msg) {
        // no op
    }
}
