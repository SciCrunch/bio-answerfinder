package org.bio_answerfinder;

import org.bio_answerfinder.common.types.Node;
import org.bio_answerfinder.common.types.ParseTreeManager;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.util.ConstituenParserUtils;
import org.bio_answerfinder.util.NumberUtils;
import org.jdom2.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/2/17.
 */
public class DataRecord implements Serializable {
    String id;
    String text;
    String label;
    String documentId;
    String sentenceId;
    String answer2Qid;
    List<ParsedSentence> sentences = new ArrayList<ParsedSentence>(1);

    public DataRecord(String id, String text, String label) {
        this.id = id;
        this.text = text;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getLabel() {
        return label;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getAnswer2Qid() {
        return answer2Qid;
    }

    public void setAnswer2Qid(String answer2Qid) {
        this.answer2Qid = answer2Qid;
    }

    public String getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }

    public void addParsedSentence(ParsedSentence ps) {
        sentences.add(ps);
    }

    public List<ParsedSentence> getSentences() {
        return sentences;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Element toXML() {
        Element drEl = new Element("dr");
        drEl.setAttribute("id", id);
        if (label != null) {
            drEl.setAttribute("label", label);
        }
        if (documentId != null) {
            drEl.setAttribute("documentId", documentId);
        }
        if (sentenceId != null) {
            drEl.setAttribute("sentenceId", sentenceId);
        }
        if (answer2Qid != null) {
            drEl.setAttribute("answer2Qid", answer2Qid);
        }
        Element textEl = new Element("text").setText(text);
        drEl.addContent(textEl);
        if (!sentences.isEmpty()) {
            Element sentencesEl = new Element("parsed-sentences");
            drEl.addContent(sentencesEl);
            for (ParsedSentence ps : sentences) {
                sentencesEl.addContent(ps.toXML());
            }
        }
        return drEl;
    }

    public static DataRecord fromXML(Element el) {
        String id = el.getAttributeValue("id");
        String label = el.getAttributeValue("label");
        String documentId = el.getAttributeValue("documentId");
        String sentenceId = el.getAttributeValue("sentenceId");
        String answer2Qid = el.getAttributeValue("answer2Qid");
        String text = el.getChildTextTrim("text");
        DataRecord dr = new DataRecord(id, text, label);
        dr.documentId = documentId;
        dr.sentenceId = sentenceId;
        dr.setAnswer2Qid(answer2Qid);
        Element parsedSentences = el.getChild("parsed-sentences");
        if (parsedSentences != null) {
            List<Element> children = parsedSentences.getChildren("parsed-sentence");
            for (Element child : children) {
                dr.addParsedSentence(ParsedSentence.fromXML(child));
            }
        }
        return dr;
    }

    public static class ParsedSentence {
        String sentence;
        String pt;
        List<String> depList = new ArrayList<String>(10);
        List<SemanticRole> roles;
        List<NamedEntity> neList;
        List<Chunk> chunks;
        List<String> posTags;

        public ParsedSentence(String sentence, String pt) {
            this.sentence = ConstituenParserUtils.normalizeSentence(sentence);
            this.pt = pt;
        }

        public ParsedSentence() {
        }


        public void setSentence(String sentence) {
            this.sentence = ConstituenParserUtils.normalizeSentence(sentence);
        }

        public void setPt(String pt) {
            this.pt = pt;
        }

        public String getSentence() {
            return sentence;
        }

        public String getPt() {
            return pt;
        }

        public List<String> getDepList() {
            return depList;
        }

        public List<SemanticRole> getRoles() {
            return roles;
        }

        public List<NamedEntity> getNeList() {
            return neList;
        }

        public List<Chunk> getChunks() {
            return chunks;
        }

        public void prepPosTags() throws ParseTreeManagerException {
            Node root = ParseTreeManager.asParseTree(pt);
            List<Node> leafNodes = root.getAllLeafNodesBelow();
            posTags = new ArrayList<>(leafNodes.size());
            for (Node n : leafNodes) {
                posTags.add(n.getTag());
            }
        }

        public List<String> getPosTags() throws ParseTreeManagerException {
            if (posTags == null) {
                prepPosTags();
            }
            return posTags;
        }

        public void addDependency(String dep) {
            depList.add(dep);
        }

        public void addRole(SemanticRole sr) {
            if (roles == null) {
                roles = new ArrayList<SemanticRole>(2);
            }
            roles.add(sr);
        }

        public void addChunk(Chunk chunk) {
            if (chunks == null) {
                chunks = new ArrayList<Chunk>(3);
            }
            chunks.add(chunk);
        }

        public void addNamedEntity(NamedEntity ne) {
            if (neList == null) {
                neList = new ArrayList<NamedEntity>(1);
            }
            neList.add(ne);
        }

        public boolean hasRoles() {
            return roles != null && !roles.isEmpty();
        }

        public boolean hasChunks() {
            return chunks != null && !chunks.isEmpty();
        }

        public boolean hasNamedEntities() {
            return neList != null && !neList.isEmpty();
        }

        public static ParsedSentence fromXML(Element el) {
            ParsedSentence ps = new ParsedSentence();
            ps.sentence = el.getChildTextTrim("sentence");
            ps.pt = el.getChildTextTrim("pt");
            Element deps = el.getChild("deps");
            if (deps != null) {
                List<Element> children = deps.getChildren("d");
                for (Element child : children) {
                    ps.depList.add(child.getTextTrim());
                }
            }
            Element chunks = el.getChild("chunks");
            if (chunks != null) {
                List<Element> children = chunks.getChildren("chunk");
                for (Element child : children) {
                    ps.addChunk(Chunk.fromXML(child));
                }
            }
            Element nes = el.getChild("named-entities");
            if (nes != null) {
                List<Element> children = nes.getChildren("ne");
                for (Element child : children) {
                    ps.addNamedEntity(NamedEntity.fromXML(child));
                }
            }
            Element semanticRoles = el.getChild("semantic-roles");
            if (semanticRoles != null) {
                List<Element> children = semanticRoles.getChildren("role");
                for (Element child : children) {
                    ps.addRole(SemanticRole.fromXML(child));
                }
            }
            return ps;
        }

        public Element toXML() {
            Element el = new Element("parsed-sentence");
            el.addContent(new Element("sentence").setText(sentence));
            el.addContent(new Element("pt").setText(pt));
            if (!depList.isEmpty()) {
                Element deps = new Element("deps");
                el.addContent(deps);
                for (String dep : depList) {
                    deps.addContent(new Element("d").setText(dep));
                }
            }
            if (hasChunks()) {
                Element cel = new Element("chunks");
                el.addContent(cel);
                for (Chunk c : chunks) {
                    if (c.getType() != null && c.getText() != null) {
                        cel.addContent(c.toXML());
                    }
                }
            }
            if (hasNamedEntities()) {
                Element nel = new Element("named-entities");
                el.addContent(nel);
                for (NamedEntity ne : neList) {
                    nel.addContent(ne.toXML());
                }
            }
            if (hasRoles()) {
                Element sel = new Element("semantic-roles");
                el.addContent(sel);
                for (SemanticRole sr : roles) {
                    sel.addContent(sr.toXML());
                }

            }
            return el;
        }
    }

    public static class NamedEntity {
        String text;
        String neType;
        int startIdx = -1;
        int endIdx = -1;

        public NamedEntity(String text, String neType) {
            this.text = text;
            this.neType = neType;
        }

        public NamedEntity(String text, String neType, int startIdx, int endIdx) {
            this.text = text;
            this.neType = neType;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        public String getText() {
            return text;
        }

        public String getNeType() {
            return neType;
        }

        public int getStartIdx() {
            return startIdx;
        }

        public int getEndIdx() {
            return endIdx;
        }

        public Element toXML() {
            Element el = new Element("ne");
            el.setAttribute("type", neType);
            el.setAttribute("text", text);
            if (startIdx >= 0) {
                el.setAttribute("start", String.valueOf(startIdx));
            }
            if (endIdx >= 0) {
                el.setAttribute("end", String.valueOf(endIdx));
            }
            return el;
        }

        public static NamedEntity fromXML(Element el) {
            String text = el.getAttributeValue("text");
            String neType = el.getAttributeValue("type");
            NamedEntity ne = new NamedEntity(text, neType);
            String start = el.getAttributeValue("start");
            String end = el.getAttributeValue("end");
            if (start != null && end != null) {
                ne.startIdx = NumberUtils.getInt(start);
                ne.endIdx = NumberUtils.getInt(end);
            }
            return ne;
        }
    }

    public static class Chunk {
        String text;
        String type;
        int startIdx = -1;
        int endIdx = -1;

        public Chunk(String text, String type) {
            this.text = text;
            this.type = type;
        }

        public Chunk(String text, String type, int startIdx, int endIdx) {
            this.text = text;
            this.type = type;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        public String getText() {
            return text;
        }

        public String getType() {
            return type;
        }

        public int getStartIdx() {
            return startIdx;
        }

        public int getEndIdx() {
            return endIdx;
        }

        public Element toXML() {
            Element el = new Element("chunk");
            el.setAttribute("type", type);
            el.setAttribute("text", text);
            if (startIdx >= 0) {
                el.setAttribute("start", String.valueOf(startIdx));
            }
            if (endIdx >= 0) {
                el.setAttribute("end", String.valueOf(endIdx));
            }
            return el;
        }

        public static Chunk fromXML(Element el) {
            String text = el.getAttributeValue("text");
            String type = el.getAttributeValue("type");
            Chunk chunk = new Chunk(text, type);
            String start = el.getAttributeValue("start");
            String end = el.getAttributeValue("end");
            if (start != null && end != null) {
                chunk.startIdx = NumberUtils.getInt(start);
                chunk.endIdx = NumberUtils.getInt(end);
            }
            return chunk;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Chunk{");
            sb.append("text='").append(text).append('\'');
            sb.append(", type='").append(type).append('\'');
            sb.append(", startIdx=").append(startIdx);
            sb.append(", endIdx=").append(endIdx);
            sb.append('}');
            return sb.toString();
        }
    }

    public static class SemanticRole {
        String verb;
        String text;
        String label;
        int startIdx = -1;
        int endIdx = -1;

        public SemanticRole(String verb, String text, String label) {
            this.verb = verb;
            this.text = text;
            this.label = label;
        }

        public SemanticRole(String verb, String text, String label, int startIdx, int endIdx) {
            this.verb = verb;
            this.text = text;
            this.label = label;
            this.startIdx = startIdx;
            this.endIdx = endIdx;
        }

        public String getVerb() {
            return verb;
        }

        public String getText() {
            return text;
        }

        public String getLabel() {
            return label;
        }

        public int getStartIdx() {
            return startIdx;
        }

        public int getEndIdx() {
            return endIdx;
        }

        public Element toXML() {
            Element el = new Element("role");
            el.setAttribute("verb", verb);
            el.setAttribute("label", label);
            el.setAttribute("text", text);
            if (startIdx >= 0) {
                el.setAttribute("start", String.valueOf(startIdx));
            }
            if (endIdx >= 0) {
                el.setAttribute("end", String.valueOf(endIdx));
            }
            return el;
        }

        public static SemanticRole fromXML(Element el) {
            String verb = el.getAttributeValue("verb");
            String label = el.getAttributeValue("label");
            String text = el.getAttributeValue("text");
            SemanticRole sr = new SemanticRole(verb, text, label);
            String start = el.getAttributeValue("start");
            String end = el.getAttributeValue("end");
            if (start != null && end != null) {
                sr.startIdx = NumberUtils.getInt(start);
                sr.endIdx = NumberUtils.getInt(end);
            }
            return sr;
        }
    }
}
