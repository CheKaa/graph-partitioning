package partitioning.algorithms;

import graph.Edge;
import graph.Graph;
import graph.SimpleDiGraph;
import graph.Vertex;

import java.util.Collections;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

import readWrite.CoordinateConversion;
import readWrite.PartitionDebugger;


final class BoundaryHandler extends SimpleDiGraph<Vertex> {
    private final Graph<Vertex> baseGraph;
    private final HashMap<Vertex, Vertex> baseBoundary; // Maps each vertex to its next vertex in the boundary in clockwise order
    private double length = 0.0;
    private int edgeVertexDiff = 0;
    private int boundToNonBoundEdges = 0;

    BoundaryHandler(Graph<Vertex> graph) {
        this.baseGraph = graph;
        this.baseBoundary = new HashMap<>();
    }

    BoundaryHandler(Graph<Vertex> graph, List<Vertex> boundary) {
        this.baseGraph = graph;
        this.baseBoundary = new HashMap<>();
        Vertex curr = boundary.get(boundary.size() - 1);
        for (int i = 0; i < boundary.size(); i++) {
            Vertex next = boundary.get(i);
            baseBoundary.put(curr, next);
            curr = next;
        }
    }

    void addVertices(List<Vertex> vertices) {
        processVertices(vertices, true);
    }

    void removeVertices(List<Vertex> vertices) {
        processVertices(vertices, false);
    }

    double length() {
        return length;
    }

    Map<Vertex, Map<Vertex, Edge>> edges() {
        return Collections.unmodifiableMap(edges);
    }

    private void processVertices(List<Vertex> vertices, boolean adding) {
        if (vertices.isEmpty()) {
            return;
        }
        PartitionDebugger debug = new PartitionDebugger(new CoordinateConversion());

        Vertex curr = vertices.get(vertices.size() - 1);
        // int contactParts = 0;
        // boolean inCo
        for (int i = 0; i < vertices.size(); i++) {
            Vertex next = vertices.get(i);
            if (curr == next) {
                continue;
            }
            
            if (baseBoundary.containsKey(curr)) {
                if (baseBoundary.get(curr) == next) { // edge could be only in one direction
                    curr = next;
                    continue;
                } 
            }
            
            Vertex start;
            Vertex end;

            if (adding) {
                start = curr;
                end = next;
            } else {
                start = next;
                end = curr;
            }
            boolean added = processEdge(start, end);

            curr = next;
        }

        clearZeroDegreeVertices(vertices);
    }

    private boolean processEdge(Vertex from, Vertex to) {
        var neibs = edges.get(to);
        Edge edge = baseGraph.getEdge(from, to);
        boolean startOnboundary = baseBoundary.containsKey(from);
        boolean endOnboundary = baseBoundary.containsKey(to);

        if (neibs != null && neibs.containsKey(from)) {
            deleteEdge(to, from);
            length -= edge.length;
            edgeVertexDiff -= 1;
            boundToNonBoundEdges += endOnboundary & !startOnboundary ? -1 : 0;
            return false;
        } else {
            if (!edges.containsKey(from)) {
                edges.put(from, new HashMap<>());
                edgeVertexDiff -= 1;
            }
            addEdge(from, to, edge);
            length += edge.length;
            edgeVertexDiff += 1;
            boundToNonBoundEdges += startOnboundary & !endOnboundary ? 1 : 0;
            return true;
        }
    }

    private void clearZeroDegreeVertices(List<Vertex> vertices) {
        for (Vertex v : vertices) {
            if (edges.containsKey(v) && edges.get(v).isEmpty()) {
                edges.remove(v);
                edgeVertexDiff += 1;
            }
        }
    }

    public boolean simpleCut(){
        return edgeVertexDiff == 0 && boundToNonBoundEdges == 1;
    }
}
