package org.bio_answerfinder.services;

import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerCompressionWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 10/31/17.
 */
public class SearchResultCache {
    DB db;
    HTreeMap<String, String> cache;

    public SearchResultCache() throws IOException {
        Properties props = FileUtils.loadProperties("bio-answerfinder.properties");
        String dbFile = props.getProperty("search.result.cache.db");
        Assertion.assertNotNull(dbFile);
        this.db = DBMaker.fileDB(dbFile).closeOnJvmShutdown().make();
        this.cache = db.hashMap("results").keySerializer(Serializer.STRING)
                .valueSerializer(new SerializerCompressionWrapper<>(Serializer.STRING))
                .expireMaxSize(5000).createOrOpen();
    }


    public SearchResultCache(String dbFile) throws IOException {
        Assertion.assertNotNull(dbFile);
        this.db = DBMaker.fileDB(dbFile).closeOnJvmShutdown().make();
        this.cache = db.hashMap("results").keySerializer(Serializer.STRING)
                .valueSerializer(new SerializerCompressionWrapper<>(Serializer.STRING))
                .expireMaxSize(5000).createOrOpen();
    }

    public void shutdown() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    public void put(String searchQuery, List<PubMedDoc> results) {
        JSONArray jsArr = new JSONArray();
        for (PubMedDoc pmd : results) {
            JSONObject json = pmd.toJSON2();
            jsArr.put(json);
        }
        this.cache.put(searchQuery, jsArr.toString());
        db.commit();
    }


    public List<PubMedDoc> getResults(String searchQuery) {
        if (!this.cache.containsKey(searchQuery)) {
            return null;
        }
        String jsonStr = this.cache.get(searchQuery);
        JSONArray jsArr = new JSONArray(jsonStr);
        List<PubMedDoc> results = new ArrayList<>(jsArr.length());
        for (int i = 0; i < jsArr.length(); i++) {
            results.add(PubMedDoc.fromJSON2(jsArr.getJSONObject(i)));
        }
        return results;
    }
}
