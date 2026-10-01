package partitioning.algorithms;

import graph.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class DualPartitioningTest {
    private CostFunction costFunction;

    @BeforeEach
    void setup() {
        costFunction = new CostFunction(1.0, 1.0);
    }

    private Graph<Vertex> createGridGraph(int n, int m) {
        Graph<Vertex> graph = new Graph<>();
        Vertex[][] vertices = new Vertex[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                vertices[i][j] = new Vertex((long) (i * m + j), i, j);
                graph.addVertex(vertices[i][j]);
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (i + 1 < n) graph.addEdge(vertices[i][j], vertices[i + 1][j], 1.0);
                if (j + 1 < m) graph.addEdge(vertices[i][j], vertices[i][j + 1], 1.0);
            }
        }
        return graph;
    }

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
        Graph<Vertex> graph = createGridGraph(3, 3);
        List<Vertex> boundary = getGridBoundary(graph, 3, 3);
        BoundaryVertexFinder<Vertex> finder = new BoundaryVertexFinder<>();
        int[] dist = finder.findMostDistantBoundaryVertices(graph, boundary);
        assertNotNull(dist);
        assertEquals(2, dist.length);
    }

    @Test
    void testMultiSourceSPT() {
        Graph<Vertex> graph = createGridGraph(2, 2);
        List<Vertex> boundary = getGridBoundary(graph, 2, 2);
        Map<Vertex, Vertex> spt = MultiSourceSPT.computeSPTForest(graph, List.of(boundary.get(0)));
        assertNotNull(spt);
    }

    @Test
    void testSmallGridsPartitioning() {
        int[][] sizes = {{2, 2}, {3, 2}, {3, 3}};
        for (int[] size : sizes) {
            int n = size[0];
            int m = size[1];
            Graph<Vertex> graph = createGridGraph(n, m);
            List<Vertex> boundary = getGridBoundary(graph, n, m);
            
            // Dummy dual graph for logic check
            Graph<VertexOfDualGraph> dualGraph = new Graph<>();
            VertexOfDualGraph face = new VertexOfDualGraph(100, new Vertex(100, 0, 0), 10.0, new ArrayList<>(boundary));
            dualGraph.addVertex(face);

            DualGraphPartitioner partitioner = new DualGraphPartitioner(100.0, costFunction);
            List<Set<VertexOfDualGraph>> result = partitioner.partition(graph, dualGraph, boundary);
            assertNotNull(result);
        }
    }
}
