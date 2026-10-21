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
                double cost = costFunction.calculateCost(currentLength, currentWeight);

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

    static record State(VertexOfDualGraph node, boolean in){}

    public Cut evaluateBestCutForTreeCircular(
        Graph<Vertex> graph,
        Graph<VertexOfDualGraph> dualGraph,
        DualForest dualTree,
        SPForest<Vertex> sptRes,
        double totalWeight,
        double minRelPartSize,
        PartitionRegion region,
        double desiredWeight
    ) {
        if (dualTree.roots().isEmpty() || desiredWeight <= 0.0 || desiredWeight >= totalWeight) {
            return null;
        }

        Stack<State> addStack = new Stack<>();
        for (VertexOfDualGraph root : dualTree.roots()) {
            addStack.push(new State(root, true));
        }

        BoundaryHandler boundary = new BoundaryHandler(graph);
        Deque<VertexOfDualGraph> facesToRemove = new ArrayDeque<>();
        double selectedWeight = 0.0;
        double bestCost = Double.MAX_VALUE;
        CircularCandidate best = null;
        boolean addMode = true;
        VertexOfDualGraph currFace = null;

        while (true) {
            if (addMode) {
                if (addStack.isEmpty()) {
                    break;
                }
                State state = addStack.pop();
                currFace = state.node();
                boolean in = state.in();

                if (!in) {
                    facesToRemove.addLast(currFace);
                    continue;
                }

                boundary.addVertices(currFace.getVerticesOfFace());
                selectedWeight += currFace.getWeight();
                double length = boundary.length();
                double cost = costFunction.calculateCost(length, selectedWeight);
                if (cost < bestCost){
                    bestCost = cost;
                    VertexOfDualGraph bottomFace = facesToRemove.getLast();

                    best = new CircularCandidate(currFace, bottomFace, selectedWeight, length, cost);
                }

                List<VertexOfDualGraph> children = dualTree.forest().get(currFace);
                if (children != null) {
                    for (VertexOfDualGraph child : children) {
                        addStack.push(new State(child, true));
                        addStack.push(new State(child, false));
                    }
                }

                if (selectedWeight > desiredWeight) {
                    addMode = false;
                }

            } else {
                if (facesToRemove.isEmpty()) {
                    break;
                }
                VertexOfDualGraph face = facesToRemove.removeLast();
                boundary.removeVertices(face.getVerticesOfFace());
                selectedWeight -= face.getWeight();
                double length = boundary.length();
                double cost = costFunction.calculateCost(length, selectedWeight);
                if (cost < bestCost){
                    bestCost = cost;
                    VertexOfDualGraph bottomFace = facesToRemove.getLast();
                    best = new CircularCandidate(face, bottomFace, selectedWeight, length, cost);
                }

                if (selectedWeight < desiredWeight) {
                    addMode = true;
                }
            }
        }

        Set<VertexOfDualGraph> part1 = new HashSet<>();
        Set<VertexOfDualGraph> part2 = new HashSet<>(region.dualVertices);
        BoundaryHandler bestBoundary = new BoundaryHandler(graph);
        collectCircularPart(dualTree, best.topFace, best.bottomFace, part1, bestBoundary);
        if (part1.isEmpty() || !region.dualVertices.containsAll(part1)) {
            return null;
        }
        part2.removeAll(part1);
        if (part2.isEmpty()) {
            return null;
        }

        List<Vertex> cutPath = extractCutPath(bestBoundary, region.boundary);
        if (cutPath == null) {
            return null;
        }

        return new Cut(
                part1,
                part2,
                cutPath,
                best.length,
                best.weight,
                totalWeight - best.weight,
                best.cost);
    }


    private void collectCircularPart(
            DualForest dualTree,
            VertexOfDualGraph topFace,
            VertexOfDualGraph bottomFace,
            Set<VertexOfDualGraph> part,
            BoundaryHandler boundary
    ) {
        Queue<VertexOfDualGraph> traversal = new ArrayDeque<>(dualTree.roots());
        boolean collecting = false;
        while (!traversal.isEmpty()) {
            VertexOfDualGraph face = traversal.remove();
            if (!collecting && face.equals(topFace)) {
                collecting = true;
            }
            if (collecting) {
                part.add(face);
                boundary.addVertices(face.getVerticesOfFace());
                if (face.equals(bottomFace)) {
                    return;
                }
            }
            List<VertexOfDualGraph> children = dualTree.forest().get(face);
            if (children != null) {
                traversal.addAll(children);
            }
        }
    }

    private List<Vertex> extractCutPath(BoundaryHandler boundary, List<Vertex> originalBoundary) {
        if (boundary.length() == 0.0) {
            return null;
        }
        HashSet<Vertex> boundarySet = new HashSet<>(originalBoundary);

        List<Vertex> cutPath = new ArrayList<>();
        Vertex startVertex = boundary.edges().keySet().iterator().next();
        cutPath.add(startVertex);
        Vertex curr = startVertex;
        boolean currOnBound = boundarySet.contains(curr);
        int cutStart = -1;

        do {
            Vertex next = boundary.edges().get(curr).keySet().iterator().next();
            boolean add = true;
            boolean nextOnBound = boundarySet.contains(next);
            if (currOnBound){
                if (nextOnBound){
                    add = false;
                } else {
                    assert (cutStart == -1): "Not a simple cut";
                    cutStart = cutPath.size();
                }
            }

            if (add) {
                cutPath.add(next);
            }

            curr = next;
            currOnBound = nextOnBound;
        } while (!curr.equals(startVertex));

        ArrayList<Vertex> ans = new ArrayList<>();
        ans.addAll(cutStart, cutPath);
        for (int i = 0; i < cutStart; i++){
            ans.add(cutPath.get(i));
        }
        return cutPath;
    }

    private record CircularCandidate(
            VertexOfDualGraph topFace,
            VertexOfDualGraph bottomFace,
            double weight,
            double length,
            double cost
    ) {}


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

                double cost = costFunction.calculateCost(length, weight1);
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
        Set<VertexOfDualGraph> part1 = getSubtreeVertices(bestDualVertex, dualTree);

        // 2. Get the complement set of faces
        HashSet<VertexOfDualGraph> part2 = new HashSet<>(region.dualVertices);
        part2.removeAll(part1);

        // 3. Reconstruct the path representing the cut.
        // The cut is formed by joining two paths from SPT joint by an edge that connects bestDualVertex and its parent.
        
        // Use the parent map in DualForest to find the edge connecting bestDualVertex and its parent.
        UnOrdPair dualEdge = dualTree.parent().get(bestDualVertex);
        Vertex vertex1;
        Vertex vertex2;
        if (edgeToDualVertexMap.get(dualEdge.one).get(dualEdge.two) == bestDualVertex) {
            vertex1 = dualEdge.one;
            vertex2 = dualEdge.two;
        } else {
            vertex1 = dualEdge.two;
            vertex2 = dualEdge.one;
        }

        List<Vertex> cutPath = sptRes.getPathToSource(vertex1);
        cutPath.addAll(sptRes.getPathToSource(vertex2, true));

        return new Cut(
            part1, 
            part2, 
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
