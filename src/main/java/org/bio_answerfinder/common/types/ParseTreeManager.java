package org.bio_answerfinder.common.types;

import java.io.BufferedReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Class providing syntactic parser output traversal, LISP to internal parse
 * tree conversion, sentence reconstruction etc.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class ParseTreeManager {
    protected String parsedFile;
    protected BufferedReader in;
    static boolean useSpecialCharConversion = true;

    public ParseTreeManager(String parsedFile) {
        super();
        this.parsedFile = parsedFile;
    }

    public ParseTreeManager() {
        super();
    }


    /**
     * Given a parse tree in LISP representation from Charniak or Collins
     * syntactic parser, creates recursively an internal parse tree
     * representation.
     *
     * @param parsedSentence
     * @return the root {@link Node} of the parse tree
     * @throws ParseTreeManagerException
     */
    public static Node asParseTree(String parsedSentence)
            throws ParseTreeManagerException {
        StringBuilder buf = new StringBuilder(parsedSentence);
        int idx = parsedSentence.indexOf("(S1");
        if (idx != -1) {
            Node rootNode = new Node("S1", null, null);
            fromLisp2TreeRep(buf, new State(idx + 3), rootNode);
            return rootNode;
        } else {
            if (parsedSentence.startsWith("(S")) {
                Node rootNode = new Node("S1", null, null);
                fromLisp2TreeRep(buf, new State(0), rootNode);
                return rootNode;
            } else if (parsedSentence.startsWith("(NP")) {
                Node rootNode = new Node("S1", null, null);
                fromLisp2TreeRep(buf, new State(0), rootNode);
                return rootNode;
            } else if (parsedSentence.startsWith("( ")) {
                Node rootNode = new Node("S1", null, null);
                fromLisp2TreeRep(buf, new State(2), rootNode);
                return rootNode;
            } else if (parsedSentence.startsWith("((")) {
                Node rootNode = new Node("S1", null, null);
                fromLisp2TreeRep(buf, new State(1), rootNode);
                return rootNode;
            } else if (parsedSentence.startsWith("(ROOT")) {
                Node rootNode = new Node("S1", null, null);
                fromLisp2TreeRep(buf, new State(0), rootNode);
                return rootNode;
            }
        }
        return null;
    }

	/*
    public static NodeStatus asParseTreeRobust(String parsedSentence) {
		StringBuilder buf = new StringBuilder(parsedSentence);
		int idx = parsedSentence.indexOf("(S1");
		if (idx != -1) {
			Node rootNode = new Node("S1", null, null);
			State state = new State(idx + 3);
			fromLisp2TreeRepRecover(buf, state, rootNode);
			return new NodeStatus(rootNode, state.hasErrors);
		}
		return null;
	}
	*/

    public static String parseTree2Sentence(Node root,
                                            boolean decodeParserSpecialChars) {
        StringBuilder buf = new StringBuilder(128);
        parseTree2Sentence(root, buf, decodeParserSpecialChars);
        return buf.toString();
    }

    public static void parseTree2Sentence(Node root, StringBuilder buf,
                                          boolean decodeParserSpecialChars) {
        if (!root.hasChildren()) {
            if (decodeParserSpecialChars) {
                buf.append(charniak2SpecialChar(root.token)).append(' ');
            } else {
                buf.append(root.token).append(' ');
            }
        }
        for (Node child : root.getChildren()) {
            parseTree2Sentence(child, buf, decodeParserSpecialChars);
        }
    }

    /**
     * given the root {@link Node} of a parse tree, returns the corresponding
     * sentence, by concatenating tokens of all the leafs (terminals) of it.
     *
     * @param root
     * @return sentence, by concatenating tokens of all the leafs (terminals) of
     * the parse tree.
     */
    public static String parseTree2Sentence(Node root) {
        StringBuilder buf = new StringBuilder(128);
        parseTree2Sentence(root, buf);
        return buf.toString();
    }

    /**
     * Given a parse tree recover the sentence it belongs.
     *
     * @param root
     * @param buf
     */
    public static void parseTree2Sentence(Node root, StringBuilder buf) {
        if (!root.hasChildren()) {
            buf.append(charniak2SpecialChar(root.token)).append(' ');
        }
        for (Node child : root.getChildren()) {
            parseTree2Sentence(child, buf);
        }
    }


    /**
     * returns the portion of the sentence from the parse tree upto the given
     * ending node.
     *
     * @param endNode the ending {@link Node} object after which no leaf is included
     *                in the returned portion of the reconstructed sentence.
     * @return the reconstructed sentence portion
     */
    public static String parseTree2SentenceUpto(Node endNode) {
        StringBuilder buf = new StringBuilder(80);
        Node rootNode = null;
        Node p = endNode;
        Set<Node> endSet = new HashSet<Node>(11);
        prepChildSet(endNode, endSet);
        do {
            Node parent = p.getParent();
            if (parent != null) {
                for (Node child : parent.getChildren()) {
                    if (child == p) {
                        endSet.add(child);
                        break;
                    } else {
                        endSet.add(child);
                        prepChildSet(child, endSet);
                    }
                }
            }
            if (p.getParent() == null) {
                rootNode = p;
            }
            p = p.getParent();
        } while (p != null);

        parseTree2Sentence(rootNode, endSet, buf);
        return buf.toString();
    }

    protected static void parseTree2Sentence(Node parent, Set<Node> nodeSet,
                                             StringBuilder buf) {
        if (!parent.hasChildren() && nodeSet.contains(parent)) {
            buf.append(charniak2SpecialChar(parent.token)).append(' ');
        }
        for (Object element : parent.getChildren()) {
            Node child = (Node) element;
            parseTree2Sentence(child, nodeSet, buf);
        }
    }

    public static String parseTree2Sentence(Node startNode, Node endNode) {
        StringBuilder buf = new StringBuilder(80);
        Set<Node> startSet = new HashSet<Node>(11);
        Set<Node> endSet = new HashSet<Node>(11);
        Node rootNode = null;
        Node p = startNode;
        while (p != null) {
            startSet.add(p);
            if (p.getParent() == null) {
                rootNode = p;
            }
            p = p.getParent();
        }
        prepChildSet(startNode, startSet);

        p = endNode;
        while (p != null) {
            endSet.add(p);
            p = p.getParent();
        }
        prepChildSet(endNode, endSet);

        parseTree2Sentence(rootNode, startSet, endSet, buf);
        return buf.toString();
    }

    protected static void prepChildSet(Node parent, Set<Node> nodeSet) {
        for (Object element : parent.getChildren()) {
            Node child = (Node) element;
            nodeSet.add(child);
            prepChildSet(child, nodeSet);
        }
    }

    protected static void parseTree2Sentence(Node parent, Set<Node> startSet,
                                             Set<Node> endSet, StringBuilder buf) {
        if (!parent.hasChildren()) {
            buf.append(charniak2SpecialChar(parent.token)).append(' ');
        }
        boolean startFound = false;
        int endIdx = -1;
        int idx = 0;
        for (Object element : parent.getChildren()) {
            Node child = (Node) element;
            if (!startFound) {
                if (startSet.contains(child)) {
                    startFound = true;
                } else {
                    if (endSet.contains(child)) {
                        startFound = true;
                    } else {
                        continue;
                    }
                }
            }
            if (startFound && (endIdx < 0 || endIdx >= idx)) {
                if (endSet.contains(child)) {
                    parseTree2Sentence(child, startSet, endSet, buf);
                    endIdx = idx;
                } else {
                    parseTree2Sentence(child, startSet, endSet, buf);
                }
            }
            ++idx;
        }
    }

    /**
     * @param charniakTok
     * @return the token with Charniak parser parenthesis/bracket
     * representations converted to their original representations.
     */
    public static String charniak2SpecialChar(String charniakTok) {
        if (charniakTok == null) {
            return "<no-terminal>";
        }
        if (!useSpecialCharConversion) {
            return charniakTok;
        }
        int idx = charniakTok.indexOf('-');
        if (idx == -1) {
            return charniakTok;
        } else {
            if (charniakTok.equals("-LRB-")) {
                return "(";
            } else if (charniakTok.equals("-RRB-")) {
                return ")";
            } else if (charniakTok.equals("-RSB-")) {
                return "]";
            } else if (charniakTok.equals("-LSB-")) {
                return "[";
            } else if (charniakTok.equals("-RCB-")) {
                return "}";
            } else if (charniakTok.equals("-LCB-")) {
                return "{";
            } else {
                return charniakTok;
            }
        }
    }

    public static void dumpParseTree(Node rootNode) {
        int level = 0;
        dumpBranch(rootNode, level);
    }

    public static void dumpBranch(Node parent, int level) {
        StringBuilder buf = new StringBuilder(256);
        indent(level, buf);
        buf.append(parent.toString());
        System.out.println(buf.toString());
        for (Object element : parent.children) {
            Node child = (Node) element;
            dumpBranch(child, level + 1);
        }
    }

    public static void indent(int level, StringBuilder buf) {
        for (int i = 0; i < level; i++) {
            buf.append("   ");
        }
    }

    /**
     * Given a parse tree in LISP representation from Charniak or Collins
     * syntactic parser, and the root node for the internal representation,
     * recursively creates the internal parse tree representation.
     *
     * @param buf
     * @param state  holds current location information for the LISP representation
     * @param parent
     * @throws ParseTreeManagerException
     */
    public static void fromLisp2TreeRep(StringBuilder buf, State state,
                                        Node parent) throws ParseTreeManagerException {
        while (state.locIdx < buf.length() && buf.charAt(state.locIdx) != ')') {
            // System.out.println(buf.charAt(state.locIdx));
            if (buf.charAt(state.locIdx) == '(') {
                TokenInfo ti = extractTag(buf, state.locIdx + 1);
                assert (ti != null);
                state.locIdx = ti.cursorLoc;
                while (state.locIdx < buf.length()
                        && buf.charAt(state.locIdx) == ' ') {
                    state.locIdx++;
                }
                if (state.locIdx < buf.length()
                        && buf.charAt(state.locIdx) == '(') {
                    Node node = new Node(ti.tok, null, parent);
                    parent.addChild(node);
                    fromLisp2TreeRep(buf, state, node);
                } else {
                    TokenInfo ti2 = extractWord(buf, state.locIdx);
                    // assert (ti2 != null);
                    if (ti2 == null) {
                        String leftStr = buf.substring(state.locIdx);
                        System.err.println(buf.toString() + "\nStarting at "
                                + leftStr);
                        throw new ParseTreeManagerException();
                    }
                    Node node = new Node(ti.tok, ti2.tok, parent);
                    parent.addChild(node);
                    state.locIdx = ti2.cursorLoc + 1;
                    continue;
                }
            }
            state.locIdx++;
        }
    }

    public static void fromLisp2TreeRepRecover(StringBuilder buf, State state,
                                               Node parent) {
        while (state.locIdx < buf.length() && buf.charAt(state.locIdx) != ')') {
            // System.out.println(buf.charAt(state.locIdx));
            if (buf.charAt(state.locIdx) == '(') {
                TokenInfo ti = extractWord(buf, state.locIdx + 1);
                assert (ti != null);
                state.locIdx = ti.cursorLoc;
                while (state.locIdx < buf.length()
                        && buf.charAt(state.locIdx) == ' ') {
                    state.locIdx++;
                }
                if (state.locIdx < buf.length()
                        && buf.charAt(state.locIdx) == '(') {
                    Node node = new Node(ti.tok, null, parent);
                    parent.addChild(node);
                    fromLisp2TreeRepRecover(buf, state, node);
                } else {
                    TokenInfo ti2 = extractWord(buf, state.locIdx);
                    assert (ti2 != null);
                    if (ti2 == null) {
                        String leftStr = buf.substring(state.locIdx);
                        System.err.println(buf.toString() + "\nStarting at "
                                + leftStr);
                        state.hasErrors = true;
                        // try to recover by skipping empty () construct
                        state.locIdx++;
                        continue;
                    } else {
                        Node node = new Node(ti.tok, ti2.tok, parent);
                        parent.addChild(node);
                        state.locIdx = ti2.cursorLoc + 1;
                        continue;
                    }
                }
            }
            state.locIdx++;
        }
    }

    private static TokenInfo extractWord(StringBuilder buf, int idx) {
        int i = idx;
        StringBuilder buffer = new StringBuilder();
        // FIXME
        while (i < buf.length()
                && (buf.charAt(i) != ')')) {
            buffer.append(buf.charAt(i));
            i++;
        }
        if (buffer.length() > 0) {
            return new TokenInfo(i, buffer.toString());
        } else {
            return null;
        }
    }

    private static TokenInfo extractTag(StringBuilder buf, int idx) {
        int i = idx;
        StringBuilder buffer = new StringBuilder();
        while (i < buf.length() && buf.charAt(i) != ' ') {
            buffer.append(buf.charAt(i));
            i++;
        }
        if (buffer.length() > 0) {
            return new TokenInfo(i, buffer.toString());
        } else {
            return null;
        }
    }

    private static class State {
        int locIdx;
        boolean hasErrors = false;

        public State(int locIdx) {
            this.locIdx = locIdx;
        }
    }// ;

    private static class TokenInfo {
        int cursorLoc;
        String tok;

        public TokenInfo(int cursorLoc, String tok) {
            this.cursorLoc = cursorLoc;
            this.tok = tok;
        }
    }// ;


    public static boolean isUseSpecialCharConversion() {
        return useSpecialCharConversion;
    }

    public static void setUseSpecialCharConversion(
            boolean useSpecialCharConversion) {
        ParseTreeManager.useSpecialCharConversion = useSpecialCharConversion;
    }



}
