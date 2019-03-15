package org.bio_answerfinder.util;

import org.bio_answerfinder.common.POSTagSet;
import org.bio_answerfinder.common.PTB2SyntacticLabelSet;
import org.bio_answerfinder.common.types.Node;
import org.bio_answerfinder.nlp.morph.Lemmanizer;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 2/25/19.
 */
public class SRLUtils {

    /**
     * @param tag tag value for a parse tree node
     * @return true if POS tag corresponds to a verb
     */
    public static boolean isVerb(String tag) {
        int tagCode = TagSetUtils.getPOSTagCode(tag);
        return (tagCode == POSTagSet.VB || tagCode == POSTagSet.VBD
                || tagCode == POSTagSet.VBG || tagCode == POSTagSet.VBN
                || tagCode == POSTagSet.VBP || tagCode == POSTagSet.VBZ);
    }

    /**
     * initializes and returns a {@link org.bio_answerfinder.nlp.morph.Lemmanizer}
     * object. Database parameters are read from <code>nlp.properties</code>. To
     * specify a separate morphology database (e.g. SQLite db file) specify
     * <code>morph.jdbc.url</code> property.
     *
     * @return a {@link Lemmanizer} object.
     * @throws Exception
     */
    public static Lemmanizer prepLemmanizer() throws Exception {
        Properties props = FileUtils.loadProperties("/bio-answerfinder.properties");
        String jdbcURL = props.getProperty("jdbc.url");
        String dbUser = props.getProperty("jdbc.user");
        String pwd = props.getProperty("jdbc.pwd");
        String morphJdbcUrl = props.getProperty("morph.jdbc.url");
        if (morphJdbcUrl != null) {
            jdbcURL = morphJdbcUrl;
        }
        Lemmanizer lemmanizer = new Lemmanizer(jdbcURL, dbUser, pwd);

        return lemmanizer;
    }

    /**
     * returns true if the POS tag is <code>POSTagSet.NN</code> or
     * <code>POSTagSet.NNS</code>.
     *
     * @param tag Brown POS tag as string
     * @return true if the POS tag is <code>POSTagSet.NN</code> or
     * <code>POSTagSet.NNS</code>.
     */
    public static boolean isCommonNoun(String tag) {
        int tagCode = TagSetUtils.getPOSTagCode(tag);
        return (tagCode == POSTagSet.NN || tagCode == POSTagSet.NNS);
    }

    public static void findNounPhrases(Node parent, List<Node> npList) {
        int tagCode;
        if (parent != null) {
            tagCode = TagSetUtils.getPTB2SynTagCode(parent.getTag());
            if (tagCode == PTB2SyntacticLabelSet.NP) {
                npList.add(parent);
            }
            if (parent.hasChildren()) {
                for (Node child : parent.getChildren()) {
                    findNounPhrases(child, npList);
                }
            }
        }
    }

}
