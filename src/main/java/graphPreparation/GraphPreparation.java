package graphPreparation;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import readWrite.CoordinateConversion;

public class GraphPreparation {
	private static final Logger logger = LoggerFactory.getLogger(GraphPreparation.class);
	private final boolean isPlanar;
	private final boolean isDual;

	public GraphPreparation() {
		this.isPlanar = false;
		this.isDual = false;
	}

	public GraphPreparation(boolean isPlanar, boolean isDual) {
		this.isPlanar = isPlanar;
		this.isDual = isDual;
	}
	
	public Graph<VertexOfDualGraph> prepareGraph(Graph<Vertex> gph, double inaccuracy) throws IOException {
		return prepareGraph(gph, inaccuracy, new CoordinateConversion(0,0));
	}

	public Graph<VertexOfDualGraph> prepareGraph(Graph<Vertex> gph, double inaccuracy, CoordinateConversion cc) throws IOException {
        long startTime = System.currentTimeMillis();
		logger.info("Number of 0 weight vertex, before correction: {}", gph.countZeroWeightVertices());
        long time1 = System.currentTimeMillis();
        logger.info("Time for correcting vertices weight: {} seconds", (time1 - startTime) / 1000.0);
		logger.info("Number of 0 weight vertex, before sweepLine: {}", gph.countZeroWeightVertices());
		logger.info("Start graph weight: {}", gph.verticesSumWeight());
		
		Graph<Vertex> graph;
        if (!isPlanar) {
            SweepLine sl = new SweepLine(inaccuracy);
            graph = sl.makePlanar(gph);
            gph.replaceWith(graph);
        } else {
            graph = gph;
        }
		
        long time2 = System.currentTimeMillis();
        logger.info("Time for sweepLine: {} seconds", (time2 - time1) / 1000.0);
		
		logger.info("Number of 0 weight vertex, after sweepLine: {}", gph.countZeroWeightVertices());
		logger.info("After sweepline graph weight: {}", gph.verticesSumWeight());

		MakingDualGraph dg = new MakingDualGraph();
		Graph<VertexOfDualGraph> dualGraph = dg.buildDualGraph(graph);

        VertexOfDualGraph externalFaceVertex = dg.findExternalFace(dualGraph);

        Set<VertexOfDualGraph> removedNestedFaces = NestedFacesRemover.removeNestedFaces(dualGraph, externalFaceVertex);
        logger.info("Found {} nested faces to remove", removedNestedFaces.size());

        // if (!removedNestedFaces.isEmpty()) {
        //     logger.info("Dual graph weight after removing nested faces: {}", dualGraph.verticesSumWeight());
        //     assert dualGraph.isConnected();
        // }
        dualGraph.deleteVertex(externalFaceVertex);
        // assert (dualGraph.isConnected()): "Disconnected dual graph";

		logger.info("Dual graph weight: {}", dualGraph.verticesSumWeight());
		return dualGraph;
	}
	
}