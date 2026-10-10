package graph;

import java.util.*;
import readWrite.PartitionDebugger;
import readWrite.CoordinateConversion;

public class DumpSpiralGraphTest {
    public static void main(String[] args) {
        int n = 10;
        int m = 10;
        Graph<Vertex> graph = TestGraphUtils.createGridGraph(n, m);
        
        // Spiral from top-left boundary
        TestGraphUtils.makeSpiralShortestPath(graph, 0, 0, 1000.0, 1.0);
        
        // Use PartitionDebugger to dump the graph as GeoJSON
        // Assuming a simple IdentityCoordinateConversion for Euclidean grid
        CoordinateConversion coordConv = new CoordinateConversion();
        
        PartitionDebugger debugger = new PartitionDebugger(coordConv);
        debugger.dumpOriginalGraphGeoJSON(graph, "spiral_graph.geojson");
        
        System.out.println("Graph dumped to spiral_graph.geojson");
    }
}
