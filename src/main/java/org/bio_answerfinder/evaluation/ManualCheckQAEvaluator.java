package org.bio_answerfinder.evaluation;


import org.bio_answerfinder.util.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bozyurt on 1/18/18.
 */
public class ManualCheckQAEvaluator {

    public static void showMRR(String qaResultsFile) throws IOException {
        Pattern rankPattern = Pattern.compile("rank:([\\d,]+)");
        BufferedReader in = null;
        double sum = 0;
        int noQuestions = 0;
        int noManual = 0;
        boolean manualRank = false;
        double manualSum = 0;
        try {
            in = FileUtils.newUTF8CharSetReader(qaResultsFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Answer:")) {
                    manualRank = false;
                    noQuestions++;
                    line = in.readLine();
                    Matcher m = rankPattern.matcher(line);
                    if (m.find()) {
                        int rank = Integer.parseInt(m.group(1));
                        line = in.readLine();
                        if (line.startsWith("Manual:")) {
                            line = in.readLine();
                            m = rankPattern.matcher(line);
                            if (m.find()) {
                                String rankStr = m.group(1);
                                rank = extractRank(rankStr);
                                manualRank = true;
                            }
                            noManual++;
                        }
                        if (rank > 0) {
                            sum += 1.0 / rank;
                        }

                        if (rank > 0 && manualRank) {
                            manualSum += 1.0 / rank;
                        }
                    }
                }
            }
            System.out.println("no manual ranks:" + noManual);
            System.out.println("# of questions:" + noQuestions);
            double MRR = sum / noQuestions;
            System.out.println("MRR:" + MRR);
            double manualMRR = manualSum / noManual;
            System.out.println("Manual ranked only MRR:" + manualMRR);

        } finally {
            FileUtils.close(in);
        }
    }

    @SuppressWarnings("Duplicates")
    public static void showPrecisionAtOne(String qaResultsFile) throws IOException {
        Pattern rankPattern = Pattern.compile("rank:([\\d,]+)");
        BufferedReader in = null;
        double sum = 0;
        int noQuestions = 0;
        int noManual = 0;
        boolean manualRank = false;
        double manualSum = 0;
        try {
            in = FileUtils.newUTF8CharSetReader(qaResultsFile);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Answer:")) {
                    manualRank = false;
                    noQuestions++;
                    line = in.readLine();
                    Matcher m = rankPattern.matcher(line);
                    if (m.find()) {
                        int rank = Integer.parseInt(m.group(1));
                        line = in.readLine();
                        if (line.startsWith("Manual:")) {
                            line = in.readLine();
                            m = rankPattern.matcher(line);
                            if (m.find()) {
                                String rankStr = m.group(1);
                                rank = extractRank(rankStr);
                                manualRank = true;
                            }
                            noManual++;
                        }
                        if (rank == 1) {
                            sum += 1.0;
                        }

                    }
                }
            }
            System.out.println("no manual ranks:" + noManual);
            System.out.println("# of questions:" + noQuestions);
            double precisionAtOne = sum / noQuestions;
            System.out.println("precision@1:" + precisionAtOne);

        } finally {
            FileUtils.close(in);
        }

    }
    public static int extractRank(String rankStr) {
        if (rankStr.indexOf(',') != -1) {
            String[] tokens = rankStr.split(",");
            return Integer.parseInt(tokens[tokens.length - 1]);
            // return Integer.parseInt(tokens[0]);
        } else {
            return Integer.parseInt(rankStr);
        }
    }


    public static void main(String[] args) throws IOException {
        String homeDir = System.getProperty("user.home");
        String qaResultsFile = homeDir + "/dev/java/bio-answerfinder/data/bioasq/qaengine1/question_results_wmd_defn_focus.txt";

        showMRR(qaResultsFile);

        System.out.println("=============");
        showPrecisionAtOne(qaResultsFile);
    }
}
