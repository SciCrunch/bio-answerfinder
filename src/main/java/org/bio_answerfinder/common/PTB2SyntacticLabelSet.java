package org.bio_answerfinder.common;

/**
 * Syntatic label constants for the Penn Treebank II tags.
 *
 * @author I. Burak Ozyurt
 * @version $Id: PTB2SyntacticLabelSet.java,v 1.4 2007/06/11 00:11:09 bozyurt
 *          Exp $
 */
public interface PTB2SyntacticLabelSet {
    // Clause level
    /**
     * simple declarative clause
     */
    public final static int S = 101;

    /**
     * Clause introduced by a (possibly empty) subordinating conjunction.
     */
    public final static int SBAR = 102;

    /**
     * Direct question introduced by a wh-word or wh-phrase
     */
    public final static int SBARQ = 103;

    /**
     * Inverted declerative sentence, i.e. one in which the subject follows the
     * tensed verb or modal
     */
    public final static int SINV = 104;

    /**
     * Inverted yes/no question, or main clause of a wh-question, following the
     * wh-phrase in SBARQ.
     */
    public final static int SQ = 105;

    // phrase level
    /**
     * Adjective Phrase. Phrasal category headed by an adjective (including
     * comparative and superlative adjectives). Example: outregeously expensive.
     */
    public final static int ADJP = 106;

    /**
     * Adverb phrase. Phrasal category headed by an adverb (including comparative
     * and superlative adverbs). Examples: rather timidly, verl well indeed,
     * rapidly.
     */
    public final static int ADVP = 107;

    /**
     * Conjuction Phrase. Used to mark certain "multi-word" conjuctions, such as
     * <code>as well as, instead of</code>
     */
    public final static int CONJP = 108;

    /**
     * Fragment.
     */
    public final static int FRAG = 109;

    /**
     * Interjection. Corresponds approximately to POS tag UH.
     */
    public final static int INTJ = 110;

    /**
     * List marker. Includes surrounding punctuation.
     */
    public final static int LST = 111;

    /**
     * Not A Constituent; used to show the scope of certain prenormial modifiers
     * within a noun phrase.
     */
    public final static int NAC = 112;

    /**
     * Noun phrase, Phrasal category that includes all constituents that depend
     * on the head noun.
     */
    public final static int NP = 113;

    /**
     * Used within certain complex noun phrases to mark the head of the noun
     * phrase.
     */
    public final static int NX = 114;

    /**
     * Prepositional Phrase. Phrasal category headed by a preposition.
     */
    public final static int PP = 115;

    /**
     * Parenthetical.
     */
    public final static int PRN = 116;

    /**
     * Particle. Category for words that should be tagged RP.
     */
    public final static int PRT = 117;

    /**
     * Quantifier phrase (i.e., complex measure/amount phrase); used within NP.
     */
    public final static int QP = 118;

    /**
     * Reduced Relative Clause (related to Gerunds and Participles)
     */
    public final static int RRC = 119;
    /**
     * Unlike Coordinated Phrase
     */
    public final static int UCP = 120;

    /**
     * Verb Phrase. Phrasal category headed by a verb.
     */
    public final static int VP = 121;

    /**
     * Wh-adjective phrase. Adjectival phrase containing a wh-abverb as in
     * <code>how hot</code>
     */
    public final static int WHADJP = 122;

    /**
     * Wh-abverb Phrase. Introduces a s clause with an ADVP gap. May be null
     * (containing the 0 complementizer) or lexical, containing a wh-abverb such
     * as <code>how</code> or <code>why</code>.
     */
    public final static int WHADVP = 123;

    /**
     * Wh-noun Phrase. Introduces a clause with a NP gap. May be null (containing
     * the 0 complementizer) or lexical, containing some wh-word, e.g.
     * <code>who, which book, whose daughter,none of which,</code> or
     * <code>how many leapards</code>.
     */
    public final static int WHNP = 124;

    /**
     * Wh-prepositional Phrase. Prepositional phrase containing a wh-noun phrase(
     * such as <code>of which</code> or <code>by whose authority</code>) that
     * either introduces a PP gap or contained by a <code>WHNP</code>.
     */
    public final static int WHPP = 125;

}
