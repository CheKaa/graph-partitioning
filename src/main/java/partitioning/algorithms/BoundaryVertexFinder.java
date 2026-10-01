package partitioning.algorithms;

import graph.Graph;
import graph.Vertex;
import java.util.List;

public class BoundaryVertexFinder<T extends Vertex> {
    /**
     * Time Complexity: O(B^2) where B is the number of boundary vertices.
     */
    public int[] findMostDistantBoundaryVertices(Graph<T> graph, List<T> boundary) {
        if (boundary == null || boundary.isEmpty()) {
            return null;
        }

        int idx1 = 0;
        int idx2 = 0;
        double maxDist = -1;

        for (int i = 0; i < boundary.size(); i++) {
            T start = boundary.get(i);
            for (int j = i + 1; j < boundary.size(); j++) {
                T end = boundary.get(j);
                double dist = start.getLength(end);
                if (dist > maxDist) {
                    maxDist = dist;
                    idx1 = i;
                    idx2 = j;
                }
            }
        }

        return new int[]{idx1, idx2};
    }
}
