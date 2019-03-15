package org.bio_answerfinder.services;


import org.bio_answerfinder.bioasq.QuestionRecord;
import org.bio_answerfinder.bioasq.QuestionRecordReader;
import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by bozyurt on 8/17/17.
 */
public class IRUtils {
    static String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";
    List<QuestionRecord> questionRecords;
    Map<String, QuestionRecord> qrMap = new HashMap<>();

    public IRUtils() throws IOException {
        this.questionRecords = QuestionRecordReader.load(bioASQJsonFile);
        for (QuestionRecord qr : questionRecords) {
            qrMap.put(qr.getId(), qr);
        }
    }

    public List<QuestionRecord> getQuestionRecords() {
        return questionRecords;
    }

    public Map<String, QuestionRecord> getQrMap() {
        return qrMap;
    }

    public void showPrecisionAtK4Question(QuestionRecord qr, List<PubMedDoc> pubMedDocs, int k) {
        Set<String> gsPMIDSet = new HashSet<>();
        for (String docURL : qr.getAnswer().getDocuments()) {
            String pmid = QuestionRecord.Answer.getPMID(docURL);
            gsPMIDSet.add(pmid);
        }
        int count = 0;
        int n = Math.min(k, pubMedDocs.size());
        for (int i = 0; i < n; i++) {
            PubMedDoc pmd = pubMedDocs.get(i);
            if (gsPMIDSet.contains(pmd.getPmid())) {
                count++;
            }
        }
        double precisionatK = count / (double) k;
        System.out.println("P@" + k + ": " + precisionatK);
    }

    public void saveTopNResults(QuestionRecord qr, List<PubMedDoc> pubMedDocs, int N) throws Exception {
        ElasticSearchService ess = new ElasticSearchService();
        int n = Math.min(N, pubMedDocs.size());
        StringBuilder sb = new StringBuilder(50000);
        File outFile = new File("/tmp/" + qr.getId() + "_result.txt");
        sb.append(qr.getId()).append("\n");
        sb.append("Q:").append(qr.getQuestion()).append("\n");
        for (int i = 0; i < n; i++) {
            PubMedDoc pmd = pubMedDocs.get(i);
            sb.append("---------------------------------------\n");
            if (pmd.getDocumentAbstract() == null) {
                pmd = ess.retrieveDocumentbyPMID(pmd.getPmid());
            }
            if (pmd.getDocumentAbstract() != null) {
                if (pmd.getTitle() != null) {
                    sb.append(pmd.getTitle()).append("\n");
                }
                sb.append(pmd.getDocumentAbstract()).append("\n");
                if (pmd.getJournal() != null) {
                    sb.append(pmd.getJournal()).append(" ");
                    if (pmd.getYear() != null) {
                        sb.append(pmd.getYear());
                    }
                    sb.append("\n");
                }
            }
        }
        FileUtils.saveText(sb.toString(), outFile.getAbsolutePath(), CharSetEncoding.UTF8);
        System.out.println("saved " + outFile);
    }
}
