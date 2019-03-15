package org.bio_answerfinder.common;

/**
 * A generic holder class for two type instances forming a pair.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Pair<X, Y> {
    private X first;
    private Y second;

    public Pair(X first, Y second) {
        super();
        this.first = first;
        this.second = second;
    }

    public X getFirst() {
        return first;
    }

    public Y getSecond() {
        return second;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pair::[");
        sb.append("first=").append(first);
        sb.append("\nsecond=").append(second);
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!first.equals(pair.first)) return false;
        return second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }
}
