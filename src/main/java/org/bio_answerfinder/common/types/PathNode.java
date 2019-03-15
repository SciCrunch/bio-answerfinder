package org.bio_answerfinder.common.types;

/**
 * @author I. Burak Ozyurt
 * @version $Id$
 * @see Node
 * @see Path
 */

public class PathNode {
    protected Node node;
    protected boolean completed = false;
    protected Node lastCompletedChild;
    protected int direction = LEFT;
    public final static int LEFT = 1;
    public final static int RIGHT = 1;

    public PathNode(Node node) {
        this.node = node;
    }

    public Node getLastCompletedChild() {
        return lastCompletedChild;
    }

    public void setLastCompletedChild(Node lastCompletedChild) {
        this.lastCompletedChild = lastCompletedChild;
    }

    public boolean isCompleted() {
        if (!node.hasChildren())
            return true;
        if (lastCompletedChild == null && node.hasChildren())
            return false;
        if (lastCompletedChild != null) {
            if (direction == LEFT) {
                int i = 0;
                for (Node c : node.getChildren()) {
                    if (c == lastCompletedChild)
                        break;
                    i++;
                }
                return i > 0;
            }
        }
        return false;
    }

    public boolean hasEligibleLeftChildren() {
        if (!node.hasChildren())
            return false;
        if (lastCompletedChild == null && node.hasChildren()) {
            return node.children.size() > 1;
        }
        int i = 0;
        for (Node c : node.getChildren()) {
            if (c == lastCompletedChild)
                break;
            i++;
        }
        return i > 0;
    }

    public boolean isEligibleLeftChild(Node n) {
        if (!node.hasChildren())
            return false;
        if (lastCompletedChild == null && node.hasChildren()) {
            return node.children.size() > 1;
        }
        for (Node c : node.getChildren()) {
            if (c == lastCompletedChild)
                break;
            if (c == n)
                return true;
        }
        return false;
    }

    public int getOrder() {
        return node.getOrder();
    }

    public Node getNode() {
        return node;
    }

    /**
     * Constructs a <code>String</code> with all attributes in name = value
     * format.
     *
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PathNode::[");
        sb.append(",node=").append(this.node);
        sb.append(",completed=").append(this.completed);
        sb.append(",lastCompletedChild=").append(this.lastCompletedChild);
        sb.append("]");
        return sb.toString();
    }

}
