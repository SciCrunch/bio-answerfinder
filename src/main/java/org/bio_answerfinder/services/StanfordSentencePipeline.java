package org.bio_answerfinder.services;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.bio_answerfinder.util.ConstituenParserUtils;

import java.util.List;
import java.util.Properties;

/**
 * Created by bozyurt on 7/7/17.
 */
public class StanfordSentencePipeline implements ISentencePipeline {
    StanfordCoreNLP pipeline;

    public StanfordSentencePipeline() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        this.pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public void extractSentences(String content, List<String> allSentences) throws Exception{
        Annotation annotation = new Annotation(content);
        pipeline.annotate(annotation);
        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
        StringBuilder sb = new StringBuilder(300);
        if (sentences != null && !sentences.isEmpty()) {
            for (CoreMap sentenceRec : sentences) {
                sb.setLength(0);
                for (CoreMap token : sentenceRec.get(CoreAnnotations.TokensAnnotation.class)) {
                    String tokenString = token.get(CoreAnnotations.ValueAnnotation.class);
                    sb.append(tokenString).append(' ');
                }
                String sent = sb.toString().trim();
                sent = ConstituenParserUtils.normalizeSentence(sent);
                allSentences.add(sent);
            }
        }
    }
}
