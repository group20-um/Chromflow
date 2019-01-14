package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.Node;

import java.util.*;
import java.util.stream.Collectors;

public class GraphCleaner {


    public static ChromaticNumber.Result clean(Graph graph) {

        final double inital_nodes = graph.getNodes().size();
        final double inital_density = graph.getDensity();
        final double inital_edges = graph.getEdgeCount();

        // remove singles
        long time = System.currentTimeMillis();
        Stack<Integer> singleNodes = graph.getNodes().keySet().stream().filter(id -> graph.getDegree(id) <= 1).collect(Collectors.toCollection(Stack::new));
        while (!singleNodes.isEmpty()) {
            final int fromId = singleNodes.pop();
            final int degree = graph.getEdges(fromId).size();

            if (degree == 1) {
                int toId = graph.getEdges(fromId).values().stream().findAny().get().getTo().getId();
                graph.getEdges(toId).remove(fromId);

                if(graph.getEdges(toId).size() == 1) {
                    singleNodes.add(toId);
                }
            }
            graph.getEdges().remove(fromId);
            graph.getNodes().remove(fromId);

        }

        //--- Tree
        if(inital_nodes > 0 && graph.getNodes().isEmpty()) {
            return new ChromaticNumber.Result(graph, 2, 2, 2, true);
        }


        //fully nodes
        if(graph.getNodes().values().stream().anyMatch(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1)){
            Stack<Graph> subgraphs = new Stack<>();
            subgraphs.add(graph);

            List<Graph> smallest = new LinkedList<>();

            while (!subgraphs.isEmpty()) {
                Graph g = subgraphs.pop();

                LinkedList<Graph> _S = new LinkedList<>();
                divider(_S, g);

                if (_S.size() == 1) {
                    smallest.addAll(_S);
                } else {
                    _S.forEach(e -> e.meta.level = e.meta.level + 1);
                    subgraphs.addAll(_S);
                }
            }

            int maxExact = Integer.MIN_VALUE;
            for(Graph g : smallest) {
                ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, false);
                int exact = r.getExact() + g.meta.level + 1;
                if(exact > maxExact) {
                    maxExact = exact;
                }
            }

            //final int exact = maxExact + (int) Math.ceil(Math.log(smallest.size()) / Math.log(2));
            return new ChromaticNumber.Result(graph, maxExact, -1, -1, true);

        }

        TestApp.debug("Cleaning (%dms) >> Removing singles, nodes: %d (%.6f%%), edges: %d (%.6f%%), density: %.6f%% (%.6f%%) %n",
                (System.currentTimeMillis() - time),
                graph.getNodes().size(),
                (1D - (graph.getNodes().size() / inital_nodes)) * 100,
                graph.getEdgeCount(),
                (1D - (graph.getEdgeCount() / inital_edges)) * 100,
                graph.getDensity() * 100,
                inital_density * 100
        );

        return new ChromaticNumber.Result(null, -1, -1, -1, false);
    }


    private static void divider(LinkedList<Graph> subgraphs, Graph graph) {

        Stack<Node> hashSet = graph.getNodes().values().stream().filter(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1).collect(Collectors.toCollection(Stack::new));
        while (!hashSet.isEmpty()) {
            Node n = hashSet.pop();
            graph.getNodes().remove(n.getId());

            List<Node> neighbours = graph.getNeighbours(n);
            graph.getEdges().remove(n.getId());

            neighbours.forEach(neighbour -> graph.getEdges(neighbour.getId()).remove(n.getId()));
        }


        Stack<Graph> check = new Stack<>();
        check.add(graph);
        while (!check.isEmpty()) {
            Graph g = check.pop();
            g.reset();
            for (Node node : g.getNodes().values()) {
                if (node.getValue() != -1) continue;
                Graph ng = discoverGraph(g, node);

                if (ng.getNodes().size() != g.getNodes().size()) {
                    check.add(ng);
                } else {
                    subgraphs.add(ng);
                }
            }
        }



    }

    private static Graph discoverGraph(Graph og, Node origin) {

        Graph graph = new Graph();
        graph.meta.level = og.meta.level;
        Stack<Node> visit = new Stack<>();
        visit.add(origin);

        while (!(visit.isEmpty())) {
            Node n = visit.pop();
            n.setValue(0);

            graph.addNode(n.getId(), -1);

            List<Node> neighbours = og.getNeighbours(n);
            neighbours.forEach(neighbour -> {
                if(!graph.hasNode(neighbour.getId())) {
                    graph.addNode(neighbour.getId(), -1);
                }

                graph.addEdge(neighbour.getId(), n.getId(), true);
            });
            visit.addAll(neighbours.stream().filter(e -> e.getValue() == -1).collect(Collectors.toList()));
        }

        return graph;
    }

}
