package edu.group20.chromflow.misc;

import edu.group20.chromflow.TestApp;
import edu.group20.chromflow.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Reads in the graph.
 */
public class GraphReader {

    public static Graph parseGraph(final String path) {
        Graph graph = new Graph();

        boolean isCol = path.endsWith(".col");
        long time = System.currentTimeMillis();
        try {
            List<String> lines = Files.readAllLines(Paths.get(path));
            TestApp.debug("ReadAllLines (%dms) >> %d line(s)%n", (System.currentTimeMillis() - time), lines.size());

            time = System.currentTimeMillis();
            lines.stream().filter(e -> !(e.startsWith("VERTICES") || e.startsWith("EDGES") || e.startsWith("//")))
                    .forEach(line -> {

                        int from = -1;
                        int to = -1;

                        if(isCol) {
                            if(line.startsWith("e")) {

                                String[] split = line.trim().replaceAll("[ ]{2,}", " ").split(" ");

                                //--- Error
                                if (split.length != 3) {
                                    TestApp.debugln(String.format("Debug %s >> %s", path, String.format("Malformed edge line: %s", line)));
                                }

                                from = Integer.parseInt(split[1]);
                                to = Integer.parseInt(split[2]);

                            }
                        } else {

                            String[] split = line.split(" ");

                            //--- Error
                            if (split.length != 2) {
                                TestApp.debugln(String.format("Debug %s >> %s", path, String.format("Malformed edge line: %s", line)));
                            }
                            from = Integer.parseInt(split[0]);
                            to = Integer.parseInt(split[1]);

                        }

                        if (!graph.hasNode(from)) {
                            graph.addNode(from, -1);
                        }

                        if (!graph.hasNode(to)) {
                            graph.addNode(to, -1);
                        }

                        graph.addEdge(from, to, true);

                    });



        } catch (IOException e) {
            TestApp.debug("Debug %s:-1 >> %s%n", path, String.format("The file could not (!) be read. (%s)", e.getMessage()));
            return null;
        }

        TestApp.debug("Build Graph (%dms) >> Graph (%s) parsed %d vertices, %d edges and a density of %.6f%%.%n",
                (System.currentTimeMillis() - time), path, graph.getNodes().size(), graph.getEdgeCount(), graph.getDensity() * 100);

        return graph;
    }

}
