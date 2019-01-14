package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.Node;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class GraphCleaner {


    public static ChromaticNumber.Result clean(Graph graph) {



        if(graph.isComplete()) {
            return new ChromaticNumber.Result(graph, graph.getNodes().size(), graph.getNodes().size(), graph.getNodes().size(), true);
        }

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
        TestApp.debug("Cleaning (%dms) >> Removing singles, nodes: %d (%.6f%%), edges: %d (%.6f%%), density: %.6f%% (%.6f%%) %n",
                (System.currentTimeMillis() - time),
                graph.getNodes().size(),
                (1D - (graph.getNodes().size() / inital_nodes)) * 100,
                graph.getEdgeCount(),
                (1D - (graph.getEdgeCount() / inital_edges)) * 100,
                graph.getDensity() * 100,
                inital_density * 100
        );

        //--- Tree
        if(inital_nodes > 0 && graph.getNodes().isEmpty()) {
            return new ChromaticNumber.Result(graph, 2, 2, 2, true);
        }

        //fully nodes
        time = System.currentTimeMillis();
        // Check if we have at least one fully-connected node
        if(graph.getNodes().values().stream().anyMatch(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1)){

            // Store all subgraphs (not only leaves)
            Stack<Graph> subgraphs = new Stack<>();
            graph.reset();
            subgraphs.add(graph); // add the graph as a starting point

            //Smallest actually contains leaves
            List<Graph> smallest = new LinkedList<>();

            //boolean isFirst = true;

            while (!subgraphs.isEmpty()) {
                Graph g = subgraphs.pop();

                LinkedList<Graph> _S = new LinkedList<>(); //contains subgraphs
                divider(_S, g);

                //size == -1 -> we found the smallest subgraph (leave)
                if (_S.size() == 1) {
                    smallest.addAll(_S);
                }
                //size > 1 -> we have found additional subgraphs which may or may not contain more subgraphs
                else {
                    //isFirst = false;
                    _S.forEach(e -> e.getMeta().setLevel(g.getMeta().getLevel() + 1)); // increase the levels of the next level of subgraphs
                    subgraphs.addAll(_S); // add to look at them
                }
            }

            int exact = Integer.MIN_VALUE;
            TestApp.OUTPUT_ENABLED = false;
            for(Graph g : smallest) {
                ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, false);
                exact = Math.max(r.getExact() + g.getMeta().getLevel() + (smallest.size() == 1 ? 1 : 0), exact);//TODO !!!! is this correct?
                //TODO fixed bug in exact (forgot to reset graph) is +1 actually correct...
                // graph09.txt and block3_2018_graph20.txt would be wrong otherwise.... IDK
                // graph09.txt is 16 according to Steven
                // block3_2018_graph20.txt -> is [8..9] before cleaning so it cannot be 10...
            }
            TestApp.OUTPUT_ENABLED = true;

            TestApp.debug("Cleaning (%dms) >> Splitting fully-connected nodes, sub-graphs: %d %n",
                    (System.currentTimeMillis() - time),
                    smallest.size()
            );

            return new ChromaticNumber.Result(graph, exact, exact, exact, true);

        }


        return new ChromaticNumber.Result(null, -1, -1, -1, false);
    }


    private static void divider(LinkedList<Graph> subgraphs, Graph graph) {

        /*
        // TODO Assuming that :MightFixCliques actually fixes the bug that we had than we no longer require this check
        if(graph.getNodes().size() == 2 && graph.getEdges().size() == 2) {
            subgraphs.add(graph.clone());
            graph.getNodes().clear();
            graph.getEdges().clear();
            return;
        }*/

        /*Stack<Node> hashSet = graph.getNodes().values().stream()
                .filter(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1)
                .collect(Collectors.toCollection(Stack::new));*/

        // :MightFixCliques TODO Validate, the idea is that instead of just removing all the nodes, we keep walking down the tree
        // so we get the correct levels, otherwise we might just get rid of an entire clique by accident
        Stack<Node> hashSet = new Stack<>();
        graph.getNodes().values().stream()
                .filter(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1).findAny().ifPresent(hashSet::add);

        // TODO Just an idea, what exactly happens if we have more than split point at once in a graph, this is causing the issues
        // with graph block3_2018_08. In theory, we would expect to split into to but we just return this one, so we get an erroneous +1
        /*if(hashSet.size() > 1) {
            subgraphs.add(graph.clone());
            graph.getNodes().clear();
            graph.getEdges().clear();
            return;
        }*/

        while (!hashSet.isEmpty()) {
            Node n = hashSet.pop();
            graph.getNodes().remove(n.getId()); //remove the fully-connected node

            List<Node> neighbours = graph.getNeighbours(n);
            graph.getEdges().remove(n.getId()); //remove all the edges from fully-connected node -> B

            // remove all the edges from B -> fully-connected node
            neighbours.forEach(neighbour -> graph.getEdges(neighbour.getId()).remove(n.getId()));
        }

        for (Node node : graph.getNodes().values()) {
            // n.v != -1 means that it already belongs to another subgraph so we can skip it
            if (node.getValue() != -1) continue;
            subgraphs.add(discoverGraph(graph, node));
        }

    }

    private static Graph discoverGraph(Graph og, Node origin) {

        // create a new graph
        Graph newGraph = new Graph();
        newGraph.getMeta().setLevel(og.getMeta().getLevel()); //inherit level from old graph

        // DFS, nodes that we have still to visit
        Stack<Node> visit = new Stack<>();
        visit.add(origin); //start at the origin

        while (!(visit.isEmpty())) {
            Node n = visit.pop();
            n.setValue(0); // 0 -> means that we have visited this node

            newGraph.addNode(n.getId(), -1); // new graph add the node

            List<Node> neighbours = og.getNeighbours(n); //get all neighbours from the original graph
            neighbours.forEach(neighbour -> {
                if(!newGraph.hasNode(neighbour.getId())) { // new graph doesn't have neighbour yet so just add it to avoid errors
                    newGraph.addNode(neighbour.getId(), -1);
                }

                newGraph.addEdge(neighbour.getId(), n.getId(), true); //add neighbour N -> neighbour, neighbour -> N
            });
            visit.addAll(neighbours.stream().filter(e -> e.getValue() == -1).collect(Collectors.toList())); //go to visit all its neighbours
        }

        return newGraph;
    }

}
