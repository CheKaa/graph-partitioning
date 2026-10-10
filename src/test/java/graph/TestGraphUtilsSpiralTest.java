package graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TestGraphUtilsSpiralTest {
    @Test
    void createsOneSpiralRouteAcrossRectangularGridFromBoundaryVertices() {
        assertSpiralRoute(0, 0);
        assertSpiralRoute(3, 6);
        assertSpiralRoute(2, 0);
        assertSpiralRoute(3, 3);
        assertSpiralRoute(1, 6);
        assertSpiralRoute(0, 4);
    }

    private void assertSpiralRoute(int startX, int startY) {
        Graph<Vertex> graph = TestGraphUtils.createGridGraph(4, 7);
        double largeWeight = 1000.0;
        double smallWeight = 1.0;

        TestGraphUtils.makeSpiralShortestPath(graph, startX, startY, largeWeight, smallWeight);

        int smallEdgeCount = 0;
        for (Map.Entry<Vertex, Map<Vertex, Edge>> entry : graph.getEdges().entrySet()) {
            Vertex vertex = entry.getKey();
            for (Map.Entry<Vertex, Edge> edgeEntry : entry.getValue().entrySet()) {
                Vertex neighbor = edgeEntry.getKey();
                double length = edgeEntry.getValue().length;
                assertTrue(length == smallWeight || length == largeWeight);
                assertEquals(length, graph.getEdges().get(neighbor).get(vertex).length);
                if (vertex.getName() < neighbor.getName() && length == smallWeight) {
                    smallEdgeCount++;
                }
            }
        }
        assertEquals(graph.vertices().size() - 1, smallEdgeCount);

        Vertex start = graph.vertices().stream()
                .filter(vertex -> (int) vertex.x == startX && (int) vertex.y == startY)
                .findFirst()
                .orElseThrow();
        Set<Vertex> reached = new HashSet<>();
        ArrayDeque<Vertex> pending = new ArrayDeque<>();
        pending.add(start);
        reached.add(start);
        while (!pending.isEmpty()) {
            Vertex vertex = pending.remove();
            for (Map.Entry<Vertex, Edge> edgeEntry : graph.getEdges().get(vertex).entrySet()) {
                if (edgeEntry.getValue().length == smallWeight && reached.add(edgeEntry.getKey())) {
                    pending.add(edgeEntry.getKey());
                }
            }
        }
        assertEquals(graph.vertices().size(), reached.size());
    }
}
