package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.GraphStructures;
import edu.group20.chromflow.graph.Node;

import java.util.*;
import java.util.stream.Collectors;

public class GraphCleaner {



    public static Result clean(Graph graph) {

        if(graph.isComplete()) {
            TestApp.debug("Cleaning (0ms) >> Detected complete graph.%n");
            return new Result(graph.getNodes().size(), graph.getNodes().size(), graph.getNodes().size());
        }
        long time = System.currentTimeMillis();

        //removing single nodes
        {
            time = System.currentTimeMillis();
            final double inital_nodes = graph.getNodes().size();
            final double inital_density = graph.getDensity();
            final double inital_edges = graph.getEdgeCount();

            // remove singles
            Stack<Integer> singleNodes = graph.getNodes().keySet().stream().filter(id -> graph.getDegree(id) <= 1).collect(Collectors.toCollection(Stack::new));
            while (!singleNodes.isEmpty()) {
                final int fromId = singleNodes.pop();
                final int degree = graph.getEdges(fromId).size();

                if (degree == 1) {
                    int toId = graph.getEdges(fromId).values().stream().findAny().get().getTo().getId();
                    graph.getEdges(toId).remove(fromId);

                    if (graph.getEdges(toId).size() == 1) {
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
            if (inital_nodes > 0 && graph.getNodes().isEmpty()) {
                return new Result(2, 2, 2);
            }
        }

        //fully-connected nodes
        {
            time = System.currentTimeMillis();
            // Check if we have at least one fully-connected node
            if (graph.getNodes().values().stream().anyMatch(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1)) {

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
                for (Graph g : smallest) {
                    ChromaticNumber.Result r = ChromaticNumber.computeExact(g, false);
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

                return new Result(exact, exact, exact);
                //return new ChromaticNumber.Result(graph, exact, exact, exact, true);

            }
        }

        //--- Planar Graphs
        if(false){
            // http://www.cs.yale.edu/homes/spielman/561/2009/lect25-09.pdf -> Corollary 25.2.4.
            time = System.currentTimeMillis();
            if (graph.getNodes().size() >= 3) {
                //  fully-triangulated planar graph
                if (graph.getEdges().size() == 3 * graph.getNodes().size() - 6) {
                    TestApp.debug("Cleaning (%dms) >> Graph is fully-triangulated planar graph. %n",
                            System.currentTimeMillis() - time);
                    return new Result(-1, 4, -1);
                } else if ((graph.getEdges().size() <= ((3 * graph.getNodes().size()) - 6)) && GraphStructures.isPlanar(graph)) {
                    TestApp.debug("Cleaning (%dms) >> Graph might be planar. %n",
                            System.currentTimeMillis() - time);
                    //return new Result(-1, 4, -1);
                }
            }
        }



        //TODO REMOVE
        // Theory: A graph's chromatic number is max(degree) iff
        //  (1) all nodes have the same degree
        //  (2) max(degree) > 2
        //  Well graph block3_2018_graph07 breaks it
        /*{
            int minDegree = Integer.MAX_VALUE;
            int maxDegree = Integer.MIN_VALUE;
            for(Map<Integer, Node.Edge> edges : graph.getEdges().values()) {
                minDegree = Math.min(minDegree, edges.size());
                maxDegree = Math.max(maxDegree, edges.size());
                if(minDegree != maxDegree) break;
            }
            if(minDegree > 2 && minDegree == maxDegree) {
                    TestApp.debug("Cleaning (0) >> !!!!! Graph has minDegree = maxDegree (EXPERIMENTAL!!!!!!) %n");
                    return new Result(maxDegree + 1, maxDegree + 1, maxDegree + 1);
            }
        }*/
        //TODO REMOVE END

        //removing unconnected cliques
        if(false){
            graph.reset();

            while(true) {
                List<HashSet<Node>> cliques = new LinkedList<>();
                cliqueDetector(graph, cliques, new HashSet<>(), new HashSet<>(graph.getNodes().values().stream().filter(e -> e.getValue() == -1).collect(Collectors.toList())), new HashSet<>());

                if (!(cliques.isEmpty())) {
                    cliques.sort((o1, o2) -> -Integer.compare(o1.size(), o2.size()));

                    HashSet<Node> maxClique = cliques.get(0);

                    if(maxClique.isEmpty()) break;

                    int outsideNeighbours = 0;
                    for (Node node : maxClique) {
                        node.setValue(0);
                        if (graph.getNeighbours(node).size() > maxClique.size() - 1) {
                            outsideNeighbours += graph.getEdges(node.getId()).size() - maxClique.size() + 1;
                        }
                    }

                    if (outsideNeighbours == 0) {
                        for (Node n : maxClique) {
                            System.out.println("remove");
                            assert null != graph.getNodes().remove(n.getId());
                            assert graph.getEdges().remove(n.getId()).size() > 0;
                        }
                    }

                    TestApp.debugln("outside neighbours >> " + outsideNeighbours);
                    TestApp.debugln("pivot >> " + ChromaticNumber.bronKerboschWithPivot(graph, new HashSet<>(), new HashSet<>(graph.getNodes().values()), new HashSet<>()));
                } else {
                    TestApp.debugln("[1]");
                    break;
                }
            }
        }


        return new Result(-1, -1, -1);
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

    private static void cliqueDetector(Graph graph, List<HashSet<Node>> cliques, HashSet<Node> _R, HashSet<Node> _P, HashSet<Node> _X) {
        if (_P.isEmpty() && _X.isEmpty()) {
            cliques.add(_R);
        }

        Iterator<Node> nodeIterator = _P.iterator();
        while (nodeIterator.hasNext()) {

            //---
            Node node = nodeIterator.next();
            List<Node> neighbours = graph.getEdges(node.getId()).values()
                    .stream()
                    .filter(e -> e.getTo().getValue() == -1)
                    .map(Node.Edge::getTo).collect(Collectors.toList());

            //---
            HashSet<Node> dR = new HashSet<>(_R);
            dR.add(node);

            HashSet<Node> dP = _P.stream()
                    .filter(e -> e.getValue() == -1)
                    .filter(neighbours::contains).collect(Collectors.toCollection(HashSet::new));
            HashSet<Node> dX = _X.stream()
                    .filter(e -> e.getValue() == -1)
                    .filter(neighbours::contains).collect(Collectors.toCollection(HashSet::new));

            cliqueDetector(graph, cliques, dR, dP, dX);

            //---
            nodeIterator.remove();
            _X.add(node);
        }

    }

    public static class Result {

        private int exact = -1;
        private int lower = -1;
        private int upper = -1;

        public Result(int lower, int upper, int exact) {
            this.lower = lower;
            this.upper = upper;
            this.exact = exact;
        }

        public int getExact() {
            return exact;
        }

        public int getLower() {
            return lower;
        }

        public int getUpper() {
            return upper;
        }

        public boolean hasLower() {
            return lower != -1;
        }

        public boolean hasUpper() {
            return upper != -1;
        }

        public boolean hasExact() {
            return exact != -1;
        }

    }

}
