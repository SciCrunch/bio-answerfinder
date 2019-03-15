package org.bio_answerfinder.util;


import org.bio_answerfinder.common.WordVector;

import java.util.Map;

/**
 * Created by bozyurt on 9/19/17.
 */
public class MathUtils {

    public static double euclidianNorm(float[] vector) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * vector[i];
        }
        if (sum == 0) return 0;
        return Math.sqrt(sum);
    }

    /**
     * optimized for 1NN a remains same , b changes
     *
     * @param a
     * @param b
     * @param normA
     * @return
     */
    public static double cosSimilarity(float[] a, float[] b, double normA) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        if (sum == 0) {
            return 0;
        }
        double normB = euclidianNorm(b);
        return sum / (normA * normB);
    }

    public static float relaxedWMD(Map<String, WordVector> queryMap, Map<String, WordVector> abstractMap, boolean both) {
        double denum = 0;
        double sum = 0;
        for (WordVector qv : queryMap.values()) {
            double norm1 = MathUtils.euclidianNorm(qv.getGloveVector());
            double maxSim = -1;
            double argmaxWeight = -1;

            for (WordVector av : abstractMap.values()) {
                double similarity = MathUtils.cosSimilarity(qv.getGloveVector(), av.getGloveVector(), norm1);
                if (similarity > maxSim) {
                    maxSim = similarity;
                    argmaxWeight = av.getWeight();
                }
            }
            //denum += argmaxWeight;
            //sum += argmaxWeight * maxSim;
            denum += qv.getWeight();
            sum += qv.getWeight() * maxSim;
        }
        float rwmd1;
        if (denum <= 0 || sum <= 0) {
            rwmd1 = 0;
        } else {
            rwmd1 = (float) (sum / denum);
        }
        if (!both) {
            return rwmd1;
        } else {
            denum = 0;
            sum = 0;

            for (WordVector av : abstractMap.values()) {
                double norm1 = MathUtils.euclidianNorm(av.getGloveVector());
                double maxSim = -1;
                double argmaxWeight = -1;
                for (WordVector qv : queryMap.values()) {
                    double similarity = MathUtils.cosSimilarity(av.getGloveVector(), qv.getGloveVector(), norm1);
                    if (similarity > maxSim) {
                        maxSim = similarity;
                        argmaxWeight = av.getWeight();
                    }
                }
                //denum += argmaxWeight;
                //sum += argmaxWeight * maxSim;
                denum += av.getWeight();
                sum += av.getWeight() * maxSim;
            }
        }
        float rwmd2;
        if (denum <= 0 || sum <= 0) {
            rwmd2 = 0;
        } else {
            rwmd2 = (float) ((sum / denum) * Math.min(abstractMap.size() / queryMap.size(), 1.0));
        }
        return Math.max(rwmd1, rwmd2);
    }

    public static double mean(double[] numbers) {
        Assertion.assertNotNull(numbers);
        Assertion.assertTrue(numbers.length > 0);
        double total = 0;
        for (double number : numbers) {
            total += number;
        }
        return total / numbers.length;
    }

    public static double std(double[] numbers) {
        double sum = 0.0, sd = 0.0;
        int length = numbers.length;

        for (double num : numbers) {
            sum += num;
        }
        double mean = sum / length;
        for (double num : numbers) {
            double diff = num - mean;
            sd += diff * diff;
        }
        return Math.sqrt(sd / length);
    }
}
