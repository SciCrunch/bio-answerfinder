package org.bio_answerfinder.evaluation;

import org.bio_answerfinder.common.GloveDBLookup;
import org.bio_answerfinder.common.Pair;
import org.bio_answerfinder.evaluation.FactoidUtils.ScoredNPInfo;
import org.bio_answerfinder.util.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Created by bozyurt on 9/16/19.
 */
public class ClusterUtils {
    List<ScoredNPInfo> spList;
    GloveDBLookup gloveMan;
    List<Cluster> clusters = new ArrayList<>();

    public ClusterUtils(List<ScoredNPInfo> spList, GloveDBLookup gloveMan) {
        this.spList = spList;
        this.gloveMan = gloveMan;
        clusters = new ArrayList<>(spList.size());
        for (ScoredNPInfo sp : spList) {
            Cluster cluster = new Cluster(Arrays.asList(sp));
            clusters.add(cluster);
        }
    }

    public List<Cluster> handle() throws Exception {

        while (clusters.size() > 2) {
            ClusterPair cp = findClusters2Merge(0.9);
            if (cp == null) {
                break;
            }
            merge(cp);
        }
        return clusters;
    }

    ClusterPair findClusters2Merge(double threshold) {
        List<ClusterPair> cpList = new ArrayList<>(clusters.size());
        for (int i = 0; i < clusters.size(); i++) {
            Cluster first = clusters.get(i);
            Cluster theClosest = null;
            double maxSim = -1;
            for (int j = i + 1; j < clusters.size(); j++) {
                Cluster second = clusters.get(j);
                double clusterSimilarity = first.findClusterSimilarity(second, gloveMan);
                if (clusterSimilarity > maxSim) {
                    maxSim = clusterSimilarity;
                    theClosest = second;
                }
            }
            if (theClosest != null) {
                ClusterPair cp = new ClusterPair(first, theClosest, maxSim);
                cpList.add(cp);
            }
        }
        ClusterPair theCP = null;
        double maxSim = -1;
        for (ClusterPair cp : cpList) {
            if (cp.similarity > maxSim) {
                theCP = cp;
                maxSim = cp.similarity;
            }
        }
        if (theCP != null && theCP.similarity > threshold) {
            return theCP;
        }
        return null;
    }

    public void merge(ClusterPair cp) {
        List<ScoredNPInfo> mergedMembers = new ArrayList<>(10);
        mergedMembers.addAll(cp.first.getMembers());
        mergedMembers.addAll(cp.second.getMembers());
        Cluster mc = new Cluster(mergedMembers);
        clusters.remove(cp.first);
        clusters.remove(cp.second);
        clusters.add(mc);
    }


    public static class ClusterPair {
        Cluster first;
        Cluster second;
        double similarity;

        public ClusterPair(Cluster first, Cluster second, double similarity) {
            this.first = first;
            this.second = second;
            this.similarity = similarity;
        }
    }

    public static class Cluster {
        List<ScoredNPInfo> members = new ArrayList<>(2);

        public Cluster(List<ScoredNPInfo> members) {
            this.members = members;
        }

        public void addMember(ScoredNPInfo sp) {
            if (members.contains(sp)) {
                members.add(sp);
            }
        }

        public double getScore() {
            OptionalDouble max = members.stream().mapToDouble(m -> m.getScore()).max();
            return max.getAsDouble();
        }

        public ScoredNPInfo getMaxScoringMember() {
            ScoredNPInfo theMember = null;
            double maxScore = -1;
            for(ScoredNPInfo member : members) {
                if (member.getScore() > maxScore) {
                    maxScore = member.getScore();
                    theMember = member;
                }
            }
            return theMember;
        }

        public List<ScoredNPInfo> getMembers() {
            return members;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Cluster{");
            for (ScoredNPInfo member : members) {
                sb.append("\n\t").append(member.getPhrase().getPhrase()).append(" (").append(member.getScore()).append(")\n");
            }
            sb.append('}');
            return sb.toString();
        }

        public double findClusterSimilarity(Cluster otherCluster, GloveDBLookup gloveMan) {
            double sum = 0;
            double maxSim = -1;
            // complete-linkage
            double minSim = Double.POSITIVE_INFINITY;
            for (ScoredNPInfo member : otherCluster.getMembers()) {
                Pair<ScoredNPInfo, Double> closest = findClosest(member, gloveMan);
                if (closest != null) {
                    sum += closest.getSecond();
                    if (closest.getSecond() < minSim) {
                        minSim = closest.getSecond();
                    }

                }
            }
            //return sum / otherCluster.getMembers().size();
            return minSim;
        }

        public Pair<ScoredNPInfo, Double> findClosest(ScoredNPInfo ref, GloveDBLookup gloveMan) {
            float[] fv = getPhraseEmbedding(ref.getPhrase().getPhrase(), gloveMan);
            double fvNorm = MathUtils.euclidianNorm(fv);
            ScoredNPInfo theClosest = null;
            double maxSim = -1;
            for (ScoredNPInfo member : members) {
                double similarity = getSimilarity(fv, fvNorm, member.getPhrase().getPhrase(), gloveMan);
                if (similarity > maxSim) {
                    maxSim = similarity;
                    theClosest = member;
                }

            }
            if (theClosest != null) {
                return new Pair<>(theClosest, maxSim);
            }

            return null;
        }
    }

    public static float[] getPhraseEmbedding(String phrase, GloveDBLookup gloveMan) {
        String sp = phrase.replaceAll("\\s+", "_");
        float[] pv = gloveMan.getGloveVector(sp);
        if (pv != null) {
            return pv;
        }
        // average of token embeddings
        float[] av = null;
        float[] sum = null;
        String[] tokens = phrase.split("\\s+");
        int count = 0;
        for (String token : tokens) {
            pv = gloveMan.getGloveVector(token);
            if (pv != null) {
                count++;
                if (sum == null) {
                    sum = pv;
                } else {
                    for (int i = 0; i < pv.length; i++) {
                        sum[i] += pv[i];
                    }
                }
            }
        }
        if (sum != null) {
            av = new float[sum.length];
            for (int i = 0; i < sum.length; i++) {
                av[i] = sum[i] / count;
            }
        }
        return av;
    }

    public static double getSimilarity(float[] fv, double fvNorm, String phrase, GloveDBLookup gloveMan) {
        float[] sv = getPhraseEmbedding(phrase, gloveMan);
        return MathUtils.cosSimilarity(fv, sv, fvNorm);
    }


    @SuppressWarnings("Duplicates")
    public static double getPhraseSimilarity(float[] fv, double fvNorm, String phrase, GloveDBLookup gloveMan) {
        String sp = phrase.replaceAll("\\s+", "_");
        float[] sv = gloveMan.getGloveVector(sp);
        if (sv != null) {
            return MathUtils.cosSimilarity(fv, sv, fvNorm);
        }

        String[] tokens = phrase.split("\\s+");
        double sum = 0;
        int count = 0;
        for (String token : tokens) {
            sv = gloveMan.getGloveVector(token);
            if (sv != null) {
                double similarity = MathUtils.cosSimilarity(fv, sv, fvNorm);
                sum += similarity;
                count++;
            }
        }
        double avg = -1;
        if (count > 0) {
            avg = sum / count;
        }
        return avg;

    }

}
