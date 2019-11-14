package org.bio_answerfinder.bioasq;

import org.bio_answerfinder.util.Assertion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bozyurt on 6/2/17.
 */
public class QuestionRecord {
    String id;
    String question;
    String type;
    Answer answer;

    public QuestionRecord() {
    }

    public String getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getType() {
        return type;
    }

    public Answer getAnswer() {
        return answer;
    }

    public static QuestionRecord fromJSON(JSONObject json) {
        QuestionRecord qr = new QuestionRecord();
        qr.id = json.getString("id");
        qr.question = json.getString("body");
        qr.type = json.getString("type");

        qr.answer = Answer.fromJSON(json);

        return qr;
    }

    public static class Answer {
        List<String> documents = new ArrayList<String>(10);
        List<String> concepts = new ArrayList<String>(10);
        List<Snippet> snippets = new ArrayList<Snippet>(10);
        List<Triple> triples = new ArrayList<Triple>(2);
        List<AnswerItem> exactAnswer = new ArrayList<AnswerItem>(1);
        List<String> idealAnswer = new ArrayList<String>(1);

        public Answer() {
        }


        public static String getPMID(String docURL) {
            int idx = docURL.lastIndexOf('/');
            Assertion.assertTrue(idx != -1);
            return docURL.substring(idx + 1);
        }

        public List<String> getDocuments() {
            return documents;
        }

        public List<String> getConcepts() {
            return concepts;
        }

        public List<Snippet> getSnippets() {
            return snippets;
        }

        public List<Triple> getTriples() {
            return triples;
        }

        public List<AnswerItem> getExactAnswer() {
            return exactAnswer;
        }

        public List<String> getIdealAnswer() {
            return idealAnswer;
        }

        public static Answer fromJSON(JSONObject json) {
            Answer answer = new Answer();
            if (json.has("documents")) {
                JSONArray jsonArray = json.getJSONArray("documents");
                for (int i = 0; i < jsonArray.length(); i++) {
                    answer.documents.add(jsonArray.getString(i));
                }
            }
            if (json.has("concepts")) {
                JSONArray jsonArray = json.getJSONArray("concepts");
                for (int i = 0; i < jsonArray.length(); i++) {
                    answer.concepts.add(jsonArray.getString(i));
                }
            }
            if (json.has("triples")) {
                JSONArray jsonArray = json.getJSONArray("triples");
                for (int i = 0; i < jsonArray.length(); i++) {
                    answer.triples.add(Triple.fromJSON(jsonArray.getJSONObject(i)));
                }
            }
            if (json.has("snippets")) {
                JSONArray jsonArray = json.getJSONArray("snippets");
                for (int i = 0; i < jsonArray.length(); i++) {
                    answer.snippets.add(Snippet.fromJSON(jsonArray.getJSONObject(i)));
                }
            }
            if (json.has("exact_answer")) {
                Object o = json.get("exact_answer");
                if (o instanceof String) {
                    AnswerItem ai = new AnswerItem((String) o);
                    answer.exactAnswer.add(ai);
                } else if (o instanceof JSONArray) {
                    JSONArray jsArr = (JSONArray) o;
                    for (int i = 0; i < jsArr.length(); i++) {
                        Object item = jsArr.get(i);
                        if (item instanceof String) {
                            answer.exactAnswer.add(new AnswerItem((String) item));
                        } else {
                            answer.exactAnswer.add(AnswerItem.fromJSON((JSONArray) item));
                        }
                    }
                }
            }
            if (json.has("ideal_answer")) {
                Object o = json.get("ideal_answer");
                if (o instanceof String) {
                    answer.idealAnswer.add((String) o);
                } else {
                    JSONArray jsonArray = (JSONArray) o;
                    for (int i = 0; i < jsonArray.length(); i++) {
                        answer.idealAnswer.add(jsonArray.getString(i));
                    }
                }
            }

            return answer;
        }
    }

    public static class Snippet {
        String documentURL;
        String text;
        String beginSection;
        String endSection;
        int offsetInBeginSection;
        int offsetInEndSection;

        public Snippet() {
        }

        public String getDocumentURL() {
            return documentURL;
        }

        public String getText() {
            return text;
        }

        public String getBeginSection() {
            return beginSection;
        }

        public String getEndSection() {
            return endSection;
        }

        public int getOffsetInBeginSection() {
            return offsetInBeginSection;
        }

        public int getOffsetInEndSection() {
            return offsetInEndSection;
        }

        public static Snippet fromJSON(JSONObject json) {
            Snippet snippet = new Snippet();
            snippet.text = json.getString("text");
            snippet.documentURL = json.getString("document");
            snippet.beginSection = json.getString("beginSection");
            snippet.endSection = json.getString("endSection");
            snippet.offsetInBeginSection = json.getInt("offsetInBeginSection");
            snippet.offsetInEndSection = json.getInt("offsetInEndSection");
            return snippet;
        }
    }

    public static class Triple {
        String predicate;
        String subject;
        String obj;

        public Triple() {
        }

        public String getPredicate() {
            return predicate;
        }

        public String getSubject() {
            return subject;
        }

        public String getObj() {
            return obj;
        }

        public static Triple fromJSON(JSONObject json) {
            Triple triple = new Triple();
            triple.predicate = json.getString("p");
            triple.subject = json.getString("s");
            triple.obj = json.getString("o");
            return triple;
        }
    }

    public static class AnswerItem {
        List<String> answers = new ArrayList<String>(1);

        public AnswerItem() {
        }

        public AnswerItem(String answer) {
            answers.add(answer);
        }

        public List<String> getAnswers() {
            return answers;
        }

        public static AnswerItem fromJSON(JSONArray jsonArr) {
            AnswerItem ai = new AnswerItem();
            for (int i = 0; i < jsonArr.length(); i++) {
                ai.answers.add(jsonArr.getString(i));
            }
            return ai;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("AnswerItem{");
            sb.append("answers=").append(answers);
            sb.append('}');
            return sb.toString();
        }
    }
}
