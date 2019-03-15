package org.bio_answerfinder.common.types;

/**
 * Created by bozyurt on 2/6/16.
 */
public class TokenInfo {
    protected String token;
    /**
     * index of the first letter of the token in a string
     */
    protected int idx;

    public TokenInfo(String token, int idx) {
        super();
        this.token = token;
        this.idx = idx;
    }

    public String getToken() {
        return token;
    }

    public int getIdx() {
        return idx;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TokenInfo other = (TokenInfo) obj;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TokenInfo [");
        if (token != null) {
            builder.append("token=");
            builder.append(token);
            builder.append(", ");
        }
        builder.append("idx=");
        builder.append(idx);
        builder.append("]");
        return builder.toString();
    }

}
