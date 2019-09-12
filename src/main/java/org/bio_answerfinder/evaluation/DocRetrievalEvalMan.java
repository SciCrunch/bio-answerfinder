package org.bio_answerfinder.evaluation;

import org.apache.commons.cli.*;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.engine.AnswerSentence;
import org.bio_answerfinder.engine.FileLogCollector;
import org.bio_answerfinder.engine.ILogCollector;
import org.bio_answerfinder.util.FileUtils;
import org.bio_answerfinder.util.Profiler;
import org.bio_answerfinder.ws.BioAnswerFinderEngineService;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 7/19/19.
 */
public class DocRetrievalEvalMan {

    public static void testBaselinePerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "baseline");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/baseline_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }


    public static void testKeywordClassifierInclusionPerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "baseline-kw");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/baseline_kw_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    public static void testKeywordClassifierIterativePerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "keyword");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/kw_iterative_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    public static void testKeywordRankClassifierIterativePerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "rank");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/rank_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    public static void testEnsemblePerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "ensemble");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/ensemble_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    public static void testAKNNPerformance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "aknn");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/aknn_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    public static void testEnsemble2Performance() throws Exception {
        try {
            BioAnswerFinderEngineService.getInstance().initialize();
            String HOME_DIR = System.getProperty("user.home");
            String rankQuestionsFile = HOME_DIR + "/dev/java/bio-answerfinder/data/rank_test/rank_test_questions.txt";
            String[] questions = FileUtils.readLines(rankQuestionsFile, true, CharSetEncoding.UTF8);
            Profiler profiler = new Profiler("bio-answerfinder");
            int numNoAnswer;
            Map<String, String> options = new HashMap<>();
            options.put("useAnswerCache", "false");
            options.put("retrievalMode", "ensemble2");
            try (BufferedWriter out = FileUtils.newUTF8CharSetWriter("/tmp/ensemble2_retrieval_results.txt")) {
                numNoAnswer = handleQA(questions, profiler, options, out);
            }
            profiler.showStats();
            System.out.println("numNoAnswer:" + numNoAnswer);
        } finally {
            System.out.println("shutting down...");
            BioAnswerFinderEngineService.getInstance().shutdown();
        }
    }

    static int handleQA(String[] questions, Profiler profiler, Map<String, String> options,
                        BufferedWriter out) throws Exception {
        int numNoAnswer = 0;
        ILogCollector logCollector = new FileLogCollector(out);
        BioAnswerFinderEngineService.getInstance().getEngine().setLogCollector(logCollector);
        for (String question : questions) {
            profiler.start("qa");
            out.write("+++++++++++++++++++++++++++++++++");
            out.newLine();
            List<AnswerSentence> sentences = BioAnswerFinderEngineService.getInstance()
                    .answerQuestion(question, 10, options);
            out.write("+++++++++++++++++++++++++++++++++");
            out.newLine();
            out.write(question);
            out.newLine();
            int len = Math.min(10, sentences.size());
            for (int i = 0; i < len; i++) {
                out.write(i + ") " + sentences.get(i).getSentence());
                out.newLine();
            }
            out.write("--------------------");
            out.newLine();
            out.flush();
            profiler.stop("qa");
            if (sentences.isEmpty()) {
                numNoAnswer++;
            }
        }
        return numNoAnswer;
    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DocRetrievalEvalMan", options);
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option cmdOption = Option.builder("c").required().hasArg().argName("command")
                .desc("command one of [baseline, baseline_kw, keyword, rank, ensemble, aknn, ensemble2]").build();

        Options options = new Options();
        options.addOption(help);
        options.addOption(cmdOption);

        CommandLineParser cli = new DefaultParser();
        CommandLine line = null;
        try {
            line = cli.parse(options, args);
        } catch (Exception x) {
            System.err.println(x.getMessage());
            usage(options);
        }
        if (line.hasOption("h")) {
            usage(options);
        }

        String cmd = line.getOptionValue("c");
        if (cmd.equals("baseline")) {
            testBaselinePerformance();
        } else if (cmd.equals("baseline_kw")) {
            testKeywordClassifierInclusionPerformance();
        } else if (cmd.equals("keyword")) {
            testKeywordClassifierIterativePerformance();
        } else if (cmd.equals("rank")) {
            testKeywordRankClassifierIterativePerformance();
        } else if (cmd.equals("ensemble")) {
            testEnsemblePerformance();
        } else if (cmd.equals("aknn")) {
            testAKNNPerformance();
        } else if (cmd.equals("ensemble2")) {
            testEnsemble2Performance();
        }

    }

}
