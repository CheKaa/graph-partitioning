package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import partitioning.algorithms.DualForestBuilder.DualForest;
import partitioning.entities.UnOrdPair;
import graph.Edge;
import graph.EdgeOfGraph;
import java.util.*;
import partitioning.algorithms.OneShotPartitioner.PartitionRegion;


public class CutEvaluator {
    private final CostFunction costFunction;

    public CutEvaluator(CostFunction costFunction) {
        this.costFunction = costFunction;
    }

    public Cut evaluateBestCut(
            Graph<Vertex> graph,
            Graph<VertexOfDualGraph> dualGraph,
            DualForest dualRootedForest,
            SPForest<Vertex> sptRes,
            double totalWeight,
            double minRelPartSize
        ) {
        // TODO ensure both subgraphs will have weight more than minRelPartSize*totalweight

        /**
         * Time Complexity: O(B * (V_dual + E_dual)) where B is the number of roots,
         * V_dual is the number of dual vertices and E_dual is the number of dual edges.
         */
        Cut bestCut = null;
        double minCost = Double.MAX_VALUE;
        var roots = dualRootedForest.roots();
        var dualForest = dualRootedForest.forest();
        Map<Vertex, Vertex> spt = sptRes.parents;

        Map<VertexOfDualGraph, TreeSet<EdgeOfGraph<VertexOfDualGraph>>> sortedDualEdges = dualGraph.arrangeByAngle();

        for (VertexOfDualGraph root : roots) {
            Stack<TraversalState> stack = new Stack<>();
            stack.push(new TraversalState(root, null, 0.0, 0.0, new ArrayList<>()));
            Set<VertexOfDualGraph> visited = new HashSet<>();

            while (!stack.isEmpty()) {
                TraversalState current = stack.pop();
                if (visited.contains(current.node)) continue;
                visited.add(current.node);

                double currentLength = current.length + 1.0; 
                double currentWeight = current.weight + current.node.getWeight();
                double cost = costFunction.calculateCost(currentLength, currentWeight, totalWeight);

                if (cost < minCost) {
                    minCost = cost;
                    bestCut = new Cut(
                        new HashSet<>(), 
                        new HashSet<>(), 
                        new ArrayList<>(current.path),
                        currentLength, 
                        currentWeight, 
                        totalWeight - currentWeight, 
                        cost
                    );
                }

                List<VertexOfDualGraph> children = dualForest.get(current.node);
                if (children != null) {
                    TreeSet<EdgeOfGraph<VertexOfDualGraph>> sortedEdges = sortedDualEdges.get(current.node);
                    List<VertexOfDualGraph> sortedChildren = new ArrayList<>();
                    if (sortedEdges != null) {
                        for (EdgeOfGraph<VertexOfDualGraph> edge : sortedEdges) {
                            if (children.contains(edge.end)) {
                                sortedChildren.add(edge.end);
                            }
                        }
                    } else {
                        sortedChildren.addAll(children);
                    }

                    for (VertexOfDualGraph child : sortedChildren) {
                        if (!visited.contains(child)) {
                            List<Vertex> nextPath = new ArrayList<>(current.path);
                            Vertex crossed = findCrossedVertex(graph, dualGraph, current.node, child);
                            if (crossed != null) {
                                nextPath.add(crossed);
                            }
                            stack.push(new TraversalState(child, current.node, currentLength, currentWeight, nextPath));
                        }
                    }
                }
            }
        }

        return bestCut;
    }

    public Cut evaluateBestCutForTree(
        Graph<Vertex> graph,
        Graph<VertexOfDualGraph> dualGraph,
        DualForest dualTree,
        SPForest<Vertex> sptRes,
        double totalWeight,
        double minRelPartSize,
        PartitionRegion region
    ) {
        VertexOfDualGraph bestDualVertex = null;
        double minCost = Double.MAX_VALUE;
        double bestWeight = 0.0;
        double bestLength = 0.0;
        var tree = dualTree.forest();

        var subtreeWeights = computeSubtreeWeights(dualTree);
        var edgeToDualVertexMap = dualGraph.edgeToDualVertexMap();
        var spt = sptRes.parents;
        var distances = sptRes.distances;
        var boundary = region.boundary;

        var vertexToBoundary = mapVerticesToBoundary(spt, new HashSet<>(boundary));
        
        for (Vertex v: boundary) {
            vertexToBoundary.put(v, v);
        }

        Set<UnOrdPair> boundaryEdges = new HashSet<>();
        for (int i = 0; i < boundary.size(); i++) {
            boundaryEdges.add(new UnOrdPair(boundary.get(i), boundary.get((i + 1) % boundary.size())));
        }
        var edges = graph.getEdges();

        for (var e : edges.entrySet()) {
            Vertex v = e.getKey();

            for (var edgeInfo: e.getValue().entrySet()) {
                Vertex u = edgeInfo.getKey();
                if (u.name > v.name) continue; // Avoid double counting edges
                if (boundaryEdges.contains(new UnOrdPair(u, v))) continue;

                Edge edge = edgeInfo.getValue();
                VertexOfDualGraph dualV = edgeToDualVertexMap.get(v).get(u);
                VertexOfDualGraph dualU = edgeToDualVertexMap.get(u).get(v);

                Vertex boundaryV = vertexToBoundary.get(v);
                Vertex boundaryU = vertexToBoundary.get(u);
                if (boundaryV.equals(boundaryU)) {
                    continue; // This will lead to a cut that makes an enclave
                }
                //TODO possibly small fraction of good cuts
                
                if (spt.get(v) == u || spt.get(u) == v) {
                    continue; // Edge is in the SPT tree
                }
                
                // Determine which dual vertex is the child in the rooted dual tree
                // to use its subtree weight as the partition weight.
                VertexOfDualGraph childDual = null;
                List<VertexOfDualGraph> vChildren = tree.get(dualV);
                if (vChildren != null && vChildren.contains(dualU)) {
                    childDual = dualU;
                } else {
                    List<VertexOfDualGraph> uChildren = tree.get(dualU);
                    if (uChildren != null && uChildren.contains(dualV)) {
                        childDual = dualV;
                    }
                }
                assert childDual != null : "for joint vertices in dual tree one is a child of the other.";

                double weight1 = subtreeWeights.getOrDefault(childDual, 0.0);
                double length = edge.length + distances.get(v) + distances.get(u);

                double cost = costFunction.calculateCost(length, weight1, totalWeight);
                if (cost < minCost) {
                    minCost = cost;
                    bestDualVertex = childDual;
                    bestWeight = weight1;
                    bestLength = length;
                }
            }
        }

        if (bestDualVertex == null) return null;

        // 1. Get the set of faces (dual vertices) in the subtree rooted at bestDualVertex
        Set<VertexOfDualGraph> partition1Faces = getSubtreeVertices(bestDualVertex, dualTree);

        // 2. Get the complement set of faces
        HashSet<VertexOfDualGraph> allFaces = new HashSet<>(region.dualVertices);
        HashSet<VertexOfDualGraph> partition2Faces = new HashSet<>(region.dualVertices);
        partition2Faces.removeAll(partition1Faces);

        // 3. Reconstruct the path representing the cut.
        // The cut is formed by joining two paths from SPT joint by an edge that connects bestDualVertex and its parent.
        
        // Use the parent map in DualForest to find the edge connecting bestDualVertex and its parent.
        UnOrdPair dualEdge = dualTree.parent().get(bestDualVertex);
        if (dualEdge == null) return null;

        Vertex vertex1 = dualEdge.one;
        Vertex vertex2 = dualEdge.two;

        List<Vertex> cutPath = sptRes.getPathToSource(vertex1);
        cutPath.addAll(sptRes.getPathToSource(vertex2, true));

        return new Cut(
            partition1Faces, 
            partition2Faces, 
            cutPath,
            bestLength, 
            bestWeight,
            totalWeight - bestWeight,
            minCost
        );
    }

    /**
     * Given a dual vertex (representing a subtree), returns all dual vertices
     * belonging to that subtree.
     */
    public Set<VertexOfDualGraph> getSubtreeVertices(VertexOfDualGraph root, DualForest dualTree) {
        Set<VertexOfDualGraph> subtree = new HashSet<>();
        Stack<VertexOfDualGraph> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            VertexOfDualGraph current = stack.pop();
            subtree.add(current);
            List<VertexOfDualGraph> children = dualTree.forest().get(current);
            if (children != null) {
                stack.addAll(children);
            }
        }
        return subtree;
    }


    public Map<Vertex, Vertex> mapVerticesToBoundary(Map<Vertex, Vertex> spt, Set<Vertex> boundary) {
        Map<Vertex, Vertex> vertexToBoundary = new HashMap<>();
        
        for (Vertex v : spt.keySet()) {
            if (vertexToBoundary.containsKey(v)) continue;

            List<Vertex> path = new ArrayList<>();
            Vertex current = v;
            Vertex boundaryVertex = null;

            while (!vertexToBoundary.containsKey(current)) {
                if (boundary.contains(current)) {
                    boundaryVertex = current;
                    break;
                }
                path.add(current);
                current = spt.get(current);
            }

            if (boundaryVertex == null){
                boundaryVertex = vertexToBoundary.get(current);
            }

            for (Vertex node : path) {
                vertexToBoundary.put(node, boundaryVertex);
            }
        }
        return vertexToBoundary;
    }


    private Vertex findCrossedVertex(Graph<Vertex> graph, Graph<VertexOfDualGraph> dualGraph, VertexOfDualGraph v1, VertexOfDualGraph v2) {
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = dualGraph.edgeToDualVertexMap();
        for (Map.Entry<Vertex, Map<Vertex, VertexOfDualGraph>> entry : edgeToDual.entrySet()) {
            Vertex u = entry.getKey();
            for (Map.Entry<Vertex, VertexOfDualGraph> inner : entry.getValue().entrySet()) {
                Vertex v = inner.getKey();
                if (inner.getValue().equals(v1)) {
                    Map<Vertex, VertexOfDualGraph> vMap = edgeToDual.get(v);
                    if (vMap != null && vMap.get(u) != null && vMap.get(u).equals(v2)) {
                        return u;
                    }
                }
            }
        }
        return null;
    }

    public Map<VertexOfDualGraph, Double> computeSubtreeWeights(DualForest dualTree) {
        Map<VertexOfDualGraph, Double> subtreeWeights = new HashMap<>();
        if (dualTree.roots().isEmpty()) return subtreeWeights;

        VertexOfDualGraph root = dualTree.roots().iterator().next();
        
        Stack<VertexOfDualGraph> stack = new Stack<>();
        Stack<VertexOfDualGraph> postOrderStack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            VertexOfDualGraph current = stack.pop();
            postOrderStack.push(current);
            List<VertexOfDualGraph> children = dualTree.forest().get(current);
            if (children != null) {
                for (VertexOfDualGraph child : children) {
                    stack.push(child);
                }
            }
        }

        while (!postOrderStack.isEmpty()) {
            VertexOfDualGraph current = postOrderStack.pop();
            double weight = current.getWeight();
            List<VertexOfDualGraph> children = dualTree.forest().get(current);
            if (children != null) {
                for (VertexOfDualGraph child : children) {
                    weight += subtreeWeights.getOrDefault(child, 0.0);
                }
            }
            subtreeWeights.put(current, weight);
        }

        return subtreeWeights;
    }


    private static class TraversalState {
        VertexOfDualGraph node;
        VertexOfDualGraph parent;
        double length;
        double weight;
        List<Vertex> path;

        TraversalState(VertexOfDualGraph node, VertexOfDualGraph parent, double length, double weight, List<Vertex> path) {
            this.node = node;
            this.parent = parent;
            this.length = length;
            this.weight = weight;
            this.path = path;
        }
    }
}
