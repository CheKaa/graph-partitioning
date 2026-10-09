package partitioning.algorithms;

import graph.Edge;
import graph.Graph;
import graph.Vertex;
import partitioning.entities.VertexDistance;
import java.util.*;

public class MultiSourceSPT {
    /**
     * Time Complexity: O(E log V) where E is the number of edges and V is the number of vertices.
     */
    public static <T extends Vertex> SPTResult<T> computeSPTForest(Graph<T> graph, List<T> sources) {
        Map<T, Double> distances = new HashMap<>();
        Map<T, T> previous = new HashMap<>();
        PriorityQueue<VertexDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(VertexDistance::distance));

        for (T vertex : graph.vertices()) {
            distances.put(vertex, Double.MAX_VALUE);
        }

        for (T source : sources) {
            distances.put(source, 0.0);
            queue.add(new VertexDistance(source, 0.0));
        }

        while (!queue.isEmpty()) {
            VertexDistance current = queue.poll();
            T u = (T) current.vertex();

            if (current.distance() > distances.get(u)) {
                continue;
            }

            Map<T, Edge> neighbors = graph.getEdges().get(u);
            if (neighbors == null) continue;

            for (Map.Entry<T, Edge> entry : neighbors.entrySet()) {
                T v = entry.getKey();
                double weight = entry.getValue().length;
                if (distances.get(u) + weight < distances.get(v)) {
                    distances.put(v, distances.get(u) + weight);
                    previous.put(v, u);
                    queue.add(new VertexDistance(v, distances.get(v)));
                }
            }
        }

        return new SPTResult<>(previous, distances);
    }
}
