package org.bio_answerfinder.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bozyurt on 9/12/16.
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {
    private final int maxEntries;
    public LRUCache(int maxEntries) {
        super(maxEntries + 1, 1.0f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > maxEntries;
    }
}
