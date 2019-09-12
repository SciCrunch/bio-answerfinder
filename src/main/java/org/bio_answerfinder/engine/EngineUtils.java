package org.bio_answerfinder.engine;

import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.common.SpanPOS;
import org.bio_answerfinder.common.Utils;
import org.bio_answerfinder.common.types.ParseTreeManagerException;

import java.util.List;

/**
 * Created by bozyurt on 4/16/19.
 */
public class EngineUtils {
    public static void prepareQuestion4Prediction(List<DataRecord> dataRecords,
                                                  StringBuilder questionBuf, StringBuilder posTagsBuf) throws ParseTreeManagerException {
        for (DataRecord dr : dataRecords) {
            DataRecord.ParsedSentence ps = dr.getSentences().get(0);
            List<String> posTags = ps.getPosTags();
            List<SpanPOS> spList = Utils.tokenizeWithPOS(ps.getSentence(), posTags);
            for (SpanPOS sp : spList) {
                questionBuf.append(sp.getToken()).append(' ');
                posTagsBuf.append(sp.getPosTag()).append(' ');
            }
        }
    }
}
