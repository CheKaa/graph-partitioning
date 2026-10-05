package partitioning.algorithms;

import graph.*;
import graph.TestGraphUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class DualPartitioningTest {

    // @BeforeEach
    // void setup() {
    //     costFunction = new CostFunction(1.0, 1.0);
    // }

    private List<Vertex> getGridBoundary(Graph<Vertex> graph, int n, int m) {
        List<Vertex> boundary = new ArrayList<>();
        // Top: (0,0) -> (0, m-1)
        for (int j = 0; j < m; j++) boundary.add(findVertexByName(graph, 0, m, j));
        // Right: (1, m-1) -> (n-1, m-1)
        for (int i = 1; i < n; i++) boundary.add(findVertexByName(graph, n, m, i * m + (m - 1)));
        // Bottom: (n-1, m-2) -> (n-1, 0)
        for (int j = m - 2; j >= 0; j--) boundary.add(findVertexByName(graph, n, m, (n - 1) * m + j));
        // Left: (n-2, 0) -> (1, 0)
        for (int i = n - 2; i >= 1; i--) boundary.add(findVertexByName(graph, n, m, i * m));
        
        return boundary;
    }

    private Vertex findVertexByName(Graph<Vertex> graph, int n, int m, int id) {
        return graph.vertices().stream().filter(v -> v.getName() == id).findFirst().orElse(null);
    }

    @Test
    void testBoundaryVertexFinder() {
        Graph<Vertex> graph = TestGraphUtils.createGridGraph(3, 3);
        List<Vertex> boundary = getGridBoundary(graph, 3, 3);
        BoundaryVertexFinder<Vertex> finder = new BoundaryVertexFinder<>();
        int[] dist = finder.findMostDistantBoundaryVertices(graph, boundary);
        assertNotNull(dist);
        assertEquals(2, dist.length);
    }

    @Test
    void testMultiSourceSPT() {
        Graph<Vertex> graph = TestGraphUtils.createGridGraph(2, 2);
        List<Vertex> boundary = getGridBoundary(graph, 2, 2);
        Map<Vertex, Vertex> spt = MultiSourceSPT.computeSPTForest(graph, List.of(boundary.get(0)));
        assertNotNull(spt);
    }

    @Test
    void testSmallGridsPartitioning() {
        // TODO correct dual graph
        // TODO check for dual graph construction
        int[][] sizes = {{2, 2}, {3, 2}, {3, 3}};
        for (int[] size : sizes) {
            int n = size[0];
            int m = size[1];
            Graph<Vertex> graph = TestGraphUtils.createGridGraph(n, m);
            List<Vertex> boundary = getGridBoundary(graph, n, m);
            
            // Dummy dual graph for logic check
            Graph<VertexOfDualGraph> dualGraph = new Graph<>();
            VertexOfDualGraph face = new VertexOfDualGraph(100, new Vertex(100, 0, 0), 10.0, new ArrayList<>(boundary));
            dualGraph.addVertex(face);

            DualGraphPartitioner partitioner = new DualGraphPartitioner(100.0, 0.1);
            List<Set<VertexOfDualGraph>> result = partitioner.partition(graph, dualGraph, boundary, 10);
            assertNotNull(result);
        }
    }
}
