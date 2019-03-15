package org.bio_answerfinder.util;


import org.bio_answerfinder.nlp.morph.ILemmanizer;
import org.bio_answerfinder.nlp.morph.MorphException;
import org.bio_answerfinder.nlp.morph.TermMorphRecord;

import java.io.BufferedReader;
import java.net.URL;
import java.util.*;

/**
 * Created by bozyurt on 12/1/17.
 */
public class VerbUtils {
    ILemmanizer lemmanizer;
    Map<String, VerbInfo> irregularVerbMap = new HashMap<>();

    public VerbUtils(ILemmanizer lemmanizer) {
        this.lemmanizer = lemmanizer;
    }

    public void initialize() throws Exception {
        URL resource = VerbUtils.class.getClassLoader().getResource("irregular_verbs.tsv");
        String path = resource.toURI().getPath();
        BufferedReader in = null;

        try {
            in = FileUtils.newUTF8CharSetReader(path);
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\\s+");
                // System.out.println(tokens[0]);
                VerbInfo vi = new VerbInfo(tokens[0]);
                irregularVerbMap.put(vi.getInfinitive(), vi);
                if (tokens[1].indexOf("/") != -1) {
                    int idx = tokens[1].indexOf("/");
                    vi.addPastTense(tokens[1].substring(0, idx));
                    vi.addPastTense(tokens[1].substring(idx + 1, tokens[1].length()));
                } else {
                    vi.addPastTense(tokens[1]);
                }
                if (tokens.length == 3) {
                    if (tokens[2].indexOf("/") != -1) {
                        int idx = tokens[2].indexOf("/");
                        vi.addPastParticipleForm(tokens[2].substring(0, idx));
                        vi.addPastParticipleForm(tokens[2].substring(idx + 1, tokens[2].length()));
                    } else {
                        vi.addPastParticipleForm(tokens[2]);
                    }
                }
                //System.out.println(vi);
            }
        } finally {
            FileUtils.close(in);
        }
    }

    public List<String> getAllVerbConjugations(String verb)  {
        TermMorphRecord infinitive;
        try {
            infinitive = lemmanizer.getInfinitive(verb);
        } catch (MorphException me){
            me.printStackTrace();
            infinitive = null;
        }
        if (infinitive != null) {
            Set<String> conjugationSet = new LinkedHashSet<>();
            conjugationSet.add(verb);
            String lemma = infinitive.getBaseWord();
            conjugationSet.add(lemma);
            if (irregularVerbMap.containsKey(lemma)) {
                VerbInfo verbInfo = irregularVerbMap.get(lemma);
                if (lemma.endsWith("o")) {
                    conjugationSet.add(lemma + "es");
                } else {
                    conjugationSet.add(lemma + "s");
                }
                for (String pastForm : verbInfo.getPastForms()) {
                    conjugationSet.add(pastForm);
                }
                for (String pastParticiple : verbInfo.getPastParticipleForms()) {
                    conjugationSet.add(pastParticiple);
                }
            } else {
                if (lemma.endsWith("o")) {
                    conjugationSet.add(lemma + "es");
                } else {
                    conjugationSet.add(lemma + "s");
                }
                if (lemma.endsWith("e")) {
                    conjugationSet.add(lemma + "d");
                } else {
                    conjugationSet.add(lemma + "ed");
                }
                if (lemma.endsWith("e")) {
                    conjugationSet.add(lemma.substring(0, lemma.length() - 1) + "ing");
                } else {
                    conjugationSet.add(lemma + "ing");
                }
            }
            return new ArrayList<>(conjugationSet);
        }

        return Arrays.asList(verb);
    }


    public static class VerbInfo {
        String infinitive;
        List<String> pastForms = new ArrayList<>(1);
        List<String> pastParticipleForms = new ArrayList<>(1);

        public VerbInfo(String infinitive) {
            this.infinitive = infinitive;
        }

        public void addPastTense(String pastTense) {
            if (!pastForms.contains(pastTense)) {
                this.pastForms.add(pastTense);
            }
        }

        public void addPastParticipleForm(String pastParticipleForm) {
            if (!pastParticipleForms.contains(pastParticipleForm)) {
                pastParticipleForms.add(pastParticipleForm);
            }
        }

        public String getInfinitive() {
            return infinitive;
        }

        public List<String> getPastForms() {
            return pastForms;
        }

        public List<String> getPastParticipleForms() {
            return pastParticipleForms;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("VerbInfo{");
            sb.append("infinitive='").append(infinitive).append('\'');
            sb.append(", pastForms=").append(pastForms);
            sb.append(", pastParticipleForms=").append(pastParticipleForms);
            sb.append('}');
            return sb.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        ILemmanizer lemmanizer = null;
        try {
            lemmanizer = SRLUtils.prepLemmanizer();
            VerbUtils verbUtils = new VerbUtils(lemmanizer);
            verbUtils.initialize();

            List<String> verbConjugations = verbUtils.getAllVerbConjugations("sits");
            System.out.println(verbConjugations);
            verbConjugations = verbUtils.getAllVerbConjugations("phosphorylates");
            System.out.println(verbConjugations);
            verbConjugations = verbUtils.getAllVerbConjugations("methylates");
            System.out.println(verbConjugations);
            verbConjugations = verbUtils.getAllVerbConjugations("gone");
            System.out.println(verbConjugations);
        } finally {
            if (lemmanizer != null) {
                lemmanizer.shutdown();
            }
        }

    }


}
