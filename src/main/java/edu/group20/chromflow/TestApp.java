package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.util.GraphReader;

public class TestApp {

    public final static boolean KELK_MODE = false;
    public static boolean OUTPUT_ENABLED = true;

    public static void main(String[] args) {

        // www.cs.uu.nl/education/scripties/pdf.php?SID=INF/SCR-2009-095

<<<<<<< HEAD
        args = new String[] {"src/main/java/data/graph01.txt"};
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
=======
        args = new String[] {"src/main/java/data/graph02.txt"};
        long time = System.currentTimeMillis();
        Graph graph = GraphReader.parseGraph(args[0]);
        debug("Build Graph (%dms) >> Graph (%s) parsed %d vertices, %d edges and a density of %.6f%%.%n",
                (System.currentTimeMillis() - time), args[0], graph.getNodes().size(), graph.getEdgeCount(), graph.getDensity() * 100);
>>>>>>> 34ce9832cdc0091c9ba2e9dda999b6ba5b02d297

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
