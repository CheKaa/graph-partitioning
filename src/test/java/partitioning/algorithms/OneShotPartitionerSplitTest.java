package partitioning.algorithms;

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
    void partitionsGridSplit3() {
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(3, 3, 1.0);
        CoordinateConversion coordinateConversion = new CoordinateConversion(new Point(0, 0));
        PartitionDebugger debugger = new PartitionDebugger(coordinateConversion);

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

        OneShotPartitioner partitioner = new OneShotPartitioner(1.0, 0.1);
        partitioner.debugger = debugger;
        List<Set<VertexOfDualGraph>> partitions =
                partitioner.partition(simpleGraph, dualGraph, boundary, 2.0);

        assertTrue(partitions.size() >= 2, "Should split the four-face grid at maxWeight=2.0");
    }


    @Test
    void partitionsGridSplit5() {
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(5, 5, 1.0);
        CoordinateConversion coordinateConversion = new CoordinateConversion(new Point(0, 0));
        PartitionDebugger debugger = new PartitionDebugger(coordinateConversion);

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
        double maxWeight = 5.0;

        OneShotPartitioner partitioner = new OneShotPartitioner(1.0, 0.1);
        partitioner.debugger = debugger;
        List<Set<VertexOfDualGraph>> partition =
                partitioner.partition(simpleGraph, dualGraph, boundary, maxWeight);
        assertTrue(PartitionChecker.isConsistent(dualGraph, partition, maxWeight), "inconsistent partition");

        assertTrue(partition.size() >= 2, "Should split the four-face grid at maxWeight=2.0");
    }

    @Test
    void partitionsGridSplit1015() {
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(10, 15, 1.0);
        CoordinateConversion coordinateConversion = new CoordinateConversion(new Point(0, 0));
        PartitionDebugger debugger = new PartitionDebugger(coordinateConversion);

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
        double maxWeight = 5.0;

        OneShotPartitioner partitioner = new OneShotPartitioner(1.0, 0.1);
        partitioner.debugger = debugger;
        List<Set<VertexOfDualGraph>> partition =
                partitioner.partition(simpleGraph, dualGraph, boundary, maxWeight);
        debugger.dumpPartitionGeoJSON(partition, "partition_10x15");
        assertTrue(PartitionChecker.isConsistent(dualGraph, partition, maxWeight), "inconsistent partition");

        assertTrue(partition.size() >= 2, "Should split the four-face grid at maxWeight=2.0");
    }
}
