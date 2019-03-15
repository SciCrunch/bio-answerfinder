package org.bio_answerfinder.common.dependency;


import org.bio_answerfinder.util.StopWords;

import java.util.*;

/**
 * Created by bozyurt on 5/22/17.
 */
public class DTUtils {
    public static final Set<String> verbTagSet = new HashSet<String>(Arrays.asList("VB", "VBP", "VBZ", "VBD", "VBN", "VBG"));
    public static final Set<String> nounTagSet = new HashSet<String>(Arrays.asList("NN", "NNS", "NNP"));

    public static DependencyNode findNodeByToken(DependencyNode parent, String tokenLC) {
        List<DependencyNode> list = new ArrayList<DependencyNode>(1);
        collectNodesByToken(parent, tokenLC, list);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    static void collectNodesByToken(DependencyNode parent, String tokenLC, List<DependencyNode> list) {
        if (parent.getToken().equalsIgnoreCase(tokenLC)) {
            list.add(parent);
        }
        for (DependencyNode child : parent.getChildren()) {
            collectNodesByToken(child, tokenLC, list);
        }
    }


    public static List<DependencyNode> findNodesMatching(DependencyNode pivot, String depLabel) {
        return findNodesMatching(pivot, new HashSet<String>(Arrays.asList(depLabel)));

    }

    public static List<DependencyNode> findNodesMatching(DependencyNode pivot, Set<String> depLabelSet) {
        List<DependencyNode> list = new ArrayList<DependencyNode>(2);

        Iterator<DependencyNode> iterator = null;
        if (pivot.getParent() != null) {
            iterator = pivot.getParent().getChildren().iterator();
        } else {
            iterator = pivot.getChildren().iterator();
        }
        while (iterator.hasNext()) {
            DependencyNode node = iterator.next();
            if (node != pivot) {
                collectNodesByDepLabel(node, depLabelSet, list);
            }
        }
        return list;

    }

    public static List<DependencyNode> findNounNodes(DependencyNode tree) {
        List<DependencyNode> verbList = new ArrayList<DependencyNode>(1);
        collectNodesByPosTag(tree, nounTagSet, verbList);
        return verbList;
    }

    public static List<DependencyNode> findVerbNodes(DependencyNode tree) {
        List<DependencyNode> verbList = new ArrayList<DependencyNode>(1);
        collectNodesByPosTag(tree, verbTagSet, verbList);

        return verbList;
    }

    public static List<DependencyNode> findExcludedChildren(DependencyNode parent, Set<DependencyNode> seenSet) {
        List<DependencyNode> excludedList = new ArrayList<DependencyNode>(2);
        for (DependencyNode child : parent.getChildren()) {
            if (!seenSet.contains(child)) {
                excludedList.add(child);
            }
        }
        return excludedList;
    }

    static void collectNodesByPosTag(DependencyNode parent, Set<String> posTagSet, List<DependencyNode> list) {
        if (posTagSet.contains(parent.getPayload().getPosTag())) {
            list.add(parent);
        }
        for (DependencyNode child : parent.getChildren()) {
            collectNodesByPosTag(child, posTagSet, list);
        }
    }


    static void collectNodesByDepLabel(DependencyNode parent, Set<String> depLabelSet, List<DependencyNode> list) {
        if (depLabelSet.contains(parent.getLabel())) {
            list.add(parent);
        } else {
            for (String dep : depLabelSet) {
                if (parent.getLabel().startsWith(dep)) {
                    list.add(parent);
                }
            }
        }
        for (DependencyNode child : parent.getChildren()) {
            collectNodesByDepLabel(child, depLabelSet, list);
        }
    }

    public static void collectLemmas(DependencyNode root, Set<String> lemmaSet) {
        if (!StopWords.isStopWord(root.getLemma())) {
            lemmaSet.add(root.getLemma());
        }
        for (DependencyNode child : root.getChildren()) {
            collectLemmas(child, lemmaSet);
        }
    }
}