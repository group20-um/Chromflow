import graph.ChromaticNumber;
import graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        args[0] = "src/main/java/data/block3_2018_graph07.txt";
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

        //ChromaticNumber.lawler(graph);
        ChromaticNumber.dijkstra(graph, graph.getMinNodeId(), graph.getMaxNodeId());
        System.out.println(ChromaticNumber.compute(ChromaticNumber.Type.EXACT_EXPERIMENTAL, graph, false));
    }

}
