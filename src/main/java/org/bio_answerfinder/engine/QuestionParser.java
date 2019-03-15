package org.bio_answerfinder.engine;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import org.bio_answerfinder.Chunker;
import org.bio_answerfinder.DataRecord;
import org.bio_answerfinder.util.ConstituenParserUtils;
import org.bio_answerfinder.util.SimpleSequentialIDGenerator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 9/12/17.
 */
public class QuestionParser {
    StanfordCoreNLP pipeline;

    public void initialize() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse");

        this.pipeline = new StanfordCoreNLP(props);
    }


    public List<DataRecord> parseQuestion(String questionId, String content) throws Exception {
        Annotation annotation = new Annotation(content);
        pipeline.annotate(annotation);
        List<DataRecord> drList = new ArrayList<DataRecord>(2);
        SimpleSequentialIDGenerator idGenerator = new SimpleSequentialIDGenerator(1);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        if (sentences != null && !sentences.isEmpty()) {
            int sentIdx = 1;
            for (CoreMap sentenceRec : sentences) {
                StringBuilder sb = new StringBuilder(100);
                for (CoreMap token : sentenceRec.get(CoreAnnotations.TokensAnnotation.class)) {
                    String tokenString = token.get(CoreAnnotations.ValueAnnotation.class);
                    sb.append(tokenString).append(' ');
                }
                String sent = sb.toString().trim();
                Tree tree = sentenceRec.get(TreeCoreAnnotations.TreeAnnotation.class);
                StringWriter sw = new StringWriter(100);
                tree.pennPrint(new PrintWriter(sw));
                // tree.pennPrint(System.out);
                sent = ConstituenParserUtils.normalizeSentence(sent);
                String pt = sw.toString().replaceAll("\\s+", " ");
                String drID = String.valueOf(idGenerator.nextID());
                DataRecord dr = new DataRecord(drID, sent, null);
                dr.setDocumentId(questionId);
                dr.setSentenceId(String.valueOf(sentIdx));
                DataRecord.ParsedSentence ps = new DataRecord.ParsedSentence(sent, pt);
                SemanticGraph graph = sentenceRec.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
                String s = graph.toString(SemanticGraph.OutputFormat.LIST);
                String[] depStrings = s.split("\\n");
                for (String depString : depStrings) {
                    ps.addDependency(depString);
                }
                dr.addParsedSentence(ps);
                drList.add(dr);
                sentIdx++;
            }
        }
        if (!drList.isEmpty()) {
            // use parse tree based chunks
            Chunker.integrateNPChunks(drList);
        }
        return drList;
    }
}
