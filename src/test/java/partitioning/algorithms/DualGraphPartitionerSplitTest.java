package partitioning.algorithms;

import graph.*;
import graphPreparation.MakingDualGraph;
import graphPreparation.NestedFacesRemover;
import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import geometry.Point;
import java.util.*;

public class DualGraphPartitionerSplitTest {
    @Test
    public void testSimpleSplit() {
        // 1. Create a Grid Graph (3x3)
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(3, 3, 1.0);
        
        CoordinateConversion coordConv = new CoordinateConversion(new Point(0, 0));
        PartitionDebugger debugger = new PartitionDebugger(coordConv);
        debugger.dumpOriginalGraphGeoJSON(simpleGraph, "test_grid_3");

        // 2. Create a Dual Graph
        MakingDualGraph dualGraphBuilder = new MakingDualGraph();
        Graph<VertexOfDualGraph> dualGraph = dualGraphBuilder.buildDualGraph(simpleGraph);
        VertexOfDualGraph externalFaceVertex = dualGraphBuilder.findExternalFace(dualGraph);

        Set<VertexOfDualGraph> removedNestedFaces = NestedFacesRemover.removeNestedFaces(dualGraph, externalFaceVertex);

        // if (!removedNestedFaces.isEmpty()) {
        //     logger.info("Dual graph weight after removing nested faces: {}", dualGraph.verticesSumWeight());
        //     assert dualGraph.isConnected();
        // }
        dualGraph.deleteVertex(externalFaceVertex);

        // Ensure dual graph vertices have weight 1.0
        for (VertexOfDualGraph v : dualGraph.vertices()) {
            v.setWeight(1.0);
        }
        
        // 4. Initialize Partitioner
        // lengthPriority: 1.0, minRelPartSize: 0.1
        DualGraphPartitioner partitioner = new DualGraphPartitioner(1.0, 0.1);
        partitioner.debugger = debugger;
        
        // We want to split the graph. For a 3x3 grid, there are 4 dual vertices (faces).
        // Total dual weight = 4.0. Let's set maxWeight to 2.0 to force a split.
        double maxWeight = 2.0;
        
        // The partition method needs the initial boundary. 
        // In our grid, the outer boundary is the boundary of the union of all faces.
        // We can use the same logic as DualGraphPartitioner.extractInitialBoundary
        Set<VertexOfDualGraph> allFaces = new HashSet<>(dualGraph.vertices());
        List<Vertex> boundary = BoundSearcher.findBound(simpleGraph, allFaces);
        
        assertNotNull(boundary, "Boundary should not be null");
        assertFalse(boundary.isEmpty(), "Boundary should not be empty");

        // 5. Execute partitioning
        List<Set<VertexOfDualGraph>> partitions = partitioner.partition(simpleGraph, dualGraph, boundary, maxWeight);

        System.out.println("Number of partitions: " + partitions.size());
        for (int i = 0; i < partitions.size(); i++) {
            System.out.println("Partition " + i + " size: " + partitions.get(i).size());
        }

        // For total weight 4.0 and maxWeight 2.0, we expect at least 2 partitions.
        assertTrue(partitions.size() >= 2, "Should have at least 2 partitions for maxWeight=2.0");
    }
}
