import org.jblas.FloatMatrix;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GephiConverter {

    public static void main(String[] args) throws IOException {

        args = new String[] {"src/main/java/data/block3_2018_graph16.txt"};
        String fileName = args[0];

        System.out.println("########### READ FROM FILE ###########");
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            Set<Integer> nodes = new HashSet<>();
            Set<int[]> edges = new HashSet<>();
            
            FloatMatrix m = null;

            int lineNumber = 1;
            for (final String line : lines) {
                if(line.startsWith("VERTICES")) {
                    final int v = Integer.valueOf(line.split(" = ")[1]);
                    System.out.println(v);
                    m = new FloatMatrix(v, v);
                } else if (!line.startsWith("EDGES") && !line.startsWith("//")) {
                    String[] split = line.split(" ");

                    //--- Error
                    if (split.length != 2) {
                        System.out.println(String.format("Debug %s:%d >> %s", fileName, lineNumber, String.format("Malformed edge line: %s", line)));
                    }

                    int from = Integer.parseInt(split[0]) - 1;
                    int to = Integer.parseInt(split[1]) - 1;

                    m.put(from, to, 1F);
                }

                lineNumber++;
            }

            generateGephiFile(m);

        } catch (IOException e) {
            System.out.println(String.format("Debug %s:-1 >> %s", fileName, String.format("The file could not (!) be read. (%s)", e.getMessage())));
            e.printStackTrace();
            System.exit(0);
        }


    }

    private static void generateGephiFile(FloatMatrix graph) {
        //--- Gephi
        try {


            StringBuilder builder = new StringBuilder();
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gexf xmlns=\"http://www.gexf.net/1.2draft\" " +
                    "xmlns:viz=\"http://www.gexf.net/1.1draft/viz\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd\" version=\"1.2\">" +
                    "<graph><nodes>");

            for(int r = 0; r < graph.getRows(); r++) {
                builder.append(String.format("<node id=\"%d\" label=\"glossy\"></node>", r));
            }

            builder.append("</nodes><edges>");

            int edgeId = 0;
            for(int r = 0; r < graph.getRows(); r++) {
                for(int c = 0; c < graph.getColumns(); c++) {
                    if(graph.get(r, c) > 0) {
                        builder.append(String.format("<edge id=\"%d\" source=\"%d\" target=\"%d\" />", edgeId, r, c));
                        edgeId++;
                    }
                }
            }

            builder.append("</edges></graph></gexf>");
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
    }

}
