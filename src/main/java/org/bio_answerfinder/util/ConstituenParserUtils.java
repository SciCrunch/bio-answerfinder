package org.bio_answerfinder.util;

import edu.stanford.nlp.trees.Tree;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by bozyurt on 6/2/17.
 */
public class ConstituenParserUtils {

    /**
     * restores parentheses in a sentence generated from the LISP notation parse tree.
     *
     * @param sentence
     * @return
     */
    public static String normalizeSentence(String sentence) {
        sentence = sentence.replaceAll("-LRB-", "(");
        sentence = sentence.replaceAll("-RRB-", ")");
        sentence = sentence.replaceAll("-LSB-", "[");
        sentence = sentence.replaceAll("-RSB-", "]");
        sentence = sentence.replaceAll("-RCB-", "}");
        sentence = sentence.replaceAll("-LCB-", "{");
        return sentence;
    }

    public static String prepTerminal(String sentence) {
        sentence = sentence.replaceAll("\\(", "-LRB-");
        sentence = sentence.replaceAll("\\)", "-RRB-");
        sentence = sentence.replaceAll("\\[", "-LSB-");
        sentence = sentence.replaceAll("\\]", "-RSB-");
        sentence = sentence.replaceAll("\\}", "-RCB-");
        sentence = sentence.replaceAll("\\{", "-LCB-");
        return sentence;
    }

    public static String pennPrint(Tree root) {
        StringWriter sw = new StringWriter(100);
        PrintWriter pw = new PrintWriter(sw);
        display(root, 0, false, false, false, true, true, pw);
        pw.flush();
        return sw.toString().replaceAll("\\s+", " ");
    }

    public static void display(Tree tree, int indent, boolean parentLabelNull, boolean firstSibling, boolean leftSiblingPreTerminal,
                               boolean topLevel, boolean onlyLabelValue, PrintWriter pw) {
        // the condition for staying on the same line in Penn Treebank
        boolean suppressIndent = (parentLabelNull || (firstSibling && tree.isPreTerminal()) ||
                (leftSiblingPreTerminal && tree.isPreTerminal() && (tree.label() == null ||
                        !tree.label().value().startsWith("CC"))));
        if (suppressIndent) {
            pw.print(" ");
        } else {
            if (!topLevel) {
                pw.println();
            }
            for (int i = 0; i < indent; i++) {
                pw.print("  ");
            }
        }
        if (tree.isLeaf() || tree.isPreTerminal()) {
            String terminalString = toStringBuilder(tree, new StringBuilder(), onlyLabelValue).toString();
            pw.print(terminalString);
            pw.flush();
            return;
        }
        pw.print("(");
        String nodeString;
        if (onlyLabelValue) {
            String value = tree.value();
            nodeString = (value == null) ? "" : value;
        } else {
            nodeString = tree.nodeString();
        }
        pw.print(nodeString);
        boolean parentIsNull = tree.label() == null || tree.label().value() == null;
        displayChildren(tree.children(), indent + 1, parentIsNull, true, pw);
        pw.print(")");
        pw.flush();
    }

    static StringBuilder toStringBuilder(Tree tree, StringBuilder sb, boolean printOnlyLabelValue){
        if (tree.isLeaf()) {
            if (tree.label() != null) {
                if(printOnlyLabelValue) {
                    sb.append(prepTerminal(tree.label().value()));
                } else {
                    sb.append(tree.label());
                }
            }
            return sb;
        } else {
            sb.append('(');
            if (tree.label() != null) {
                if (printOnlyLabelValue) {
                    if (tree.value() != null) {
                        sb.append(prepTerminal(tree.label().value()));
                    }
                    // don't print a null, just nothing!
                } else {
                    sb.append(tree.label());
                }
            }
            Tree[] kids = tree.children();
            if (kids != null) {
                for (Tree kid : kids) {
                    sb.append(' ');
                    toStringBuilder(kid, sb, printOnlyLabelValue);
                }
            }
            return sb.append(')');
        }
    }

    static void displayChildren(Tree[] children, int indent, boolean parentLabelNull, boolean onlyLabelValue, PrintWriter pw) {
        boolean firstSibling = true;
        boolean leftSibIsPreTerm = true;  // counts as true at beginning
        for (Tree currentTree : children) {
            display(currentTree, indent, parentLabelNull, firstSibling, leftSibIsPreTerm, false, onlyLabelValue, pw);
            leftSibIsPreTerm = currentTree.isPreTerminal();
            // CC is a special case for English, but leave it in so we can exactly match PTB3 tree formatting
            if (currentTree.value() != null && currentTree.value().startsWith("CC")) {
                leftSibIsPreTerm = false;
            }
            firstSibling = false;
        }
    }


    public static String fixStanfordParseTreeNotation(String pt) {
        StringBuilder sb = new StringBuilder(pt.length() + 10);
        char[] carr = pt.toCharArray();
        boolean inParen = false;
        int level = 0;
        boolean afterLabel = false;
        int i = 0;
        while (i < pt.length()) {
            char c = carr[i];
            if (c == '(') {
                boolean eligible = (i + 1 < pt.length()) && carr[i + 1] == ')';
                if (i == 0) {
                    sb.append(c);
                    level++;
                    afterLabel = false;
                } else if (afterLabel && eligible) {
                    sb.append("-LRB-");
                    afterLabel = false;
                } else {
                    sb.append(c);
                    level++;
                    afterLabel = false;
                }
            } else if (c == ')') {
                if (afterLabel) {
                    sb.append("-RRB-");
                    afterLabel = false;
                    afterLabel = false;
                } else {
                    sb.append(c);
                    level--;
                }

            } else {
                if (c == '[') {
                    sb.append("-LSB-");
                    afterLabel = false;
                } else if (c == ']') {
                    sb.append("-RSB-");
                    afterLabel = false;
                } else if (c == '{') {
                    sb.append("-LCB-");
                    afterLabel = false;
                } else if (c == ']') {
                    sb.append("-RCB-");
                    afterLabel = false;
                } else if (Character.isSpaceChar(c)) {
                    afterLabel = isLabel(sb);
                    sb.append(c);
                } else {
                    sb.append(c);
                    afterLabel = false;
                }
            }
            i++;
        }
        return sb.toString();

    }

    static boolean isLabel(StringBuilder sb) {
        int len = sb.length();
        for (int i = len - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if (Character.isWhitespace(c) || c == ')') {
                return false;
            } else if (c == '(') {
                return true;
            }
        }
        return false;
    }

    public static String stripSpaces(String sentence) {
        StringBuilder sb = new StringBuilder(sentence.length());
        char[] chars = sentence.toCharArray();
        for (char c : chars) {
            if (Character.isWhitespace(c)) {
                continue;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
