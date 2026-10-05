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
            Map<T, T> sptForest,
            List<T> startingBoundary,
            List<T> finishingBoundary) {
        
        Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest = new HashMap<>();
        for (VertexOfDualGraph v : dualGraph.vertices()) {
            dualForest.put(v, new ArrayList<>());
        }

        // Create a set of edges that form the starting boundary to avoid crossing them
        Set<String> startBoundaryEdges = new HashSet<>();
        for (int i = 0; i < startingBoundary.size(); i++) {
            T u = startingBoundary.get(i);
            T v = startingBoundary.get((i + 1) % startingBoundary.size());
            startBoundaryEdges.add(edgeToKey(u, v));
        }

        // Map to find which dual vertex corresponds to an edge in the original graph
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = dualGraph.edgeToDualVertexMap();

        // We start building the forest from the finishing boundary
        Queue<VertexOfDualGraph> queue = new LinkedList<>();
        Set<VertexOfDualGraph> visited = new HashSet<>();

        for (int i = 0; i < finishingBoundary.size(); i++) {
            T u = finishingBoundary.get(i);
            T v = finishingBoundary.get((i + 1) % finishingBoundary.size());
            if (edgeToDual.containsKey(u) && edgeToDual.get(u).containsKey(v)) {
                VertexOfDualGraph dualV = edgeToDual.get(u).get(v);
                if (dualV != null && !visited.contains(dualV)) {
                    queue.add(dualV);
                    visited.add(dualV);
                }
            }
        }

        while (!queue.isEmpty()) {
            VertexOfDualGraph u = queue.poll();
            
            // Traverse faces (vertices of dual graph) along edges that are NOT in sptForest
            // and NOT in startingBoundary
            List<Vertex> faceVertices = u.getVerticesOfFace();
            for (int i = 0; i < faceVertices.size(); i++) {
                Vertex v1 = faceVertices.get(i);
                Vertex v2 = faceVertices.get((i + 1) % faceVertices.size());

                String edgeKey = edgeToKey(v1, v2);
                
                // Check if the edge is in the starting boundary
                if (startBoundaryEdges.contains(edgeKey)) continue;

                // Check if the edge is in the SPT forest
                boolean inSPT = (sptForest.containsKey(v2) && sptForest.get(v2).equals(v1)) ||
                                 (sptForest.containsKey(v1) && sptForest.get(v1).equals(v2));

                if (!inSPT) {
                    VertexOfDualGraph dualV = edgeToDual.get(v1).get(v2);
                    if (dualV != null && !dualV.equals(u)) {
                        if (!visited.contains(dualV)) {
                            // Rooted forest: u is parent, dualV is child
                            dualForest.get(u).add(dualV);
                            visited.add(dualV);
                            queue.add(dualV);
                        }
                    }
                }
            }
        }

        return dualForest;
    }

    private String edgeToKey(Vertex u, Vertex v) {
        return u.getName() < v.getName() ? u.getName() + "-" + v.getName() : v.getName() + "-" + u.getName();
    }
}
