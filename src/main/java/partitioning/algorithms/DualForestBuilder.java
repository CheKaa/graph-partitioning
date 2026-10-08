package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import partitioning.entities.Pair;

import java.util.*;

public class DualForestBuilder {
    /**
     * Time Complexity: O(V + E) where V is the number of vertices and E is the number of edges.
     */
    public DualForest buildDualForest(
            Graph<Vertex> graph, 
            Graph<VertexOfDualGraph> dualGraph, 
            Map<Vertex, Vertex> sptForest,
            List<Vertex> startingBoundary,
            List<Vertex> finishingBoundary
        ) {
        Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest = new HashMap<>();
        for (VertexOfDualGraph v : dualGraph.vertices()) {
            dualForest.put(v, new ArrayList<>());
        }

        // Create a set of edges that form the starting boundary to avoid crossing them
        HashSet<Pair> startBoundaryEdges = new HashSet<>();
        Vertex curr = startingBoundary.get(0);
        for (int i = 1; i < startingBoundary.size(); i++) {
            Vertex next = startingBoundary.get(i);
            startBoundaryEdges.add(new Pair(curr, next));
            curr = next;
        }

        // Map to find which dual vertex corresponds to an edge in the original graph
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = dualGraph.edgeToDualVertexMap();

        // We start building the forest from the finishing boundary
        Queue<VertexOfDualGraph> queue = new LinkedList<>();
        HashMap<VertexOfDualGraph, Pair> parent = new HashMap<>();
        SPTCheck forestCheck = new SPTCheck(sptForest);
        ArrayList<VertexOfDualGraph> roots = new ArrayList<>();
        ArrayList<VertexOfDualGraph> leaves = new ArrayList<>();

        Set<Pair> finishBoundaryEdges = new HashSet<>();
        curr = finishingBoundary.get(0);
        for (int i = 1; i < finishingBoundary.size(); i++) {
            Vertex next = finishingBoundary.get(i);
            if (forestCheck.check(curr, next)) {
                curr = next;
                continue;
            }

            Pair edgePair = new Pair(curr, next);
            finishBoundaryEdges.add(edgePair);
            if (edgeToDual.containsKey(curr) && edgeToDual.get(curr).containsKey(next)) {
                VertexOfDualGraph dualV = edgeToDual.get(curr).get(next);
                if (dualV != null && !parent.containsKey(dualV)) {
                    queue.add(dualV);
                    parent.put(dualV, edgePair);
                    roots.add(dualV);
                }
            curr = next;
            }
        }

        while (!queue.isEmpty()) {
            VertexOfDualGraph u = queue.poll();
            Pair edgeWitness = parent.get(u);
            var forestNeibs = dualForest.get(u);
            
            List<Vertex> faceVertices = u.getVerticesOfFace();
            int startId = 0;
            for (int i = 0; i < faceVertices.size(); i++) {
                Vertex v1 = faceVertices.get(i);
                Vertex v2 = faceVertices.get((i + 1) % faceVertices.size());
                if (new Pair(v1, v2).equals(edgeWitness)) {
                    startId = i;
                    break;
                }
            }

            boolean isLeaf = true;

            for (int i = 1; i <= faceVertices.size()-1; i++) {
                int idx1 = (startId + i) % faceVertices.size();
                int idx2 = (idx1 + 1) % faceVertices.size();
                Vertex v1 = faceVertices.get(idx1);
                Vertex v2 = faceVertices.get(idx2);
                if (forestCheck.check(v1, v2)) continue;

                Pair edgePair = new Pair(v1, v2);

                assert(!edgePair.equals(edgeWitness));
                
                // Check if the edge is in the starting boundary
                if (startBoundaryEdges.contains(edgePair)) continue;
                if (finishBoundaryEdges.contains(edgePair)) continue;

                // Check if the edge is in the SPT forest


                VertexOfDualGraph dualV = edgeToDual.get(v2).get(v1); // take neighbour side - edge in opposite direction

                if (dualV != null && !dualV.equals(u)) {
                    if (!parent.containsKey(dualV)) {
                        // Rooted forest: u is parent, dualV is child
                        forestNeibs.add(dualV);
                        parent.put(dualV, edgePair);
                        queue.add(dualV);
                        isLeaf = false;
                    }
                }
            
            }
            if (isLeaf){
                leaves.add(u);
            }
        }

        return new DualForest(dualForest, roots, leaves, parent);
    }

    static class SPTCheck {
        public Map<Vertex, Vertex> forest;

        public SPTCheck(Map<Vertex, Vertex> forest){
            this.forest = forest;
        }

        public boolean check(Vertex v1, Vertex v2){
            if (forest.containsKey(v2) && forest.get(v2).equals(v1)){
                return true;
            } 

            if (forest.containsKey(v1) && forest.get(v1).equals(v2)){
                return true;
            }

            return false;
        }
    }

    static record DualForest(
        Map<VertexOfDualGraph, List<VertexOfDualGraph>> forest,
        List<VertexOfDualGraph> roots, // In boundary order
        List<VertexOfDualGraph> leaves, // in order of appearence from most left to most right
        HashMap<VertexOfDualGraph, Pair> parent
    ) {}
}
