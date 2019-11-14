package org.bio_answerfinder;

import org.bio_answerfinder.DataRecord.Chunk;
import org.bio_answerfinder.DataRecord.ParsedSentence;
import org.bio_answerfinder.common.*;
import org.bio_answerfinder.common.types.Node;
import org.bio_answerfinder.common.types.ParseTreeManager;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.GenUtils;
import org.bio_answerfinder.util.SRLUtils;
import org.bio_answerfinder.util.TagSetUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 6/26/17.
 */
public class Chunker {


    public static List<Chunk> extractNPChunks(ParsedSentence parsedSentence) throws ParseTreeManagerException {
        Node root = ParseTreeManager.asParseTree(parsedSentence.getPt());
        if (root == null) {
            return Collections.emptyList();
        }
        String sentence = parsedSentence.getSentence();
        List<Span> spanList = Utils.tokenize(sentence);

        List<Node> leafNodes = root.getAllLeafNodesBelow();
        List<Chunk> chunks = new ArrayList<>(5);
        collect(root, chunks, spanList, leafNodes);
        return chunks;
    }


    /**
     * replaces all already existing NP chunks with NP chunks from the parse tree.
     *
     * @param drList
     * @throws Exception
     */
    public static void integrateNPChunks(List<DataRecord> drList) throws Exception {
        for (DataRecord dr : drList) {
            for (ParsedSentence ps : dr.getSentences()) {
                if (ps.getChunks() != null) {
                    for (Iterator<Chunk> it = ps.getChunks().iterator(); it.hasNext(); ) {
                        Chunk c = it.next();
                        if (c.getType().equals("NP")) {
                            it.remove();
                        }
                    }
                } else {
                    ps.chunks = new ArrayList<>(5);
                }
                List<Chunk> npChunks = extractNPChunks(ps);
                npChunks = expandChunks(ps, npChunks);
                ps.getChunks().addAll(npChunks);
            }
        }
    }

    static void collect(Node node, List<Chunk> chunks, List<Span> spanList, List<Node> leafNodes) {
        if (TagSetUtils.getPTB2SynTagCode(node.getTag()) == PTB2SyntacticLabelSet.NP) {
            List<Node> leafNodesBelow = node.getAllLeafNodesBelow();
            if (isEligible(leafNodesBelow)) {
                List<NodeLoc> nodeLocList = new ArrayList<>(leafNodesBelow.size());
                Node lastLeafNode = null;
                if (leafNodes.size() > 1) {
                    lastLeafNode = leafNodesBelow.get(leafNodesBelow.size() - 1);
                }
                for (Node ln : leafNodesBelow) {
                    // noun phrases don't and with a verb
                    if (ln == lastLeafNode && ln.getTag().startsWith("V")) {
                        continue;
                    }
                    int locIdx = leafNodes.indexOf(ln);
                    nodeLocList.add(new NodeLoc(ln, locIdx));
                }
                if (!nodeLocList.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    int startIdx = spanList.get(nodeLocList.get(0).locIdx).getStartIdx();
                    int endIdx = spanList.get(nodeLocList.get(nodeLocList.size() - 1).locIdx).getEndIdx();
                    for (NodeLoc nodeLoc : nodeLocList) {
                        sb.append(spanList.get(nodeLoc.locIdx).getToken()).append(' ');
                    }
                    Chunk chunk = new Chunk(sb.toString().trim(), "NP", startIdx, endIdx);
                    chunks.add(chunk);
                }
            }
        }

        if (node.hasChildren()) {
            for (Node child : node.getChildren()) {
                collect(child, chunks, spanList, leafNodes);
            }
        }
    }

    static boolean isEligible(List<Node> leafNodes) {
        if (leafNodes.size() == 1) {
            int code = TagSetUtils.getPOSTagCode(leafNodes.get(0).getTag());
            if (code == POSTagSet.EX || code == POSTagSet.PRP || code == POSTagSet.PP) {
                return false;
            }
        }
        for (Node node : leafNodes) {
            int code = TagSetUtils.getPOSTagCode(node.getTag());
            if (code == POSTagSet.IN || code == POSTagSet.TO) {
                return false;
            }
        }
        return true;
    }

    public static class NodeLoc {
        final Node node;
        final int locIdx;

        public NodeLoc(Node node, int locIdx) {
            this.node = node;
            this.locIdx = locIdx;
        }
    }

    public static void testDriver() throws Exception {
        String dataRecordXmlFile = "${home}/dev/java/bnerkit/data/bioasq/bioasq_questions_nlp.xml";
        DataRecordReader reader = null;
        try {
            reader = new DataRecordReader(dataRecordXmlFile);
            DataRecord dr;
            while ((dr = reader.next()) != null) {

                ParsedSentence ps = dr.getSentences().get(0);
                List<Chunk> chunks = Chunker.extractNPChunks(ps);
                chunks = Chunker.expandChunks(ps, chunks);
                System.out.println(GenUtils.formatText(ps.getSentence(), 100));
                System.out.println("Noun Phrases:");
                for (Chunk chunk : chunks) {
                    System.out.println(chunk);
                }
            }

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    /**
     * tries to correct split NPs because of incorrect POS tagging for attributive verbs
     *
     * @param parsedSentence
     * @param chunks
     * @return
     */

    public static List<Chunk> expandChunks(ParsedSentence parsedSentence, List<Chunk> chunks) throws ParseTreeManagerException {
        String sentence = parsedSentence.getSentence();
        List<String> posTags = parsedSentence.getPosTags();
        String[] tokens = sentence.split("\\s+");
        List<String> attributiveVerbs = findAttributiveVerbs(tokens, posTags);
        if (!attributiveVerbs.isEmpty()) {
            for (String attributiveVerb : attributiveVerbs) {
                Pair<Chunk, List<Chunk>> chunks2Join = findChunks2Join(attributiveVerb, chunks, sentence);
                if (chunks2Join != null) {
                    chunks.remove(chunks2Join.getFirst());
                    chunks.removeAll(chunks2Join.getSecond());
                    for (Chunk second : chunks2Join.getSecond()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(chunks2Join.getFirst().getText()).append(' ').append(attributiveVerb).append(' ');
                        sb.append(second.getText());
                        Chunk nc = new Chunk(sb.toString(), "NP", chunks2Join.getFirst().getStartIdx(), second.getEndIdx());
                        chunks.add(nc);
                    }
                }
            }
        }

        return chunks;
    }


    public static Pair<Chunk, List<Chunk>> findChunks2Join(String attributiveVerb, List<Chunk> chunks, String sentence) {
        int idx = sentence.indexOf(attributiveVerb);

        int endIdx = idx + attributiveVerb.length();
        Chunk first = null;
        List<Chunk> secondList = new ArrayList<>(1);
        for (Chunk chunk : chunks) {
            if (chunk.getEndIdx() < idx && immediatelyBefore(chunk.getEndIdx(), idx, sentence)) {
                first = chunk;
            } else if (chunk.getStartIdx() > endIdx && immediatelyAfter(endIdx, chunk.getStartIdx(), sentence)) {
                secondList.add(chunk);
            }
        }
        if (first != null && !secondList.isEmpty()) {
            return new Pair<>(first, secondList);
        }

        return null;
    }

    public static boolean immediatelyBefore(int chunkEndIdx, int avStartIdx, String sentence) {
        for (int i = chunkEndIdx + 1; i < avStartIdx; i++) {
            if (!Character.isWhitespace(sentence.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean immediatelyAfter(int avendIdx, int chunkStartIdx, String sentence) {
        for (int i = avendIdx; i < chunkStartIdx; i++) {
            if (!Character.isWhitespace(sentence.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static List<String> findAttributiveVerbs(String[] tokens, List<String> posTags) {
        List<String> verbs = new ArrayList<>(1);
        int len = posTags.size();
        for (int i = 0; i < len; i++) {
            String posTag = posTags.get(i);
            if (SRLUtils.isVerb(posTag) && !tokens[i].equals("are") && !tokens[i].equals("is")) {
                if (i - 1 > 1 && i + 1 < len) {
                    String prevPOS = posTags.get(i - 1);
                    String nextPOS = posTags.get(i + 1);
                    if ((prevPOS.startsWith("J") || prevPOS.equals("DT")) && (nextPOS.startsWith("N"))) {
                        verbs.add(tokens[i]);
                    }
                }
            }
        }
        return verbs;
    }

    public static void main(String[] args) throws Exception {
        testDriver();
    }
}
