package partitioning.algorithms;

import graph.BoundSearcher;
import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;

public class DualGraphPartitioner extends BalancedPartitioningOfPlanarGraphs {
    private static final Logger logger = LoggerFactory.getLogger(DualGraphPartitioner.class);
    private double lengthPriority;
    private double minRelPartSize;
    private PartitionDebugger debugger;

    public DualGraphPartitioner(double lengthPriority, double minRelPartSize) {
        this.lengthPriority = lengthPriority;
        this.minRelPartSize = minRelPartSize;
    }

    @Override
    public void balancedPartitionAlgorithm(
            Graph<Vertex> simpleGraph,
            Graph<VertexOfDualGraph> graph,
            int maxSumVerticesWeight,
            CoordinateConversion coordinateConversion) {
        
        this.graph = graph;
        this.debugger = new PartitionDebugger(coordinateConversion);

        
        // The DualGraphPartitioner requires an initial boundary of the planar graph.
        // In a typical planar graph, the outer face forms the initial boundary.
        // We extract the boundary vertices from the dual graph's outer face or
        // from a known boundary provider.
        List<Vertex> initialBoundary = extractInitialBoundary(simpleGraph);
        
        if (initialBoundary == null || initialBoundary.isEmpty()) {
            logger.error("Could not determine initial boundary for DualGraphPartitioner");
            return;
        }

        // Use the existing partitioning logic to populate this.partition
        this.partition = partition(simpleGraph, graph, initialBoundary, maxSumVerticesWeight);
    }

    private List<Vertex> extractInitialBoundary(Graph<Vertex> simpleGraph) {
        // Use BoundSearcher to correctly find the ordered boundary of the planar graph.
        Set<VertexOfDualGraph> allFaces = new HashSet<>(this.graph.vertices());
        return BoundSearcher.findBound(simpleGraph, allFaces);
    }

    public List<Set<VertexOfDualGraph>> partition(Graph<Vertex> graph, Graph<VertexOfDualGraph> dualGraph, List<Vertex> boundary, double maxWeight) {
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


        while (!queue.isEmpty()) {
            PartitionRegion region = queue.poll();
            double regionWeight = region.dualVertices.stream().mapToDouble(Vertex::getWeight).sum();

            CutEvaluator cutEvaluator = new CutEvaluator(new CostFunction(lengthPriority, maxWeight));
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
            List<Vertex> startBoundary = getCyclePart(region.boundary, distIdx[0], distIdx[1]);
            List<Vertex> finishBoundary = getCyclePart(region.boundary, distIdx[1], distIdx[0]);

            Map<Vertex, Vertex> spt = MultiSourceSPT.computeSPTForest(graph, startBoundary);


            Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest = forestBuilder.buildDualForest(graph, dualGraph, spt,  startBoundary, finishBoundary);

            List<VertexOfDualGraph> roots = getDualRoots(graph, region.boundary);
            Cut bestCut = cutEvaluator.evaluateBestCut(graph, dualGraph, dualForest, roots, regionWeight, minRelPartSize);

            if (bestCut == null) {
                finalPartitions.add(region.dualVertices);
            } else {
                List<Vertex>[] newBoundaries = updateBoundaries(graph, region.boundary, bestCut);
                
                // Debug dumping
                debugger.dumpBoundaryGeoJSON(region.boundary, "region_boundary_" + System.nanoTime());
                debugger.dumpSPTGeoJSON(spt, "spt_" + System.nanoTime());
                debugger.dumpDualTreeGeoJSON(dualForest, "dual_tree_" + System.nanoTime());
                
                queue.add(new PartitionRegion(bestCut.part1, newBoundaries[0]));
                queue.add(new PartitionRegion(bestCut.part2, newBoundaries[1]));
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

    private List<Vertex>[] updateBoundaries(Graph<Vertex> graph, List<Vertex> boundary, Cut cut) {
        if (cut == null || cut.cutPath == null || cut.cutPath.isEmpty()) {
            return new List[]{boundary, boundary};
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

        return new List[]{boundary1, boundary2};
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
