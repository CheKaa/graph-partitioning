package graph;

import java.util.HashMap;

public class SimpleDiGraph<T extends Vertex> extends Graph<T> {
    @Override
    public void addEdge(T begin, T end, double length) {
        // if (from.equals(to)) {
        //     throw new IllegalArgumentException("Self-loops are not allowed in SimpleDiGraph");
        // }
        edges.get(begin).put(end, new Edge(length));
    }

    @Override
    public T addVertex(T v) {
        edges.put(v, new HashMap<>());
        return v;
    }

    @Override 
    public void addEdge(T begin, T end, Edge edge) {
        edges.get(begin).put(end, edge);
    }


    @Override
    public void deleteEdge(T begin, T end) {
        // if (from.equals(to)) {
        //     throw new IllegalArgumentException("Self-loops are not allowed in SimpleDiGraph");
        // }
        edges.get(begin).remove(end);
    }

}
