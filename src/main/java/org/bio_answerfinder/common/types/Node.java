package org.bio_answerfinder.common.types;


import org.bio_answerfinder.util.Assertion;

import java.util.*;


/**
 * Represents a node in a parse tree.
 *
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Node {
    /**
     * tag of non-terminal/ terminal represented by this tree node. Penn
     * Treebank II Tags are used.
     */
    protected String tag;
    /**
     * the token string (if terminal node) for the node or null
     */
    protected String token;
    /**
     * the parent of this node
     */
    protected Node parent;
    /**
     * list of the children of this parse tree node.
     */
    protected List<Node> children = new ArrayList<Node>(1);
    protected int order = -1;
    public final static boolean verbose = true;

    public Node(String tag, String token, Node parent) {
        super();
        this.tag = tag;
        this.token = token;
        this.parent = parent;
    }

    public Node(String tag, String token) {
        this(tag, token, null);
    }

    public void addChild(Node child) {
        children.add(child);
        child.parent = this;
    }

    public List<Node> getChildren() {
        return children;
    }

    public boolean hasChildren() {
        return (children != null) && !children.isEmpty();
    }

    public int numOfChildren() {
        return children.size();
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Node getChildByTag(String tag) {
        return getChildByTag(tag, 0);
    }

    public Node getChildByTag(String tag, int startIdx) {
        if (!hasChildren())
            return null;
        int i = 0;
        for (Node c : children) {
            if (startIdx > i) {
                i++;
                continue;
            }
            if (c.getTag().equals(tag))
                return c;
            i++;
        }
        return null;
    }

    public Node findDescendantByTag(String tag) {
        if (!hasChildren())
            return null;
        for (Node c : children) {
            Node d = findFirstDescendantByTag(c, tag);
            if (d != null)
                return d;
        }
        return null;
    }

    protected Node findFirstDescendantByTag(Node n, String tag) {
        if (n.getTag().equals(tag)) {
            return n;
        }
        if (!n.hasChildren())
            return null;
        for (Node c : n.getChildren()) {
            Node d = findFirstDescendantByTag(c, tag);
            if (d != null)
                return d;
        }
        return null;
    }

    public boolean hasAncestor(Node n) {
        if (n == this)
            return true;
        Node p = getParent();
        while (p != null) {
            if (p == n)
                return true;
            p = p.getParent();
        }
        return false;
    }

    public boolean isParentOf(Node posChild) {
        if (!hasChildren())
            return false;
        for (Node c : children) {
            if (c == posChild || c.isParentOf(posChild)) {
                return true;
            }
        }
        return false;
    }

    public int getSiblingIndex() {
        if (parent == null)
            return -1;
        List<Node> siblings = parent.getChildren();
        int i = 0;
        for (Node sibling : siblings) {
            if (sibling == this)
                return i;
            ++i;
        }
        return -1;
    }

    public Node getChildAt(int idx) {
        if (children.isEmpty() || idx < 0 || idx >= children.size()) {
            return null;
        }
        return children.get(idx);
    }

    public Node getFirstChild() {
        if (!children.isEmpty()) {
            return children.get(0);
        }
        return null;
    }

    public Node getLastChild() {
        if (!children.isEmpty()) {
            return children.get(children.size() - 1);
        }
        return null;
    }

    public Node getLastChild(IPredicate<Node> predicate) {
        if (!children.isEmpty()) {
            ListIterator<Node> iter = children.listIterator(children.size());
            while (iter.hasPrevious()) {
                Node node = iter.previous();
                if (predicate.satisfied(node)) {
                    return node;
                }
            }
        }
        return null;
    }

    public static List<Node> getChildren2RightBFS(Node parent, Path path) {
        if (parent.children.isEmpty())
            return new ArrayList<Node>(0);
        List<Node> list = new ArrayList<Node>(parent.children.size());
        int curOrder = parent.getFirstChild().getOrder();
        Node pivotNode = null;
        for (PathNode p : path.getPath()) {
            if (p.getOrder() == curOrder) {
                pivotNode = p.getNode();
                break;
            }
        }

        // parent and all its siblings to the right
        List<Node> siblings = new ArrayList<Node>(10);
        if (!parent.hasParent()) {
            siblings.add(parent);
        } else {
            boolean found = false;
            for (Node n : parent.getParent().getChildren()) {
                if (n == parent) {
                    found = true;
                }
                if (found) {
                    siblings.add(n);
                }
            }
        }

        boolean found = false;
        for (Node ps : siblings) {
            for (Node n : ps.children) {
                if (n == pivotNode) {
                    found = true;
                    continue;
                }
                if (found) {
                    list.add(n);
                }
            }
        }
        return list;
    }

    public static List<Node> getChildren2LeftBFS(Node parent, Path path) {
        if (parent.children.isEmpty())
            return null;
        List<Node> list = new ArrayList<Node>(parent.children.size());

        int curOrder = parent.getFirstChild().getOrder();
        int pathTopOrder = path.getFirst().getOrder();
        if (curOrder == (pathTopOrder + 1) && path.size() == 1) {
            int psi = parent.getSiblingIndex();
            int pti = path.getFirst().getNode().getSiblingIndex();
            if (pti > psi) {
                return new ArrayList<Node>(parent.getChildren());
            } else {
                return new ArrayList<Node>(0);
            }
        }

        PathNode pivotNode = null;
        for (PathNode p : path.getPath()) {
            if (p.getOrder() == curOrder) {
                pivotNode = p;
                break;
            }
        }

        if (pivotNode == null) {
            // climb up parent till pathTopOrder
            Node next = parent;
            Node p = parent.getParent();
            while (p != null && p.getOrder() != pathTopOrder) {
                next = p;
                p = p.getParent();
            }

            // check if p is to the left of the top path node
            int psi = p.getSiblingIndex();
            int pti = path.getFirst().getNode().getSiblingIndex();
            if (pti > psi) {
                return new ArrayList<Node>(parent.getChildren());
            } else {
                if (pti == psi) {
                    PathNode pn = path.getFirst();
                    if (pn.isEligibleLeftChild(next)) {
                        return new ArrayList<Node>(parent.getChildren());
                    }
                }
                // no pivot node means there is no left child
                return new ArrayList<Node>(0);
            }
        }

        for (Node ps : parent.getChildren()) {
            if (ps == pivotNode.getNode()) {
                if (pivotNode.hasEligibleLeftChildren()) {
                    list.add(ps);
                }
                break;
            }
            list.add(ps);
        }
        return list;
    }

    public List<Node> getChildren2Left(Node pivotNode) {
        if (children.isEmpty())
            return null;
        List<Node> list = new ArrayList<Node>(children.size());
        for (Node n : children) {
            if (n != pivotNode) {
                list.add(n);
            } else {
                break;
            }
        }
        return list;
    }

    public List<Node> getChildren2Right(Node pivotNode) {
        if (children.isEmpty())
            return null;
        List<Node> list = new ArrayList<Node>(children.size());
        boolean found = false;
        for (Node n : children) {
            if (n == pivotNode) {
                found = true;
            } else if (found) {
                list.add(n);
            }
        }
        if (!found) {
            list.addAll(children);
        }
        return list;
    }

    public String getTag() {
        return tag;
    }

    public String getToken() {
        return token;
    }

    public Node getParent() {
        return parent;
    }

    public Node getRoot() {
        if (getParent() == null)
            return this;
        Node p = getParent();
        while (p != null) {
            if (p.getParent() == null)
                break;
            p = p.getParent();
        }
        return p;
    }

    public int getOrder() {
        if (order < 0) {
            int i = 0;
            Node p = this.parent;
            while (p != null) {
                i++;
                p = p.parent;
            }
            order = i;
        }
        return order;
    }

    public String branch2String() {
        StringBuilder sb = new StringBuilder();
        if (!hasChildren()) {
            sb.append(token);
        } else {
            branch2String(this, sb);
        }
        return sb.toString().trim();
    }

    protected void branch2String(Node p, StringBuilder sb) {
        if (!p.hasChildren()) {
            sb.append(p.token).append(' ');
        } else {
            for (Node c : p.getChildren()) {
                branch2String(c, sb);
            }
        }
    }

    public String toLispString() {
        StringBuilder sb = new StringBuilder();
        Counter c = new Counter();
        toLispString(this, sb);
        return sb.toString();
    }

    void toLispString(Node n, StringBuilder sb) {
        if (!n.hasChildren()) {
            sb.append(" (").append(n.getTag()).append(' ').append(n.getToken())
                    .append(')');
        } else {
            sb.append(" (").append(n.getTag()).append(' ');
            for (Node child : n.getChildren()) {
                toLispString(child, sb);
            }
            sb.append(')');
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Node::[");
        buf.append("tag=").append(tag);
        if (token != null) {
            buf.append(",token=").append(token);
        }
        if (verbose) {
            if (hasChildren()) {
                buf.append(" ( ");
                buf.append(ParseTreeManager.parseTree2Sentence(this).trim());
                buf.append(" )");
            }
        }
        buf.append(']');
        return buf.toString();
    }

    public static Node cloneTree(Node root) {
        Node newRoot = new Node(root.getTag(), root.getToken(), null);
        for (Node child : root.getChildren()) {
            cloneBranch(child, newRoot);
        }
        return newRoot;
    }

    protected static void cloneBranch(Node parent, Node newGrandParent) {
        Node newParent = new Node(parent.getTag(), parent.getToken(),
                newGrandParent);
        newGrandParent.addChild(newParent);
        for (Node child : parent.getChildren()) {
            cloneBranch(child, newParent);
        }
    }

    /**
     * returns true if the given <code>node</code> is before (to the left) of
     * the reference node <code>refNode</code>. Assumes both nodes belong to the
     * same parse tree.
     *
     * @param node
     * @param refNode
     * @return true if the given <code>node</code> is before (to the left) of
     * the reference node <code>refNode</code>.
     */
    public static boolean isNodeBefore(Node node, Node refNode) {
        Assertion.assertTrue(node != refNode);
        // first find the first common parent
        Map<Node, Node> path = new HashMap<Node, Node>(7);

        path.put(node, null);
        Node p = node.getParent();
        Node next = node;
        while (p != null) {
            path.put(p, next);
            next = p;
            p = p.getParent();
        }
        Node commonParent = null;
        p = refNode.getParent();
        next = refNode;
        while (p != null) {
            if (path.containsKey(p)) {
                commonParent = p;
                Node nextNode = path.get(p);
                List<Node> leftChildren = p.getChildren2Left(next);
                if (leftChildren.contains(nextNode)) {
                    return true;
                }
                break;
            }
            next = p;
            p = p.getParent();
        }
        Assertion.assertNotNull(commonParent);
        return false;
    }

    public static String getUniqueKey(Node n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getTag());
        if (n.getToken() != null && n.getToken().length() > 0)
            sb.append(' ').append(n.getToken());
        if (n.getParent() == null) {
            sb.append(" null");
        } else {
            Node p = n.getParent();
            sb.append(' ').append(p.getTag());
            if (p.getToken() != null && p.getToken().length() > 0)
                sb.append(' ').append(p.getToken());
        }
        if (n.hasChildren()) {
            for (Node child : n.getChildren()) {
                sb.append(' ').append(child.getTag());
                if (child.getToken() != null && child.getToken().length() > 0)
                    sb.append(' ').append(child.getToken());
            }
        }
        return sb.toString();
    }

    public List<Node> getAllLeafNodesBelow(IPredicate<Node> predicate) {
        List<Node> leafs = new ArrayList<Node>(10);
        collectLeafNodes(this, leafs, predicate);
        return leafs;
    }

    public List<Node> getAllLeafNodesBelow() {
        List<Node> leafs = new ArrayList<Node>(10);
        collectLeafNodes(this, leafs);
        return leafs;
    }

    protected void collectLeafNodes(Node p, List<Node> leafs,
                                    IPredicate<Node> predicate) {
        if (!p.hasChildren() && predicate.satisfied(p)) {
            leafs.add(p);
        } else {
            for (Node c : p.getChildren()) {
                if (predicate.satisfied(c)) {
                    collectLeafNodes(c, leafs, predicate);
                }
            }
        }
    }

    protected void collectLeafNodes(Node p, List<Node> leafs) {
        if (!p.hasChildren()) {
            leafs.add(p);
        } else {
            for (Node c : p.getChildren()) {
                collectLeafNodes(c, leafs);
            }
        }
    }

    public boolean contentEquals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Node other = (Node) obj;
        if (getOrder() != other.getOrder())
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    public List<Node> getSpanningLeafNodes(Node endNode) {
        // first find a common ancestor
        List<Node> startNodePath = getPath2Root();
        List<Node> endNodePath = endNode.getPath2Root();
        Node cn = findCommon(startNodePath, endNodePath);
        if (cn == null) {
            throw new RuntimeException("No common ancestor has been found!");
        }
        List<Node> leafNodes = cn.getAllLeafNodesBelow();
        Node startLeafNode = getLeftMostLeaf();
        Node endLeafNode = endNode.getRightMostLeaf();
        List<Node> spanList = new ArrayList<Node>(leafNodes.size());
        boolean inSpan = false;
        for (Node ln : leafNodes) {
            if (!inSpan) {
                if (ln == startLeafNode) {
                    spanList.add(ln);
                    if (ln != endLeafNode)
                        inSpan = true;
                }
            } else {
                spanList.add(ln);
                if (ln == endLeafNode)
                    break;
            }
        }
        return spanList;
    }

    protected Node findCommon(List<Node> path1, List<Node> path2) {
        for (Node p1 : path1) {
            for (Node p2 : path2) {
                if (p1 == p2)
                    return p1;
            }
        }
        return null;
    }

    public List<Node> getPath2Root() {
        List<Node> path = new ArrayList<Node>(10);
        Node n = this;
        while (n != null) {
            path.add(n);
            n = n.getParent();
        }
        return path;
    }

    public Node getLeftMostLeaf() {
        Node n = this;
        while (n.hasChildren()) {
            n = n.getFirstChild();
        }
        return n;
    }

    public Node getRightMostLeaf() {
        Node n = this;
        while (n.hasChildren()) {
            n = n.getLastChild();
        }
        return n;
    }

    public int getNumberOfNodes() {
        Counter c = new Counter();
        countNodes(this, c);
        return c.i;
    }

    protected void countNodes(Node p, Counter counter) {
        if (p != null) {
            counter.i++;
            if (p.hasChildren()) {
                for (Node c : p.getChildren()) {
                    countNodes(c, counter);
                }
            }
        }
    }

    class Counter {
        int i = 0;
    }

    public void removeChild(Node node) {
        for (Iterator<Node> it = children.iterator(); it.hasNext(); ) {
            Node n = it.next();
            if (n == node) {
                it.remove();
                break;
            }
        }
    }

    /**
     * For WSJ Penn Tree Bank remove any suffices such as -SBJ-1 in NP-SBJ-1,
     * -TPC-1 in S-TPC-1
     */
    public void normalizeTag() {
        // 02/10/2015
        if (this.tag.equals("-LRB-") || this.tag.equals("-RRB-")) {
            return;
        }
        int idx = this.tag.indexOf('-');
        if (idx != -1) {
            tag = tag.substring(0, idx);
        }
    }

    public static Node findCommonParent(Node n1, Node n2) {
        Set<Node> pathSet = new HashSet<Node>(17);
        Node p = n1;
        while (p != null && p.getParent() != null) {
            pathSet.add(p);
            p = p.getParent();
        }
        p = n2.getParent();
        while (p != null) {
            if (pathSet.contains(p)) {
                return p;
            }
            p = p.getParent();
        }
        return null;
    }
}
