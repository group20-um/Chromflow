import org.jblas.*;
import org.jscience.mathematics.vector.Float64Matrix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class App {
    public static void main(String[] args) {

        //---
        String fileName = args[0];

        System.out.println("########### READ FROM FILE ###########");
        double[][] m = null;
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));

            int lineNumber = 1;
            for (final String line : lines) {
                if(line.startsWith("VERTICES")) {
                    final int v = Integer.valueOf(line.split(" = ")[1]);
                    System.out.println(v);
                    m = new double[v][v];
                } else if (!line.startsWith("EDGES") && !line.startsWith("//")) {
                    String[] split = line.split(" ");

                    //--- Error
                    if (split.length != 2) {
                        System.out.println(String.format("Debug %s:%d >> %s", fileName, lineNumber, String.format("Malformed edge line: %s", line)));
                    }

                    int from = Integer.parseInt(split[0]) - 1;
                    int to = Integer.parseInt(split[1]) - 1;

                    m.
                    m.put(from, to, 1F)
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
        ComplexDoubleMatrix result = Eigen.eigenvalues(new DoubleMatrix(m));
        System.out.println(System.currentTimeMillis() - time);

    }

}