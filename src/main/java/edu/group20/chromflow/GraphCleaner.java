package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.GraphStructures;
import edu.group20.chromflow.graph.Node;
import edu.group20.chromflow.util.Mergesort;

import java.util.*;
import java.util.stream.Collectors;

public class GraphCleaner {

    /**
     * Cleans the graph by running several algorithms on it.
     * @param graph The graph to clean.
     * @param lower Lower bound for the chromatic number.
     * @param upper Upper bound for the chromatic number.
     * @return Returns a result class that can contain the chromatic number or better boundss but this is not
     * guaranteed and heavily depends on the structure of the graph.
     */
    public static Result clean(final Graph graph, final int depth, int lower, int upper) {

        if(graph.isComplete()) {
            TestApp.debug("Cleaning (0ms) >> Detected complete graph.%n");
            return new Result(graph.getNodes().size(), graph.getNodes().size(), graph.getNodes().size());
        }

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
                for (Graph g : smallest) {
                    ChromaticNumber.Result r = ChromaticNumber.computeExact(g, true, depth + 1);
                    exact = Math.max(r.getExact() + g.getMeta().getLevel(), exact);
                }

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
        if(false && graph.getNodes().size() < 1000 && lower < 4) {
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
            TestApp.debug("Cleaning (%dms) >> Wheels, even: %s (%d), odd: %s (%d), broke early: %s%n",
                    (System.currentTimeMillis() - time),
                    evenWheels > 0,
                    evenWheels,
                    oddWheels > 0,
                    oddWheels,
                    brokeEarly
            );
            if(oddWheels + evenWheels > 0) {
                lower = Math.max(lower, evenWheels > 0 ? 4 : 3);
            }
        } else {
            TestApp.debugln("Cleaner >> WHEELS CHECK IS DISABLED");
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
            if(kRegular < upper && kRegular != -1 && GraphStructures.Test.isConnected(graph)) {
                time = System.currentTimeMillis();
                if(GraphStructures.Connectivity.OneConnectivity.check(graph)) {
                    TestApp.debug("Cleaning (%dms) > k-regular, one-connectivity",
                            (System.currentTimeMillis() - time)
                    );
                    return new Result(kRegular, kRegular, kRegular);
                } else if(GraphStructures.Connectivity.TwoConnectivity.check(graph)) {
                    upper = Math.min(upper, kRegular);
                    TestApp.debug("Cleaning (%dms) > k-regular, two-connected%n",
                            (System.currentTimeMillis() - time)
                    );
                }
            }

        }

        // disconnect graph at points
        {
            time = System.currentTimeMillis();
            graph.reset();
            Set<Node> points = GraphStructures.Connectivity.Points.getArticulationPoints(graph);

            List<Graph> smallest = new LinkedList<>();

            if(!(points.isEmpty())) {

                graph.reset();
                for (Node p : points) {
                    p.setValue(1);
                    LinkedList<Graph> subgraphs = new LinkedList<>();
                    for (Node n : graph.getNodes().values()) {
                        if (n.getValue() == -1) {
                            subgraphs.add(discoverGraph(graph, n));
                        }
                    }

                    if (subgraphs.size() <= 1) {
                        p.setValue(-1);
                    } else {
                        smallest.addAll(subgraphs);
                    }
                }

                if(!smallest.isEmpty()) {
                    int max = Integer.MIN_VALUE;
                    for (Graph sub : smallest) {
                        ChromaticNumber.Result r = ChromaticNumber.computeExact(sub, true, depth + 1);
                        max = Math.max(max, r.getExact());
                    }

                    TestApp.debug("Cleaning (%dms) > articulation points, sub-graphs: %d, points: %d%n",
                            (System.currentTimeMillis() - time),
                            smallest.size(),
                            points.size()
                    );
                    return new Result(max, max, max);
                }
            } else {
                TestApp.debug("Cleaning (%dms) > articulation points, sub-graphs: 0, points: 0%n",
                        (System.currentTimeMillis() - time),
                        smallest.size(),
                        points.size()
                );
            }
        }


        return new Result(lower, upper, -1);
    }

    /**
     * Tries to split the given graphs into smaller subgraphs at a fully-connected nodes.
     * @param subgraphs The list that will contain the subgraphs of the graph.
     * @param graph The graph to divide.
     * @return Whether or not it could be further divided.
     */
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

    /**
     * Builds a new graph by running a DFS from the origin node and marks all visited nodes with the value '0'.
     * @param og Th original graph.
     * @param origin The node to start at.
     * @return The new graph builds outgoing from the origin node.
     */
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
