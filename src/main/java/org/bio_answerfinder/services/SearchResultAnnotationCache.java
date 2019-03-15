package org.bio_answerfinder.services;

import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.FrequencyTable;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by bozyurt on 11/16/17.
 */
public class SearchResultAnnotationCache {
    DB db;
    HTreeMap<String, String> cache;

    public SearchResultAnnotationCache() throws IOException {
        Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
        String dbFile = props.getProperty("search.annotation.cache.db");
        Assertion.assertNotNull(dbFile);
        this.db = DBMaker.fileDB(dbFile).closeOnJvmShutdown().make();
        this.cache = db.hashMap("results").keySerializer(Serializer.STRING)
                .valueSerializer(new SerializerCompressionWrapper<>(Serializer.STRING))
                .createOrOpen();
    }

    public void shutdown() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    public void put(AnnotatedAbstract annotatedAbstract) {
        this.cache.put(annotatedAbstract.getPmid(), annotatedAbstract.toJSON().toString());
        db.commit();
    }

    public AnnotatedAbstract getAnnnotatedAbstract(String pmid) {
        if (!this.cache.containsKey(pmid)) {
            return null;
        }
        String jsonStr = this.cache.get(pmid);
        return AnnotatedAbstract.fromJSON(new JSONObject(jsonStr));
    }


    public Set<String> getKeySet() {
        return this.cache.keySet();
    }


    /*
    public void put(String searchQuery, List<AnnotatedAbstract> results) {
        JSONArray jsArr = new JSONArray();
        for (AnnotatedAbstract aa : results) {
            jsArr.put(aa.toJSON());
        }
        this.cache.put(searchQuery, jsArr.toString());
        db.commit();
    }

    public List<AnnotatedAbstract> getAnnotatedResults(String searchQuery) {
        if (!this.cache.containsKey(searchQuery)) {
            return null;
        }
        String jsonStr = this.cache.get(searchQuery);
        JSONArray jsArr = new JSONArray(jsonStr);
        List<AnnotatedAbstract> results = new ArrayList<>(jsArr.length());
        for (int i = 0; i < jsArr.length(); i++) {
            results.add(AnnotatedAbstract.fromJSON(jsArr.getJSONObject(i)));
        }
        return results;
    }
    */


    public static void main(String[] args) throws Exception {
        SearchResultAnnotationCache cache = null;
        try {
            cache = new SearchResultAnnotationCache();

            FrequencyTable<String> ft = new FrequencyTable<>();
            System.out.println("# of pmids:" + cache.cache.size());
            Set<String> keySet = cache.getKeySet();
            int i = 1;
            for(String pmid : keySet) {
                AnnotatedAbstract aa = cache.getAnnnotatedAbstract(pmid);
                for(AnnotatedAbstract.AnnotatedSentence as : aa.getAnnotatedSentences()) {
                    Map<String, List<Concept>> annotations = AnnotationService.getAnnotations(as);
                    for(String term : annotations.keySet()) {
                        ft.addValue(term);
                    }
                }
                System.out.print("\r handling abstract:" + i);
                i++;
            }
            ft.dumpSortedByFreq();
        } finally {
            if (cache != null) {
                cache.shutdown();
            }
        }

    }
}
