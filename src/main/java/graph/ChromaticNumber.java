package graph;

import org.tensorflow.op.core.UpperBound;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
     * If this flag is set to true then this class will print useful messages that help the developer to debug
     * issues, if required.
     */
    public static boolean DEBUG_FLAG = true;

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
        EXACT,
        EXACT_EXPERIMENTAL,
        EXACT_LOW_TO_HIGH
    }

    public static void lawler(Graph graph) {
        List<List<Node>> independentSets = new LinkedList<>();
        List<List<Node>> maximalIndependentSets = new LinkedList<>();
        independentSets(graph, independentSets, maximalIndependentSets);

        independentSets.sort(Comparator.comparingInt(List::size));

        Map<Integer, Integer> ncolours = new HashMap<>();
        ncolours.put(0, compute(Type.EXACT_EXPERIMENTAL, graph, false).getExact());

        System.out.println();
        for(int i = 0; i < independentSets.size(); i++) {

            List<Node> S = independentSets.get(i);

            graph.reset();
            if(exact(graph, 3, S.get(0), S, 0)) {
                ncolours.put(i, 3);
                final int limit = S.size() / 3;


            }

        }

    }

    private static void independentSets(Graph graph, List<List<Node>> independentSets, List<List<Node>> maxIndependentSets) {
        int maxSize = 0;
        for(Node n : graph.getNodes().values()) {

            List<Node> subset = new LinkedList<>(graph.getNodes().values());
            subset.remove(n);
            for(Node.Edge neighbour : graph.getEdges(n.getId())) {
                subset.remove(neighbour.getTo());
            }

            independentSets.add(subset);

            if(subset.size() > maxSize) {
                maxSize = subset.size();
                maxIndependentSets.clear();
                maxIndependentSets.add(subset);
            } else if(maxSize == subset.size()) {
                maxIndependentSets.add(subset);
            }
        }

    }

    private static void clean(Graph graph) {

        final double inital_nodes = graph.getNodes().size();
        final double inital_edges = (graph.getEdges().values().stream().mapToInt(List::size).sum() / 2D);

        // remove singles
        boolean removedSmth = true;
        while (removedSmth) {
            removedSmth = false;
            {
                Iterator<Map.Entry<Integer, Node>> i = graph.getNodes().entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry<Integer, Node> e = i.next();
                    Node from = e.getValue();

                    if (graph.getEdges(from.getId()).size() <= 1) {
                        if (graph.getEdges(from.getId()).size() == 1) {
                            int toId = graph.getEdges(from.getId()).get(0).getTo().getId();
                            graph.getEdges(toId).removeIf(eA -> eA.getTo().getId() == from.getId());
                        }
                        graph.getEdges().remove(e.getValue().getId());
                        i.remove();
                        removedSmth = true;
                    }
                }
            }
        }



        System.out.printf("Debug >> Removing singles, nodes: %d (%.8f), edges: %d%n", graph.getNodes().size(), (1D - (graph.getNodes().size() / inital_nodes)), graph.getEdges().values().stream().mapToInt(List::size).sum() / 2);
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
        //clean(graph);
        switch (type) {

            case LOWER: return runTimeBound ? limitedTimeLowerBound(graph) : new Result(null,-1, lowerBound(graph), -1, true);
            case UPPER: return runTimeBound ? limitedTimeUpper(graph) : new Result(null,-1, -1, upperBound(graph,1), true);
            case EXACT: return runTimeBound ? limitedTimeExactTest(graph) : exactTest(graph, false);
            case EXACT_LOW_TO_HIGH: return exactTestLowToHigh(graph, false);
            case EXACT_EXPERIMENTAL: return runTimeBound ? limitedTimeExactTest(graph) : exactParallelled(graph, false);

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
                Result r = new Result(null,-1, -1, upperBound(graph,1), true);
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
        return (tmp == 1) ? 2 : tmp;
    }


    // --- EXACT_EXPERIMENTAL SECTION ---
    private static Result exactTest(Graph graph, boolean runTimeBound) {
        //---
        final int upper = runTimeBound ? limitedTimeUpper(graph).getUpper() : upperBound(graph,1);
        final int lower = runTimeBound ? limitedTimeLowerBound(graph).getLower() : lowerBound(graph);
        System.out.printf("<Exact Test> Range: [%d..%d]%n", lower, upper);

        graph.reset();

        //TODO implement binary search
        if(upper == lower) {
            exact(graph, upper);
            System.out.printf("<Exact Test>>> Exact: %d%n", lower);
            return new Result(graph, upper, upper, upper, true);
        }

        // TODO if graph is bipartite -> chromatic number = 2 - https://math.stackexchange.com/questions/32508/a-graph-g-is-bipartite-if-and-only-if-g-can-be-coloured-with-2-colours
        // TODO find for 3, 4(, and maybe more)

        int testValue = upper - 1;
        Graph result = graph.clone();
        while(exact(graph, testValue)) {
            System.out.printf("<Exact Test> The graph CAN be coloured with %d colours.%n", testValue);
            result = graph.clone();

            if(testValue == lower) {
                testValue--;
                break;
            }

            graph.reset();
            testValue--;

        }


        final int exact = testValue+1;
        System.out.printf("<Exact Test>>  Exact: %d%n", exact);
        return new Result(result, exact, lower, upper, true);
    }

    private static Result exactTestLowToHigh(Graph graph, boolean runTimeBound) {
        //---
        final int upper = runTimeBound ? limitedTimeUpper(graph).getUpper() : upperBound(graph,1);
        final int lower = runTimeBound ? limitedTimeLowerBound(graph).getLower() : lowerBound(graph);
        System.out.printf("<Exact Test> Range: [%d..%d]%n", lower, upper);

        graph.reset();

        if(upper == lower) {
            exact(graph, upper);
            System.out.printf("<Exact Test>>> Exact: %d%n", lower);
            return new Result(graph, upper, upper, upper, true);
        }

        int testValue = lower;
        Graph result = graph.clone();
        while(exact(graph, testValue)) {
            System.out.printf("<Exact Test> The graph CAN be coloured with %d colours.%n", testValue);
            result = graph.clone();

            if(testValue == upper) {
                break;
            }

            graph.reset();
            testValue++;

        }


        final int exact = testValue;
        System.out.printf("<Exact Test>>  Exact: %d%n", exact);
        return new Result(result, exact, lower, upper, true);
    }

    /**
     * This code is experimental and still has a couple of problems with race-conditions therefore it is currently not
     * usable in an non-experimental environment. - This code uses the property of our current exact chromatic number test,
     * we always start at the upper-bound and work our way down to the lower-bound. This means that we actually only need
     * the upper-bound to start any meaningful tests, so we only compute the upper-bound and then start both the exact-tests
     * and lower-bound at the same time.
     *  - If the lower-bound is greater finishes, and it is equal to the upper bound -> then we are done and just cancel the current
     *  test and return the upper/lower bound because they are the exact chromatic number.
     *  - If the lower-bound is less then the current value we test then we at last know now when to stop.
     * This is beneficial because the lower-bound takes up a significant amount of computational time, even though it
     * does not yield any information that is crucial to running the exact tests.
     * @param graph The graph to perform the computation on.
     * @param runTimeBound Whether or not to time limit the execution of the upper & lower bound.
     * @return Never null, the results of the computations.
     */
    private static Result exactParallelled(Graph graph, boolean runTimeBound) {
        //--- the upper bound that we either find by running our upper-bound algorithm
        final AtomicInteger upper = new AtomicInteger(runTimeBound ? limitedTimeUpper(graph).getUpper() : upperBound(graph,1));

        // if the upper bound algorithm fails, we cannot do anything anymore
        if (upper.get() == -1) {
            return new Result(null, -1, -1, -1, true);
        }

        // run the lower bound algorithm, if it is supposed to be time-limited
        AtomicInteger lower = new AtomicInteger(basicLowerBound(graph));
        if (runTimeBound) {
            lower.set(limitedTimeLowerBound(graph).getLower());
        }

        //--- the current range of values we are expecting to inspect
        final int upperResult = upper.get();
        final int lowerResult = lower.get();
        if (DEBUG_FLAG)
            System.out.printf("<Exact Test: %d> Range: [%d..%d]%n", graph.hashCode(), lowerResult, upperResult);

        //--- if the bounds are equal then this is the chromatic number
        if (upperResult == lowerResult) {
            return new Result(graph, lowerResult, lowerResult, upperResult, true);
        }

        //--- we do start testing upperBound-1 because we know for sure that upper-bound itself is going to work, so
        // testing it is a waste of resources.
        AtomicInteger testValue = new AtomicInteger(upper.get());
        testValue.addAndGet(-1);
        graph.reset();

        AtomicReference<Graph> colouredGraph = new AtomicReference<>();

        //--- Run the exact test async, so we can run the lower-bound algorithm in parallel
        final AtomicReference<Thread> exactFuture = new AtomicReference<>();
        final AtomicReference<Thread> lowerBoundFuture = new AtomicReference<>();

        {
            Thread t = new Thread(() -> {

                while (exactFuture.get() == null) {
                }

                while (!exactFuture.get().isInterrupted() && exact(graph, testValue.get())) {
                    if (DEBUG_FLAG)
                        System.out.printf("<Exact Test: %d> The graph CAN be coloured with %d colours.%n", graph.hashCode(), testValue.get());
                    colouredGraph.set(graph.clone());
                    graph.reset();

                    if (testValue.get() == lower.get()) {
                        if (!exactFuture.get().isInterrupted()) {
                            testValue.addAndGet(-1);
                        }
                        break;
                    }
                    // TODO cleanup
                    else if (testValue.get() < lower.get()) {
                        if (!exactFuture.get().isInterrupted()) {
                            testValue.set(lower.get() - 1);
                        }
                        break;
                    }
                    testValue.addAndGet(-1);
                }

                while (lowerBoundFuture.get() == null) { }

                if (!(exactFuture.get().isInterrupted())) {
                    if (lowerBoundFuture.get() != null) {
                        lowerBoundFuture.get().interrupt();
                    }
                }

            });
            exactFuture.set(t);
            t.start();
        }

        //--- run the lower-bound algorithm async at the same time as the exact tests are going on
        if(!(runTimeBound)) {
            Thread t = new Thread(() -> {
                final int result = lowerBound(graph);

                lower.set(result);
                if(DEBUG_FLAG) System.out.printf("<Exact Test: %d> Updated lower bound: %d%n", graph.hashCode(), lower.get());
                if(DEBUG_FLAG) System.out.printf("<Exact Test: %d> Range: [%d..%d]%n", graph.hashCode(), lower.get(), upperResult);

                //--- if the result is greater the upper-bound
                // then we are done, and the result (lower-bound) is the chromatic number.
                if (result == upperResult) {
                    exactFuture.get().interrupt(); // cancel the main check to stop it from eroding our data.

                    while (lowerBoundFuture.get() == null) {}

                    if (!lowerBoundFuture.get().isInterrupted()) {
                        testValue.set(result - 1); // set result
                        if(DEBUG_FLAG) System.out.printf("<Exact Test: %d> Exact: %d (determined by lower-bound async execution)%n", graph.hashCode(), (testValue.get() + 1));
                    }
                }
            });
            lowerBoundFuture.set(t);
            t.start();
        }

        //--- we have to wait for both the lower-bound (if running at all), and the exact test to finish before submitting
        // any results
        try {
            if(lowerBoundFuture.get() != null) {
                lowerBoundFuture.get().join();
            }

            exactFuture.get().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //--- we are done, we have to increase the upper-bound by +1 because it contains the current upper-bound we tested
        // was no longer valid so the value before that is the chromatic number.
        final int exact = testValue.get() + 1;
        if(DEBUG_FLAG) System.out.printf("<Exact Test: %d> Exact: %d%n", graph.hashCode(), exact);
        return new Result(colouredGraph.get(), exact, lower.get(), upper.get(), true);


    }

    private static boolean exact(Graph graph, int colours) {

        // sort by degree descending
        List<Node> nodes = graph.getNodes().values().stream().sorted(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Integer.compare(graph.getEdges(o2.getId()).size(), graph.getEdges(o1.getId()).size());
            }
        }).collect(Collectors.toList());


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

                //Node next = graph.getNextAvailableNode(node);
                if(list_index + 1 < nodes.size()  && exact(graph, color_nb,  nodes.get(list_index + 1), nodes, list_index + 1)) {
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
        return graph.getEdges(node.getId()).stream().noneMatch(e -> e.getTo().getValue() == colour);
    }

    // --- UPPER BOUND SECTION ---

    /**
     * Returns (the maximum amount of edges + 1) of a node in the graph.
     * @param graph The graph to perform the computation on.
     * @return The upper bound for the given graph.
     */
    private static int simpleUpperBound(Graph graph) {
        return graph.getEdges().values().stream().mapToInt(List::size).max().getAsInt() + 1;
    }

    /**
     * Runs and returns {@link ChromaticNumber#upperBoundIterative(Graph)}.
     */
    private static int upperBound(Graph graph, int mode) {
        return upperBoundIterative(graph, mode);
    }

    /**
     * Colours the graph with the greedy-algorithm. - It simply goes to every node and at every node it checks
     * if it can just reuse a colour to colour the node, or if it has to create a new colour.
     * @param graph The graph to perform the computation on.
     * @return The upper bound, the amount of colours used to colour the graph.
     */
    private static int upperBoundIterative(Graph graph, int mode) {

        Stack<Node> unvisited = null;
        //--- Build different unvisited maps
        switch (mode){
            case 1:
                //--- map ordered by degree of nodes descending
                unvisited = graph.getNodes().values().stream()
                        .sorted(Comparator.comparingInt(o -> graph.getEdges(o.getId()).size()))
                        .collect(Collectors.toCollection(Stack::new));
                break;

            case 2:
                //--- map starting from random starting point
                unvisited = graph.getNodes().values().stream().collect(Collectors.toCollection(Stack::new));
                Collections.shuffle(unvisited);
                break;

            case 3:
                //--- map with order from graph
                unvisited = graph.getNodes().values().stream().collect(Collectors.toCollection(Stack::new));
                break;
        }

        int max = 0;
        while (!unvisited.isEmpty()){
            Node node = unvisited.pop();

            //--- What colours does its neighbours have?
            List<Node.Edge> edges = graph.getEdges(node.getId());
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

    public static int triplleCheck(Graph graph){
        return Math.min(upperBound(graph,1),Math.min(upperBound(graph,2),upperBound(graph,3)));
    }

    //--- LOWER BOUND --

    /**
     * Calls and returns {@link ChromaticNumber#bronKerbosch(Graph, List, List, List)}.
     */
    private static int lowerBound(Graph graph) {
        return bronKerbosch(graph, new ArrayList<>(), new ArrayList<>(graph.getNodes().values()), new ArrayList<>());
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
    private static int bronKerbosch(Graph graph, List<Node> _R, List<Node> _P, List<Node> _X) {
        int max = Integer.MIN_VALUE;
        if(_P.isEmpty() && _X.isEmpty()) {
            max = Math.max(max, _R.size());
        }

        Iterator<Node> nodeIterator = _P.iterator();
        while (nodeIterator.hasNext()) {

            //---
            Node node = nodeIterator.next();
            List<Node> neighbours = graph.getEdges(node.getId()).stream().map(Node.Edge::getTo).collect(Collectors.toList());

            //---
            List<Node> dR = new ArrayList<>(_R);
            dR.add(node);

            List<Node> dP = _P.stream().filter(neighbours::contains).collect(Collectors.toList());
            List<Node> dX = _X.stream().filter(neighbours::contains).collect(Collectors.toList());

            max = Math.max(bronKerbosch(graph, dR, dP, dX), max);

            //---
            nodeIterator.remove();
            _X.add(node);
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
            System.out.print(""); //for some reason this code does not work without this. is there maybe some sort of byte-code optimisation going on removing this type of loop
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
