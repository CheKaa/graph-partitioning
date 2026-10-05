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
                vertices[i][j] = new Vertex((long) (i * m + j), (int)(i * edgeLength), (int)(j * edgeLength));
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
}
