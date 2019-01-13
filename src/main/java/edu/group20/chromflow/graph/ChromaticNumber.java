package edu.group20.chromflow.graph;

import Jama.Matrix;
import edu.group20.chromflow.GephiConverter;
import edu.group20.chromflow.TestApp;
import edu.group20.chromflow.util.Mergesort;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChromaticNumber {

    // overview >> https://thorehusfeldt.files.wordpress.com/2010/08/gca.pdf
    // old & maybe complicated >> https://sci-hub.se/https://dl.acm.org/citation.cfm?id=321837
    // simple overview >> https://www.ics.uci.edu/~eppstein/pubs/Epp-WADS-01-slides.pdf
    // max independent sets >> https://www.cs.rit.edu/~ark/fall2016/654/team/11/report.pdf
    // >> https://blogs.msdn.microsoft.com/ericlippert/tag/graph-colouring/
    // Epstein overview >> https://www.ics.uci.edu/~eppstein/pubs/graph-color.html

    private static ScheduledThreadPoolExecutor schedule = new ScheduledThreadPoolExecutor(4);

    /**
     * The time limits for the different modes.
     */
    public final static long TIME_LIMIT_EXACT = TimeUnit.SECONDS.toNanos(60);
    public final static long TIME_LIMIT_LOWER = TimeUnit.SECONDS.toNanos(10);
    public final static long TIME_LIMIT_UPPER = TimeUnit.SECONDS.toNanos(10);

    /**
     * The different types of information somebody can request from this class for any given graph.
     */
    public enum Type {
        UPPER,
        LOWER,
        EXACT
    }

    private static void clean(Graph graph) {

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

        /*
        while (graph.getNodes().isEmpty()) { //TODO
            HashSet<Node> clique = new HashSet<>();
            int min = bronKerboschWithPivot(
                    graph,
                    clique,
                    graph.getNodes().values().stream().collect(Collectors.toCollection(HashSet::new)),
                    new HashSet<>());
            if(min == 0) break;

            clique.forEach(node -> {

            });
            TestApp.debugln("clique >>" + min + " <-> " + clique.size());
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Integer id : graph.getNodes().keySet()) {
            min = Math.min(min, graph.getDegree(id));
            max = Math.max(max, graph.getDegree(id));
        }
        TestApp.debugln("minmax>>" + min + " <> " + max);
        */


        TestApp.debug("Cleaning (%dms) >> Removing singles, nodes: %d (%.6f%%), edges: %d (%.6f%%), density: %.6f%% (%.6f%%) %n",
                (System.currentTimeMillis() - time),
                graph.getNodes().size(),
                (1D - (graph.getNodes().size() / inital_nodes)) * 100,
                graph.getEdgeCount(),
                (1D - (graph.getEdgeCount() / inital_edges)) * 100,
                graph.getDensity() * 100,
                inital_density * 100
            );
    }

    /**
     * Computes the requested data in a async-fashion, and supplies it back via a callback.
     * @param type The type of information that gets requested.
     * @param graph The graph we want to perform the computations on.
     * @param consumer The consumers gets called when a result is ready.
     */
    public static void computeAsync(Type type, Graph graph, Consumer<Result> consumer) {
        CompletableFuture.supplyAsync(() -> compute(type, graph, false), schedule).thenAccept(consumer);
    }

    /**
     * Computes the requested data.
     * @param type The type of information that gets requested.
     * @param graph The graph we want to perform the computations on.
     * @param runTimeBound Terminates the algorithm after a time limit.
     * @return Never null, always a result.
     */
    public static Result compute(Type type, Graph graph, boolean runTimeBound) {
        graph.reset();
        clean(graph);
        GephiConverter.generateGephiFile(graph);
        switch (type) {

            case LOWER: return runTimeBound ? limitedTimeLowerBound(graph) : new Result(null,-1, lowerBound(graph), -1, true);
            case UPPER: return runTimeBound ? limitedTimeUpper(graph) : new Result(null,-1, -1, upperBound(graph, UpperBoundMode.DEGREE_DESC), true);
            case EXACT: return runTimeBound ? limitedTimeExactTest(graph) : exactTest(graph, false);

        }
        throw new IllegalStateException();
    }

    //---

    /**
     * Runs the exact tests in a time-limited fashion, this means that the method is guaranteed to finish after {@link ChromaticNumber#TIME_LIMIT_EXACT}.
     * It does not always yield a finished computation, if it finishes earlier then it will return earlier.
     * @param graph The graph to run the computation on.
     * @return Never null, the result.
     */
    private static Result limitedTimeExactTest(Graph graph) {
        return timeBoundMethodExecution(new MethodRunnable() {
            @Override
            public void run() {
                this.setResult(exactTest(graph, true));
            }
        }, TIME_LIMIT_EXACT);
    }

    /**
     * Runs the lower-bound tests in a time-limited fashion, this means that the method is guaranteed to finish after {@link ChromaticNumber#TIME_LIMIT_LOWER}.
     * It does not always yield a finished computation, if it finishes earlier then it will return earlier.
     * @param graph The graph to run the computation on.
     * @return Never null, the result.
     */
    private static Result limitedTimeLowerBound(Graph graph) {
        Result result = timeBoundMethodExecution(new MethodRunnable() {
            @Override
            public void run() {
                Result r = new Result(null,-1, lowerBound(graph), -1, true);
                this.setResult(r);
            }
        }, TIME_LIMIT_LOWER);

        if(result.getLower() == -1) {
            result = new Result(null,-1, basicLowerBound(graph), -1, true);
        }

        return result;
    }

    /**
     * Runs the upper-bound tests in a time-limited fashion, this means that the method is guaranteed to finish after {@link ChromaticNumber#TIME_LIMIT_UPPER}.
     * It does not always yield a finished computation, if it finishes earlier then it will return earlier.
     * @param graph The graph to run the computation on.
     * @return Never null, the result.
     */
    private static Result limitedTimeUpper(Graph graph) {
        Result result = timeBoundMethodExecution(new MethodRunnable() {
            @Override
            public void run() {
                Result r = new Result(null,-1, -1, upperBound(graph, UpperBoundMode.DEGREE_DESC), true);
                this.setResult(r);
            }
        }, TIME_LIMIT_UPPER);

        if(result.getUpper() == -1) {
            result = new Result(null,0, 0, simpleUpperBound(graph), true);
        }
        return result;
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
    private static Result exactTest(Graph graph, boolean runTimeBound) {
        //---
        graph.reset();
        long time = System.currentTimeMillis();
        final int upper = runTimeBound ? limitedTimeUpper(graph).getUpper() : upperBound(graph, UpperBoundMode.DEGREE_DESC);
        TestApp.debug("Upper bound (%dms) >> %d%n", (System.currentTimeMillis() - time), upper);
        TestApp.kelkOutput("NEW BEST UPPER BOUND = %d%n", upper);

        int lower = 1;
        TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", lower);

        if(upper > 4) {
            graph.reset();
            time = System.currentTimeMillis();
            lower = TestLowerBound.search(graph);
            TestApp.debug("Lower bound (%dms) >> %d%n", (System.currentTimeMillis() - time), lower);
            TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", lower);

            if (lower > upper) {
                lower = 1;
            }

            if (upper == lower) {
                TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", lower);
                TestApp.debug("<Exact Test>>> Exact: %d%n", lower);
                return new Result(graph, upper, upper, upper, true);
            }/* else if(lower * 2 < upper) {
                graph.reset();
                lower = Math.max(lower, runTimeBound ? limitedTimeLowerBound(graph).getLower() : lowerBound(graph));

                TestApp.debug("<Exact Test> Improved Range: [%d..%d]%n", lower, upper);
                TestApp.kelkOutput("NEW BEST LOWER BOUND = %d%n", lower);

                if(upper == lower) {
                    TestApp.debug("<Exact Test>>> Exact: %d%n", lower);
                    TestApp.kelkOutput("CHROMATIC NUMBER = %d%n", lower);
                    return new Result(graph, upper, upper, upper, true);
                }
            }*/
        }

        graph.reset();

        //---
        final boolean SORT_BY_DEGREE_DESC = true;
        final boolean SORT_BY_NEIGHBOURS = false;

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
        //---

        int testValue = upper - 1;
        Graph result = graph.clone();

        time = System.currentTimeMillis();
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

    public static Result lowerBoundEigenvalues(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        double[] eigs = matrix.eig().getRealEigenvalues();

        for(int i = 0; i < eigs.length; i++) {
            min = Math.min(eigs[i], min);
            max = Math.max(eigs[i], max);
        }

        return new Result(null, 0, (int) Math.ceil(1 - max / min), 0, false);
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
            if(isBipartie(graph) /*&&graph.hasOnlyEvenCycles() */) { // TODO get Graph#hasOnlyEvenCycles
                return true;
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
        for(Node n : graph.getNodes().values())
            if(n.getValue() == -1) {
                sc = false;
                break;
            }
        if(sc) return true;

        //--- Check this note for all colours
        for(int c = 1; c <= color_nb; c++) {
            if(exactIsColourAvailable(graph, node, c)) {
                node.setValue(c);

                Node next = graph.getNextAvailableNode(node);
                if(list_index + 1 >= nodes.size() || exact(graph, color_nb,  nodes.get(list_index + 1), nodes, list_index + 1)) {
                //if(next == null || exact(graph, color_nb, next, null, -1)) {
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
     * Runs and returns {@link ChromaticNumber#upperBoundIterative(Graph,UpperBoundMode)}.
     */
    private static int upperBound(Graph graph, UpperBoundMode mode) {
        return upperBoundIterative(graph, mode);
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

    private static boolean isBipartie(Graph graph) {
        Stack<Node> unvisited = graph.getNodes().values().stream()
                .sorted(Comparator.comparingInt(o -> -graph.getEdges(o.getId()).size()))
                .collect(Collectors.toCollection(Stack::new));

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

                if(max > 1) {
                    return false;
                }

            }

        }

        return true;

    }

    //--- LOWER BOUND --
    /**
     * Calls and returns {@link ChromaticNumber#bronKerbosch(Graph, List, List, List)}.
     */
    private static int lowerBound(Graph graph) {
        return bronKerboschWithPivot(graph, new HashSet<>(), new HashSet<>(graph.getNodes().values()), new HashSet<>());
    }

    /**
     * The method runs through the graph and gives back a list with all the cliques
     * within the graph
     * @param graph the considered graph
     * @param _R set of current Nodes in the clique
     * @param _P set of candidate Nodes
     * @param _X set of excluded Nodes
     * @return The size of the biggest clique in the given graph.
     **/
    private static int bronKerbosch(Graph graph, HashSet<Node> _R, HashSet<Node> _P, HashSet<Node> _X) {
        int max = Integer.MIN_VALUE;
        if(_P.isEmpty() && _X.isEmpty()) {
            max = Math.max(max, _R.size());
            return max;
        }

        Iterator<Node> nodeIterator = _P.iterator();
        while (nodeIterator.hasNext()) {

            //---
            Node node = nodeIterator.next();
            //List<Node> neighbours = graph.getEdges(node.getId()).values().stream().map(Node.Edge::getTo).collect(Collectors.toList());
            List<Node> neighbours = graph.getNeighbours(node);

            //---
            HashSet<Node> dR = _R; //List<Node> dR = new ArrayList<>(_R);
            dR.add(node);

            HashSet<Node> dP = _P.stream().filter(neighbours::contains).collect(Collectors.toCollection(HashSet::new));
            HashSet<Node> dX = _X.stream().filter(neighbours::contains).collect(Collectors.toCollection(HashSet::new));

            max = Math.max(bronKerbosch(graph, dR, dP, dX), max);

            //---
            nodeIterator.remove();
            _X.add(node);
        }

        return max;
    }

    private static int bronKerboschWithPivot(Graph graph, HashSet<Node> _R, HashSet<Node> _P, HashSet<Node> _X) {
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


                max = Math.max(max, bronKerboschWithPivot(
                        graph,
                        _R,
                        P1.stream().filter(e -> graph.hasEdge(e.getId(), v.getId())).collect(Collectors.toCollection(HashSet::new)),
                        _X.stream().filter(e -> graph.hasEdge(e.getId(), v.getId())).collect(Collectors.toCollection(HashSet::new))
                ));

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

    //--- Utility

    /**
     * Runs a method in a time-bound fashion. - If the method finishes be fore being done then it just returns normally,
     * otherwise if it runs beyond its target time it will get cancelled.
     * @param runnable The method to run.
     * @param timeInMilliseconds The max execution time.
     * @return The result of running the method.
     */
    private static Result timeBoundMethodExecution(MethodRunnable runnable, final long timeInMilliseconds) {
        Thread thread = new Thread(runnable);
        thread.start();
        long time = System.nanoTime();
        long countdown = time + timeInMilliseconds;

        // TODO replace busy waiting
        while (!runnable.getResult().isReady() && time < countdown) {
            TestApp.debug(""); //for some reason this code does not work without this. is there maybe some sort of byte-code optimisation going on removing this type of loop
            time = System.nanoTime();
        }
        //thread.interrupt();

        return runnable.getResult();

    }

    /**
     * Used to run the methods in a time-bound fashion.
     */
    private static abstract class MethodRunnable implements Runnable {

        private Result result = new Result(null, -1, -1, -1, false);

        @Override
        public abstract void run();

        public void setResult(Result result) {
            this.result = result;
        }

        public Result getResult() {
            return this.result;
        }

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
