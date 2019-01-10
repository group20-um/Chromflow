import graph.ChromaticNumber;
import graph.Graph;
import org.graphstream.algorithm.BetweennessCentrality;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestApp {


    public static void main(String[] args) {

        // www.cs.uu.nl/education/scripties/pdf.php?SID=INF/SCR-2009-095

        // We can solve: 1,6,8,10,11,17,19
        // 02 -> 3 & 4 coloring algo needed
        // 03 -> <special structure>
        // 04 -> 4 coloring algo needed
        // 05 -> some combination of everything... probably
        // 07 -> no idea...
        // 09 ->
        // 12 -> Seems to have the same "hub"-like structure as '10'
        // 13 ->
        // 14 -> looks interesting in Gephi...
        // 15 -> Clique removal should do wonders here, 3 cliques (clusters) and only a single point of connection <3
        // 16 -> Seems to have many "hub"-like structures
        // 18 -> Similar structure to 15
        // 20 -> Interesting symmetrical structure
        args[0] = "src/main/java/data/block3_2018_graph15.txt";
        Graph graph = new Graph();
        if(args.length == 0) {
            System.out.println("Debug: No file path provided!");
            return;
        }

        String fileName = args[0];
        System.out.println("########### READ FROM FILE ###########");
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            Set<Integer> nodes = new HashSet<>();
            Set<int[]> edges = new HashSet<>();

            int lineNumber = 1;
            for (final String line : lines) {
                if (!line.startsWith("VERTICES") && !line.startsWith("EDGES") && !line.startsWith("//")) {
                    String[] split = line.split(" ");

                    //--- Error
                    if (split.length != 2) {
                        System.out.println(String.format("Debug %s:%d >> %s", fileName, lineNumber, String.format("Malformed edge line: %s", line)));
                    }

                    int from = Integer.parseInt(split[0]);
                    int to = Integer.parseInt(split[1]);
                    nodes.add(from);
                    nodes.add(to);
                    edges.add(new int[]{from, to});
                }

                lineNumber++;
            }

            nodes.forEach(id -> graph.addNode(id, -1));
            edges.forEach(edge -> graph.addEdge(edge[0], edge[1], true));

            System.out.printf("Debug: Graph (%s) parsed with %d vertices and %d edges.%n", fileName, nodes.size(), edges.size());

        } catch (IOException e) {
            System.out.println(String.format("Debug %s:-1 >> %s", fileName, String.format("The file could not (!) be read. (%s)", e.getMessage())));
            System.exit(0);
            e.printStackTrace();
        }

        // test
        SingleGraph g = new SingleGraph("Test", false, true);
        graph.getNodes().forEach((id, n) -> g.addNode(String.valueOf(id)));
        final int[] edgeId = {0};
        graph.getEdges().forEach((id, edges) -> {
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
        for(Node a : n) {
            for(Node b : n) {
                if(a != b) {
                    graph.getEdges(Integer.valueOf(a.getId())).remove(Integer.valueOf(b.getId())); // == graph.getEdges(Integer.valueOf(a.getId())).removeIf(edge -> edge.getTo().getId() == Integer.valueOf(b.getId()));
                }
            }
        }
        // find subgraphs
        List<Graph> subgraphs = new LinkedList<>();
        graph.reset();
        for(Node a : n) {

            final int node_id = Integer.parseInt(a.getId());

            if(graph.getNode(node_id).getValue() == -1) {
                Graph sg = new Graph();
                subgraphs.add(sg);

                Stack<graph.Node> visit = new Stack<>();
                visit.add(graph.getNode(node_id));

                while (!visit.isEmpty()) {
                    graph.Node pop = visit.pop();
                    pop.setValue(1);
                    sg.addNode(pop.getId(), -1);
                    visit.addAll(graph.getEdges(pop.getId()).values().stream().filter(e -> e.getTo().getValue() == -1).map(edge -> edge.getTo()).collect(Collectors.toList()));
                }

                sg.getNodes().forEach((from_id, node_) -> {
                    graph.getEdges(from_id).forEach((to_id, edge) -> {
                        if(graph.hasEdge(from_id, to_id)) {
                            sg.addEdge(from_id, to_id, true);
                        }
                    });
                });

            }


        }

        //---
        // GephiConverter.generateGephiFile(graph);

        ChromaticNumber.Result lR = ChromaticNumber.compute(ChromaticNumber.Type.LOWER, graph, false);
        ChromaticNumber.Result uR =  ChromaticNumber.compute(ChromaticNumber.Type.UPPER, graph, false);
        System.out.printf("Original bounds: [%d..%d] Density: %.4f%n", lR.getLower(), uR.getUpper(), graph.getDensity());
        int lower = Integer.MIN_VALUE;
        int upper = Integer.MAX_VALUE;
        for(Graph sg : subgraphs.stream().sorted(Comparator.comparingInt(o -> o.getNodes().size())).collect(Collectors.toList())) {
            ChromaticNumber.Result lR_ = ChromaticNumber.compute(ChromaticNumber.Type.LOWER, sg, false);
            ChromaticNumber.Result uR_ =  ChromaticNumber.compute(ChromaticNumber.Type.UPPER, sg, false);
            lower = Math.max(lower, lR_.getLower());
            upper = Math.min(upper, uR_.getUpper());
            System.out.println("Subgraph Density >> " + sg.getDensity());
        }
        System.out.printf("New bounds: [%d..%d]%n", lower, upper);

        //ChromaticNumber.lawler(graph);
        //System.out.println(ChromaticNumber.compute(ChromaticNumber.Type.EXACT_EXPERIMENTAL, graph, false));
        //ChromaticNumber.dijkstra(graph, graph.getMinNodeId(), graph.getMaxNodeId());
        //System.out.println(ChromaticNumber.compute(ChromaticNumber.Type.EXACT_EXPERIMENTAL, graph, false));
    }

}
