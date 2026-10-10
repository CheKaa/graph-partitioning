package graph;

import java.util.*;

public class TestGraphUtils {
    public static Graph<Vertex> createGridGraph(int n, int m) {
        return createGridGraph(n, m, 1.0);
    }

    public static Graph<Vertex> createGridGraph(int n, int m, double edgeLength) {
        Graph<Vertex> graph = new Graph<>();
        Vertex[][] vertices = new Vertex[n][m];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                vertices[i][j] = new Vertex((long) (i * m + j), i , j);
                graph.addVertex(vertices[i][j]);
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (i + 1 < n) graph.addEdge(vertices[i][j], vertices[i + 1][j], edgeLength);
                if (j + 1 < m) graph.addEdge(vertices[i][j], vertices[i][j + 1], edgeLength);
            }
        }
        return graph;
    }

    /**
     * Sets edge lengths such that a spiral path starting from a boundary point is the shortest path.
     * This is achieved by assigning very small weights to edges on the spiral and very large weights elsewhere.
     *
     * @param graph The grid graph to modify.
     * @param startX The starting x-coordinate of the spiral.
     * @param startY The starting y-coordinate of the spiral.
     * @param largeWeight The weight for edges not on the spiral.
     * @param smallWeight The weight for edges on the spiral.
     */
    public static void makeSpiralShortestPath(Graph<Vertex> graph, int startX, int startY, double largeWeight, double smallWeight) {
        Map<Long, Vertex> verticesByName = verticesByName(graph);
        int xSize = graph.vertices().stream().mapToInt(v -> (int) v.x).max().orElse(-1) + 1;
        int ySize = graph.vertices().stream().mapToInt(v -> (int) v.y).max().orElse(-1) + 1;

        validateSpiralRequest(graph, verticesByName, xSize, ySize, startX, startY, largeWeight, smallWeight);

        Map<Vertex, Map<Vertex, Edge>> allEdges = graph.getEdges();
        for (Map<Vertex, Edge> neighbors : allEdges.values()) {
            for (Edge edge : neighbors.values()) {
                edge.length = largeWeight;
            }
        }

        List<Vertex> spiral = buildSpiralPath(verticesByName, xSize, ySize, startX, startY);
        for (int i = 1; i < spiral.size(); i++) {
            Vertex previous = spiral.get(i - 1);
            Vertex current = spiral.get(i);
            Edge forward = allEdges.get(previous).get(current);
            Edge reverse = allEdges.get(current).get(previous);
            if (forward == null || reverse == null) {
                throw new IllegalArgumentException("Spiral has a non-edge between " + previous + " and " + current);
            }
            forward.length = smallWeight;
            reverse.length = smallWeight;
        }
    }

    private static Map<Long, Vertex> verticesByName(Graph<Vertex> graph) {
        Map<Long, Vertex> verticesByName = new HashMap<>();
        for (Vertex vertex : graph.vertices()) {
            verticesByName.put(vertex.getName(), vertex);
        }
        return verticesByName;
    }

    private static void validateSpiralRequest(
            Graph<Vertex> graph, Map<Long, Vertex> verticesByName,
            int xSize, int ySize, int startX, int startY,
            double largeWeight, double smallWeight) {
        if (graph.vertices().isEmpty()) {
            throw new IllegalArgumentException("Cannot spiral an empty graph");
        }
        if (!Double.isFinite(largeWeight) || !Double.isFinite(smallWeight)
                || smallWeight <= 0 || largeWeight <= 0) {
            throw new IllegalArgumentException("Edge lengths must be finite and positive");
        }
        if (largeWeight <= (graph.vertices().size() - 1) * smallWeight) {
            throw new IllegalArgumentException("Large edge length must exceed the longest spiral-path prefix");
        }

        Vertex start = verticesByName.get((long) startX * ySize + startY);
        boolean onBoundary = startX == 0 || startX == xSize - 1
                || startY == 0 || startY == ySize - 1;
        if (!onBoundary) {
            throw new IllegalArgumentException("The spiral must start at a grid boundary vertex");
        }
        if (start == null || (int) start.x != startX || (int) start.y != startY) {
            throw new IllegalArgumentException("No grid vertex exists at the starting coordinate");
        }
    }

    private static List<Vertex> buildSpiralPath(
            Map<Long, Vertex> verticesByName, int xSize, int ySize, int startX, int startY) {
        int minX = 0;
        int maxX = xSize - 1;
        int minY = 0;
        int maxY = ySize - 1;
        List<Vertex> spiral = new ArrayList<>(verticesByName.size());
        Vertex entry = vertexAt(verticesByName, ySize, startX, startY);

        while (minX <= maxX && minY <= maxY) {
            List<Vertex> ring = perimeter(verticesByName, ySize, minX, maxX, minY, maxY);
            int entryIndex = ring.indexOf(entry);

            // Rotate this perimeter so the route enters at the requested point.
            for (int i = 0; i < ring.size(); i++) {
                spiral.add(ring.get((entryIndex + i) % ring.size()));
            }

            if (minX + 1 > maxX - 1 || minY + 1 > maxY - 1) {
                break;
            }

            entry = inwardNeighbor(ring.get((entryIndex + ring.size() - 1) % ring.size()),
                    minX, maxX, minY, maxY, verticesByName, ySize);
            minX++;
            maxX--;
            minY++;
            maxY--;
        }
        return spiral;
    }

    private static Vertex inwardNeighbor(
            Vertex ringEnd, int minX, int maxX, int minY, int maxY,
            Map<Long, Vertex> verticesByName, int ySize) {
        if ((int) ringEnd.y == minY) {
            return vertexAt(verticesByName, ySize, (int) ringEnd.x, minY + 1);
        }
        if ((int) ringEnd.x == maxX) {
            return vertexAt(verticesByName, ySize, maxX - 1, (int) ringEnd.y);
        }
        if ((int) ringEnd.y == maxY) {
            return vertexAt(verticesByName, ySize, (int) ringEnd.x, maxY - 1);
        }
        return vertexAt(verticesByName, ySize, minX + 1, (int) ringEnd.y);
    }

    private static Vertex vertexAt(Map<Long, Vertex> verticesByName, int ySize, int x, int y) {
        Vertex vertex = verticesByName.get((long) x * ySize + y);
        if (vertex == null) {
            throw new IllegalArgumentException("Grid is missing a vertex at (" + x + ", " + y + ")");
        }
        return vertex;
    }

    private static List<Vertex> perimeter(
            Map<Long, Vertex> verticesByName, int ySize,
            int minX, int maxX, int minY, int maxY) {
        List<Vertex> ring = new ArrayList<>();
        if (minY == maxY) {
            for (int x = minX; x <= maxX; x++) {
                ring.add(vertexAt(verticesByName, ySize, x, minY));
            }
        } else if (minX == maxX) {
            for (int y = minY; y <= maxY; y++) {
                ring.add(vertexAt(verticesByName, ySize, minX, y));
            }
        } else {
            for (int x = minX; x <= maxX; x++) {
                ring.add(vertexAt(verticesByName, ySize, x, minY));
            }
            for (int y = minY + 1; y <= maxY; y++) {
                ring.add(vertexAt(verticesByName, ySize, maxX, y));
            }
            for (int x = maxX - 1; x >= minX; x--) {
                ring.add(vertexAt(verticesByName, ySize, x, maxY));
            }
            for (int y = maxY - 1; y > minY; y--) {
                ring.add(vertexAt(verticesByName, ySize, minX, y));
            }
        }
        return ring;
    }
}
