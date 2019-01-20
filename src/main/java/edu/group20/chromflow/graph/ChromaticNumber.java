package edu.group20.chromflow.graph;

import edu.group20.chromflow.GraphCleaner;
import edu.group20.chromflow.TestApp;
import edu.group20.chromflow.util.Mergesort;

import java.util.*;
import java.util.stream.Collectors;

public class ChromaticNumber {

    public static Result computeExact(Graph graph, boolean clean) {
        graph.reset();
        final GraphCleaner.Result cleanResult = clean ? GraphCleaner.clean(graph) : new GraphCleaner.Result(-1, -1, -1);
        return exactTest(graph, cleanResult, false);
    }

    /**
     * The basic lower bound checks returns the least amount of connections that can be found in the graph. If the least-amount
     * is only 1 then it will return 2, because there will be at least 2 nodes which means they require at least 2 different
     * colours.
     * @param graph The graph to perform the computation on.
     * @return The lower bound.
     */
    private static int basicLowerBound(Graph graph) {
        int tmp = graph.getEdges().entrySet().stream().mapToInt(e -> e.getValue().size()).min().getAsInt();
        return Math.max(tmp,2) ;
    }


    // --- EXACT_EXPERIMENTAL SECTION ---
    private static Result exactTest(Graph graph, GraphCleaner.Result cleanResult, boolean runTimeBound) {

        // This can happen when GraphCleaner breaks down a fully-connected graph
        if(graph.getNodes().size() == 1) {
            cleanResult = new GraphCleaner.Result(1, 1, 1);
        }

        if(cleanResult.hasExact()) {
            TestApp.kelkOutput("NEW BEST UPPER BOUND = %d%n", cleanResult.getExact());
            TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", cleanResult.getExact());
            TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", cleanResult.getExact());
            return new Result(graph, cleanResult.getExact(), cleanResult.getExact(), cleanResult.getExact(), true);
        }

        //---
        long time = System.currentTimeMillis();
        int upper = upperBoundIterative(graph, UpperBoundMode.SUPERMAN);
        if(cleanResult.hasUpper()) {
             upper = Math.min(cleanResult.getUpper(), upper);
        }
        TestApp.debug("Upper bound (%dms) >> %d%n", (System.currentTimeMillis() - time), upper);
        TestApp.kelkOutput("NEW BEST UPPER BOUND = %d%n", upper);

        /*graph.reset();
        if(graph.getNodes().size() > 1000) {
            upper = upperBound(graph, UpperBoundMode.DEGREE_DESC);
        } else {
            upper = Integer.MAX_VALUE;
            for(int i = 0; i < 10000; i++) {
                graph.reset();
                upper = upperBound(graph, UpperBoundMode.SHUFFLE);
            }
        }
        TestApp.debug("Upper bound (%dms) >> %d%n", (System.currentTimeMillis() - time), upper);
        */



        int lower = Math.max(cleanResult.getLower(), graph.getEdges().isEmpty() ? 1 : 2);
        TestApp.debug("Lower bound (0ms) >> %d%n", lower);
        TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", lower);

        if (upper == lower) {
            TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", lower);
            TestApp.debug("<Exact Test>>> Exact: %d%n", lower);
            return new Result(graph, upper, upper, upper, true);
        } else if((upper > 4 || graph.getNodes().size() < 1000)) {
            graph.reset();
            time = System.currentTimeMillis();
            lower = Math.max(lower, lowerBound(graph, upper));
            TestApp.debug("Lower bound (%dms) >> %d%n", (System.currentTimeMillis() - time), lower);
            TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", lower);


            if (upper == lower) {
                TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", lower);
                TestApp.debug("<Exact Test>>> Exact: %d%n", lower);
                return new Result(graph, upper, upper, upper, true);
            }
        }

        graph.reset();

        //---
        final boolean SORT_BY_DEGREE_DESC = true;
        final boolean SORT_BY_NEIGHBOURS = false;
        final boolean SORT_BY_K_SHORTEST_PATH = false;

        LinkedList<Node> nodes = new LinkedList<>(graph.getNodes().values());

        if(SORT_BY_DEGREE_DESC) {
            time = System.currentTimeMillis();
            nodes = Mergesort.sort(nodes, (o1, o2) -> -Integer.compare(graph.getDegree(o1.getId()), graph.getDegree(o2.getId())));
            TestApp.debug("Sort nodes by degree (%dms) >> Done%n", (System.currentTimeMillis() - time));
        }
        if(SORT_BY_NEIGHBOURS) {
            time = System.currentTimeMillis();
            nodes = Mergesort.sort(nodes, (o1, o2) -> {
                if(o1 == o2) return 0;

                if (graph.hasEdge(o1.getId(), o2.getId())) {
                    return -1;
                } else {
                    return 1;
                }
            });
            TestApp.debug("Sort nodes by relation (%dms) >> Done%n", (System.currentTimeMillis() - time));
        }
        if(SORT_BY_K_SHORTEST_PATH) {
            time = System.currentTimeMillis();
            graph.reset();
            Map<Integer, Integer> score = new HashMap<>();
            for(Node n : graph.getNodes().values()) {
                graph.reset();
                Dijkstra.buildPaths(graph, n.getId()).values().forEach(i -> {
                    score.put(i, score.getOrDefault(i, 0) + 1);
                });
            }
            nodes = Mergesort.sort(nodes, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return -Integer.compare(score.get(o1.getId()), score.get(o2.getId()));
                }
            });

            graph.reset();
            int value = Integer.MAX_VALUE;
            for(Node n : nodes) {
                if(n.getValue() == -1) {
                    n.setValue(value);
                    int finalValue = value;
                    graph.getEdges(n.getId()).values().forEach(e -> e.getTo().setValue(finalValue));
                    value--;
                }
            }
            nodes = Mergesort.sort(nodes, new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return -Integer.compare(o1.getValue(), o2.getValue());
                }
            });
            TestApp.debug("Sort nodes by k-shortest-path (%dms) >> Done%n", (System.currentTimeMillis() - time));
        }
        //---

        int testValue = upper - 1;
        Graph result = graph.clone();

        time = System.currentTimeMillis();
        graph.reset();
        while(exact(graph, nodes, testValue)) {
            TestApp.debug("Exact Test >> The graph CAN be coloured with %d colours.%n", testValue);
            TestApp.kelkOutput("NEW BEST UPPER BOUND = %d%n", testValue);

            if(testValue == lower) {
                testValue--;
                break;
            }

            graph.reset();
            testValue--;

        }
        final int exact = testValue+1;

        TestApp.debug("Exact Test (%dms) >> Exact: %d%n", (System.currentTimeMillis() - time), exact);
        TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", exact);

        return new Result(result, exact, lower, upper, true);
    }

    private static boolean exact(Graph graph, List<Node> nodes, int colours) {

        if(graph.getNodes().size() <= colours) {
            return true;
        }

        //--- 1
        if(colours == 1 && graph.getEdges().isEmpty()) {
            return true; //coloring the graph with only one colour when there is at least one edge is impossible
        } else if(colours == 1) {
            return false;
        }

        //--- 2 isBipartie
        if(colours == 2) {
            if(nodes.size() % 2 == 0 && GraphStructures.EVBAsed.isBipartiteEigenValue(graph) /*&&graph.hasOnlyEvenCycles() */) { // TODO get Graph#hasOnlyEvenCycles
                return true;
            } else {
                graph.reset();
            }
        }

        //--- 3

        //--- 4
        if(colours == 4) {
            //https://www.quora.com/Is-there-an-easy-method-to-determine-if-a-graph-is-planar-or-not
            // Euler criteria for planar graphs
            if(graph.getNodes().size() >= 3) {
                if(graph.getEdgeCount() <= (3 * graph.getNodes().size() - 6)
                    //graph.noCyleLength == 3 && graph.getEdges() / 2 <= 2 * graph.getNodes().size() - 4
                ) {
                    // check if planar
                }
            }
        }

        // shuffle
        //List<Node> nodes = new ArrayList<>(graph.getNodes().values());
        //Collections.shuffle(nodes);

        return exact(graph, colours, nodes.get(0), nodes, 0);
    }

    /**
     * Runs to check if the graph can be coloured with a certain amount of colours.
     * @param graph The graph it has to check.
     * @param color_nb The amount of colours.
     * @param node The node it starts at.
     * @return Whether or not it can be coloured in the given amount of colours.
     */
    private static boolean exact(Graph graph, int color_nb, Node node, List<Node> nodes, int list_index) {
        //--- Are all nodes coloured? If so, we are done.
        /*if(graph.getNodes().values().stream().noneMatch(e -> e.getValue() == -1)) {
            return true;
        }*/

        boolean sc = true;
        for(Node n : nodes) {
            if (n.getValue() == -1) {
                sc = false;
                break;
            }
        }
        if(sc) return true;

        //--- Check this note for all colours
        for(int c = 1; c <= color_nb; c++) {
            if(exactIsColourAvailable(graph, node, c)) {
                node.setValue(c);

                if(list_index + 1 >= nodes.size() || exact(graph, color_nb,  nodes.get(list_index + 1), nodes, list_index + 1)) {
                    return true;
                }

                node.setValue(-1);
            }
        }

        return false;
    }

    /**
     * Check if any of the nodes neighbour already use that colour.
     * @param graph The graph the node belongs to.
     * @param node The node we have to check.
     * @param colour The colour we want to use to colour this node.
     * @return True, if the colour can be used, otherwise false.
     */
    private static boolean exactIsColourAvailable(Graph graph, Node node, int colour) {
        for(Node n : graph.getNeighbours(node)) {
            if(n.getValue() == colour) {
                return false;
            }
        }
        return true;
        //return graph.getEdges(node.getId()).values().stream().noneMatch(e -> e.getTo().getValue() == colour);
    }

    // --- UPPER BOUND SECTION ---

    /**
     * Returns (the maximum amount of edges + 1) of a node in the graph.
     * @param graph The graph to perform the computation on.
     * @return The upper bound for the given graph.
     */
    private static int simpleUpperBound(Graph graph) {
        return graph.getEdges().values().stream().mapToInt(Map::size).max().getAsInt() + 1;
    }

    /**
     * Colours the graph with the greedy-algorithm. - It simply goes to every node and at every node it checks
     * if it can just reuse a colour to colour the node, or if it has to create a new colour.
     * @param graph The graph to perform the computation on.
     * @return The upper bound, the amount of colours used to colour the graph.
     */
    private static int upperBoundIterative(Graph graph, UpperBoundMode upperBoundMode) {
        Stack<Node> unvisited = null;
        //--- Build different unvisited maps
        switch (upperBoundMode){
            case DEGREE_DESC:
                //--- map ordered by degree of nodes descending
                unvisited = graph.getNodes().values().stream()
                        .sorted(Comparator.comparingInt(o -> -graph.getEdges(o.getId()).size()))
                        .collect(Collectors.toCollection(Stack::new));
                break;

            case SHUFFLE:
                //--- map starting from random starting point
                unvisited = graph.getNodes().values().stream().collect(Collectors.toCollection(Stack::new));
                Collections.shuffle(unvisited);
                break;

            case UNORDERED:
                //--- map with order from graph
                unvisited = graph.getNodes().values().stream().collect(Collectors.toCollection(Stack::new));
                break;

            //TODO verify correctness of code (passes all unit tests but you never know ¯\_(ツ)_/¯
            case SUPERMAN:
                Map<Integer, Integer> degrees = new HashMap<>();
                HashSet<Integer> removed = new HashSet<>();
                graph.getEdges().forEach((fromId, edges) -> degrees.put(fromId, edges.size()));
                unvisited = new Stack<>();
                while (removed.size() < /*!=*/ graph.getNodes().size()) {
                    int node = Integer.MAX_VALUE;
                    for(int id : graph.getNodes().keySet()) {
                        if(!(removed.contains(id))) {
                            if(degrees.getOrDefault(node, Integer.MAX_VALUE) > degrees.get(id)) {
                                node = id;
                            }
                        }
                    }

                    for(int neighbour : graph.getEdges(node).keySet()) {
                        if(!(removed.contains(neighbour))) {
                            degrees.compute(neighbour, (key, oldValue) -> oldValue -1);
                        }
                    }
                    unvisited.add(graph.getNode(node));
                    removed.add(node);
                }
                break;

        }

        int max = 0;
        while (!unvisited.isEmpty()){
            Node node = unvisited.pop();

            //--- What colours does its neighbours have?
            Collection<Node.Edge> edges = graph.getEdges(node.getId()).values();
            List<Integer> colours = edges.stream()
                    .filter(edge -> edge.getTo().getValue() != -1)
                    .map(edge -> edge.getTo().getValue())
                    .collect(Collectors.toList());

            //--- No colours -> first node being visited in the graph
            if (colours.isEmpty()) {
                node.setValue(0);
            }
            //--- At least one colour -> not the first node anymore
            else {

                //--- "Highest"  value/colour adjacent to the node
                final int maxColour = colours.stream().max(Comparator.naturalOrder()).get();

                int colour = 0; // Lowest value we can chose for a valid colour

                //--- try to ideally find an existing colour that we can reuse
                while (colour <= maxColour) {
                    if (!colours.contains(colour)) {
                        break;
                    }
                    colour++;
                }

                node.setValue(colour);
                max = Math.max(max, colour);

            }

        }

        return max + 1;

    }

    //--- LOWER BOUND --
    private static int lowerBound(Graph graph, int upperBound) {
        return bronKerboschWithPivot(graph, new HashSet<>(), new HashSet<>(graph.getNodes().values()), new HashSet<>(), upperBound);
    }

    public static int bronKerboschWithPivot(Graph graph, HashSet<Node> _R, HashSet<Node> _P, HashSet<Node> _X, final int upperBound) {
        int max = Integer.MIN_VALUE;
        if(_P.isEmpty() && _X.isEmpty()) {
            max = Math.max(max, _R.size());
            return max;
        }

        //--- pivot
        HashSet<Node> P1 = new HashSet<>(_P); //List<Node> dR = new ArrayList<>(_R);
        final Optional<Node> pivotA = _P.stream().max((o1, o2) -> -Integer.compare(graph.getDegree(o1.getId()), graph.getDegree(o2.getId())));
        final Optional<Node> pivotX = _X.stream().max((o1, o2) -> -Integer.compare(graph.getDegree(o1.getId()), graph.getDegree(o2.getId())));

        Node pivot = null;
        if(pivotA.isPresent() && pivotX.isPresent()) {
            pivot = graph.getDegree(pivotA.get().getId()) > graph.getDegree(pivotX.get().getId()) ? pivotA.get() : pivotX.get();
        } else if(pivotA.isPresent()) {
            pivot = pivotA.get();
        } else if(pivotX.isPresent()) {
            pivot = pivotX.get();
        }

        if(pivot != null) {
            final Node finalPivot = pivot;
            _P.removeIf(e -> graph.hasEdge(e.getId(), finalPivot.getId()));

            Iterator<Node> nodeIterator = _P.iterator();
            while (nodeIterator.hasNext()) {
                Node v = nodeIterator.next();

                _R.add(v);


                int tMax = max;
                max = Math.max(max, bronKerboschWithPivot(
                        graph,
                        _R,
                        P1.stream().filter(e -> graph.hasEdge(e.getId(), v.getId())).collect(Collectors.toCollection(HashSet::new)),
                        _X.stream().filter(e -> graph.hasEdge(e.getId(), v.getId())).collect(Collectors.toCollection(HashSet::new)),
                        upperBound
                ));

                // TODO Verify, if the max clique is equal to our upperBound then we are done right? Greatly reduces
                //  time to compute lowerBound for benchmark/miles1500.col
                if(max == upperBound) {
                    return max;
                }

                _R.remove(v);
                P1.remove(v);
                _X.add(v);
                //---


                nodeIterator.remove();
                _X.add(v);
            }
        }

        return max;
    }

    /**
     * Contains the result of the computations.
     */
    public static class Result {

        private Graph solution;

        private int exact = -1;
        private int upper = -1;
        private int lower = -1;

        private boolean isReady = false;

        public Result(Graph solution, int exact, int lower, int upper, boolean isReady) {
            this.solution = solution;
            this.exact = exact;
            this.lower = lower;
            this.upper = upper;
            this.isReady = isReady;
        }

        public Graph getSolution() {
            return solution;
        }

        public void ready() {
            this.isReady = true;
        }

        public boolean isReady() {
            return isReady;
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


        @Override
        public String toString() {
            return String.format("[Result;isReady=%s,exact=%d,lower=%d,upper=%d]", isReady, exact, lower, upper);
        }
    }

}
