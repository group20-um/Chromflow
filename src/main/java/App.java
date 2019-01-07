import org.jblas.ComplexFloat;
import org.jblas.ComplexFloatMatrix;
import org.jblas.Eigen;
import org.jblas.FloatMatrix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class App {
    public static void main(String[] args) {

        //---
        String fileName = args[0];

        System.out.println("########### READ FROM FILE ###########");
        FloatMatrix m = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));

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

        } catch (IOException e) {
            System.out.println(String.format("Debug %s:-1 >> %s", fileName, String.format("The file could not (!) be read. (%s)", e.getMessage())));
            e.printStackTrace();
            System.exit(0);
        }
        //--

        long time = System.currentTimeMillis();
        ComplexFloatMatrix result = Eigen.eigenvalues(m);
        float max = result.get(0).abs();
        for(int i = 1; i < result.length; i++) {
            max = Math.max(max, result.get(i).real());
        }
        System.out.println(max);
        System.out.println(System.currentTimeMillis() - time);

    }

}