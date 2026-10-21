package partitioning.algorithms;

import graph.Graph;
import graph.TestGraphUtils;
import graph.Vertex;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BoundaryHandlerTest {
    @Test
    void cornerFaceIsASimpleCut() {
        GridFixture grid = createGridFixture();
        BoundaryHandler boundary = grid.newBoundary();

        boundary.addVertices(grid.face(0, 0));

        assertTrue(boundary.simpleCut());
        assertEquals(2.0, boundary.length());
    }

    @Test
    void disconnectedCenterFaceMakesCornerCutNonSimpleUntilJoined() {
        GridFixture grid = createGridFixture();
        BoundaryHandler boundary = grid.newBoundary();

        boundary.addVertices(grid.face(0, 0));
        assertTrue(boundary.simpleCut());

        boundary.addVertices(grid.face(2, 2));
        assertFalse(boundary.simpleCut(), "The separate central face creates a disconnected boundary");

        boundary.addVertices(grid.face(2, 1));
        assertFalse(boundary.simpleCut(), "The central face and its left neighbor remain detached");

        boundary.addVertices(grid.face(0, 1));
        boundary.addVertices(grid.face(0, 2));
        boundary.addVertices(grid.face(1, 2));
        assertTrue(boundary.simpleCut(), "The added face chain joins the center back to the corner");
    }

    @Test
    void ringOfFacesThatSeparatesTheUnselectedCenterIsNotSimpleCut() {
        GridFixture grid = createGridFixture();
        BoundaryHandler boundary = grid.newBoundary();

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row == 2 || column == 2) {
                    boundary.addVertices(grid.face(row, column));
                }
            }
        }

        assertFalse(boundary.simpleCut(), "The ring leaves the central face disconnected from the exterior");
    }

    @Test
    void adjacentFacesUpdatePerimeterAndCanBeRemovedInReverse() {
        GridFixture grid = createGridFixture();
        BoundaryHandler boundary = grid.newBoundary();
        List<Vertex> firstFace = grid.face(0, 0);
        List<Vertex> adjacentFace = grid.face(0, 1);

        boundary.addVertices(firstFace);
        assertEquals(2.0, boundary.length());

        boundary.addVertices(adjacentFace);
        assertEquals(3.0, boundary.length());

        boundary.removeVertices(adjacentFace);
        assertEquals(2.0, boundary.length());

        boundary.removeVertices(firstFace);
        assertEquals(0.0, boundary.length());
        assertTrue(boundary.edges().isEmpty());
    }

    private GridFixture createGridFixture() {
        Graph<Vertex> graph = TestGraphUtils.createGridGraph(5, 5);
        Vertex[][] vertices = new Vertex[5][5];
        for (Vertex vertex : graph.vertices()) {
            vertices[(int) vertex.x][(int) vertex.y] = vertex;
        }

        List<Vertex> outerBoundary = new ArrayList<>();
        for (int column = 0; column < 5; column++) {
            outerBoundary.add(vertices[0][column]);
        }
        for (int row = 1; row < 5; row++) {
            outerBoundary.add(vertices[row][4]);
        }
        for (int column = 3; column >= 0; column--) {
            outerBoundary.add(vertices[4][column]);
        }
        for (int row = 3; row > 0; row--) {
            outerBoundary.add(vertices[row][0]);
        }

        return new GridFixture(graph, vertices, outerBoundary);
    }

    private record GridFixture(Graph<Vertex> graph, Vertex[][] vertices, List<Vertex> outerBoundary) {
        BoundaryHandler newBoundary() {
            return new BoundaryHandler(graph, outerBoundary);
        }

        List<Vertex> face(int row, int column) {
            return List.of(
                    vertices[row][column],
                    vertices[row][column + 1],
                    vertices[row + 1][column + 1],
                    vertices[row + 1][column]);
        }
    }
}
