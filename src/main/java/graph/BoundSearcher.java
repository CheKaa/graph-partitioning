package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class BoundSearcher {
    // Find boundary of given set of faces of planar graph in clockwise order 
    public static List<Vertex> findBound(
            Graph<Vertex> graph,
            Set<VertexOfDualGraph> part
    ) {

        final int partSize = part.size();
        final int estimatedVertices = partSize * 4;

        List<VertexOfDualGraph> orderedFaces = new ArrayList<>(part);
        List<List<Vertex>> verticesByFaces = new ArrayList<>(partSize);
        List<Map<Vertex, Integer>> vertexPositionInFace = new ArrayList<>(partSize);
        Map<Vertex, Integer> numberOfFaces = new HashMap<>(estimatedVertices);
        Map<Long, Integer> edgeToFaceIndex = new HashMap<>(estimatedVertices * 2);
        Set<Vertex> allVertices = new HashSet<>(estimatedVertices);

        for (int i = 0; i < partSize; i++) {
            List<Vertex> faceVertices = orderedFaces.get(i).getVerticesOfFace();
            verticesByFaces.add(faceVertices);

            final int faceSize = faceVertices.size();
            HashMap<Vertex, Integer> posMap = new HashMap<>(faceSize + faceSize / 3); // load factor

            for (int j = 0; j < faceSize; j++) {
                Vertex current = faceVertices.get(j);
                numberOfFaces.merge(current, 1, Integer::sum);
                edgeToFaceIndex.put(computeEdgeKey(current, faceVertices.get((j + 1) % faceSize)), i);
                allVertices.add(current);
                posMap.put(current, j);
            }
            vertexPositionInFace.add(posMap);
        }

        //Assertions.assertTrue(graph.isConnected());
        Graph<Vertex> partSubgraph = graph.createSubgraphFromFaces(verticesByFaces);

        Map<Vertex, TreeSet<EdgeOfGraph<Vertex>>> arrangedEdges = partSubgraph.arrangeByAngle();

        Vertex start = findLeftmostVertex(allVertices);
        List<Vertex> bound = new ArrayList<>(allVertices.size());
        bound.add(start);

        EdgeOfGraph<Vertex> startEdge = findMaxEdgeLessThanPiOver2(arrangedEdges.get(start));

        assert ((0 <= startEdge.getAngle() && startEdge.getAngle() < Math.PI / 2.0) ||
                (3.0 * Math.PI / 2.0) <= startEdge.getAngle() && startEdge.getAngle() < 2 * Math.PI);

        EdgeOfGraph<Vertex> prevEdge = new EdgeOfGraph<>(startEdge.end, startEdge.begin, 0);
        Vertex current = startEdge.end;

        int faceIndex = findCommonFaceFast(startEdge.begin, startEdge.end, edgeToFaceIndex);

        while (!current.equals(start)) {
            bound.add(current);
            Vertex next;

            if (numberOfFaces.get(current) > 1) {
                EdgeOfGraph<Vertex> edge = findNextEdge(prevEdge, arrangedEdges.get(current));
                assert edge != null;
                faceIndex = findCommonFaceFast(edge.begin, edge.end, edgeToFaceIndex);
                next = edge.end;
            } else {
                List<Vertex> face = verticesByFaces.get(faceIndex);
                int currentPos = vertexPositionInFace.get(faceIndex).get(current);
                next = face.get((currentPos + 1) % face.size());
            }

            prevEdge = new EdgeOfGraph<>(next, current, 0);
            current = next;
        }

        return bound;
    }

    private static long computeEdgeKey(Vertex v1, Vertex v2) {
        long a = v1.name;
        long b = v2.name;
        return ((a + b) * (a + b + 1) / 2) + b;
    }

    private static int findCommonFaceFast(Vertex v1, Vertex v2,
                                          Map<Long, Integer> edgeToFaceIndex) {
        long key = computeEdgeKey(v1, v2);
        Integer faceIndex = edgeToFaceIndex.get(key);

        if (faceIndex == null) {
            key = computeEdgeKey(v2, v1);
            faceIndex = edgeToFaceIndex.get(key);
        }

        if (faceIndex == null) {
            throw new RuntimeException("Can't find common face for edge " +
                    v1.getName() + " -> " + v2.getName());
        }
        return faceIndex;
    }

    private static EdgeOfGraph<Vertex> findNextEdge(EdgeOfGraph<Vertex> prevEdge, TreeSet<EdgeOfGraph<Vertex>> orderedEdges) {
        if (orderedEdges.isEmpty()) {
            return null;
        }
        EdgeOfGraph<Vertex> result = orderedEdges.lower(prevEdge);
        return result == null ? orderedEdges.last() : result;
    }

    private static Vertex findLeftmostVertex(Set<Vertex> partition) {
        Vertex leftmost = null;
        for (Vertex v : partition) {
            if (leftmost == null || v.x < leftmost.x ||
                    (v.x == leftmost.x &&
                            v.y > leftmost.y)) {
                leftmost = v;
            }
        }
        return leftmost;
    }

    private static EdgeOfGraph<Vertex> findMaxEdgeLessThanPiOver2(TreeSet<EdgeOfGraph<Vertex>> sortedEdges) {
        EdgeOfGraph<Vertex> bestEdge = null;
        double bestAngle = -1;

        for (EdgeOfGraph<Vertex> edge : sortedEdges) {
            double angle = edge.getAngle();

            if (angle < Math.PI / 2 && angle > bestAngle) {
                bestAngle = angle;
                bestEdge = edge;
            }
        }
        return bestEdge == null? sortedEdges.last() : bestEdge;
    }

}