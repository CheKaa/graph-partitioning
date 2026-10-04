package partitioning.algorithms;

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
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import geometry.Point;
import graph.Edge;
import graph.Graph;
import graph.Vertex;
import graph.VertexOfDualGraph;
import partitioning.entities.FlowResult;
import partitioning.maxflow.MaxFlow;
import partitioning.maxflow.MaxFlowCuttedReif;
import partitioning.maxflow.MaxFlowDinic;
import partitioning.maxflow.MaxFlowReif;
import readWrite.CoordinateConversion;

public class InertialFlowPartitioning extends BalancedPartitioningOfPlanarGraphs {
    private static final Logger logger = LoggerFactory.getLogger(InertialFlowPartitioning.class);

    private final double PARAMETER_SOURCE, PARAMETER_SINK;
    private final boolean USE_REIF, USE_BINARY_SEARCH, USE_CUTTED_REIF;
    private final double LENGTH_PRIORITY;

    public InertialFlowPartitioning(boolean useReif) {
        this.PARAMETER_SOURCE = 0.5;
        this.PARAMETER_SINK = 0.5;
        this.USE_REIF = useReif;
        this.LENGTH_PRIORITY = 0.5;
        this.USE_BINARY_SEARCH = false;
        this.USE_CUTTED_REIF = false;
    }

    public InertialFlowPartitioning(double parameter, boolean useReif) {
        this.PARAMETER_SOURCE = parameter;
        this.PARAMETER_SINK = parameter;
        this.USE_REIF = useReif;
        this.LENGTH_PRIORITY = 0.5;
        this.USE_BINARY_SEARCH = false;
        this.USE_CUTTED_REIF = false;
    }

    public InertialFlowPartitioning(double parameter, boolean useReif, double lengthPriority, boolean useBinarySearch) {
        this.PARAMETER_SOURCE = parameter;
        this.PARAMETER_SINK = parameter;
        this.USE_REIF = useReif;
        this.LENGTH_PRIORITY = lengthPriority;
        this.USE_BINARY_SEARCH = useBinarySearch;
        this.USE_CUTTED_REIF = false;
    }

    public InertialFlowPartitioning(double parameter, boolean useReif, double lengthPriority, boolean useBinarySearch, boolean useCuttedReif) {
        this.PARAMETER_SOURCE = parameter;
        this.PARAMETER_SINK = parameter;
        this.USE_REIF = useReif;
        this.LENGTH_PRIORITY = lengthPriority;
        this.USE_BINARY_SEARCH = useBinarySearch;
        this.USE_CUTTED_REIF = useCuttedReif;
    }


    @Override
    public void balancedPartitionAlgorithm(Graph<Vertex> simpleGraph,
										   Graph<VertexOfDualGraph> graph, 
										   int maxSumVerticesWeight,
										   CoordinateConversion coordinateConversion) {

        Stack<Graph<VertexOfDualGraph>> stack = new Stack<>();
        graph = graph.getLargestConnectedComponent();
        this.graph = graph;
        long startTime = System.currentTimeMillis();

        stack.push(graph);

        while (!stack.isEmpty()) {
            Graph<VertexOfDualGraph> currentGraph = stack.pop();

            long time1 = System.currentTimeMillis();

            Set<VertexOfDualGraph> vertices = currentGraph.vertices();
            double totalWeight = currentGraph.verticesWeight();
            if (totalWeight <= maxSumVerticesWeight) {
                partition.add(new HashSet<>(vertices));
                continue;
            }

            double targetWeightSource = PARAMETER_SOURCE * totalWeight;
            double targetWeightSink = PARAMETER_SINK * totalWeight;

            // Compute stretch for each line and sort descending
            var guess = PartitionInit.guess(currentGraph, targetWeightSource, targetWeightSink);
            var sourceSet = guess.getKey();
            var sinkSet = guess.getValue();

            long time2 = System.currentTimeMillis();
            logger.info("Time for selecting source and sink: {} seconds", (time2 - time1) / 1000.0);

            logger.debug("sourceSet size = {}, sinkSet size = {}", sourceSet.size(), sinkSet.size());
            
            long maxIndex = vertices.stream().max(Comparator.comparingLong(VertexOfDualGraph::getName)).get().getName();

            VertexOfDualGraph source = new VertexOfDualGraph(maxIndex + 1, 0, 0, 0);
            VertexOfDualGraph sink = new VertexOfDualGraph(maxIndex + 2, 0, 0, 0);
            Graph<VertexOfDualGraph> copyGraph = createGraphWithSourceSink(currentGraph, sourceSet, source, sinkSet, sink);

            long time3 = System.currentTimeMillis();
            logger.info("Time for creating graph with source and sink: {} seconds", (time3 - time2) / 1000.0);

            Assertions.assertEquals(currentGraph.verticesNumber() + 2, copyGraph.verticesNumber());

            logger.debug("process graph with {} vertices, source size = {}, sink size = {}", copyGraph.verticesNumber(), sourceSet.size(), sinkSet.size());
            MaxFlow maxFlow;
            if (USE_REIF) {
                if (USE_CUTTED_REIF) {
                    logger.info("Using MaxFlowCuttedReif algorithm");
                    maxFlow = new MaxFlowCuttedReif(simpleGraph, copyGraph, source, sink, coordinateConversion, maxSumVerticesWeight, LENGTH_PRIORITY);
                } else {
                    logger.info("Using MaxFlowReif algorithm");
                    maxFlow = new MaxFlowReif(simpleGraph, copyGraph, source, sink, coordinateConversion, maxSumVerticesWeight, LENGTH_PRIORITY, USE_BINARY_SEARCH);
                }
            } else {
                logger.info("Using MaxFlowDinic algorithm");
                maxFlow = new MaxFlowDinic(copyGraph, source, sink);
            }
            FlowResult flowResult = maxFlow.findFlow();
            logger.debug("Flow size: {}", flowResult.flowSize());
            long time4 = System.currentTimeMillis();
            logger.info("Time for finding flow: {} seconds", (time4 - time3) / 1000.0);

            List<Graph<VertexOfDualGraph>> subpartition;
            if (USE_REIF) {
                subpartition = partitionGraphReif(flowResult);
            } else {
                subpartition = partitionGraph(flowResult);
            }

            long time5 = System.currentTimeMillis();
            logger.info("Time for partitioning graph: {} seconds", (time5 - time4) / 1000.0);
            logger.debug("SUBPARTITION SIZE: {}", subpartition.size());
            logger.debug("Subgraph 0 vertices: {}, weight: {}", subpartition.get(0).verticesNumber(), subpartition.get(0).verticesWeight());
            logger.debug("Subgraph 1 vertices: {}, weight: {}", subpartition.get(1).verticesNumber(), subpartition.get(1).verticesWeight());
            logger.debug("Original graph vertices: {}, weight: {}\n\n", currentGraph.verticesNumber(), currentGraph.verticesWeight());

            // Логируем длину разреза и баланс частей
            double cutLength = flowResult.flowSize();
            double weight0 = subpartition.get(0).verticesWeight();
            double weight1 = subpartition.get(1).verticesWeight();
            double balanceRatio = weight0 / weight1;
            double balancePercent0 = (weight0 / totalWeight) * 100;
            double balancePercent1 = (weight1 / totalWeight) * 100;
            
            logger.info("Cut length: {}", cutLength);
            logger.info("Partition balance: {} / {} ({}% / {}%), ratio: {}",
                        weight0, weight1, balancePercent0, balancePercent1, balanceRatio);


            for (Graph<VertexOfDualGraph> subgraph : subpartition) {
                stack.push(subgraph);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.info("Total time in Inertial Flow: {} seconds", (endTime - startTime) / 1000.0);
    }

    

    private List<Graph<VertexOfDualGraph>> partitionGraph(FlowResult flow) {
        Graph<VertexOfDualGraph> graphWithFlow = flow.graphWithFlow();
        List<Graph<VertexOfDualGraph>> subpartition = new ArrayList<>();
        List<VertexOfDualGraph> vertices = graphWithFlow.verticesArray();
        Map<VertexOfDualGraph, Boolean> isConnectedWithSource = vertices.stream().collect(Collectors.toMap(Function.identity(), v -> Boolean.FALSE));

        markComponent(graphWithFlow, flow.source(), isConnectedWithSource);

        graphWithFlow.deleteVertex(flow.source());
        graphWithFlow.deleteVertex(flow.sink());

        for (int i = 0; i < 2; i++) {
            subpartition.add(graphWithFlow.createSubgraph(i == 0 ?
                    isConnectedWithSource.keySet().stream().filter(isConnectedWithSource::get).collect(Collectors.toSet()) :
                    isConnectedWithSource.keySet().stream().filter(v -> !isConnectedWithSource.get(v)).collect(Collectors.toSet())));
        }

        Assertions.assertEquals(graphWithFlow.verticesNumber(), subpartition.get(0).verticesNumber() + subpartition.get(1).verticesNumber());

        if (subpartition.get(0).verticesSumWeight() < subpartition.get(1).verticesSumWeight()) {
            return new ArrayList<>(Arrays.asList(subpartition.get(1), subpartition.get(0)));
        }
        return subpartition;
    }


    void markComponent(
            Graph<VertexOfDualGraph> graph,
            VertexOfDualGraph source,
            Map<VertexOfDualGraph, Boolean> isConnectedWithSource
    ) {
        LinkedList<VertexOfDualGraph> queue = new LinkedList<>();
        queue.add(source);
        isConnectedWithSource.put(source, true);
        while (!queue.isEmpty()) {
        	VertexOfDualGraph vertex = queue.poll();
            var edges = graph.getEdges();
            for (var connectedVertex : edges.get(vertex).entrySet()) {
                var neighbor = connectedVertex.getKey();
                if (isConnectedWithSource.get(neighbor)){
                    continue;
                }
                var edge = connectedVertex.getValue();
                if (edge.flow < edge.bandwidth) {
                    isConnectedWithSource.put(neighbor, true);
                    queue.add(neighbor);
                }
            }
        }

    }

    public static Graph<VertexOfDualGraph> createGraphWithSourceSink(
            Graph<VertexOfDualGraph> currentGraph,
            Set<VertexOfDualGraph> sourceSet,
            VertexOfDualGraph source,
            Set<VertexOfDualGraph> sinkSet,
            VertexOfDualGraph sink
    ) {
        Graph<VertexOfDualGraph> newGraph = currentGraph.clone();

        for (VertexOfDualGraph s : sourceSet) {
            newGraph.addEdge(source, s, 0, Integer.MAX_VALUE);
            newGraph.addEdge(s, source, 0, Integer.MAX_VALUE);
        }

        for (VertexOfDualGraph t : sinkSet) {
            newGraph.addEdge(t, sink, 0, Integer.MAX_VALUE);
            newGraph.addEdge(sink, t, 0, Integer.MAX_VALUE);
        }

        return newGraph;
    }

    private List<Graph<VertexOfDualGraph>> partitionGraphReif(FlowResult flow) {
        Graph<VertexOfDualGraph> graph = flow.graphWithFlow().clone();
        
        VertexOfDualGraph source = flow.source();
        VertexOfDualGraph sink = flow.sink();
        
        List<VertexOfDualGraph> allVertices = new ArrayList<>(graph.verticesArray());

        for (VertexOfDualGraph v : allVertices) {
            if (graph.getEdges().get(v) == null) continue;
            
            List<VertexOfDualGraph> neighbors = new ArrayList<>(graph.getEdges().get(v).keySet());
            for (VertexOfDualGraph neighbor : neighbors) {
                Edge edge = graph.getEdges().get(v).get(neighbor);
                if (edge.flow >= edge.getBandwidth()) {
                    graph.deleteEdge(v, neighbor);
                }
            }
        }

        graph.deleteVertex(source);
        graph.deleteVertex(sink);
        
        List<Set<VertexOfDualGraph>> components = graph.splitForConnectedComponents();

        if (components.size() > 2) {
            logger.warn("Total components: {}", components.size());
        }
        for (int i = 0; i < components.size(); i++) {
            Set<VertexOfDualGraph> component = components.get(i);

            if (i > 1) {
                logger.warn("Component {} vertices: {}", i + 1,
                        component.stream().map(v -> v.name).limit(20).toArray());
            }
        }

        List<Graph<VertexOfDualGraph>> subpartition = new ArrayList<>();
        
        if (components.isEmpty()) {
            logger.warn("No components found, returning empty graphs");
            subpartition.add(new Graph<>());
            subpartition.add(new Graph<>());
        } else if (components.size() == 1) {
            logger.warn("Only one component, graph not separated");
            subpartition.add(flow.graphWithFlow().createSubgraph(components.get(0)));
            subpartition.add(new Graph<>());
        } else {
            components.sort((a, b) -> Integer.compare(b.size(), a.size()));

            subpartition.add(flow.graphWithFlow().createSubgraph(components.get(0)));
            subpartition.add(flow.graphWithFlow().createSubgraph(components.get(1)));

            for (int i = 2; i < components.size(); i++) {
                Graph<VertexOfDualGraph> smallComponent = flow.graphWithFlow().createSubgraph(components.get(i));
                for (VertexOfDualGraph v : smallComponent.verticesArray()) {
                    components.get(0).add(v);
                }
            }
            
            subpartition.set(0, flow.graphWithFlow().createSubgraph(components.get(0)));
            }

        if (subpartition.get(0).verticesSumWeight() < subpartition.get(1).verticesSumWeight()) {
            return new ArrayList<>(Arrays.asList(subpartition.get(1), subpartition.get(0)));
        }
        
        return subpartition;
    }
}