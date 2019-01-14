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

        args = new String[] {"src/main/java/data/block3_2018_graph20.txt"};
        Graph graph = new Graph();

        String fileName = args[0];

         try {
            long time = System.currentTimeMillis();
            List<String> lines = Files.readAllLines(Paths.get(fileName));

            debug("ReadAllLines (%dms) >> %d line(s)%n", (System.currentTimeMillis() - time), lines.size());

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

            debug("Build Graph (%dms) >> Graph (%s) parsed %d vertices, %d edges and a density of %.6f%%.%n",
                    (System.currentTimeMillis() - time), fileName, graph.getNodes().size(), graph.getEdgeCount(), graph.getDensity() * 100);

        } catch (IOException e) {
            debug("Debug %s:-1 >> %s%n", fileName, String.format("The file could not (!) be read. (%s)", e.getMessage()));
            System.exit(0);
            e.printStackTrace();
        }

        debug("Result>> %s%n", ChromaticNumber.compute(ChromaticNumber.Type.EXACT, graph, false, true));

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
