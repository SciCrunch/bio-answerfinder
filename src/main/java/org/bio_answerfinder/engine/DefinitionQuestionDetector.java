package org.bio_answerfinder.engine;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.common.Acronym;
import org.bio_answerfinder.common.types.ParseTreeManagerException;
import org.bio_answerfinder.nlp.acronym.AcronymDetector;
import org.bio_answerfinder.nlp.sentence.TokenInfo;
import org.bio_answerfinder.nlp.sentence.WSSentenceLexer;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.SRLUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 11/9/17.
 */
public class DefinitionQuestionDetector {


    public static List<String> isDefinitionQuestion(List<DataRecord> questionRecords) {
        if (questionRecords.size() > 1 || questionRecords.isEmpty()) {
            return null;
        }
        DataRecord.ParsedSentence parsedSentence = questionRecords.get(0).getSentences().get(0);
        try {
            List<String> posTags = parsedSentence.getPosTags();
            String[] tokens = parsedSentence.getSentence().split("\\s+");
            if (hasWhWordNotAtTheStart(posTags)) {
                return null;
            }
            if (hasWhWordFollowedByNoun(posTags, tokens)) {
                return null;
            }

            boolean startsWithWH = posTags.get(0).startsWith("W");

            String firstToken = tokens[0].toLowerCase();
            //if (firstToken.equals("how")) {
            //    return false;
            //}
            if (startsWithWH && (firstToken.equals("where") || firstToken.equals("why") || firstToken.equals("when"))) {
                return null;
            }
            List<String> verbs = findVerbs(posTags, tokens);
            if (verbs.size() > 1) {
                for (Iterator<String> it = verbs.iterator(); it.hasNext(); ) {
                    String verb = it.next();
                    if (isCapitalizedVerbNotAtSentenceStart(verb, parsedSentence.getSentence()) ||
                            !isVerbByContext(verb, tokens, posTags)) {
                        it.remove();
                    }
                }
            }
            if (verbs.size() > 2) {
                return null;
            }


            if (verbs.size() == 1 && firstToken.equalsIgnoreCase(verbs.get(0))) {
                if (firstToken.equals("is") || firstToken.equals("are")) {
                    // most probably a yesno question
                    return null;
                }
            }
            String thePassiveVerb = null;
            if (verbs.size() == 2) {
                thePassiveVerb = getPassiveVoiceQuestionVerb(verbs, tokens);
                if (thePassiveVerb == null) {
                    return null;
                }
            }
            String theVerb = verbs.isEmpty() ? firstToken : verbs.get(0);
            if (thePassiveVerb != null) {
                theVerb = thePassiveVerb;
            }
            if (!isEligibleVerb(theVerb)) {
                return null;
            }

            String sentence = parsedSentence.getSentence();
            if (verbs.isEmpty() && parsedSentence.getChunks().size() == 1) {
                return extractQuestionPhrases(parsedSentence.getChunks().get(0), sentence, posTags);
            }

            List<String> qpList = null;
            if (parsedSentence.getChunks().size() == 1) {
                return extractQuestionPhrases(parsedSentence.getChunks().get(0), sentence, posTags);
            } else if ((qpList = hasChunkContainingOthers(parsedSentence.getChunks(), sentence, posTags)) != null) {
                return qpList;
            } else if ((qpList = hasDefinitionLikeNPCombination(parsedSentence.getChunks(), sentence, posTags)) != null) {
                return qpList;
            }
            /* else if (areChunksPartOfAcronymExpansion(parsedSentence.getChunks(), parsedSentence)) {
                return true;
            }
            */


        } catch (ParseTreeManagerException e) {
            e.printStackTrace();
        }


        return null;
    }


    public static List<String> extractQuestionPhrases(List<DataRecord.Chunk> chunks, String sentence, List<String> posTags) {
        List<String> qpList = new ArrayList<>(chunks.size());
        //TODO
        return qpList;
    }

    public static List<String> extractQuestionPhrases(DataRecord.Chunk chunk, String sentence, List<String> posTags) {
        List<String> qpList = new ArrayList<>(1);
        WSSentenceLexer lexer = new WSSentenceLexer(sentence);
        try {
            List<TokenInfo> tiList = lexer.tokenize();
            StringBuilder sb = new StringBuilder();
            int startIdx = -1;
            int endIdx = -1;
            for (int i = 0; i < tiList.size(); i++) {
                TokenInfo ti = tiList.get(i);
                if (ti.getStart() == chunk.getStartIdx()) {
                    startIdx = i;
                }
                if (ti.getEnd() == chunk.getEndIdx() + 1) {
                    endIdx = i;
                    break;
                }
            }
            while (startIdx < endIdx) {
                String startToken = tiList.get(startIdx).getTokValue();
                if (startToken.equalsIgnoreCase("the") || startToken.equalsIgnoreCase("a") || startToken.equalsIgnoreCase("an")
                        || startToken.equals("concept")) {
                    startIdx++;
                } else {
                    break;
                }
            }
            for (int i = startIdx; i <= endIdx; i++) {
                String tok = tiList.get(i).getTokValue();
                if (tok.equals(".") || tok.equals("?") || tok.equals("List") || tok.equals("``")
                        || tok.equals("''") || tok.equals("\"")) {
                    continue;
                }
                sb.append(tok).append(' ');
            }
            String qp = sb.toString().trim();
            //   np1 ( np2 )  => np1, np2
            int lpStartIdx = qp.indexOf('(');
            int lpEndIdx = qp.indexOf(')');
            if (lpStartIdx != -1 && lpEndIdx != -1) {
                qpList.add( qp.substring(0, lpStartIdx).trim());
                qpList.add(qp.substring(lpStartIdx + 1, lpEndIdx).trim());
            } else {
                qpList.add(qp);
            }
        } catch (IOException iox) {
            iox.printStackTrace();
            qpList.add(chunk.getText());
        }


        return qpList;
    }


    public static boolean isCapitalizedVerbNotAtSentenceStart(String verb, String sentence) {
        if (!Character.isUpperCase(verb.charAt(0))) {
            return false;
        }
        return sentence.indexOf(verb) != 0;
    }

    public static boolean isVerbByContext(String verb, String[] tokens, List<String> posTags) {
        int idx = indexOf(verb, tokens);
        if (idx - 1 > 0 && idx + 1 < tokens.length) {
            if ((posTags.get(idx - 1).startsWith("J") || posTags.get(idx - 1).equals("DT")) &&
                    (posTags.get(idx + 1).startsWith("N"))) {
                return false;
            }
        }
        return true;
    }

    public static String getPassiveVoiceQuestionVerb(List<String> verbs, String[] tokens) {
        if (verbs.size() != 2) {
            return null;
        }
        String copulaVerb = null;
        String theVerb = null;
        for (String verb : verbs) {
            if (verb.equals("is") || verb.equals("are")) {
                copulaVerb = verb;
            } else {
                theVerb = verb;
            }
        }
        if (copulaVerb == null) {
            return null;
        }
        int idx = indexOf(theVerb, tokens);
        // in a grammatical passive question, the verb is at the end of the question before the question mark
        if (idx != tokens.length - 1 && idx != tokens.length - 2) {
            return null;
        }
        return theVerb;
    }

    public static int indexOf(String ref, String[] tokens) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equals(ref)) {
                return i;
            }
        }
        return -1;
    }


    public static boolean areChunksPartOfAcronymExpansion(List<DataRecord.Chunk> chunks, DataRecord.ParsedSentence parsedSentence) {
        if (chunks.size() != 2) {
            return false;
        }
        AcronymDetector ad = new AcronymDetector();
        try {
            List<Acronym> acronyms = ad.findAcronyms(parsedSentence.getSentence(), parsedSentence.getPt());
            if (acronyms.size() != 1) {
                return false;
            }
            Acronym acronym = acronyms.get(0);
            boolean acrMatched = false;
            boolean expansionMatched = false;
            for (DataRecord.Chunk chunk : chunks) {
                if (acronym.getAcronym().equals(chunk.getText())) {
                    acrMatched = true;
                } else if (acronym.getMostFreqExpansion().equals(chunk.getText())) {
                    expansionMatched = true;
                }
            }

            return acrMatched && expansionMatched;
        } catch (ParseTreeManagerException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> hasChunkContainingOthers(List<DataRecord.Chunk> chunks, String sentence, List<String> posTags) {
        DataRecord.Chunk theLongest = null;
        int maxLen = -1;
        for (DataRecord.Chunk c : chunks) {
            if (c.getText().length() > maxLen) {
                theLongest = c;
                maxLen = c.getText().length();
            }
        }
        for (DataRecord.Chunk c : chunks) {
            if (c != theLongest) {
                if (!(c.getStartIdx() >= theLongest.getStartIdx() && c.getEndIdx() <= theLongest.getEndIdx())) {
                    return null;
                }
            }
        }

        return extractQuestionPhrases(theLongest, sentence, posTags);

    }

    public static List<String> hasDefinitionLikeNPCombination(List<DataRecord.Chunk> chunks, String sentence, List<String> posTags) {
        if (chunks.size() != 2) {
            return null;
        }
        // NP of NP
        DataRecord.Chunk left = chunks.get(0);
        DataRecord.Chunk right = chunks.get(1);
        if (left.getStartIdx() > right.getEndIdx()) {
            right = chunks.get(1);
            left = chunks.get(0);
        }

        int leftIdx = sentence.indexOf(left.getText());
        int rightIdx = sentence.indexOf(right.getText());
        String gap = sentence.substring(leftIdx + left.getText().length(), rightIdx).trim();
        if (!gap.equals("of")) {
            return null;
        }

        String leftNP = left.getText().toLowerCase();
        if (leftNP.endsWith("definition") || leftNP.endsWith("concept")) {
            return extractQuestionPhrases(right, sentence, posTags);
        }


        return null;
    }

    public static boolean hasWhWordNotAtTheStart(List<String> posTags) {
        if (posTags.size() < 2) {
            return false;
        }
        for (int i = 1; i < posTags.size(); i++) {
            if (posTags.get(i).startsWith("W")) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasWhWordFollowedByNoun(List<String> posTags, String[] tokens) {
        if (posTags.size() < 2) {
            return false;
        }
        for (int i = 0; i < posTags.size(); i++) {
            if (posTags.get(i).startsWith("W")) {
                if (i + 1 < posTags.size() && (posTags.get(i + 1).startsWith("N") || posTags.get(i + 1).startsWith("J"))) {
                    if (tokens[i + 1].startsWith("one")) {
                        // which one, which ones
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isEligibleVerb(String verb) {
        verb = verb.toLowerCase();
        if (verb.equals("is") || verb.equals("are") || verb.equals("describe") || verb.equals("name")
                || verb.equals("list") || verb.equals("define") || verb.equals("explain") ||
                verb.equals("defined") || verb.equals("described")) {
            return true;
        }
        return false;
    }

    public static List<String> findVerbs(List<String> posTags, String[] tokens) {
        List<String> verbs = new ArrayList<>(1);
        for (int i = 0; i < posTags.size(); i++) {
            String posTag = posTags.get(i);
            if (SRLUtils.isVerb(posTag)) {
                verbs.add(tokens[i]);
            }
        }
        return verbs;
    }


    public static void testDetector() throws Exception {
        String HOME_DIR = System.getProperty("user.home");
        String csvFile = HOME_DIR + "/dev/java/bnerkit/data/bioasq/question_type_annot_data.csv";
        BufferedReader in = null;
        QuestionParser qp = new QuestionParser();
        qp.initialize();
        int numCorrect = 0;
        int FP = 0;
        int FN = 0;
        try {
            in = FileUtils.newUTF8CharSetReader(csvFile);
            Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
            boolean first = true;
            for (Iterator<CSVRecord> it = records.iterator(); it.hasNext(); ) {
                CSVRecord cr = it.next();
                if (first) {
                    first = false;
                    continue;
                }
                String question = cr.get(1);
                String answerType = cr.get(8);
                if (!answerType.isEmpty()) {
                    //  System.out.println(answerType);
                }
                List<DataRecord> dataRecords = qp.parseQuestion("q1", question);
                List<String> qpList = DefinitionQuestionDetector.isDefinitionQuestion(dataRecords);
                boolean definitionQuestion = qpList != null && !qpList.isEmpty();
                if (answerType.equals("definition") && definitionQuestion) {
                    numCorrect++;
                    System.out.println("Correct:" + question);
                    System.out.println("\t" + qpList);
                    // uncomment to save definition questions to a file
                    // FileUtils.appendLine("/tmp/definition_questions.txt", question);
                } else {
                    if (!answerType.equals("definition")) {
                        if (definitionQuestion) {
                            System.out.println("FP: " + question);
                            FP++;
                        }
                    } else {
                        FN++;
                        System.out.println("FN: " + question);
                    }
                }
            }
            System.out.println("numCorrect:" + numCorrect + " FP:" + FP + " FN:" + FN);
        } finally {
            FileUtils.close(in);
        }
    }


    public static void main(String[] args) throws Exception {
        // testDriver();
        testDetector();
    }

    static void testDriver() throws Exception {
        QuestionParser qp = new QuestionParser();
        qp.initialize();

        List<DataRecord> dataRecords = qp.parseQuestion("q1", "Which are the Yamanaka factors ?");
        List<String> qpList = DefinitionQuestionDetector.isDefinitionQuestion(dataRecords);

        System.out.println(qpList);
    }
}
