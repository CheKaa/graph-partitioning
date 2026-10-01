package partitioning.algorithms;

import java.util.List;
import java.util.Set;

import graph.Vertex;
import graph.VertexOfDualGraph;

public class Cut {
    public final Set<VertexOfDualGraph> part1;
    public final Set<VertexOfDualGraph> part2;
    public final List<Vertex> cutPath;
    public final double length;
    public final double weight1;
    public final double weight2;
    public final double cost;

    public Cut(Set<VertexOfDualGraph> part1, Set<VertexOfDualGraph> part2, List<Vertex> cutPath, double length, double weight1, double weight2, double cost) {
        this.part1 = part1;
        this.part2 = part2;
        this.cutPath = cutPath;
        this.length = length;
        this.weight1 = weight1;
        this.weight2 = weight2;
        this.cost = cost;
    }
}
