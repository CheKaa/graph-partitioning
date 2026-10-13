package partitioning.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import geometry.Point;
import graph.BoundSearcher;
import graph.Graph;
import graph.TestGraphUtils;
import graph.Vertex;
import graph.VertexOfDualGraph;
import graphPreparation.MakingDualGraph;
import graphPreparation.NestedFacesRemover;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;

class OneShotPartitionerSplitTest {
    @Test
    void calculatesPathLengthFromConsecutiveVertices() {
        Vertex first = new Vertex(1, 0.0, 0.0, 0.0);
        Vertex second = new Vertex(2, 3.0, 0.0, 0.0);
        Vertex third = new Vertex(3, 3.0, 4.0, 0.0);
        Graph<Vertex> graph = new Graph<>();
        graph.addEdge(first, second, 10.0);
        graph.addEdge(second, third, 20.0);

        assertEquals(30.0, OneShotPartitioner.calculatePathLength(graph, List.of(first, second, third), false));
    }

    @Test
    void partitionsGridSplit3() {
        testOnGrid(3, 3, 2.0);
    }

    @Test
    void partitionsGridSplit5() {
        testOnGrid(5, 5, 3.0, 1.0, 0.1, false, true);
    }

    @Test
    void partitionsGridSplit1015() {
        testOnGrid(10, 15, 5.0, 1.0, 0.1, true, true);
    }

    @Test
    void partitionsGridSplit100100100() {
        testOnGrid(100, 100, 100.0, 1, 0.1, false, true);
    }
   
    // @Test
    // void partitionsGridSplit100010001000() {
    //     testOnGrid(1000, 1000, 1000.0, 1, 0.1, false, true);
    // }

    void testOnGrid(int n, int m, double maxWeight) {
        testOnGrid(n, m, maxWeight, 1.0, 0.1, false, false);
    }

    void testOnGrid(int n, int m, double maxWeight, double lengthPriority, double minRelPartSize, boolean debug, boolean dumpRes) {
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(n, m, 1.0);
        CoordinateConversion coordinateConversion = new CoordinateConversion(new Point(0, 0));

        MakingDualGraph dualGraphBuilder = new MakingDualGraph();
        Graph<VertexOfDualGraph> dualGraph = dualGraphBuilder.buildDualGraph(simpleGraph);
        VertexOfDualGraph externalFace = dualGraphBuilder.findExternalFace(dualGraph);
        NestedFacesRemover.removeNestedFaces(dualGraph, externalFace);
        dualGraph.deleteVertex(externalFace);
        dualGraph.vertices().forEach(face -> face.setWeight(1.0));

        Set<VertexOfDualGraph> allFaces = new HashSet<>(dualGraph.vertices());
        List<Vertex> boundary = BoundSearcher.findBound(simpleGraph, allFaces);
        assertNotNull(boundary, "Boundary should not be null");
        assertFalse(boundary.isEmpty(), "Boundary should not be empty");
        PartitionDebugger debugger = new PartitionDebugger(coordinateConversion);

        OneShotPartitioner partitioner = new OneShotPartitioner(lengthPriority, minRelPartSize);
        if (debug) {
            partitioner.debugger = debugger;
        }
        List<Set<VertexOfDualGraph>> partition =
                partitioner.partition(simpleGraph, dualGraph, boundary, maxWeight);
        
        if (dumpRes) {
            debugger.dumpPartitionGeoJSON(partition, "partition_debug_" + n + "_" + m);
        }
        assertTrue(PartitionChecker.isConsistent(dualGraph, partition, maxWeight), "inconsistent partition");
    }
}
