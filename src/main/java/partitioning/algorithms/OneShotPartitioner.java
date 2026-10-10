package partitioning.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graph.BoundSearcher;
import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import partitioning.algorithms.DualForestBuilder.DualForest;
import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;

public class OneShotPartitioner extends BalancedPartitioningOfPlanarGraphs {
    private static final Logger logger = LoggerFactory.getLogger(OneShotPartitioner.class);
    private double lengthPriority;
    private double minRelPartSize;
    public PartitionDebugger debugger;

    public OneShotPartitioner(double lengthPriority, double minRelPartSize) {
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
        
        // The DualGraphPartitioner requires an initial boundary of the planar graph.
        // In a typical planar graph, the outer face forms the initial boundary.
        // We extract the boundary vertices from the dual graph's outer face or
        // from a known boundary provider.
        // currently boundary in clockwise order
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
        this.graph = dualGraph;
        /**
         * Overall Time Complexity: O(K * (B^2 + E log V + V_dual + E_dual)) 
         * where K is the number of recursive splits, B is boundary size, 
         * E/V are original graph sizes, and V_dual/E_dual are dual graph sizes.
         */
        List<Set<VertexOfDualGraph>> finalPartitions = new ArrayList<>();
        Queue<PartitionRegion> queue = new LinkedList<>();

        Set<VertexOfDualGraph> allDualVertices = new HashSet<>(dualGraph.vertices());
        queue.add(new PartitionRegion(allDualVertices, boundary));

        while (!queue.isEmpty()) {
            PartitionRegion region = queue.poll();

            var res = splitInTwoParts(region, graph, maxWeight);

            if (res == null){
                finalPartitions.add(region.dualVertices);
                continue;
            }

            if (res.length == 1){
                finalPartitions.add(region.dualVertices);
                continue;
            }

            queue.add(res[0]);
            queue.add(res[1]);
        }

        return finalPartitions;
    }

    public PartitionRegion[] splitInTwoParts(PartitionRegion region, Graph<Vertex> simpleGraph, double maxWeight){
        double regionWeight = region.dualVertices.stream().mapToDouble(v -> v.getWeight()).sum();

        if (regionWeight <= maxWeight) {
            return new PartitionRegion[]{region};
        }

        List<List<Vertex>> regionFaces = new ArrayList<>();
        for (VertexOfDualGraph face : region.dualVertices) {
            regionFaces.add(face.getVerticesOfFace());
        }
        Graph<Vertex> regionGraph = simpleGraph.createSubgraphFromFaces(regionFaces);
        Graph<VertexOfDualGraph> dualgraph = this.graph;
        CutEvaluator cutEvaluator = new CutEvaluator(new CostFunction(lengthPriority, maxWeight));

        SPForest<Vertex> spt = MultiSourceSPT.computeSPTForest(regionGraph, region.boundary);

        DualForest forest = DualForestBuilder.buildDualTree(regionGraph, dualgraph, spt, region.boundary);

        Cut bestCut = cutEvaluator.evaluateBestCutForTree(
                regionGraph, dualgraph, forest, spt, regionWeight, minRelPartSize, region);

        if (bestCut == null) {
            return null;
        } else {
            List<Vertex>[] newBoundaries = updateBoundaries(region.boundary, bestCut);
            
            if (debugger != null) {
                String suffix = "g_size_" + regionGraph.vertices().size();
                debugger.dumpBoundaryGeoJSON(region.boundary, "region_boundary_" + suffix);
                debugger.dumpSPTGeoJSON(spt.parents, "spt_" + suffix);
                debugger.dumpDualTreeGeoJSON(forest.forest(), "dual_tree_" + suffix);
                debugger.dumpPathGeoJSON(bestCut.cutPath, "cut_path_" + suffix);
            }
            PartitionRegion region1 = new PartitionRegion(bestCut.part1, newBoundaries[0]);
            PartitionRegion region2 = new PartitionRegion(bestCut.part2, newBoundaries[1]);

            return new PartitionRegion[]{region1, region2};
            
        }
    }


    private List<Vertex>[] updateBoundaries(List<Vertex> boundary, Cut cut) {
        if (cut == null || cut.cutPath == null || cut.cutPath.isEmpty()) {
            return null;
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

        assert startIdx >= 0 && endIdx >= 0 : "Both cut endpoints must lie on the region boundary";

        List<Vertex> boundary1 = new ArrayList<>();
        List<Vertex> boundary2 = new ArrayList<>();

        List<Vertex> cutPathVertices = new ArrayList<>(cut.cutPath);

        // Part 1: startIdx -> endIdx along boundary, then cutPath reversed
        int curr1 = startIdx;
        while (curr1 != endIdx) {
            boundary2.add(boundary.get(curr1));
            curr1 = (curr1 + 1) % boundary.size();
        }
        boundary2.add(boundary.get(endIdx));
        List<Vertex> revCut = new ArrayList<>(cutPathVertices);
        Collections.reverse(revCut);
        boundary2.addAll(revCut);

        // Part 2: endIdx -> startIdx along boundary, then cutPath
        int curr2 = endIdx;
        while (curr2 != startIdx) {
            boundary1.add(boundary.get(curr2));
            curr2 = (curr2 + 1) % boundary.size();
        }
        boundary1.add(boundary.get(startIdx));
        boundary1.addAll(cutPathVertices);

        return new List[]{boundary1, boundary2};
    }

    public static class PartitionRegion {
        Set<VertexOfDualGraph> dualVertices;
        List<Vertex> boundary;

        PartitionRegion(Set<VertexOfDualGraph> dualVertices, List<Vertex> boundary) {
            this.dualVertices = dualVertices;
            this.boundary = boundary;
        }
    }
}
