package org.bio_answerfinder;

import org.bio_answerfinder.DataRecord.Chunk;
import org.bio_answerfinder.DataRecord.SemanticRole;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.NumberUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 6/7/17.
 */
public class DataRecordReader {
    BufferedReader in;
    XmlPullParser xpp;

    public DataRecordReader(String dataRecordXmlFile) throws IOException, XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        this.xpp = factory.newPullParser();
        in = FileUtils.newUTF8CharSetReader(dataRecordXmlFile);
        this.xpp.setInput(in);
    }

    public void close() {
        FileUtils.close(in);
    }

    public DataRecord next() throws XmlPullParserException, IOException {
        int eventType = xpp.getEventType();
        boolean inPS = false;
        boolean inChunks = false;
        boolean inRoles = false;
        boolean inNEs = false;
        DataRecord dr = null;
        DataRecord.ParsedSentence ps = null;
        do {
            if (eventType == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                if (name.equals("dr")) {
                    dr = handleDataRecord();
                } else if (name.equals("text")) {
                    dr.setText(getText());
                } else if (name.equals("chunks")) {
                    inChunks = true;
                } else if (name.equals("named-entities")) {
                    inNEs = true;
                } else if (name.equals("semantic-roles")) {
                    inRoles = true;
                } else if (name.equals("parsed-sentence")) {
                    inPS = true;
                    ps = new DataRecord.ParsedSentence();
                    dr.addParsedSentence(ps);
                } else if (name.equals("sentence")) {
                    if (inPS) {
                        ps.setSentence(getText());
                    }
                } else if (name.equals("pt")) {
                    if (inPS) {
                        ps.setPt(getText());
                    }
                } else if (name.equals("d")) {
                    if (inPS) {
                        ps.addDependency(getText());
                    }
                } else if (name.equals("chunk")) {
                    if (inPS && inChunks) {
                        ps.addChunk(handleChunk());
                    }
                } else if (name.equals("ne")) {
                    if (inPS && inNEs) {
                        ps.addNamedEntity(handleNE());
                    }
                } else if (name.equals("role")) {
                    if (inPS && inRoles) {
                        ps.addRole(handleRole());
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                String name = xpp.getName();
                if (name.equals("dr")) {
                    xpp.next();
                    return dr;
                } else if (name.equals("chunks")) {
                    inChunks = false;
                } else if (name.equals("named-entities")) {
                    inNEs = false;
                } else if (name.equals("semantic-roles")) {
                    inRoles = false;
                } else if (name.equals("parsed-sentence")) {
                    inPS = false;
                }
            }

            eventType = xpp.next();
        } while (eventType != xpp.END_DOCUMENT);
        return null;
    }

    String getText() throws IOException, XmlPullParserException {
        int eventType = xpp.next();
        if (eventType == XmlPullParser.TEXT) {
            return xpp.getText();
        }
        return null;
    }

    DataRecord handleDataRecord() {
        DataRecord dr;
        String id = null;
        String documentId = null;
        String sentenceId = null;
        String label = null;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String attrName = xpp.getAttributeName(i);
            String attrValue = xpp.getAttributeValue(i);
            if (attrName.equals("id")) {
                id = attrValue;
            } else if (attrName.equals("documentId")) {
                documentId = attrValue;
            } else if (attrName.equals("sentenceId")) {
                sentenceId = attrValue;
            } else if (attrName.equals("label")) {
                label = attrValue;
            }
        }
        dr = new DataRecord(id, null, label);
        dr.setDocumentId(documentId);
        dr.setSentenceId(sentenceId);
        return dr;
    }

    Chunk handleChunk() {
        String type = null;
        String text = null;
        String start = null;
        String end = null;
        Chunk chunk;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String attrName = xpp.getAttributeName(i);
            String attrValue = xpp.getAttributeValue(i);
            if (attrName.equals("type")) {
                type = attrValue;
            } else if (attrName.equals("text")) {
                text = attrValue;
            } else if (attrName.equals("start")) {
                start = attrValue;
            } else if (attrName.equals("end")) {
                end = attrValue;
            }
        }
        if (start == null || end == null) {
            chunk = new Chunk(text, type);
        } else {
            chunk = new Chunk(text, type, NumberUtils.getInt(start), NumberUtils.getInt(end));
        }
        return chunk;
    }

    DataRecord.NamedEntity handleNE() {
        String type = null;
        String text = null;
        String start = null;
        String end = null;

        DataRecord.NamedEntity ne;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String attrName = xpp.getAttributeName(i);
            String attrValue = xpp.getAttributeValue(i);
            if (attrName.equals("type")) {
                type = attrValue;
            } else if (attrName.equals("text")) {
                text = attrValue;
            } else if (attrName.equals("start")) {
                start = attrValue;
            } else if (attrName.equals("end")) {
                end = attrValue;
            }
        }
        if (start == null || end == null) {
            ne = new DataRecord.NamedEntity(text, type);
        } else {
            ne = new DataRecord.NamedEntity(text, type, NumberUtils.getInt(start), NumberUtils.getInt(end));
        }
        return ne;
    }

    SemanticRole handleRole() {
        String verb = null;
        String text = null;
        String label = null;
        String start = null;
        String end = null;

        SemanticRole sr;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String attrName = xpp.getAttributeName(i);
            String attrValue = xpp.getAttributeValue(i);
            if (attrName.equals("verb")) {
                verb = attrValue;
            } else if (attrName.equals("text")) {
                text = attrValue;
            } else if (attrName.equals("label")) {
                label = attrValue;
            } else if (attrName.equals("start")) {
                start = attrValue;
            } else if (attrName.equals("end")) {
                end = attrValue;
            }
        }
        if (start == null && end == null) {
            sr = new SemanticRole(verb, text, label);
        } else {
            sr = new SemanticRole(verb, text, label, NumberUtils.getInt(start), NumberUtils.getInt(end));
        }
        return sr;
    }

    public static List<DataRecord> loadRecords(String dataRecordXmlFile) throws Exception {
        List<DataRecord> drList = new LinkedList<DataRecord>();
        DataRecordReader reader = null;
        try {
            reader = new DataRecordReader(dataRecordXmlFile);
            DataRecord dr;
            while ((dr = reader.next()) != null) {
                drList.add(dr);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return drList;
    }

    public static Map<String, List<DataRecord>> prepQid2DataRecordsMap(List<DataRecord> drList) {
        Map<String, List<DataRecord>> map = new HashMap<>();
        for(DataRecord dr : drList) {
            String qid = dr.getDocumentId();
            List<DataRecord> list = map.get(qid);
            if (list == null) {
                list = new ArrayList<>(1);
                map.put(qid, list);
            }
            list.add(dr);
        }
        return map;
    }

    public static void main(String[] args) throws Exception {
        DataRecordReader reader = null;
        try {
            reader = new DataRecordReader("/tmp/bioasq_questions_parse.xml");
            DataRecord dr;
            List<DataRecord> drList = new ArrayList<DataRecord>();
            while ((dr = reader.next()) != null) {
                drList.add(dr);
            }
            System.out.println("# data records:" + drList.size());

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
