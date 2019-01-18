package edu.group20.chromflow;

import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class GephiConverter {


    public static void generateGephiFile(Graph graph, String name) {
        //--- Gephi
        try {

            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gexf xmlns=\"http://www.gexf.net/1.2draft\" " +
                    "xmlns:viz=\"http://www.gexf.net/1.1draft/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd\" version=\"1.2\">" +
                    "<graph><nodes>");
            graph.getNodes().forEach((k, v) -> {
                builder.append(String.format("<node id=\"%d\" label=\"glossy\"></node>",
                        v.getId()
                ));
            });
            builder.append("</nodes><edges>");

            int edgeId = 0;
            for (Map.Entry<Integer, Map<Integer, Node.Edge>> entry : graph.getEdges().entrySet()) {
                for (Node.Edge edge : entry.getValue().values()) {
                    builder.append(String.format("<edge id=\"%d\" source=\"%d\" target=\"%d\" />", edgeId, edge.getFrom().getId(), edge.getTo().getId()));
                    edgeId++;
                }
            }

            builder.append("</edges></graph></gexf>");
            File file = new File(String.format("src/main/java/data/gephi/%s.gexf", name));
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(builder.toString().getBytes(Charset.forName("UTF-8")));
            fileOutputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /*
    public static void generateGephiFile(FloatMatrix edu.group20.chromflow.graph) {
        //--- Gephi
        try {


            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gexf xmlns=\"http://www.gexf.net/1.2draft\" " +
                    "xmlns:viz=\"http://www.gexf.net/1.1draft/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd\" version=\"1.2\">" +
                    "<edu.group20.chromflow.graph><nodes>");

            for(int r = 0; r < edu.group20.chromflow.graph.getRows(); r++) {
                builder.append(String.format("<node id=\"%d\" label=\"glossy\"></node>", r));
            }

            builder.append("</nodes><edges>");

            int edgeId = 0;
            for(int r = 0; r < edu.group20.chromflow.graph.getRows(); r++) {
                for(int c = 0; c < edu.group20.chromflow.graph.getColumns(); c++) {
                    if(edu.group20.chromflow.graph.get(r, c) > 0) {
                        builder.append(String.format("<edge id=\"%d\" source=\"%d\" target=\"%d\" />", edgeId, r, c));
                        edgeId++;
                    }
                }
            }

            builder.append("</edges></edu.group20.chromflow.graph></gexf>");
            File file = new File("src/main/java/data/gephi.gexf");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(builder.toString().getBytes(Charset.forName("UTF-8")));
            fileOutputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }*/

}
