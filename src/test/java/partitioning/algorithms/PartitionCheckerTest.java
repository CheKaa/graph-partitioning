package partitioning.algorithms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import graph.Graph;
import graph.VertexOfDualGraph;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PartitionCheckerTest {
    private final PartitionChecker checker = new PartitionChecker();

    @Test
    void acceptsCompleteDisjointConnectedPartitionsWithinWeightLimit() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        List<Set<VertexOfDualGraph>> partitions = List.of(
                Set.of(face(graph, 1), face(graph, 2)),
                Set.of(face(graph, 3)));

        assertTrue(checker.check(graph, partitions, 2.0).isEmpty());
    }

    @Test
    void reportsUncoveredFace() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        List<String> errors = checker.check(
                graph, List.of(Set.of(face(graph, 1), face(graph, 2))), 2.0);

        assertTrue(errors.stream().anyMatch(error -> error.contains("not covered")));
    }

    @Test
    void reportsFaceAssignedToMultipleParts() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        List<String> errors = checker.check(graph, List.of(
                Set.of(face(graph, 1), face(graph, 2)),
                Set.of(face(graph, 2), face(graph, 3))), 2.0);

        assertTrue(errors.stream().anyMatch(error -> error.contains("occurs in partitions")));
    }

    @Test
    void reportsDisconnectedPart() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        List<String> errors = checker.check(
                graph, List.of(Set.of(face(graph, 1), face(graph, 3)), Set.of(face(graph, 2))), 2.0);

        assertTrue(errors.stream().anyMatch(error -> error.contains("is not connected")));
    }

    @Test
    void reportsPartOverMaximumWeight() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        List<String> errors = checker.check(
                graph, List.of(Set.of(face(graph, 1), face(graph, 2)), Set.of(face(graph, 3))), 1.5);

        assertTrue(errors.stream().anyMatch(error -> error.contains("exceeds maxWeight")));
    }

    @Test
    void booleanConvenienceMethodReflectsConsistency() {
        Graph<VertexOfDualGraph> graph = createDualPath();
        assertFalse(checker.isConsistent(graph, List.of(Set.of(face(graph, 1))), 3.0));
    }

    private Graph<VertexOfDualGraph> createDualPath() {
        Graph<VertexOfDualGraph> graph = new Graph<>();
        VertexOfDualGraph first = new VertexOfDualGraph(1, 0, 0, 1.0);
        VertexOfDualGraph second = new VertexOfDualGraph(2, 1, 0, 1.0);
        VertexOfDualGraph third = new VertexOfDualGraph(3, 2, 0, 1.0);
        graph.addEdge(first, second, 1.0);
        graph.addEdge(second, third, 1.0);
        return graph;
    }

    private VertexOfDualGraph face(Graph<VertexOfDualGraph> graph, long name) {
        return graph.vertices().stream()
                .filter(face -> face.getName() == name)
                .findFirst()
                .orElseThrow();
    }
}
