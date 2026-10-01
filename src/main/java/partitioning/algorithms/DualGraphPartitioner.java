package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import java.util.*;

public class DualGraphPartitioner {
    private final double maxWeight;
    private final CostFunction costFunction;

    public DualGraphPartitioner(double maxWeight, CostFunction costFunction) {
        this.maxWeight = maxWeight;
        this.costFunction = costFunction;
    }

    public List<Set<VertexOfDualGraph>> partition(Graph<Vertex> graph, Graph<VertexOfDualGraph> dualGraph, List<Vertex> boundary) {
        /**
         * Overall Time Complexity: O(K * (B^2 + E log V + V_dual + E_dual)) 
         * where K is the number of recursive splits, B is boundary size, 
         * E/V are original graph sizes, and V_dual/E_dual are dual graph sizes.
         */
        List<Set<VertexOfDualGraph>> finalPartitions = new ArrayList<>();
        Queue<PartitionRegion> queue = new LinkedList<>();

        Set<VertexOfDualGraph> allDualVertices = new HashSet<>(dualGraph.vertices());
        queue.add(new PartitionRegion(allDualVertices, boundary));

        BoundaryVertexFinder<Vertex> boundaryFinder = new BoundaryVertexFinder<>();
        DualForestBuilder<Vertex> forestBuilder = new DualForestBuilder<>();
        CutEvaluator cutEvaluator = new CutEvaluator(costFunction);

        while (!queue.isEmpty()) {
            PartitionRegion region = queue.poll();
            double regionWeight = region.dualVertices.stream().mapToDouble(Vertex::getWeight).sum();

            if (regionWeight <= maxWeight) {
                finalPartitions.add(region.dualVertices);
                continue;
            }

            // 1. Find most distant boundary vertices
            int[] distIdx = boundaryFinder.findMostDistantBoundaryVertices(graph, region.boundary);
            if (distIdx == null) {
                finalPartitions.add(region.dualVertices);
                continue;
            }

            // 2. Multi-source Dijkstra from first part of boundary
            List<Vertex> part1Boundary = getCyclePart(region.boundary, distIdx[0], distIdx[1]);
            Map<Vertex, Vertex> spt = MultiSourceSPT.computeSPTForest(graph, part1Boundary);



            Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest = forestBuilder.buildDualForest(graph, dualGraph, spt);

            List<VertexOfDualGraph> roots = getDualRoots(graph, region.boundary);
            Cut bestCut = cutEvaluator.evaluateBestCut(graph, dualGraph, dualForest, roots, regionWeight);

            if (bestCut == null) {
                finalPartitions.add(region.dualVertices);
            } else {
                List<List<Vertex>> newBoundaries = updateBoundaries(graph, region.boundary, bestCut);
                
                queue.add(new PartitionRegion(bestCut.part1, newBoundaries.get(0)));
                queue.add(new PartitionRegion(bestCut.part2, newBoundaries.get(1)));
            }
        }

        return finalPartitions;
    }

    private List<Vertex> getCyclePart(List<Vertex> boundary, int i1, int i2) {
        if (i1 == -1 || i2 == -1) return List.of(boundary.get(0));
        int b = boundary.size();
        List<Vertex> part = new ArrayList<>();
        int curr = i1;
        while (curr != i2) {
            part.add(boundary.get(curr));
            curr = (curr + 1) % b;
        }
        part.add(boundary.get(i2));
        return part;
    }

    private List<VertexOfDualGraph> getDualRoots(Graph<Vertex> graph, List<Vertex> boundary) {
        List<VertexOfDualGraph> roots = new ArrayList<>();
        Map<Vertex, Map<Vertex, VertexOfDualGraph>> edgeToDual = graph.edgeToDualVertexMap();
        for (int i = 0; i < boundary.size(); i++) {
            Vertex v = boundary.get(i);
            Vertex next = boundary.get((i + 1) % boundary.size());
            VertexOfDualGraph dualV = edgeToDual.get(v).get(next);
            if (dualV != null) roots.add(dualV);
        }
        return roots;
    }

    private List<List<Vertex>> updateBoundaries(Graph<Vertex> graph, List<Vertex> boundary, Cut cut) {
        if (cut == null || cut.cutPath == null || cut.cutPath.isEmpty()) {
            return List.of(boundary, boundary);
        }

        Vertex startVertex = cut.cutPath.get(0);
        Vertex endVertex = cut.cutPath.get(cut.cutPath.size() - 1);

        int startIdx = -1;
        int endIdx = -1;

        for (int i = 0; i < boundary.size(); i++) {
            Vertex v = boundary.get(i);
            if (v.equals(startVertex)) startIdx = i;
            if (v.equals(endVertex)) endIdx = i;
        }

        assert (startIdx <0 || endIdx <0);

        List<Vertex> boundary1 = new ArrayList<>();
        List<Vertex> boundary2 = new ArrayList<>();
        List<Vertex> cutPathVertices = new ArrayList<>(cut.cutPath);

        // Part 1: startIdx -> endIdx along boundary, then cutPath reversed
        int curr1 = startIdx;
        while (curr1 != endIdx) {
            boundary1.add(boundary.get(curr1));
            curr1 = (curr1 + 1) % boundary.size();
        }
        boundary1.add(boundary.get(endIdx));
        List<Vertex> revCut = new ArrayList<>(cutPathVertices);
        Collections.reverse(revCut);
        boundary1.addAll(revCut);

        // Part 2: endIdx -> startIdx along boundary, then cutPath
        int curr2 = endIdx;
        while (curr2 != startIdx) {
            boundary2.add(boundary.get(curr2));
            curr2 = (curr2 + 1) % boundary.size();
        }
        boundary2.add(boundary.get(startIdx));
        boundary2.addAll(cutPathVertices);

        return List.of(boundary1, boundary2);
    }

    private static class PartitionRegion {
        Set<VertexOfDualGraph> dualVertices;
        List<Vertex> boundary;

        PartitionRegion(Set<VertexOfDualGraph> dualVertices, List<Vertex> boundary) {
            this.dualVertices = dualVertices;
            this.boundary = boundary;
        }
    }
}
