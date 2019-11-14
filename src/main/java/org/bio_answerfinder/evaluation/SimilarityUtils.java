package org.bio_answerfinder.evaluation;

import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.evaluation.FactoidExtractionEvaluation.QuestionInfo;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.MathUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by bozyurt on 9/17/19.
 */
public class SimilarityUtils {




    public static boolean fuzzyMatches(String phrase, Set<String> refTokenSet, GloveDBLookup gloveMan, double threshold) {
        String[] tokens = phrase.split("\\s+");
        double sum = 0;
        for (String token : tokens) {
            if (refTokenSet.contains(token.toLowerCase())) {
                sum += 1.0;
            } else {
                float[] fv = gloveMan.getGloveVector(token);
                if (fv != null) {
                    double fvNorm = MathUtils.euclidianNorm(fv);
                    double maxSim = -1;
                    for (String refToken : refTokenSet) {
                        float[] sv = gloveMan.getGloveVector(refToken);
                        if (sv != null) {
                            double similarity = MathUtils.cosSimilarity(fv, sv, fvNorm);
                            if (similarity > maxSim) {
                                maxSim = similarity;
                            }
                        }
                    }
                    sum += maxSim;
                }
            }
        }
        double avg = sum / tokens.length;

        return avg >= threshold;
    }

    public static class MatchingResult {
        QuestionInfo qi;
        FactoidUtils.ScoredNPInfo theAnswer;
        int rank = 0;

        public MatchingResult(QuestionInfo qi, FactoidUtils.ScoredNPInfo theAnswer, int rank) {
            this.qi = qi;
            this.theAnswer = theAnswer;
            this.rank = rank;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("MatchingResult{");
            sb.append("theAnswer=").append(theAnswer);
            sb.append(", rank=").append(rank);
            sb.append("\ngsAnswer: ").append(qi.getQr().getAnswer().getExactAnswer());
            sb.append('}');
            return sb.toString();
        }
    }
}
