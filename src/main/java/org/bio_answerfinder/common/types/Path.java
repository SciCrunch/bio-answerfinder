package org.bio_answerfinder.common.types;


import org.bio_answerfinder.util.Assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author I. Burak Ozyurt
 * @version $Id$
 */
public class Path {
    protected boolean prepared = false;
    protected List<PathNode> pathList = new ArrayList<PathNode>(5);

    public Path() {
    }

    public void add(Node node) {
        PathNode pn = new PathNode(node);
        if (!pathList.isEmpty()) {
            PathNode prevPN = pathList.get(pathList.size() - 1);
            Assertion.assertTrue(prevPN.getNode().getParent() == pn.getNode());
            pn.setLastCompletedChild(prevPN.getNode());
        }
        pathList.add(pn);
    }

    public void prepare() {
        if (prepared)
            throw new RuntimeException("Already prepared!");
        pathList.remove(pathList.size() - 1);
        Collections.reverse(pathList);
        prepared = true;
    }

    public PathNode getFirst() {
        return pathList.get(0);
    }

    public PathNode getLast() {
        return pathList.get(pathList.size() - 1);
    }

    public int size() {
        return pathList.size();
    }

    public List<PathNode> getPath() {
        return pathList;
    }

    public void append(Path other) {
        if (!prepared || !other.prepared)
            throw new RuntimeException("needs to be prepared before append!");
        PathNode ln = getLast();
        PathNode fn = other.getFirst();
        Assertion.assertTrue(fn.getNode().getParent() == ln.getNode());
        ln.setLastCompletedChild(ln.getNode());
        pathList.addAll(other.getPath());
    }

    public boolean foundInPath(Node node) {
        for (PathNode pn : pathList)
            if (node == pn.getNode())
                return true;
        return false;
    }

    /**
     * Constructs a <code>String</code> with all attributes in name = value
     * format.
     *
     * @return a <code>String</code> representation of this object.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Path::[");
        sb.append(",pathList=").append(this.pathList);
        sb.append("]");
        return sb.toString();
    }

}
