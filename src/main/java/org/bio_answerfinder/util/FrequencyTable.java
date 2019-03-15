package org.bio_answerfinder.util;


import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class FrequencyTable<T> {
    protected TObjectIntHashMap freqMap = new TObjectIntHashMap();
    protected List<Comparable<T>> sortedKeys;

    public void addValue(T value) {
        int sum = freqMap.get(value);
        if (sum < 0) {
            sum = 0;
        }
        sum++;
        freqMap.put(value, sum);
    }

    public TObjectIntHashMap getFreqMap() {
        return freqMap;
    }

    public int getFrequency(T key) {
        return freqMap.get(key);
    }

    public void load(String tsvFile) throws IOException {
        BufferedReader in = null;
        try {
            in = FileUtils.newUTF8CharSetReader(tsvFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                String[] tokens = line.split("\\t");
                if (tokens.length != 2) {
                    System.out.println(">> " + line);
                    System.out.println("Skipping...");
                    continue;
                }
                int freq = NumberUtils.toInt(tokens[1], -1);
                if (freq > 0) {
                    freqMap.put(tokens[0], freq);
                }
            }
        } finally {
            FileUtils.close(in);
        }
    }

    public void save(String tsvFile) throws IOException {
        BufferedWriter out = null;
        try {
            out = FileUtils.newUTF8CharSetWriter(tsvFile);
            for (Comparable<T> key : getSortedKeys()) {
                String term = key.toString();
                out.write(term + "\t" + freqMap.get(term));
                out.newLine();
            }
            System.out.println("saved " + tsvFile);
        } finally {
            FileUtils.close(out);
        }
    }

    @SuppressWarnings("unchecked")
    public void orderKeys() {
        sortedKeys = new ArrayList<Comparable<T>>(freqMap.size());
        for (TObjectIntIterator it = freqMap.iterator(); it.hasNext(); ) {
            it.advance();
            sortedKeys.add((Comparable<T>) it.key());
        }
        Collections.sort(sortedKeys, new Comparator<Comparable<T>>() {

            public int compare(Comparable<T> o1, Comparable<T> o2) {
                return o1.compareTo((T) o2);
            }
        });
    }

    public void dumpSortedByFreq() {
        List<Freq<T>> list = new ArrayList<Freq<T>>(freqMap.size());
        int total = 0;
        for (TObjectIntIterator it = freqMap.iterator(); it.hasNext(); ) {
            it.advance();
            Freq<T> freq = new Freq<T>((T) it.key(), it.value());
            list.add(freq);
            total += freq.count;
        }
        Collections.sort(list, new Comparator<Freq<T>>() {
            @Override
            public int compare(Freq<T> o1, Freq<T> o2) {
                return o1.count - o2.count;
            }
        });
        System.out.println("-----------------------------");
        for (Freq<T> freq : list) {
            System.out.println(freq);
        }
        System.out.println("Total:" + total);


    }

    public static class Freq<T> {
        T value;
        int count;

        public Freq(T value, int count) {
            super();
            this.value = value;
            this.count = count;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(value).append('\t').append(count);
            return sb.toString();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        orderKeys();

        sb.append("Value\tFrequency\n");
        sb.append("------------------\n");
        for (Comparable<T> key : sortedKeys) {
            int count = freqMap.get(key);
            sb.append(key).append('\t').append(count).append("\n");
        }
        return sb.toString();
    }

    public List<Comparable<T>> getSortedKeys() {
        if (sortedKeys == null)
            orderKeys();
        return sortedKeys;
    }

    public TObjectIntIterator<T> getIterator() {
        return freqMap.iterator();
    }

}
