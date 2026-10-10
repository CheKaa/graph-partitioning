package readWrite;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartitionDebugger {
    private static final Logger logger = LoggerFactory.getLogger(PartitionDebugger.class);
    private final CoordinateConversion coordConv;

    public PartitionDebugger(CoordinateConversion coordConv) {
        this.coordConv = coordConv;
    }

    public void dumpVertices(Collection<Vertex> vertices, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        boolean first = true;
        for (Vertex vertex : vertices) {
            if (!first) sb.append(",");
            Vertex geoVertex = coordConv.fromEuclidean(vertex);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "%f, %f", geoVertex.x, geoVertex.y));
            sb.append("]}, \"properties\": {\"name\": ").append(vertex.getName())
                    .append(", \"type\": \"graph_vertex\"}}");
            first = false;
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpPartitionGeoJSON(Collection<VertexOfDualGraph> partition, int partId, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        boolean firstFeature = true;

        for (VertexOfDualGraph face : partition) {
            List<Vertex> faceVertices = face.getVerticesOfFace();
            if (faceVertices.size() < 3) {
                throw new IllegalArgumentException("Partition face " + face.getName() + " has fewer than 3 vertices");
            }

            if (!firstFeature) sb.append(",");
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"Polygon\", \"coordinates\": [[");
            for (int i = 0; i <= faceVertices.size(); i++) {
                if (i > 0) sb.append(", ");
                Vertex vertex = faceVertices.get(i % faceVertices.size());
                Vertex geoVertex = coordConv.fromEuclidean(vertex);
                sb.append(String.format(java.util.Locale.ROOT, "[%f, %f]", geoVertex.x, geoVertex.y));
            }
            sb.append("]]}, \"properties\": {\"name\": ").append(face.getName())
                    .append(", \"part_id\": ").append(partId)
                    .append(", \"weight\": ").append(String.format(java.util.Locale.ROOT, "%f", face.getWeight()))
                    .append(", \"type\": \"partition_face\"}}");
            firstFeature = false;
        }

        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpBoundaryGeoJSON(List<Vertex> boundary, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        
        // 1. Add vertices as points
        for (int i = 0; i < boundary.size(); i++) {
            Vertex v = boundary.get(i);
            Vertex geoV = coordConv.fromEuclidean(v);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "%f, %f", geoV.x, geoV.y));
            sb.append("]}, \"properties\": {\"name\": ").append(v.getName()).append(", \"type\": \"boundary_vertex\"}},");
        }

        // 2. Add boundary edges as lines
        for (int i = 0; i < boundary.size(); i++) {
            Vertex v1 = boundary.get(i);
            Vertex v2 = boundary.get((i + 1) % boundary.size());
            Vertex geo1 = coordConv.fromEuclidean(v1);
            Vertex geo2 = coordConv.fromEuclidean(v2);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geo1.x, geo1.y, geo2.x, geo2.y));
            sb.append("]}, \"properties\": {\"v1\": ").append(v1.getName()).append(", \"v2\": ").append(v2.getName()).append(", \"type\": \"boundary_edge\"}},");
        }
        
        if (boundary.isEmpty()) {
            sb.setLength(sb.length() - 1);
        } else {
            // Remove last trailing comma if any
            int lastComma = sb.lastIndexOf(",");
            if (lastComma != -1) sb.setLength(lastComma);
        }
        
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpSPTGeoJSON(Map<Vertex, Vertex> spt, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        boolean first = true;
        for (Map.Entry<Vertex, Vertex> entry : spt.entrySet()) {
            if (!first) sb.append(",");
            Vertex child = entry.getKey();
            Vertex parent = entry.getValue();
            Vertex geoChild = coordConv.fromEuclidean(child);
            Vertex geoParent = coordConv.fromEuclidean(parent);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geoChild.x, geoChild.y, geoParent.x, geoParent.y));
            sb.append("]}, \"properties\": {\"child\": ").append(child.getName()).append(", \"parent\": ").append(parent.getName()).append("}}");
            first = false;
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpDualTreeGeoJSON(Map<VertexOfDualGraph, List<VertexOfDualGraph>> dualForest, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        boolean first = true;
        for (Map.Entry<VertexOfDualGraph, List<VertexOfDualGraph>> entry : dualForest.entrySet()) {
            VertexOfDualGraph parent = entry.getKey();
            for (VertexOfDualGraph child : entry.getValue()) {
                if (!first) sb.append(",");
                Vertex geoParent = coordConv.fromEuclidean(parent);
                Vertex geoChild = coordConv.fromEuclidean(child);
                sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
                sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geoParent.x, geoParent.y, geoChild.x, geoChild.y));
                sb.append("]}, \"properties\": {\"parent\": ").append(parent.getName()).append(", \"child\": ").append(child.getName()).append("}}");
                first = false;
            }
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpOriginalGraphGeoJSON(Graph<Vertex> graph, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        
        // 1. Add vertices as points
        boolean first = true;
        for (Vertex v : graph.vertices()) {
            if (!first) sb.append(",");
            Vertex geoV = coordConv.fromEuclidean(v);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "%f, %f", geoV.x, geoV.y));
            sb.append("]}, \"properties\": {\"name\": ").append(v.getName()).append(", \"type\": \"graph_vertex\"}}");
            first = false;
        }

        // 2. Add edges as lines
        for (Vertex u : graph.vertices()) {
            Map<Vertex, graph.Edge> neighbors = graph.getEdges().get(u);
            if (neighbors == null) continue;
            for (Map.Entry<Vertex, graph.Edge> edgeEntry : neighbors.entrySet()) {
                Vertex v = edgeEntry.getKey();
                if (u.getName() > v.getName()) continue; // Undirected edges once
                if (!first) sb.append(",");
                Vertex geoU = coordConv.fromEuclidean(u);
                Vertex geoV = coordConv.fromEuclidean(v);
                double length = edgeEntry.getValue().length;
                sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
                sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geoU.x, geoU.y, geoV.x, geoV.y));
                sb.append("]}, \"properties\": {\"u\": ").append(u.getName()).append(", \"v\": ").append(v.getName())
                        .append(", \"length\": ").append(String.format(java.util.Locale.ROOT, "%f", length))
                        .append(", \"type\": \"graph_edge\"}}");
                first = false;
            }
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpDualGraphGeoJSON(Graph<VertexOfDualGraph> dualGraph, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        
        // 1. Add dual vertices as points
        boolean first = true;
        for (VertexOfDualGraph v : dualGraph.vertices()) {
            if (!first) sb.append(",");
            Vertex geoV = coordConv.fromEuclidean(v);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "%f, %f", geoV.x, geoV.y));
            sb.append("]}, \"properties\": {\"name\": ").append(v.getName()).append(", \"type\": \"dual_vertex\"}}");
            first = false;
        }

        // 2. Add dual edges as lines
        for (VertexOfDualGraph u : dualGraph.vertices()) {
            Map<VertexOfDualGraph, graph.Edge> neighbors = dualGraph.getEdges().get(u);
            if (neighbors == null) continue;
            for (VertexOfDualGraph v : neighbors.keySet()) {
                if (u.getName() > v.getName()) continue; // Undirected edges once
                if (!first) sb.append(",");
                Vertex geoU = coordConv.fromEuclidean(u);
                Vertex geoV = coordConv.fromEuclidean(v);
                sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
                sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geoU.x, geoU.y, geoV.x, geoV.y));
                sb.append("]}, \"properties\": {\"u\": ").append(u.getName()).append(", \"v\": ").append(v.getName()).append(", \"type\": \"dual_edge\"}}");
                first = false;
            }
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpPathGeoJSON(List<Vertex> path, String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\": \"FeatureCollection\", \"features\": [");
        boolean first = true;
        for (int i = 0; i < path.size() - 1; i++) {
            if (!first) sb.append(",");
            Vertex v1 = path.get(i);
            Vertex v2 = path.get(i + 1);
            Vertex geo1 = coordConv.fromEuclidean(v1);
            Vertex geo2 = coordConv.fromEuclidean(v2);
            sb.append("{\"type\": \"Feature\", \"geometry\": {\"type\": \"LineString\", \"coordinates\": [");
            sb.append(String.format(java.util.Locale.ROOT, "[%f, %f], [%f, %f]", geo1.x, geo1.y, geo2.x, geo2.y));
            sb.append("]}, \"properties\": {\"v1\": ").append(v1.getName()).append(", \"v2\": ").append(v2.getName()).append(", \"type\": \"path_edge\"}}");
            first = false;
        }
        sb.append("]}");
        saveGeoJSON(fileName, sb.toString());
    }

    public void dumpCycleGeoJSON(List<Vertex> cycle, String fileName) {
        // A cycle is a path where the last vertex connects back to the first.
        // We can reuse dumpPathGeoJSON logic but ensure the closing edge is added.
        if (cycle == null || cycle.isEmpty()) return;
        
        List<Vertex> cycleWithClosing = new ArrayList<>(cycle);
        cycleWithClosing.add(cycle.get(0));
        dumpPathGeoJSON(cycleWithClosing, fileName);
    }


    private void saveGeoJSON(String fileName, String content) {
        try {
            Files.createDirectories(Paths.get("src/main/output/debug"));
            try (FileWriter writer = new FileWriter("src/main/output/debug/" + fileName + ".geojson")) {
                writer.write(content);
            }
        } catch (IOException e) {
            logger.error("Failed to dump GeoJSON: {}", e.getMessage());
        }
    }
}
