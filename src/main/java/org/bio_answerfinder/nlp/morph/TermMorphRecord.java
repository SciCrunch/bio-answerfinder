package org.bio_answerfinder.nlp.morph;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 * @see ILemmanizer
 * @see Lemmanizer
 */
public class TermMorphRecord {
    protected String baseWord;
    protected String POS;
    /**
     * grammatical person info
     */
    protected String inflection;

    public TermMorphRecord(String word, String inflection, String pos) {
        baseWord = word;
        this.inflection = inflection;
        POS = pos;
    }

    public String getBaseWord() {
        return baseWord;
    }

    public String getInflection() {
        return inflection;
    }

    public String getPOS() {
        return POS;
    }

    public boolean isPlural() {
        if (inflection == null)
            return false;
        int idx = inflection.indexOf(' ');
        if (idx == -1 && inflection.endsWith("pl"))
            return true;
        return idx != -1 && inflection.substring(0, idx).endsWith("pl");
    }

    public boolean isSingular() {
        if (inflection == null)
            return false;
        int idx = inflection.indexOf(' ');
        if (idx == -1 && inflection.endsWith("sg"))
            return true;
        return idx != -1 && inflection.substring(0, idx).endsWith("sg");
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("TermMorphRecord::[");
        buf.append("baseWord=").append(baseWord);
        buf.append(",POS=").append(POS);
        buf.append(",inflection=").append(inflection);
        buf.append(']');
        return buf.toString();
    }

}
