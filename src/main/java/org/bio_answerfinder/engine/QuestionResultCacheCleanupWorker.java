package org.bio_answerfinder.engine;

public class QuestionResultCacheCleanupWorker implements Runnable {
    private static long CACHE_EVICTION_INTERVAL = 24 * 3600000; // 24 hours

    @Override
    public void run() {

        while(true) {
            long start = System.currentTimeMillis();
            long diff;
            do {
                synchronized (this) {
                    try {
                        wait(CACHE_EVICTION_INTERVAL);
                    } catch (InterruptedException e) {
                        // no-op
                    }
                }
                diff =  CACHE_EVICTION_INTERVAL - (System.currentTimeMillis() - start);
            } while (diff > 0);
            System.out.println("Evicting QuestionResultCache");
            QuestionResultCache.getInstance().reset();
        }
    }
}
