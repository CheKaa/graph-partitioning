package graphPreparation;

import java.util.*;

import graph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MakingDualGraph {

   public Graph<VertexOfDualGraph> buildDualGraph(Graph<Vertex> gph) {
       Graph<VertexOfDualGraph> res = new Graph<>();
       EdgeOfGraph<Vertex>[] edgesList = gph.edgesArray();
       Map<Vertex, Integer> vertexInFaceNumber = gph.initVertexInFaceCounter();
       Map<EdgeOfGraph<Vertex>, VertexOfDualGraph> inFace = new HashMap<>();
       Map<Vertex, TreeSet<EdgeOfGraph<Vertex>>> sortedGraph = gph.arrangeByAngle();
       buildDualVertices(res, inFace, sortedGraph, edgesList, vertexInFaceNumber);
       addDualEdges(res, inFace);
       return res;
   }

   private void addDualEdges(
      Graph<VertexOfDualGraph> res,
      Map<EdgeOfGraph<Vertex>, VertexOfDualGraph> inFace
    ) {
        // TODO rewrite 
        EdgeOfGraph<Vertex> back;
        double oldLength;
        for (EdgeOfGraph<Vertex> edge : inFace.keySet()) {
            back = new EdgeOfGraph<>(edge.end, edge.begin, edge.length);
            if (inFace.get(edge).equals((inFace).get(back))) {
                continue;
            }
            oldLength = 0;
            if (res.getEdges().get(inFace.get(edge)).containsKey(inFace.get(back))) {
                oldLength = res.getEdges().get(inFace.get(edge)).get(inFace.get(back)).length;
                res.getEdges().get(inFace.get(edge)).remove(inFace.get(back));
                res.getEdges().get(inFace.get(back)).remove(inFace.get(edge));
            }
            res.getEdges().get(inFace.get(edge)).put(inFace.get(back), new Edge(oldLength + edge.length / 2));
            res.getEdges().get(inFace.get(back)).put(inFace.get(edge), new Edge(oldLength + edge.length / 2));
        }
    }

    private void buildDualVertices(
            Graph<VertexOfDualGraph> res,
            Map<EdgeOfGraph<Vertex>,
            VertexOfDualGraph> inFace,
            Map<Vertex, TreeSet<EdgeOfGraph<Vertex>>> sortedGraph,
            EdgeOfGraph<Vertex>[] edgesList,
            Map<Vertex, Integer> vertexInFaceNumber
    ) {
        //TODO strange method (C like)
        ArrayList<Vertex> verticesOfFace = new ArrayList<>();
        HashSet<EdgeOfGraph<Vertex>> inActualFace = new HashSet<>();
        long vertName = 0;
        for (EdgeOfGraph<Vertex> vertexEdgeOfGraph : edgesList) {
            if (inFace.containsKey(vertexEdgeOfGraph)) {
                //System.out.println("already in partition: " + edgesList[i].getBegin().getName() + "->" + edgesList[i].getEnd().getName());
                continue;
            }
            findFace(verticesOfFace, inActualFace, sortedGraph, vertexEdgeOfGraph, vertexInFaceNumber);
            assert verticesOfFace.size() >= 3: "Too little vertices" ;
            vertName++;
            //System.out.print(vertName + " ");
            VertexOfDualGraph vert = new VertexOfDualGraph(
                    vertName,
                    Vertex.findCenter(verticesOfFace),
                    VertexOfDualGraph.sumVertexWeight(verticesOfFace),
                    verticesOfFace
            );
            res.addVertex(vert);
            for (EdgeOfGraph<Vertex> edge : inActualFace) {
                inFace.put(edge, vert);
            }
            verticesOfFace.clear();
            inActualFace.clear();
        }
        //System.out.println("cnt_vert = " + cnt_vert);
    }

    public <T extends Vertex> void findFace(
            List<T> verticesOfFace,
            Set<EdgeOfGraph<T>> inActualFace,
            Map<T, TreeSet<EdgeOfGraph<T>>> sortedGraph,
            EdgeOfGraph<T> firstEdge,
            Map<T, Integer> vertexInFaceNumber
    ) {
        double faceWeight = 0;
        T prev = firstEdge.begin;
        T begin = firstEdge.end;

        vertexInFaceNumber.put(prev, vertexInFaceNumber.get(prev) + 1);
        vertexInFaceNumber.put(begin, vertexInFaceNumber.get(begin) + 1);
        faceWeight += prev.getWeight();
        faceWeight += begin.getWeight();
        inActualFace.add(firstEdge);
        EdgeOfGraph<T> actualEdge;
        do {
            if (sortedGraph.get(begin).isEmpty()) {
                return;
            }
            EdgeOfGraph<T> back = new EdgeOfGraph<>(begin, prev, begin.getLength(prev));
            actualEdge = sortedGraph.get(begin).higher(back);
            if (actualEdge == null) {
                actualEdge = sortedGraph.get(begin).first();
            }
            prev = actualEdge.begin;
            begin = actualEdge.end;
            verticesOfFace.add(begin);
            vertexInFaceNumber.put(begin, vertexInFaceNumber.get(begin) + 1);
            faceWeight = faceWeight + begin.getWeight();
            inActualFace.add(actualEdge);
        } while (!(begin.equals(firstEdge.end) && prev.equals(firstEdge.begin)));
   }

   public VertexOfDualGraph findExternalFace(Graph<VertexOfDualGraph> dualGraph) {
    //TODO currently do not work for encircled graphs where outerface has only one neighbour. Rewrite based on edges    
    Vertex leftTop = null;
       Vertex rightBottom = null;

       for (VertexOfDualGraph dualVertex : dualGraph.verticesArray()) {
           for (Vertex v : dualVertex.getVerticesOfFace()) {
               if (leftTop == null || (v.x < leftTop.x ||
                       (v.x == leftTop.x && v.y > leftTop.y))) {
                   leftTop = v;
               }
               if (rightBottom == null || (v.x > rightBottom.x ||
                       (v.x == rightBottom.x && v.y < rightBottom.y))) {
                   rightBottom = v;
               }
           }
       }

       if (leftTop == null || rightBottom == null) {
           throw new RuntimeException("Couldn't find external vertices");
       }

       VertexOfDualGraph externalFaceVertex = null;

       for (VertexOfDualGraph dualVertex : dualGraph.verticesArray()) {
           if (dualVertex.getVerticesOfFace().contains(leftTop) &&
                   dualVertex.getVerticesOfFace().contains(rightBottom)) {
               externalFaceVertex = dualVertex;
               break;
           }
       }

       if (externalFaceVertex == null) {
           throw new RuntimeException("Couldn't find external face");
       }
       return externalFaceVertex;
   }
}