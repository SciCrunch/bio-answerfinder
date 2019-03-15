package org.bio_answerfinder.common.dependency;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bozyurt on 3/28/17.
 */
public class DependencyNode {
    DependencyNode parent;
    String id;
    List<DependencyNode> children = new ArrayList<DependencyNode>(1);
    String label;
    NLPToken payload;

    public DependencyNode(String id, String label, NLPToken payload, DependencyNode parent) {
        this.id = id;
        this.label = label;
        this.payload = payload;
        this.parent = parent;
    }

    public void addChild(DependencyNode child) {
        children.add(child);
    }

    public static boolean hasCycle(DependencyNode node, DependencyNode parentNode) {
        if (parentNode == null) {
            return false;
        }
        if (parentNode.getId().equals(node.getId()) && parentNode.getLabel().equals(node.getLabel()) &&
                parentNode.getPayload().getToken().equals(node.getPayload().getToken())) {
            return true;
        }
        for (DependencyNode child : parentNode.getChildren()) {
            if (child.getId().equals(node.getId()) && child.getLabel().equals(node.getLabel()) &&
                    child.getPayload().getToken().equals(node.getPayload().getToken())) {
                return true;
            }
        }
        return hasCycle(node, parentNode.getParent());


    }

    public DependencyNode getParent() {
        return parent;
    }

    public List<DependencyNode> getChildren() {
        return children;
    }

    public String getLabel() {
        return label;
    }

    public NLPToken getPayload() {
        return payload;
    }


    public String getToken() {
        return payload.getToken();
    }

    public String getLemma() {
        return payload.getLemma();
    }

    public String getPosTag() {
        return payload.getPosTag();
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        int level = 1;
        final StringBuilder sb = buildTreeRep(level);
        return sb.toString();
    }

    private StringBuilder buildTreeRep(int level) {
        final StringBuilder sb = new StringBuilder("DependencyNode{");
        sb.append("id=").append(id);
        sb.append(", label='").append(label).append('\'');
        sb.append(", payload=").append(payload);
        if (!children.isEmpty()) {
            sb.append(", children=[\n");

            for (Iterator<DependencyNode> iter = children.iterator(); iter.hasNext(); ) {
                DependencyNode child = iter.next();
                for (int i = 0; i < level; i++) {
                    sb.append('\t');
                }
                sb.append(child.buildTreeRep(level + 1));
                if (iter.hasNext()) {
                    sb.append("\n");
                } else {
                    sb.append(']');
                }
            }
        }
        sb.append('}');
        return sb;
    }
}
