package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestApp {

    public final static boolean KELK_MODE = false;


    public static void main(String[] args) {

        // www.cs.uu.nl/education/scripties/pdf.php?SID=INF/SCR-2009-095


        //3-algo: 5
        //4-algo: 6

        // Solve: 9

        // 01 -> [3..4] -> 3
        // 02 -> [3..6] (3&4-algo)
        // 03 -> [6..8]
        // 04 -> [4..8] (4-algo)
        // 05 -> [0..3] -> 2 // methods violates
        // 06 -> [3..3] -> 3
        // 07 -> [8..12]
        // 08 -> [98..98] -> 98
        // 09 -> [3..6]
        // 10 -> [2..6] -> 3
        // 11 -> [15..15] -> 15
        // 12 -> [2..3] -> 3
        // 13 -> [9..12]
        // 14 -> [4...5]
        // 15 -> [5..10] // method violates
        // 16 -> [2..4] // method violates
        // 17 -> [8..8] -> 8
        // 18 -> [10..13] -> 10
        // 19 -> [11..13] -> 11
        // 20 -> [8..9]
        //args = new String[] {"src/main/java/data/block3_2018_graph05.txt"};
        Graph graph = new Graph();

        String fileName = args[0];

         try {
            long time = System.currentTimeMillis();
            List<String> lines = Files.readAllLines(Paths.get(fileName));

            debugln("ReadAllLines >> " + (System.currentTimeMillis() - time));

            time = System.currentTimeMillis();

            lines.stream().filter(e -> !(e.startsWith("VERTICES") || e.startsWith("EDGES") || e.startsWith("//")))
                    .forEach(line -> {
                    String[] split = line.split(" ");

                    //--- Error
                    if (split.length != 2) {
                        debugln(String.format("Debug %s >> %s", fileName, String.format("Malformed edge line: %s", line)));
                    }

                    int from = Integer.parseInt(split[0]);
                    int to = Integer.parseInt(split[1]);

                    if (!graph.hasNode(from)) {
                        graph.addNode(from, -1);
                    }

                    if (!graph.hasNode(to)) {
                        graph.addNode(to, -1);
                    }

                    graph.addEdge(from, to, true);

            });

            debugln("Build Graph [1] >> " + (System.currentTimeMillis() - time));
            debug("Debug: Graph (%s) parsed with %d vertices and %d edges.%n", fileName, graph.getNodes().size(), graph.getEdges().size() / 2);

        } catch (IOException e) {
            debug("Debug %s:-1 >> %s%n", fileName, String.format("The file could not (!) be read. (%s)", e.getMessage()));
            System.exit(0);
            e.printStackTrace();
        }

        debug("Graph Density: %.6f%%%n", graph.getDensity() * 100);

        long time = System.currentTimeMillis();
        debugln("Result>> " + ChromaticNumber.compute(ChromaticNumber.Type.EXACT, graph, false));
        debug("Result>> %dms", System.currentTimeMillis() - time);

    }

    public static void debug(String format, Object... vars) {
         if(!KELK_MODE) System.out.printf(format, vars);
    }

    public static void debugln(String message) {
        if(!KELK_MODE) System.out.printf("%s%n", message);
    }



    public static void kelkOutput(String format, Object... vars) {
        if(KELK_MODE) System.out.printf(format, vars);
    }

    /*


        // test
        boolean communities = false;

        if(communities) {
            SingleGraph g = new SingleGraph("Test", false, true);
            edu.group20.chromflow.graph.getNodes().forEach((id, n) -> g.addNode(String.valueOf(id)));
            final int[] edgeId = {0};
            edu.group20.chromflow.graph.getEdges().forEach((id, edges) -> {
                edges.forEach((to, e) -> {
                    g.addEdge(String.valueOf(edgeId[0]), String.valueOf(e.getFrom().getId()), String.valueOf(e.getTo().getId()));
                    edgeId[0]++;
                });
            });
            BetweennessCentrality betweennessCentrality = new BetweennessCentrality();
            betweennessCentrality.init(g);
            betweennessCentrality.compute();

            List<Node> n = g.getNodeSet().stream().sorted(new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return -Double.compare(o1.getAttribute("Cb", Double.class), o2.getAttribute("Cb", Double.class));
                }
            }).limit(4).collect(Collectors.toList());
            for (Node a : n) {
                for (Node b : n) {
                    if (a != b) {
                        edu.group20.chromflow.graph.getEdges(Integer.valueOf(a.getId())).remove(Integer.valueOf(b.getId())); // == edu.group20.chromflow.graph.getEdges(Integer.valueOf(a.getId())).removeIf(edge -> edge.getTo().getId() == Integer.valueOf(b.getId()));
                    }
                }
            }
            // find subgraphs
            List<Graph> subgraphs = new LinkedList<>();
            edu.group20.chromflow.graph.reset();
            for (Node a : n) {

                final int node_id = Integer.parseInt(a.getId());

                if (edu.group20.chromflow.graph.getNode(node_id).getValue() == -1) {
                    Graph sg = new Graph();
                    subgraphs.add(sg);

                    Stack<edu.group20.chromflow.Node0.chromflow.graph.Node> visit = new Stack<>();
                    visit.add(edu.group20.chromflow.graph.getNode(node_id));

                    while (!visit.isEmpty()) {
                        edu.group20.chromflow.graphroup20.chromflow.Node pop = visit.pop();
                        pop.setValue(1);
                        sg.addNode(pop.getId(), -1);
                        visit.addAll(edu.group20.chromflow.graph.getEdges(pop.getId()).values().stream().filter(e -> e.getTo().getValue() == -1).map(edge -> edge.getTo()).collect(Collectors.toList()));
                    }

                    sg.getNodes().forEach((from_id, node_) -> {
                        edu.group20.chromflow.graph.getEdges(from_id).forEach((to_id, edge) -> {
                            if (edu.group20.chromflow.graph.hasEdge(from_id, to_id)) {
                                sg.addEdge(from_id, to_id, true);
                            }
                        });
                    });

                }


            }

            ChromaticNumber.Result lR = ChromaticNumber.compute(ChromaticNumber.Type.LOWER, edu.group20.chromflow.graph, false);
            ChromaticNumber.Result uR =  ChromaticNumber.compute(ChromaticNumber.Type.UPPER, edu.group20.chromflow.graph, false);
            System.out.printf("Original bounds: [%d..%d] Density: %.4f%n", lR.getLower(), uR.getUpper(), edu.group20.chromflow.graph.getDensity());
            int lower = Integer.MIN_VALUE;
            int upper = Integer.MAX_VALUE;
            for(Graph sg : subgraphs.stream().sorted(Comparator.comparingInt(o -> o.getNodes().size())).collect(Collectors.toList())) {
                System.out.println("Result>> " + ChromaticNumber.compute(ChromaticNumber.Type.EXACT, sg, false));
                System.out.println("Subgraph Density >> " + sg.getDensity());
            }
            System.out.printf("New bounds: [%d..%d]%n", lower, upper);

        }


     */

}
