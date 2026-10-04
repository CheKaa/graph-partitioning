package partitioning.algorithms;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import geometry.Point;

public class PartitionInit {
    private static final Logger logger = LoggerFactory.getLogger(PartitionInit.class);

    static List<Point> lines = Arrays.asList(
            new Point(0, 1),
            new Point(1, 0),
            new Point(1, 1),
            new Point(1, -1),
            new Point(1, 2),
            new Point(2, 1),
            new Point(-1, 2),
            new Point(2, -1)
    );

    /**
     * Выбирает вершину с экстремальной проекцией на линию (самую левую или правую)
     * @param vertices список вершин для выбора
     * @param excludeSet множество вершин, которые нужно исключить
     * @param line линия для проекции
     * @param selectMin true - выбрать минимум (самую левую), false - максимум (самую правую)
     * @return Optional с выбранной вершиной
     */
    private static Optional<VertexOfDualGraph> selectExtremumVertex(
            List<VertexOfDualGraph> vertices,
            Set<VertexOfDualGraph> excludeSet,
            Point line,
            boolean selectMin) {

        VertexOfDualGraph selectedVertex = null;
        double extremumProjection = selectMin ? Double.MAX_VALUE : -Double.MAX_VALUE;

        for (VertexOfDualGraph vertex : vertices) {
            // Пропускаем вершины из excludeSet
            if (excludeSet.contains(vertex)) {
                continue;
            }

            double projValue = calculateFaceProjection(vertex, line, selectMin);

            // Проверяем, является ли текущая проекция более экстремальной
            boolean isMoreExtreme = selectMin 
                    ? (projValue < extremumProjection) 
                    : (projValue > extremumProjection);

            if (isMoreExtreme) {
                extremumProjection = projValue;
                selectedVertex = vertex;
            }
        }

        return Optional.ofNullable(selectedVertex);
    }


    public static Entry<Set<VertexOfDualGraph>, Set<VertexOfDualGraph>> guess(
        Graph<VertexOfDualGraph> g,
        double sourceWeight,
        double sinkWeight
    ){
        var vertices = new ArrayList<>(g.vertices());
        List<Map.Entry<Point, Double>> linesByStretch = new ArrayList<>();
        for (Point direction : lines) {
            vertices.sort(Comparator.comparing(v -> direction.scalar(v)));
            var minV = vertices.getFirst();
            double minP = direction.scalar(minV)/direction.norm();
            var maxV = vertices.getLast();
            double maxP = direction.scalar(maxV)/direction.norm();
            linesByStretch.add(Map.entry(direction, maxP - minP));
        }
        linesByStretch.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<VertexOfDualGraph> boundaryCandidates = getBoundaryCandidatesBySingleFaceEdges(vertices);

        // Pick first line where sourceInit != sinkInit
        Point chosenLine = linesByStretch.get(0).getKey();
        VertexOfDualGraph sourceInitVertex = null;
        VertexOfDualGraph sinkInitVertex = null;
        for (Map.Entry<Point, Double> entry : linesByStretch) {
            Point direction = entry.getKey();
            VertexOfDualGraph src = selectExtremumVertex(boundaryCandidates, Set.of(), direction, true).orElse(null);
            if (src == null) continue;
            VertexOfDualGraph snk = selectExtremumVertex(boundaryCandidates, Set.of(src), direction, false).orElse(null);
            if (snk != null && !snk.equals(src)) {
                chosenLine = direction;
                sourceInitVertex = src;
                sinkInitVertex = snk;
                logger.debug("chosen line stretch={}: source={} sink={}", entry.getValue(), src.getName(), snk.getName());
                break;
            }
            logger.debug("line stretch={}: source==sink or sink null, trying next", entry.getValue());
        }

        Point finalChosenLine = chosenLine;
        vertices.sort(Comparator.comparing(v -> finalChosenLine.scalar(v)));

        int sourceInitIndex = vertices.indexOf(sourceInitVertex);
        int sinkInitIndex = vertices.indexOf(sinkInitVertex);

        HashSet<VertexOfDualGraph> sourceSet = selectVerticesForSet(vertices, sourceInitIndex, sourceWeight, Set.of(sinkInitVertex), g);
        logger.debug("source init={}, index={}, set size={}, weight={}", sourceInitVertex.getName(),
                sourceInitIndex, sourceSet.size(), sourceSet.stream().mapToDouble(VertexOfDualGraph::getWeight).sum());

        HashSet<VertexOfDualGraph> sinkSet = selectVerticesForSet(vertices, sinkInitIndex, sinkWeight, sourceSet, g);
        logger.debug("sink init={}, index={}, set size={}, weight={}", sinkInitVertex.getName(),
                sinkInitIndex, sinkSet.size(), sinkSet.stream().mapToDouble(VertexOfDualGraph::getWeight).sum());

        // Добираем вершины, достижимые только из одного множества
        expandSetsWithUnreachableRegions(g, sourceSet, sinkSet);

        return new AbstractMap.SimpleEntry<>(sourceSet, sinkSet);
    }

        /**
     * Вычисляет экстремальную проекцию грани на линию.
     * Если у грани есть вершины, берёт минимальную (для source) или максимальную (для sink) 
     * проекцию среди всех вершин грани.
     */
    private static double calculateFaceProjection(VertexOfDualGraph vertex, Point line, boolean selectMin) {
        ArrayList<Vertex> faceVertices = vertex.getVerticesOfFace();

        if (faceVertices == null || faceVertices.isEmpty()) {
            return line.scalar(vertex);
        }
        // TODO check no such exceptions

        // Для вершин с гранями берём экстремальную проекцию
        double extremumProjection = selectMin ? Double.MAX_VALUE : -Double.MAX_VALUE;
        for (Vertex v : faceVertices) {
            double projValue = line.scalar(v);
            
            if (selectMin) {
                extremumProjection = Math.min(extremumProjection, projValue);
            } else {
                extremumProjection = Math.max(extremumProjection, projValue);
            }
        }
        return extremumProjection;
    }

    private static List<VertexOfDualGraph> getBoundaryCandidatesBySingleFaceEdges(
            List<VertexOfDualGraph> vertices
    ) {
        Map<Map.Entry<Vertex, Vertex>, Integer> edgeFaceCount = countInternalFaceIncidences(vertices);

        List<VertexOfDualGraph> result = new ArrayList<>();

        for (VertexOfDualGraph face : vertices) {
            for (Map.Entry<Vertex, Vertex> edge : getFaceEdges(face)) {
                if (edgeFaceCount.getOrDefault(edge, 0) == 1) {
                    result.add(face);
                    break;
                }
            }
        }

        return result;
    }

    private static Map<Map.Entry<Vertex, Vertex>, Integer> countInternalFaceIncidences(
            Collection<VertexOfDualGraph> dualVertices
    ) {
        Map<Map.Entry<Vertex, Vertex>, Integer> edgeFaceCount = new HashMap<>();

        for (VertexOfDualGraph face : dualVertices) {
            for (Map.Entry<Vertex, Vertex> edge : getFaceEdges(face)) {
                edgeFaceCount.merge(edge, 1, Integer::sum);
            }
        }

        return edgeFaceCount;
    }

    private static List<Map.Entry<Vertex, Vertex>> getFaceEdges(VertexOfDualGraph face) {
        ArrayList<Vertex> vs = face.getVerticesOfFace();

        if (vs == null || vs.size() < 2) {
            return List.of();
        }

        List<Map.Entry<Vertex, Vertex>> edges = new ArrayList<>();

        for (int i = 0; i < vs.size(); i++) {
            Vertex a = vs.get(i);
            Vertex b = vs.get((i + 1) % vs.size());

            if (!a.equals(b)) {
                edges.add(Map.entry(a, b));
                edges.add(Map.entry(b, a));
            }
        }

        return edges;
    }


    public static HashSet<VertexOfDualGraph> selectVerticesForSet(
            List<VertexOfDualGraph> vertices,
            int startIndex,
            double targetWeight,
            Set<VertexOfDualGraph> sourceSet,
            Graph<VertexOfDualGraph> currentGraph
    ) {
        HashSet<VertexOfDualGraph> vertexSet = new HashSet<>();
        double currentWeight = 0;
        Queue<VertexOfDualGraph> queue = new LinkedList<>();
        VertexOfDualGraph startVertex = vertices.get(startIndex);
        queue.add(startVertex);

        while (!queue.isEmpty() && currentWeight < targetWeight) {
        	VertexOfDualGraph current = queue.poll();
            if (!vertexSet.contains(current) && !sourceSet.contains(current) && Math.abs(vertices.indexOf(current) - startIndex) < vertices.size() / 2) {
                vertexSet.add(current);
                currentWeight += current.getWeight();
                for (VertexOfDualGraph neighbor : currentGraph.getEdges().get(current).keySet()) {
                    if (!vertexSet.contains(neighbor) && !sourceSet.contains(neighbor)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return vertexSet;
    }

    /**
     * Расширяет sourceSet и sinkSet, добавляя вершины, достижимые только из одного множества.
     */
    private static void expandSetsWithUnreachableRegions(
            Graph<VertexOfDualGraph> graph,
            Set<VertexOfDualGraph> sourceSet,
            Set<VertexOfDualGraph> sinkSet) {
        // Находим вершины, достижимые только из sourceSet
        Set<VertexOfDualGraph> reachableFromSource = findReachableVertices(graph, sourceSet, sinkSet);
        Set<VertexOfDualGraph> onlyFromSource = new HashSet<>(reachableFromSource);
        onlyFromSource.removeAll(sourceSet);

        // Находим вершины, достижимые только из sinkSet
        Set<VertexOfDualGraph> reachableFromSink = findReachableVertices(graph, sinkSet, sourceSet);
        Set<VertexOfDualGraph> onlyFromSink = new HashSet<>(reachableFromSink);
        onlyFromSink.removeAll(sinkSet);

        // Исключаем вершины, достижимые из обоих множеств
        onlyFromSource.removeAll(reachableFromSink);
        onlyFromSink.removeAll(reachableFromSource);

        if (!onlyFromSource.isEmpty() || !onlyFromSink.isEmpty()) {
            logger.debug("Expanding sets: adding {} vertices only reachable from source, {} only from sink",
                    onlyFromSource.size(), onlyFromSink.size());
        }

        sourceSet.addAll(onlyFromSource);
        sinkSet.addAll(onlyFromSink);
    }

    /**
     * Находит все вершины, достижимые из startSet, не проходя через blockedSet
     */
    private static Set<VertexOfDualGraph> findReachableVertices(
            Graph<VertexOfDualGraph> graph,
            Set<VertexOfDualGraph> startSet,
            Set<VertexOfDualGraph> blockedSet) {

        Set<VertexOfDualGraph> reachable = new HashSet<>(startSet);
        Queue<VertexOfDualGraph> queue = new LinkedList<>(startSet);

        while (!queue.isEmpty()) {
            VertexOfDualGraph current = queue.poll();

            if (!graph.getEdges().containsKey(current)) {
                continue;
            }

            for (VertexOfDualGraph neighbor : graph.getEdges().get(current).keySet()) {
                if (blockedSet.contains(neighbor)) {
                    continue;
                }

                if (!reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return reachable;
    }
}
