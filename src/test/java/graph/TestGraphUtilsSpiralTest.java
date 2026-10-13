package graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TestGraphUtilsSpiralTest {
    @Test
    void setsDualVertexWeightsToAbsoluteFaceAreas() {
        Graph<VertexOfDualGraph> dualGraph = new Graph<>();
        VertexOfDualGraph counterClockwiseFace = face(1,
                new Vertex(1, 0, 0), new Vertex(2, 2, 0), new Vertex(3, 0, 2));
        VertexOfDualGraph clockwiseFace = face(2,
                new Vertex(4, 0, 0), new Vertex(5, 0, 2), new Vertex(6, 2, 0));
        dualGraph.addVertex(counterClockwiseFace);
        dualGraph.addVertex(clockwiseFace);

        TestGraphUtils.setDualGraphWeightsToFaceAreas(dualGraph);

        assertEquals(2.0, counterClockwiseFace.getWeight());
        assertEquals(2.0, clockwiseFace.getWeight());
    }

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

    private static VertexOfDualGraph face(long name, Vertex... vertices) {
        List<Vertex> faceVertices = List.of(vertices);
        return new VertexOfDualGraph(name, new Vertex(name), 0.0, new ArrayList<>(faceVertices));
    }
}
