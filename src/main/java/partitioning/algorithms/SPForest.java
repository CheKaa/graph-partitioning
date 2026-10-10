package partitioning.algorithms;

import graph.Vertex;
import java.util.*;

/**
 * Represents the result of a Shortest Path Tree (SPT) computation,
 * containing both the tree structure (parent pointers) and the distances from sources.
 */
public class SPForest<T extends Vertex> {
    final Map<T, T> parents;
    final Map<T, Double> distances;

    public SPForest(Map<T, T> parents, Map<T, Double> distances) {
        this.parents = parents;
        this.distances = distances;
    }

    public Map<T, T> getParents() {
        return parents;
    }

    public Map<T, Double> getDistances() {
        return distances;
    }

    public T getParent(T vertex) {
        return parents.get(vertex);
    }

    public double getDistance(T vertex) {
        return distances.getOrDefault(vertex, Double.MAX_VALUE);
    }

    public List<T> getPathToSource(T vertex) {
        return getPathToSource(vertex, false);
    }

    public List<T> getPathToSource(T vertex, boolean reverse) {
        List<T> path = new ArrayList<>();
        T current = vertex;
        while (current != null) {
            path.add(current);
            current = parents.get(current);
        }
        if (!reverse) {
            Collections.reverse(path);
        }
        return path;
    }
}
