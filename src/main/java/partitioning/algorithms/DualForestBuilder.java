package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import java.util.*;

public class DualForestBuilder<T extends Vertex> {
    /**
     * Time Complexity: O(V + E) where V is the number of vertices and E is the number of edges.
     */
    public Map<VertexOfDualGraph, List<VertexOfDualGraph>> buildDualForest(
            Graph<T> graph, 
            Graph<VertexOfDualGraph> dualGraph, 
            Map<T, T> sptForest) {
        
        Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest = new HashMap<>();
        for (VertexOfDualGraph v : dualGraph.vertices()) {
            dualForest.put(v, new ArrayList<>());
        }

        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = graph.edgeToDualVertexMap();

        Map<T, Map<T, graph.Edge>> allEdges = graph.getEdges();
        for (T u : allEdges.keySet()) {
            Map<T, graph.Edge> neighbors = allEdges.get(u);
            if (neighbors == null) continue;

            for (Map.Entry<T, graph.Edge> entry : neighbors.entrySet()) {
                T v = entry.getKey();
                if (u.getName() > v.getName()) continue;

                boolean inSPT = (sptForest.containsKey(v) && sptForest.get(v).equals(u)) ||
                                 (sptForest.containsKey(u) && sptForest.get(u).equals(v));

                if (!inSPT) {
                    VertexOfDualGraph dualV1 = edgeToDual.get(u).get(v);
                    if (dualV1 != null) {
                        VertexOfDualGraph dualV2 = edgeToDual.get(v).get(u);
                        
                        if (dualV2 != null && !dualV1.equals(dualV2)) {
                            dualForest.get(dualV1).add(dualV2);
                            dualForest.get(dualV2).add(dualV1);
                        }
                    }
                }
            }
        }

        return dualForest;
    }
}
