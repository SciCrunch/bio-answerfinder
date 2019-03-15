package org.bio_answerfinder.nlp.morph;

/**
 * 
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public interface ILemmanizer {
    public final static String NOUN = "N";
    public final static String VERB = "V";
    public final static String ADJECTIVE = "A";
    public final static String PRONOUN = "Pron";
    

    public TermMorphRecord getInfinitive(String verb) throws MorphException;

    /**
         * 
         * @param term
         * @param posType one of <code>Lemmanizer.NOUN</code> ,
         *        <code>Lemmanizer.VERB</code> or
         *        <code>Lemmanizer.ADJECTIVE</code>.
         * @return
         * @throws MorphException
         */
    public TermMorphRecord getLemma(String term, String posType)
	    throws MorphException;

    public void shutdown();
}
