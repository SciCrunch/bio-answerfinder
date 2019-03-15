package org.bio_answerfinder.bioasq;

import org.bio_answerfinder.common.CharSetEncoding;
import org.bio_answerfinder.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/2/17.
 */
public class QuestionRecordReader {

    public static List<QuestionRecord> load(String bioASQJsonFile) throws IOException {
        String content = FileUtils.loadAsString(bioASQJsonFile, CharSetEncoding.UTF8);
        JSONObject json = new JSONObject(content);
        JSONArray questions = json.getJSONArray("questions");
        List<QuestionRecord> qrList = new ArrayList<QuestionRecord>();
        for (int i = 0; i < questions.length(); i++) {
            JSONObject js = questions.getJSONObject(i);
            qrList.add(QuestionRecord.fromJSON(js));
        }
        return qrList;
    }



    public static void main(String[] args) throws Exception {
        String bioASQJsonFile = "${home}/data/bioasq/5b/BioASQ-training5b/BioASQ-trainingDataset5b.json";

        List<QuestionRecord> questionRecords = load(bioASQJsonFile);
        System.out.println("# of questions:" + questionRecords.size());

        int qCount = 0;
        for(QuestionRecord qr : questionRecords) {
            String question = qr.getQuestion().toLowerCase();
            if (question.indexOf("alzheim") != -1 || question.indexOf("schizo") != -1 || question.indexOf("ssri")  != -1
                    || question.indexOf("ocd") != -1 || question.indexOf("dementia") != -1 || question.indexOf("parkinson") != -1) {
                System.out.println(qr.getQuestion());
                FileUtils.appendLine("/tmp/mental_health_questions.txt", qr.getQuestion());
                qCount++;
            } else {
                FileUtils.appendLine("/tmp/biomedical_questions.txt", qr.getQuestion());
            }
        }
        System.out.println("# of mental disorder questions:" + qCount);

    }
}
