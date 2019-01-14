package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TestApp {

    public final static boolean KELK_MODE = false;
    public static boolean OUTPUT_ENABLED = true;

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
         if(!KELK_MODE && OUTPUT_ENABLED) System.out.printf(format, vars);
    }

    public static void debugln(String message) {
        if(!KELK_MODE && OUTPUT_ENABLED) System.out.printf("%s%n", message);
    }

    public static void kelkOutput(String format, Object... vars) {
        if(KELK_MODE && OUTPUT_ENABLED) System.out.printf(format, vars);
    }


}
