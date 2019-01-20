package edu.group20.chromflow;

import edu.group20.chromflow.graph.*;
import edu.group20.chromflow.util.Mergesort;

import java.util.*;
import java.util.stream.Collectors;

public class GraphCleaner {



    public static Result clean(Graph graph) {

        if(graph.isComplete()) {
            TestApp.debug("Cleaning (0ms) >> Detected complete graph.%n");
            return new Result(graph.getNodes().size(), graph.getNodes().size(), graph.getNodes().size());
        }

        // TODO :NothingOfConcern
        int bestLower = Integer.MIN_VALUE;
        int bestUpper = Integer.MAX_VALUE;
        //---

        long time = System.currentTimeMillis();
        //removing single nodes

        {
            time = System.currentTimeMillis();
            final double initial_nodes = graph.getNodes().size();
            final double initial_density = graph.getDensity();
            final double initial_edges = graph.getEdgeCount();


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
                    (1D - (graph.getNodes().size() / initial_nodes)) * 100,
                    graph.getEdgeCount(),
                    (1D - (graph.getEdgeCount() / initial_edges)) * 100,
                    graph.getDensity() * 100,
                    initial_density * 100
            );

            //--- Tree
            if (initial_nodes > 0 && graph.getNodes().isEmpty()) {
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

                while (!subgraphs.isEmpty()) {
                    Graph g = subgraphs.pop();

                    LinkedList<Graph> _S = new LinkedList<>(); //contains subgraphs
                    divider(_S, g);

                    //size == -1 -> we found the smallest subgraph (leave)
                    if (_S.isEmpty()) {
                        smallest.add(g);
                    } else {
                        _S.forEach(e -> e.getMeta().setLevel(g.getMeta().getLevel() + 1)); // increase the levels of the next level of subgraphs
                        subgraphs.addAll(_S); // add to look at them
                    }
                }

                int exact = Integer.MIN_VALUE;
                Mergesort.sort(smallest, (o1, o2) -> -Integer.compare(o1.getMeta().getLevel(), o2.getMeta().getLevel()));
                TestApp.OUTPUT_ENABLED = false;
                for (Graph g : smallest) {
                    ChromaticNumber.Result r = ChromaticNumber.computeExact(g, true);
                    exact = Math.max(r.getExact() + g.getMeta().getLevel(), exact);
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

        //Wheels
        //TODO seems to be working, better qualifier required
        if(graph.getNodes().size() < 1000) {
            time = System.currentTimeMillis();
            graph.reset();
            int oddWheels = 0;
            int evenWheels = 0;
            boolean brokeEarly = false;
            for(Node n : graph.getNodes().values()) {
                final int x = GraphStructures.Test.isWheelCenter(graph, n);
                if(x > 0) {
                    if(x % 2 == 0) {
                        evenWheels++;
                    } else {
                        oddWheels++;
                    }
                }

                if(evenWheels > 0) {
                    brokeEarly = true;
                    break;
                }
            }
            TestApp.debug("Cleaning (%dms) >> Wheels, even: %s, odd: %s, broke early: %s%n",
                    (System.currentTimeMillis() - time),
                    evenWheels > 0,
                    oddWheels > 0,
                    brokeEarly
            );
            if(oddWheels + evenWheels > 0) {
                bestLower = Math.max(bestLower, evenWheels > 0 ? 4 : 3);
            }
        }

        //is k-regular
        {
            time = System.currentTimeMillis();

            LinkedList<Map<Integer, Node.Edge>> values = new LinkedList<>(graph.getEdges().values());
            int kRegular = values.get(0).size();
            for (int i = 1; i < values.size() && kRegular != -1; i++) {
                if(values.get(i).size() != kRegular) {
                    kRegular = -1;
                }
            }

            TestApp.debug("Cleaning (%dms) >> k-regular, k: %s%n",
                    (System.currentTimeMillis() - time),
                    (kRegular == -1 ? "NA" : String.valueOf(kRegular))
            );

            /**
             * This segment is based on two theorems from this paper
             *  Brandes algorithm [PDF]. (n.d.). Retrieved from https://www.cl.cam.ac.uk/teaching/1617/MLRD/handbook/brandes.pdf
             *   I Theory 3.8 states that every one-connectivity and k-regular graph has k as its chromatic number
             *   II Theory 3.9 states that every two-connectivity and k-regular graph has k as its upper-bound
             */
            if(kRegular != -1) {
                time = System.currentTimeMillis();
                if(GraphStructures.Connectivity.TwoConnectivity.check(graph)) {
                    bestUpper = Math.min(bestUpper, kRegular);
                    TestApp.debug("Cleaning (%dms) > k-regular, two-connected%n",
                            (System.currentTimeMillis() - time)
                    );
                }/* else if(GraphStructures.Connectivity.Test.check(graph)) {
                    TestApp.debug("Cleaning (%dms) > k-regular, one-connectivity",
                            (System.currentTimeMillis() - time)
                    );
                    return new Result(kRegular, kRegular, kRegular);
                }*/
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
                } else if ((graph.getEdges().size() <= ((3 * graph.getNodes().size()) - 6)) && GraphStructures.EVBAsed.isPlanar(graph)) {
                    TestApp.debug("Cleaning (%dms) >> Graph might be planar. %n",
                            System.currentTimeMillis() - time);
                    //return new Result(-1, 4, -1);
                }
            }
        }

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
                    //TestApp.debugln("pivot >> " + ChromaticNumber.bronKerboschWithPivot(graph, new HashSet<>(), new HashSet<>(graph.getNodes().values()), new HashSet<>()));
                } else {
                    TestApp.debugln("[1]");
                    break;
                }
            }
        }

        // finding communities
        if(false) {
            time = System.currentTimeMillis();
            graph.reset();
            LinkedList<Node> nodes = new LinkedList<>(graph.getNodes().values());
            Map<Integer, Double> score = new HashMap<>();
            double maxScore = Double.MIN_VALUE;
            for(Node n : graph.getNodes().values()) {
                graph.reset();
                Map<Integer, Integer> previous = Dijkstra.buildPaths(graph, n.getId());

                for(int prev : previous.values()) {
                    final double v = score.getOrDefault(prev, 0D) + 1;
                    maxScore = Math.max(v, maxScore);
                    score.put(prev, v);
                }
            }
            for (Map.Entry<Integer, Double> entry : score.entrySet()) {
                score.put(entry.getKey(), (entry.getValue() / maxScore));
            }
            nodes = Mergesort.sort(nodes, (o1, o2) -> -Double.compare(score.getOrDefault(o1.getId(), 0D), score.getOrDefault(o2.getId(), 0D)));
            TestApp.debug("Sort nodes by k-shortest-path (%dms) >> Done%n", (System.currentTimeMillis() - time));
        }

        // TODO :NothingOfConcern
        if(bestLower != Integer.MIN_VALUE && bestUpper != Integer.MAX_VALUE) {
            if(bestLower == bestUpper) {
                return new Result(bestLower, bestUpper, bestLower);
            } else {
                return new Result(bestLower, bestUpper, -1);
            }
        } else if(bestLower != Integer.MIN_VALUE) {
            return new Result(bestLower, -1, -1);
        } else if(bestUpper != Integer.MAX_VALUE) {
            return new Result(-1, bestUpper, -1);
        } else{
            return new Result(-1, -1, -1);
        }
    }


    private static boolean divider(LinkedList<Graph> subgraphs, Graph graph) {

        if(graph.getNodes().size() == 1) {
            return false;
        }

        Optional<Node> nodeOptional = graph.getNodes().values().stream()
                .filter(e -> graph.getDegree(e.getId()) == graph.getNodes().size() - 1)
                .findAny();

        if(!nodeOptional.isPresent()) return false;

        // remove fully-connected node
        Node n = nodeOptional.get();
        graph.getNodes().remove(n.getId()); //remove the fully-connected node
        List<Node> neighbours = graph.getNeighbours(n);
        graph.getEdges().remove(n.getId()); //remove all the edges from fully-connected node -> B
        // remove all the edges from B -> fully-connected node
        neighbours.forEach(neighbour -> graph.getEdges(neighbour.getId()).remove(n.getId()));

        // find subgraphs
        final int subSize = subgraphs.size();
        for (Node node : graph.getNodes().values()) {
            // n.v != -1 means that it already belongs to another subgraph so we can skip it
            if (node.getValue() != -1) continue;
            subgraphs.add(discoverGraph(graph, node));
        }

        return (subgraphs.size() > subSize);
    }

    public static Graph discoverGraph(Graph og, Node origin) {

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
                if (!newGraph.hasNode(neighbour.getId())) { // new graph doesn't have neighbour yet so just add it to avoid errors
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
