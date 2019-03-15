package org.bio_answerfinder.common.dependency;

/**
 * Created by bozyurt on 3/28/17.
 */
public class NLPToken {
    String token;
    String lemma;
    String posTag;
    String ne;

    public NLPToken(String token) {
        this.token = token;
        this.lemma = token.toLowerCase();
    }
    public NLPToken(String token, String posTag) {
        this(token);
        this.posTag = posTag;
    }

    public String getToken() {
        return token;
    }

    public String getLemma() {
        return lemma;
    }

    public String getPosTag() {
        return posTag;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    public String getNe() {
        return ne;
    }

    public void setNe(String ne) {
        this.ne = ne;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NLPToken{");
        sb.append("token='").append(token).append('\'');
        sb.append(", lemma='").append(lemma).append('\'');
        sb.append(", posTag='").append(posTag).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
