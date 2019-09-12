package org.bio_answerfinder.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bozyurt on 7/15/19.
 */
public class ElasticSearchUtils {
    public static String[] splitServerURLAndPath(String urlStr) throws MalformedURLException {
        URL url = new URL(urlStr);
        String[] toks = new String[2];
        String path = url.getPath();
        int idx = urlStr.indexOf(path);
        Assertion.assertTrue(idx != -1);
        toks[0] = urlStr.substring(0, idx);
        toks[1] = path;
        return toks;
    }
}
