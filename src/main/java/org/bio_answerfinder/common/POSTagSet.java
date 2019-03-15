package org.bio_answerfinder.common;

/**
 * The set of Penn Treebank II part-of-speech tags.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public interface POSTagSet {
    /**
     * double quote
     */
    public static final int DBL_QUOTE = 48; // double quote
    /**
     * Pound sign
     */
    public static final int HASH = 37; // Pound sign
    /**
     * Dollar sign
     */
    public static final int DOLLAR = 38; // Dollar sign
    /**
     * Right close single quote
     */
    public static final int RC_SINGLE_QUOTE = 47; // Right close single
    // quote
    /**
     * Left bracket character (
     */
    public static final int LEFT_BRACKET = 42; // Left bracket character (
    /**
     * Right bracket character )
     */
    public static final int RIGHT_BRACKET = 43; // Right bracket character )
    /**
     * Comma
     */
    public static final int COMMA = 40; // Comma
    /**
     * Sentence-final punctuation
     */
    public static final int PERIOD = 39; // Sentence-final punctuation
    /**
     * Colon, semi-colon :
     */
    public static final int COLON = 41; // Colon, semi-colon :
    /**
     * Coordinating conjunction
     */
    public static final int CC = 1; // Coordinating conjunction
    /**
     * Cardinal number
     */
    public static final int CD = 2; // Cardinal number
    /**
     * Determiner
     */
    public static final int DT = 3; // Determiner
    /**
     * Existential there
     */
    public static final int EX = 4; // Existential there
    /**
     * Foreign word
     */
    public static final int FW = 5; // Foreign word
    /**
     * Preposition/subord. con
     */
    public static final int IN = 6; // Preposition/subord. con
    /**
     * Adjective
     */
    public static final int JJ = 7; // Adjective
    /**
     * Adjective, comparative
     */
    public static final int JJR = 8; // Adjective, comparative
    /**
     * Adjective, superlative
     */
    public static final int JJS = 9; // Adjective, superlative
    /**
     * List item marker
     */
    public static final int LS = 10; // List item marker
    /**
     * Modal
     */
    public static final int MD = 11; // Modal
    /**
     * Noun, singular or mass
     */
    public static final int NN = 12; // Noun, singular or mass
    /**
     * Proper noun, singular
     */
    public static final int NNP = 14; // Proper noun, singular
    /**
     * Proper noun, plural
     */
    public static final int NNPS = 15; // Proper noun, plural
    /**
     * Noun, plural
     */
    public static final int NNS = 13; // Noun, plural
    /**
     * Predeterminer
     */
    public static final int PDT = 16; // Predeterminer
    /**
     * Possessive ending
     */
    public static final int POS = 17; // Possessive ending
    /**
     * Possessive pronoun
     */
    public static final int PP = 19; // Possessive pronoun
    /**
     * Personal pronoun
     */
    public static final int PRP = 18; // Personal pronoun
    /**
     * Adverb
     */
    public static final int RB = 20; // Adverb
    /**
     * Adverb, comparative
     */
    public static final int RBR = 21; // Adverb, comparative
    /**
     * Adverb, superlative
     */
    public static final int RBS = 22; // Adverb, superlative
    /**
     * Particle
     */
    public static final int RP = 23; // Particle
    /**
     * Symbol
     */
    public static final int SYM = 24; // Symbol
    /**
     * to
     */
    public static final int TO = 25; // to
    /**
     * Interjection
     */
    public static final int UH = 26; // Interjection
    /**
     * Verb, base form
     */
    public static final int VB = 27; // Verb, base form
    /**
     * Verb, past tense
     */
    public static final int VBD = 28; // Verb, past tense
    /**
     * Verb, gerund/present participle
     */
    public static final int VBG = 29; // Verb, gerund/present participle
    /**
     * Verb, past participle
     */
    public static final int VBN = 30; // Verb, past participle
    /**
     * Verb, non-3rd ps. sing. present
     */
    public static final int VBP = 31; // Verb, non-3rd ps. sing. present
    /**
     * Verb, 3rd ps. sing. present
     */
    public static final int VBZ = 32; // Verb, 3rd ps. sing. present
    /**
     * wh-determiner
     */
    public static final int WDT = 33; // wh-determiner
    /**
     * Possessive wh-pronoun
     */
    public static final int WP = 35; // Possessive wh-pronoun
    /**
     * wh-adverb
     */
    public static final int WRB = 36; // wh-adverb
    /**
     * Left open single quote `
     */
    public static final int LO_SINGLE_QUOTE = 45; // Left open single
    // quote `
    /**
     * used by Charniak's parser for verbs have, is etc
     */
    public static final int AUX = 50; // used by Charniak's parser for verbs
    // have, is etc
}
