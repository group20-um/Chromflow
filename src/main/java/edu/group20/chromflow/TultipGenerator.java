package edu.group20.chromflow;

import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class TultipGenerator {


    public static void generareTultipFiles(Graph graph, String name) {
        //--- Gephi
        try {
            {
                StringBuilder build_nodes = new StringBuilder();
                build_nodes.append("node_id\n");
                for (Node n : graph.getNodes().values()) {
                    build_nodes.append(String.format("%d%n", n.getId()));
                }

                File file = new File(String.format("src/main/java/data/tultip/%s_nodes.csv", name));
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(build_nodes.toString().getBytes(Charset.forName("UTF-8")));
                fileOutputStream.flush();
            }
            {
                StringBuilder build_edges = new StringBuilder();
                build_edges.append("source,target\n");
                for(Map<Integer, Node.Edge> n : graph.getEdges().values()) {
                    for(Node.Edge e : n.values()) {
                        build_edges.append(String.format("%d,%d%n", e.getFrom().getId(), e.getTo().getId()));
                    }
                }
                File file = new File(String.format("src/main/java/data/tultip/%s_edges.csv", name));
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(build_edges.toString().getBytes(Charset.forName("UTF-8")));
                fileOutputStream.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
