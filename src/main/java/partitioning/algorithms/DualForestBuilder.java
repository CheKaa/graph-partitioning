package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import partitioning.entities.Pair;

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
        HashSet<Pair> startBoundaryEdges = new HashSet<>();
        T curr = startingBoundary.get(0);
        for (int i = 1; i < startingBoundary.size(); i++) {
            T next = startingBoundary.get(i);
            startBoundaryEdges.add(new Pair(curr, next));
            curr = next;
        }

        // Map to find which dual vertex corresponds to an edge in the original graph
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = dualGraph.edgeToDualVertexMap();

        // We start building the forest from the finishing boundary
        Queue<VertexOfDualGraph> queue = new LinkedList<>();
        HashMap<VertexOfDualGraph, Pair> parent = new HashMap<>();

        Set<Pair> finishBoundaryEdges = new HashSet<>();
        curr = finishingBoundary.get(0);
        for (int i = 1; i < finishingBoundary.size(); i++) {
            T next = finishingBoundary.get(i);
            Pair edgePair = new Pair(curr, next);
            finishBoundaryEdges.add(edgePair);
            if (edgeToDual.containsKey(curr) && edgeToDual.get(curr).containsKey(next)) {
                VertexOfDualGraph dualV = edgeToDual.get(curr).get(next);
                if (dualV != null && !parent.containsKey(dualV)) {
                    queue.add(dualV);
                    parent.put(dualV, edgePair);
                }
            curr = next;
            }
        }

        while (!queue.isEmpty()) {
            VertexOfDualGraph u = queue.poll();
            Pair edgeWitness = parent.get(u);
            
            List<Vertex> faceVertices = u.getVerticesOfFace();
            int startId = 0;
            if (edgeWitness != null) {
                for (int i = 0; i < faceVertices.size(); i++) {
                    Vertex v1 = faceVertices.get(i);
                    Vertex v2 = faceVertices.get((i + 1) % faceVertices.size());
                    if (new Pair(v1, v2).equals(edgeWitness)) {
                        startId = i;
                        break;
                    }
                }
            }

            for (int i = 1; i <= faceVertices.size(); i++) {
                int idx1 = (startId + i) % faceVertices.size();
                int idx2 = (idx1 + 1) % faceVertices.size();
                Vertex v1 = faceVertices.get(idx1);
                Vertex v2 = faceVertices.get(idx2);

                Pair edgePair = new Pair(v1, v2);

                assert(!edgePair.equals(edgeWitness));
                
                // Check if the edge is in the starting boundary
                if (startBoundaryEdges.contains(edgePair)) continue;
                if (finishBoundaryEdges.contains(edgePair)) continue;

                // Check if the edge is in the SPT forest
                if (sptForest.containsKey(v2) && sptForest.get(v2).equals(v1)){
                    continue;
                } 

                if (sptForest.containsKey(v1) && sptForest.get(v1).equals(v2)){
                    continue;
                }

                VertexOfDualGraph dualV = edgeToDual.get(v2).get(v1); // take neighbour side - edge in opposite direction

                if (dualV != null && !dualV.equals(u)) {
                    if (!parent.containsKey(dualV)) {
                        // Rooted forest: u is parent, dualV is child
                        dualForest.get(u).add(dualV);
                        parent.put(dualV, edgePair);
                        queue.add(dualV);
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
