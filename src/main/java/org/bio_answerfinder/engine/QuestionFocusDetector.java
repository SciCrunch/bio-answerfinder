package org.bio_answerfinder.engine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.bio_answerfinder.Chunker;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.DataRecord.Chunk;
import org.bio_answerfinder.DataRecordReader;
import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.dependency.DependencyTreeFactory;
import org.bio_answerfinder.kb.LookupUtils;
import org.bio_answerfinder.nlp.sentence.TokenInfo;
import org.bio_answerfinder.nlp.sentence.WSSentenceLexer;
import org.bio_answerfinder.util.Assertion;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.*;

/**
 * Created by bozyurt on 10/11/17.
 */
public class QuestionFocusDetector {


    public QuestionFocus detectFocus(List<DataRecord> questionRecords) throws Exception {
        DataRecord.ParsedSentence ps = questionRecords.get(questionRecords.size() - 1).getSentences().get(0);
        String sentence = ps.getSentence();
        // Node rootNode = ParseTreeManager.asParseTree(ps.getPt());

        List<Chunk> chunks = ps.getChunks();
        for (Iterator<Chunk> it = chunks.iterator(); it.hasNext(); ) {
            Chunk c = it.next();
            if (!c.getType().equals("NP")) {
                it.remove();
            }
        }
        List<String> posTags = ps.getPosTags();
        WSSentenceLexer lexer = new WSSentenceLexer(sentence);
        List<TokenInfo> tiList = lexer.tokenize();

        String[] tokens = sentence.split("\\s+");
        Assertion.assertEquals(tiList.size(), posTags.size());
        int idx = 0;
        if (tiList.get(0).getTokValue().equalsIgnoreCase("list") || posTags.get(0).startsWith("V") || posTags.get(0).equals("MD")) {
            idx = 0;
        } else {
            while (idx < tiList.size() && !posTags.get(idx).startsWith("W")) {
                idx++;
            }
        }
        for (int i = idx + 1; i < tiList.size(); i++) {
            if (i == idx + 1 && posTags.get(i).startsWith("V")) {
                if (posTags.get(idx).startsWith("W") && isCopulaOrAuxVerb(tiList.get(i).getTokValue(), posTags.get(i))) {
                    continue;
                }
                break;
            }

            if (posTags.get(i).startsWith("N")) {
                int pivotIdx = getNounIdxAfterIfPropositionalPhrase(i, tiList, posTags);
                if (pivotIdx != -1) {
                    return getQuestionFocus(chunks, posTags, tiList, i, pivotIdx);
                } else {
                    Chunk chunk = findChunk(i, tiList, chunks);
                    HeadChunk hc = findHeadWord(i, tiList, posTags, chunk);
                    String phrase = hc.chunk != null ? hc.chunk.getText() : tokens[i];
                    return new QuestionFocus(phrase, hc.head, null);
                }
            }
        }
        return null;
    }

    public QuestionFocus getQuestionFocus(List<Chunk> chunks, List<String> posTags, List<TokenInfo> tiList, int i, int pivotIdx) {
        Chunk chunk2 = findChunk(pivotIdx, tiList, chunks);
        HeadChunk hc = findHeadWord(pivotIdx, tiList, posTags, chunk2);
        String modifier = tiList.get(i).getTokValue();
        if (hc.chunk != null && hc.chunk == chunk2) {
            int locIdx = tiList.indexOf(hc.ti);
            if (locIdx > 0) {
                int secondPivotIdx = getNounIdxAfterIfPropositionalPhrase2(locIdx, tiList, posTags);
                if (secondPivotIdx != -1) {
                    Chunk chunk3 = findChunk(secondPivotIdx, tiList, chunks);
                    HeadChunk hc2 = findHeadWord(secondPivotIdx, tiList, posTags, chunk3);
                    StringBuilder sb = new StringBuilder();
                    sb.append(modifier).append(' ').append(tiList.get(i + 1).getTokValue());
                    sb.append(' ').append(hc.chunk.getText());
                    sb.append(' ').append(tiList.get(locIdx + 1).getTokValue());
                    if (hc2.chunk != null) {
                        sb.append(' ').append(hc2.chunk.getText());
                    } else {
                        sb.append(' ').append(tiList.get(locIdx + 2));
                    }
                    String phrase = sb.toString();
                    QuestionFocus questionFocus = new QuestionFocus(phrase, hc.head, modifier);
                    questionFocus.setSecondFocus(hc2.head);
                    return questionFocus;
                }

            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(modifier).append(' ').append(tiList.get(i + 1).getTokValue());
        if (hc.chunk != null) {
            sb.append(' ').append(hc.chunk.getText());
        } else {
            sb.append(' ').append(tiList.get(i + 2));
        }
        String phrase = sb.toString();
        return new QuestionFocus(phrase, hc.head, modifier);
    }


    public static boolean isCopulaVerb(String token, String posTag) {
        if (!posTag.startsWith("V")) {
            return false;
        }
        String lemma = DependencyTreeFactory.getLemma(token, posTag);
        return lemma.equalsIgnoreCase("be");
    }

    public static boolean isCopulaOrAuxVerb(String token, String posTag) {
        if (!posTag.startsWith("V")) {
            return false;
        }
        String lemma = DependencyTreeFactory.getLemma(token.toLowerCase(), posTag);
        return lemma.equalsIgnoreCase("be") || lemma.equalsIgnoreCase("do") || lemma.equalsIgnoreCase("can");
    }

    public static int getNounIdxAfterIfPropositionalPhrase(int i, List<TokenInfo> tiList, List<String> posTags) {
        if (i + 2 < tiList.size() && posTags.get(i + 1).equals("IN") && !tiList.get(i + 1).getTokValue().equals("that")) {
            if (posTags.get(i + 2).startsWith("N")) {
                return i + 2;
            } else {
                int idx = i + 2;
                while (idx < posTags.size() && !posTags.get(idx).startsWith("N")) {
                    idx++;
                }
                if (idx >= posTags.size()) {
                    return -1;
                }
                return idx;
            }
        }
        return -1;
    }

    public static int getNounIdxAfterIfPropositionalPhrase2(int i, List<TokenInfo> tiList, List<String> posTags) {
        if (i + 2 < tiList.size() && posTags.get(i + 1).equals("IN")) {
            String preposition = tiList.get(i + 1).getTokValue();
            if (!preposition.equals("of") && !preposition.equals("in")) {
                return -1;
            }

            if (posTags.get(i + 2).startsWith("N")) {
                return i + 2;
            } else {
                int idx = i + 2;
                while (idx < posTags.size() && !posTags.get(idx).startsWith("N")) {
                    idx++;
                }
                if (idx >= posTags.size()) {
                    return -1;
                }
                return idx;
            }
        }
        return -1;
    }

    public static Chunk findChunk(int idx, List<TokenInfo> tokens, List<Chunk> chunks) {
        TokenInfo pivotToken = tokens.get(idx);
        if (chunks != null && !chunks.isEmpty()) {

            Chunk theChunk = null;
            int maxLen = -1;
            for (Chunk c : chunks) {
                if (pivotToken.getStart() >= c.getStartIdx() && pivotToken.getEnd() - 1 <= c.getEndIdx()) {
                    if (c.getText().length() > maxLen) {
                        theChunk = c;
                        maxLen = c.getText().length();
                    }
                }
            }
            return theChunk;
        }
        return null;
    }

    public static HeadChunk findHeadWord(int idx, List<TokenInfo> tiList, List<String> posTags, Chunk chunk) {
        if (chunk != null) {
            String[] chunkTokens = chunk.getText().split("\\s+");
            if (chunkTokens != null) {
                int offset = idx;
                int chunkOffset = 0;
                for (int i = 0; i < chunkTokens.length; i++) {
                    if (tiList.get(idx).getTokValue().equals(chunkTokens[i])) {
                        chunkOffset = i;
                        break;
                    }
                }
                boolean ok = true;
                for (int i = chunkOffset + 1; i < chunkTokens.length; i++) {
                    offset = idx + i - chunkOffset;
                    if (offset >= tiList.size() || !tiList.get(offset).getTokValue().equals(chunkTokens[i])) {
                        ok = false;
                        break;
                    }
                    if (!isEligibleHeadWord(tiList.get(offset).getTokValue()) && i + 1 == chunkTokens.length) {
                        offset--;
                    }
                }
                if (ok) {
                    String headWord = DependencyTreeFactory.getLemmaOrToken(tiList.get(offset).getTokValue(), posTags.get(offset));
                    return new HeadChunk(headWord, chunk, tiList.get(offset));
                }
            }
        }
        int start = idx;
        while (idx + 1 < posTags.size() && (posTags.get(idx + 1).startsWith("N") || posTags.get(idx + 1).startsWith("J"))) {
            idx++;
        }
        while (!posTags.get(idx).startsWith("N")) {
            idx--;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= idx; i++) {
            sb.append(tiList.get(i).getTokValue()).append(' ');
        }
        String phrase = sb.toString().trim();
        Chunk nc = new Chunk(phrase, "NP");

        String headWord = DependencyTreeFactory.getLemmaOrToken(tiList.get(idx).getTokValue(), posTags.get(idx));
        return new HeadChunk(headWord, nc, tiList.get(idx));
    }

    public static boolean isEligibleHeadWord(String token) {
        if (StringUtils.isAllSpecial(token)) {
            return false;
        }
        return true;
    }

    static class HeadChunk {
        Chunk chunk;
        String head;
        TokenInfo ti;

        public HeadChunk(String head, Chunk chunk, TokenInfo ti) {
            this.head = head;
            this.chunk = chunk;
            this.ti = ti;
        }
    }

    public static class QuestionFocus {
        String phrase;
        String focusWord;
        String modifier;
        String secondFocus;

        public QuestionFocus(String phrase, String focusWord, String modifier) {
            this.phrase = phrase;
            this.focusWord = focusWord;
            this.modifier = modifier;
        }

        public String getPhrase() {
            return phrase;
        }

        public String getFocusWord() {
            return focusWord;
        }

        public String getModifier() {
            return modifier;
        }

        public String getSecondFocus() {
            return secondFocus;
        }

        public void setSecondFocus(String secondFocus) {
            this.secondFocus = secondFocus;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("QuestionFocus{");
            sb.append("phrase:").append(phrase);
            sb.append(", focusWord:").append(focusWord);
            if (secondFocus != null) {
                sb.append(", secondFocus:").append(secondFocus);
            }
            if (modifier != null) {
                sb.append(", modifier:").append(modifier);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    static void showEntityTypes4FocusWords() throws Exception {
        String csvFile = "/tmp/question_type_annot_data.csv";
        BufferedReader in = null;
        try {
            LookupUtils lookupUtils = new LookupUtils();
            lookupUtils.initialize();
            in = FileUtils.newUTF8CharSetReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                String focus = cr.get(3);
                if (!focus.equals("Not Found") && !focus.equals("protein") && !focus.equals("gene") && !focus.equals("disease")) {
                    List<String> entityTypes = lookupUtils.getEntityType(focus);
                    if (!entityTypes.isEmpty()) {
                        String question = cr.get(1);
                        System.out.println("Q:" + question);
                        System.out.println("Focus:" + focus + " Entity Types:" + entityTypes);
                        System.out.println("=================================");
                    }
                }


            }

        } finally {
            FileUtils.close(in);
        }
    }

    static void prepQuestionTypeAnnotationCSVFile() throws Exception {
        Map<String, QuestionRecord> qidMap = new HashMap<>();
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            qidMap.put(qr.getId(), qr);
        }
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        List<DataRecord> drList = DataRecordReader.loadRecords(dataRecordXmlFile);
        // use parse tree based chunks
        Chunker.integrateNPChunks(drList);
        Map<String, List<DataRecord>> qid2DataRecordsMap = DataRecordReader.prepQid2DataRecordsMap(drList);
        QuestionFocusDetector detector = new QuestionFocusDetector();
        BufferedWriter out = null;
        CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
        try {
            LookupUtils lookupUtils = new LookupUtils();
            lookupUtils.initialize();
            out = FileUtils.newUTF8CharSetWriter("/tmp/question_type_annot_data.csv");
            CSVPrinter csvPrinter = new CSVPrinter(out, csvFormat);
            csvPrinter.printRecord(new Object[]{"qid", "question", "type", "focus", "focus modifier",
                    "secondary focus", "focus entity types", "secondary focus entity types", "answer type", "answer sub type"});
            List<String> record = new ArrayList<>(10);
            for (QuestionRecord qr : qidMap.values()) {
                List<DataRecord> qrList = qid2DataRecordsMap.get(qr.getId());

                record.clear();
                record.add(qr.getId());
                QuestionFocus questionFocus = detector.detectFocus(qrList);
                StringBuilder sb = new StringBuilder();
                for (DataRecord dr : qrList) {
                    DataRecord.ParsedSentence ps = dr.getSentences().get(0);
                    sb.append(ps.getSentence()).append(' ');
                }
                String question = sb.toString().trim();
                record.add(question);
                record.add(qr.getType());
                if (questionFocus == null) {
                    record.add("Not Found");
                    record.add("Not Found");
                    record.add("Not Found");
                } else {
                    String focus = questionFocus.getFocusWord();
                    record.add(focus);
                    record.add(questionFocus.getModifier() != null ? questionFocus.getModifier() : "");
                    record.add(questionFocus.getSecondFocus() != null ? questionFocus.getSecondFocus() : "");
                    //if (!focus.equals("protein") && !focus.equals("gene") && !focus.equals("disease") && !focus.equals("drug")) {
                    List<String> entityTypes = lookupUtils.getEntityType(focus);
                    if (entityTypes.isEmpty()) {
                        record.add("");
                    } else {
                        record.add(GenUtils.join(entityTypes, ";"));
                    }
                    //}
                    if (!GenUtils.isEmpty(questionFocus.getSecondFocus())) {
                        entityTypes = lookupUtils.getEntityType(questionFocus.getSecondFocus());
                        if (entityTypes.isEmpty()) {
                            record.add("");
                        } else {
                            record.add(GenUtils.join(entityTypes, ";"));
                        }
                    } else {
                        record.add("");
                    }
                }
                record.add("");
                record.add("");
                csvPrinter.printRecord(record);
            }
            System.out.println("wrote /tmp/question_type_annot_data.csv");
        } finally {
            FileUtils.close(out);
        }
    }


    static void testDriver() throws Exception {
        Map<String, QuestionRecord> qidMap = new HashMap<>();
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
        List<QuestionRecord> questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            qidMap.put(qr.getId(), qr);
        }
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        List<DataRecord> drList = DataRecordReader.loadRecords(dataRecordXmlFile);
        // use parse tree based chunks
        Chunker.integrateNPChunks(drList);
        Map<String, List<DataRecord>> qid2DataRecordsMap = DataRecordReader.prepQid2DataRecordsMap(drList);

        int questionCount = 0;
        QuestionFocusDetector detector = new QuestionFocusDetector();
        String questionType = "factoid";
        questionType = "list";
        questionType = "summary";
        int secondFocusCount = 0;
        for (QuestionRecord qr : qidMap.values()) {
            if (!qr.getType().equals(questionType)) {
                continue;
            }
            questionCount++;
            List<DataRecord> qrList = qid2DataRecordsMap.get(qr.getId());

            QuestionFocus questionFocus = detector.detectFocus(qrList);
            DataRecord.ParsedSentence ps = qrList.get(qrList.size() - 1).getSentences().get(0);
            String question = ps.getSentence();
            System.out.println("Q:" + question);
            System.out.println("S:" + ps.getPt());
            System.out.println(questionFocus);
            System.out.println("----------------------------------");
            if (questionFocus != null && questionFocus.getSecondFocus() != null) {
                secondFocusCount++;
            }
        }
        System.out.println("Question count:" + questionCount);
        System.out.println("# of questions with secondary focus:" + secondFocusCount);
    }

    public static void main(String[] args) throws Exception {
        // testDriver();
        prepQuestionTypeAnnotationCSVFile();
        // showEntityTypes4FocusWords();
    }
}
