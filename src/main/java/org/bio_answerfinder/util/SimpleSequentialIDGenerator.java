package org.bio_answerfinder.util;

/**
 * Created by bozyurt on 12/13/16.
 */

public class SimpleSequentialIDGenerator {
    private int counter = 0;

    public SimpleSequentialIDGenerator() {
    }

    public SimpleSequentialIDGenerator(int start) {
        this.counter = start;
    }

    public int nextID() {
        int id = this.counter++;
        return id;
    }
}