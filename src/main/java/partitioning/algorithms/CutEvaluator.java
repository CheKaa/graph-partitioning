package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import graph.EdgeOfGraph;
import java.util.*;

public class CutEvaluator {
    private final CostFunction costFunction;

    public CutEvaluator(CostFunction costFunction) {
        this.costFunction = costFunction;
    }

    public Cut evaluateBestCut(
            Graph<Vertex> graph,
            Graph<VertexOfDualGraph> dualGraph,
            Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest,
            List<VertexOfDualGraph> roots,
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
                            Vertex crossed = findCrossedVertex(graph, current.node, child);
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

    private Vertex findCrossedVertex(Graph<Vertex> graph, VertexOfDualGraph v1, VertexOfDualGraph v2) {
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = graph.edgeToDualVertexMap();
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
