package org.bio_answerfinder.engine;

import org.apache.commons.cli.*;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.common.types.Ngram;
import org.bio_answerfinder.util.*;

import java.io.*;
import java.util.*;

/**
 * Created by bozyurt on 7/17/17.
 */
public class GloveDataPreparer {
    protected Properties properties;

    public GloveDataPreparer(String propertiesFile) throws IOException {
        this.properties = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(propertiesFile);
            this.properties.load(in);
        } finally {
            FileUtils.close(in);
        }
    }

    public void prepare(String gloveFile) throws Exception {
        new File(gloveFile).delete();
        int maxNgramSize = NumberUtils.getInt(this.properties.getProperty("max.ngram.size"));
        Set<String> ngramSet = new HashSet<>();
        for (int i = 1; i < maxNgramSize; i++) {
            String ngramTSVFile = this.properties.getProperty("ngram.file." + i);
            Assertion.assertNotNull(ngramTSVFile);
            List<Ngram> ngrams = FileUtils.loadNGramFile(ngramTSVFile);
            for (Ngram ngram : ngrams) {
                ngramSet.add(ngram.getPhrase());
            }
        }


        File rootDir = new File(this.properties.getProperty("rootDir"));
        for (File journalDir : rootDir.listFiles()) {
            List<File> paperList = new ArrayList<>();
            GenUtils.getPapers(journalDir, paperList);
            for (File paperPath : paperList) {
                String[] lines = FileUtils.readLines(paperPath.getAbsolutePath(), false, CharSetEncoding.UTF8);
                for (String line : lines) {
                    String sentence = prepSentence4Glove(line, ngramSet, maxNgramSize);
                    // System.out.println(sentence);
                    FileUtils.appendLine(gloveFile, sentence);
                }
            }
        }
        System.out.println("done. Results are in " + gloveFile);
    }

    public static String prepSentence4Glove(String line, Set<String> ngramSet, int maxNgramSize) {
        StringBuilder sb = new StringBuilder();
        StringBuilder buf = new StringBuilder();
        String[] tokens = line.split("\\s+");
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];
            if (StringUtils.isAllSpecial(token)) {
                i++;
                continue;
            }
            int len = Math.min(i + maxNgramSize, tokens.length);
            String longestPhrase = null;
            int offset = 0;
            for (int j = i + 1; j < len; j++) {
                sb.setLength(0);
                for (int k = i; k <= j; k++) {
                    sb.append(tokens[k]).append(' ');
                }
                String p = sb.toString().trim();
                if (ngramSet.contains(p)) {
                    longestPhrase = p;
                    offset = j;
                } else {
                    break;
                }
            }
            if (longestPhrase != null) {
                i = offset + 1;
                buf.append(longestPhrase.replaceAll("\\s+", "_")).append(' ');
            } else {
                buf.append(token).append(' ');
                i++;
            }
        }
        return buf.toString().trim();
    }


    public static void thresholdConditionNGramFile(String inTSVFile, String outTSVFile, float threshold) throws IOException {
        List<Ngram> ngramList = FileUtils.loadNGramFile(inTSVFile);
        for (Iterator<Ngram> it = ngramList.iterator(); it.hasNext(); ) {
            Ngram ngram = it.next();
            if (ngram.getScore() >= threshold) {
                String phrase = ngram.getPhrase();
                if (phrase.indexOf('_') == -1) {
                    String[] tokens = phrase.split("\\s+");
                    boolean found = false;
                    for (int i = 0; i < tokens.length; i++) {
                        if (StopWords.isStopWord(tokens[i])) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        it.remove();
                    }
                } else {
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
        BufferedWriter out = null;
        try {
            out = FileUtils.newUTF8CharSetWriter(outTSVFile);
            for (Ngram ngram : ngramList) {
                out.write(ngram.getPhrase() + "\t" + ngram.getScore());
                out.newLine();
            }
            System.out.println("wrote " + outTSVFile);
        } finally {
            FileUtils.close(out);
        }

    }

    public static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("GloveDataPreparer", options);
        System.exit(1);
    }

    public static void cli(String[] args) throws Exception {
        Option help = new Option("h", "print this message");
        Option commandOption = Option.builder("c").hasArg().argName("command").desc("one of threshold,prepare").required().build();
        Option ngFileOption = Option.builder("n").hasArg().desc("ngram TSV file").build();
        Option propsFileOption = Option.builder("p").hasArg().desc("properties file for Glove data preparation").build();
        Option thresholdOption = Option.builder("t").hasArg().desc("threshold for ngram score based filtering [default 0.01]").build();
        Option gloveFileOption = Option.builder("g").hasArg().desc("file for Glove training").build();
        Options options = new Options();
        options.addOption(help);
        options.addOption(commandOption);
        options.addOption(ngFileOption);
        options.addOption(propsFileOption);
        options.addOption(thresholdOption);
        options.addOption(gloveFileOption);
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
        String command = line.getOptionValue('c');
        if (!command.equalsIgnoreCase("threshold") && !command.equalsIgnoreCase("prepare")) {
            usage(options);
        }
        if (command.equalsIgnoreCase("threshold")) {
            if (!line.hasOption('n')) {
                usage(options);
            }
            float threshold = 0.01f;
            if (line.hasOption('t')) {
                threshold = NumberUtils.toFloat(line.getOptionValue('t'), 0.01f);
            }
            System.out.println("using threshold:" + threshold);
            String ngramTSVFile = line.getOptionValue('n');
            String outFilename = new File(ngramTSVFile).getName().replaceFirst("\\.\\w+$", "_th.tsv");
            String outTSVFile = "/tmp/" + outFilename;
            GloveDataPreparer.thresholdConditionNGramFile(ngramTSVFile, outTSVFile, threshold);
        } else {
            if (!line.hasOption('p') || !line.hasOption('g')) {
                usage(options);
            }
            String propertiesFile = line.getOptionValue('p');
            String gloveFile = line.getOptionValue('g');
            GloveDataPreparer gdp = new GloveDataPreparer(propertiesFile);
            gdp.prepare(gloveFile);
        }

    }

    public static void main(String[] args) throws Exception {
        cli(args);
    }

}
