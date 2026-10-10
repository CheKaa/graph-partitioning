package partitioning.algorithms;

import graph.Graph;
import graph.VertexOfDualGraph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PartitionChecker {
    /**
     * Returns an empty list when the partitions cover every graph face exactly once,
     * each part is connected, and no part exceeds maxWeight.
     */
    public static List<String> check(
            Graph<VertexOfDualGraph> graph,
            List<Set<VertexOfDualGraph>> partitions,
            double maxWeight) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(partitions, "partitions");
        if (!Double.isFinite(maxWeight) || maxWeight < 0) {
            throw new IllegalArgumentException("maxWeight must be finite and non-negative");
        }

        List<String> errors = new ArrayList<>();
        Set<VertexOfDualGraph> graphFaces = graph.vertices();
        Map<VertexOfDualGraph, Integer> faceToPart = new HashMap<>();

        for (int partId = 0; partId < partitions.size(); partId++) {
            Set<VertexOfDualGraph> part = Objects.requireNonNull(
                    partitions.get(partId), "partition " + partId);
            if (part.isEmpty()) {
                errors.add("Partition " + partId + " is empty");
                continue;
            }

            double weight = 0.0;
            Set<VertexOfDualGraph> knownFaces = new HashSet<>();
            for (VertexOfDualGraph face : part) {
                if (face == null) {
                    errors.add("Partition " + partId + " contains a null face");
                    continue;
                }
                if (!graphFaces.contains(face)) {
                    errors.add("Partition " + partId + " contains face " + face.getName()
                            + " that is not in the graph");
                    continue;
                }
                Integer previousPart = faceToPart.putIfAbsent(face, partId);
                if (previousPart != null) {
                    errors.add("Face " + face.getName() + " occurs in partitions "
                            + previousPart + " and " + partId);
                }
                knownFaces.add(face);
                weight += face.getWeight();
            }

            if (weight > maxWeight) {
                errors.add("Partition " + partId + " weight " + weight
                        + " exceeds maxWeight " + maxWeight);
            }
            if (!knownFaces.isEmpty() && !isConnected(graph, knownFaces)) {
                errors.add("Partition " + partId + " is not connected");
            }
        }

        for (VertexOfDualGraph face : graphFaces) {
            if (!faceToPart.containsKey(face)) {
                errors.add("Face " + face.getName() + " is not covered by any partition");
            }
        }
        return errors;
    }

    public static boolean isConsistent(
        Graph<VertexOfDualGraph> graph,
        List<Set<VertexOfDualGraph>> partitions,
        double maxWeight
    ) {
        var res = check(graph, partitions, maxWeight);
        if (res.isEmpty()) {
            return true;
        } else {
            System.err.println("Partition check failed with errors:");
            for (String error : res) {
                System.err.println("  " + error);
            }
            return false;
        } 
    }

    private static boolean isConnected(
            Graph<VertexOfDualGraph> graph,
            Set<VertexOfDualGraph> part) {
        VertexOfDualGraph start = part.iterator().next();
        Set<VertexOfDualGraph> visited = new HashSet<>();
        ArrayDeque<VertexOfDualGraph> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            VertexOfDualGraph current = queue.remove();
            Map<VertexOfDualGraph, graph.Edge> neighbors = graph.getEdges().get(current);
            if (neighbors == null) continue;
            for (VertexOfDualGraph neighbor : neighbors.keySet()) {
                if (part.contains(neighbor) && visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return visited.size() == part.size();
    }
}
