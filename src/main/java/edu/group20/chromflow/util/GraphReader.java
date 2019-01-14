package edu.group20.chromflow.util;

import edu.group20.chromflow.TestApp;
import edu.group20.chromflow.graph.Graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class GraphReader {

    public static Graph parseGraph(String path) {
        Graph graph = new Graph();
        try {
            long time = System.currentTimeMillis();
            List<String> lines = Files.readAllLines(Paths.get(path));

            TestApp.debug("ReadAllLines (%dms) >> %d line(s)%n", (System.currentTimeMillis() - time), lines.size());

            time = System.currentTimeMillis();

            lines.stream().filter(e -> !(e.startsWith("VERTICES") || e.startsWith("EDGES") || e.startsWith("//")))
                    .forEach(line -> {
                        String[] split = line.split(" ");

                        //--- Error
                        if (split.length != 2) {
                            TestApp.debugln(String.format("Debug %s >> %s", path, String.format("Malformed edge line: %s", line)));
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



        } catch (IOException e) {
            TestApp.debug("Debug %s:-1 >> %s%n", path, String.format("The file could not (!) be read. (%s)", e.getMessage()));
            return null;
        }

        return graph;
    }

}
