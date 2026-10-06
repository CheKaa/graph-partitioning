package partitioning.entities;

import graph.Vertex;

/**
 * Pair
 */
public class Pair {
    public Vertex one;
    public Vertex two;

    public Pair(Vertex one, Vertex two){
        if (one.name > two.name){
            this.one = two;
            this.two = one;
        } else {
            this.one = one;
            this.two = two;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return java.util.Objects.equals(one.name, pair.one.name) && java.util.Objects.equals(two.name, pair.two.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(one.name, two.name);
    }
}
