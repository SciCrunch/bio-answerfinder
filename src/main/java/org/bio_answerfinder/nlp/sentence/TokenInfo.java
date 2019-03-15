package org.bio_answerfinder.nlp.sentence;

import java.io.Serializable;

/**
 * A class representing a token including its start and end positions from a
 * flat sentence as determined by {@link SentenceLexer}.
 * 
 * @author I. Burak Ozyurt
 * @version $Id$
 * 
 */
public class TokenInfo implements Serializable {
	String tokValue;
	int start;
	int end;

	public TokenInfo(String value, int start, int end) {
		tokValue = value;
		this.start = start;
		this.end = end;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("TokenInfo::[");
		buf.append("tv=").append(tokValue);
		buf.append(",start=").append(start);
		buf.append(",end=").append(end);
		buf.append(']');
		return buf.toString();
	}

	public String getTokValue() {
		return tokValue;
	}

	public String getCharniakTokValue() {
		if (tokValue.equals("-LRB-")) {
			return "(";
		}
		if (tokValue.equals("-RRB-")) {
			return ")";
		}
		if (tokValue.equals("-LSB-")) {
			return "[";
		}
		if (tokValue.equals("-RSB-")) {
			return "]";
		}
		return tokValue;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}
}