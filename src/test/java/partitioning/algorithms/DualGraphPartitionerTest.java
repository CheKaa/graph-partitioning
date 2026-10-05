package partitioning.algorithms;

import graph.*;
import graph.TestGraphUtils;
import graphPreparation.MakingDualGraph;
import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import geometry.Point;

public class DualGraphPartitionerTest {
    @Test
    public void testDualGraphDumps() {
        // 1. Create a Grid Graph
        Graph<Vertex> simpleGraph = TestGraphUtils.createGridGraph(3, 3);

        // 2. Create a Dual Graph using MakingDualGraph
        MakingDualGraph dualGraphBuilder = new MakingDualGraph();
        Graph<VertexOfDualGraph> dualGraph = dualGraphBuilder.buildDualGraph(simpleGraph);

        // 3. Coordinate Conversion with base (0,0)
        CoordinateConversion coordConv = new CoordinateConversion(new Point(0, 0));
        PartitionDebugger debugger = new PartitionDebugger(coordConv);

        // 4. Dump graphs
        debugger.dumpOriginalGraphGeoJSON(simpleGraph, "test_grid_original");
        debugger.dumpDualGraphGeoJSON(dualGraph, "test_grid_dual");

        System.out.println("Test dumps completed in src/main/output/debug/");
        assertNotNull(simpleGraph);
        assertNotNull(dualGraph);
    }
}
