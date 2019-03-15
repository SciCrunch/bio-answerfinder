package org.bio_answerfinder.common;

import org.jdom2.Element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.bio_answerfinder.util.NumberUtils.getInt;


/**
 * Created by bozyurt on 6/12/17.
 */
public class Acronym implements Serializable{
    private static final long serialVersionUID = -9035511825532919222L;
    protected String acronym;
    protected List<AcrExpansion> expansionList = new ArrayList<AcrExpansion>(
            1);

    public Acronym(String acronym) {
        super();
        this.acronym = acronym;
    }

    public String getAcronym() {
        return acronym;
    }

    public AcrExpansion getExpansion(int idx) {
        return expansionList.get(idx);
    }

    public void addExpansion(AcrExpansion ae) {
        this.expansionList.add(ae);
    }

    public AcrExpansion getMostFreqExpansion() {
        if (expansionList.size() == 1) {
            return expansionList.get(0);
        }
        int max = -1;
        AcrExpansion theExp = null;
        for (AcrExpansion ae : expansionList) {
            if (ae.getFreq() > max) {
                max = ae.getFreq();
                theExp = ae;
            }
        }
        return theExp;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((acronym == null) ? 0 : acronym.hashCode());
        if (!expansionList.isEmpty()) {
            String expansion = expansionList.get(0).getExpansion();
            result = PRIME * result
                    + ((expansion == null) ? 0 : expansion.hashCode());

        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Acronym other = (Acronym) obj;
        if (acronym == null) {
            if (other.acronym != null)
                return false;
        } else if (!acronym.equals(other.acronym))
            return false;
        if (expansionList.isEmpty() && !other.expansionList.isEmpty()) {
            return false;
        } else {
            AcrExpansion ae = expansionList.get(0);
            if (!ae.getExpansion().equals(other.getExpansion(0).getExpansion())) {
                return false;
            }
        }

        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Acronym::[");
        sb.append("acronym=").append(acronym);
        for (AcrExpansion ae : this.expansionList) {
            sb.append("\n\t").append(ae.toString());
        }
        sb.append(']');
        return sb.toString();
    }

    public Element toXML() {
        Element e = new Element("acronym");
        e.setAttribute("def", acronym);
        for (AcrExpansion exp : this.expansionList) {
            e.addContent(exp.toXML());
        }
        return e;
    }

    public static Acronym fromXML(Element e) {
        String acronym = e.getAttributeValue("def");
        Acronym a = new Acronym(acronym);
        List<?> children = e.getChildren("exp");
        for (Object o : children) {
            Element expEl = (Element) o;
            a.expansionList.add(AcrExpansion.fromXML(expEl));
        }

        return a;
    }

    public static class AcrExpansion {
        String expansion;
        String expansionPT;
        /**
         * syntactic parse tree source
         */
        String source;
        String id;
        boolean canonical = false;
        String variantOf;
        int freq = 0;

        public AcrExpansion(String expansion, String expansionPT,
                            String source, String id) {
            this.expansion = expansion;
            this.expansionPT = expansionPT;
            this.source = source;
            this.id = id;
        }

        public String getExpansion() {
            return expansion;
        }

        public String getExpansionPT() {
            return expansionPT;
        }

        public String getSource() {
            return source;
        }

        public static AcrExpansion fromXML(Element e) {
            String expansion = e.getAttributeValue("text");
            String id = e.getAttributeValue("id");
            Element ptEl = e.getChild("pt");
            String source = ptEl.getAttributeValue("src");
            String pt = ptEl.getTextTrim();
            AcrExpansion ae = new AcrExpansion(expansion, pt, source, id);
            if (e.getAttribute("canonical") != null) {
                ae.setCanonical(e.getAttributeValue("canonical")
                        .equalsIgnoreCase("true"));
            }
            if (e.getAttribute("variantOf") != null) {
                ae.setVariantOf(e.getAttributeValue("variantOf"));
            }
            if (e.getAttribute("freq") != null) {
                ae.setFreq(getInt(e.getAttributeValue("freq")));
            }

            return ae;
        }

        public Element toXML() {
            Element e = new Element("exp");
            e.setAttribute("text", expansion);
            e.setAttribute("id", id);
            e.setAttribute("freq", String.valueOf(freq));
            if (variantOf != null) {
                e.setAttribute("variantOf", variantOf);
            }

            if (canonical) {
                e.setAttribute("canonical", "true");
            }
            Element ptEl = new Element("pt");
            ptEl.setAttribute("src", source);
            ptEl.setText(expansionPT);

            e.addContent(ptEl);
            return e;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("AcrExpansion::[");
            sb.append("expansion:").append(expansion);
            sb.append(",id:").append(id);
            sb.append(",pt:").append(expansionPT);
            sb.append(",src:").append(source);

            sb.append(']');
            return sb.toString();
        }

        public boolean isCanonical() {
            return canonical;
        }

        public void setCanonical(boolean canonical) {
            this.canonical = canonical;
        }

        public String getVariantOf() {
            return variantOf;
        }

        public void setVariantOf(String variantOf) {
            this.variantOf = variantOf;
        }

        public int getFreq() {
            return freq;
        }

        public void setFreq(int freq) {
            this.freq = freq;
        }

        public String getId() {
            return id;
        }

    }

    public List<AcrExpansion> getExpansionList() {
        return expansionList;
    }
}
