import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.math3.linear.*;
import org.ejml.simple.SimpleEVD;
import org.jblas.*;
import org.jblas.util.Logger;
import org.jscience.JScience;
import org.jscience.mathematics.vector.Float64Matrix;
import org.jscience.mathematics.vector.Matrix;
import org.ojalgo.array.Array1D;
import org.ojalgo.matrix.PrimitiveMatrix;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.scalar.ComplexNumber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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

                    m[from][to] = 1F;
                    m[to][from] = 1F;
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
        //jblas
        ComplexDoubleMatrix result = Eigen.eigenvalues(new DoubleMatrix(m));
        for(ComplexDouble f : result.toArray()) {
            System.out.println(f.abs());
        }


        /* apache - way too slow
        RealMatrix t = MatrixUtils.createRealMatrix(m);
        EigenDecomposition e = new EigenDecomposition(t);
        System.out.println(Arrays.toString(e.getRealEigenvalues()));
        */

        //Float64Matrix t = Float64Matrix.valueOf(m);

        //ojalgo
        /*PrimitiveMatrix primitiveMatrix = PrimitiveMatrix.FACTORY.columns(m);
        List<Eigenvalue.Eigenpair> result = primitiveMatrix.getEigenpairs();
        result.forEach(e -> {
            System.out.printf("%.2f\n", e.value.getReal());
        });*/

        //colt
        /*DoubleMatrix2D matrix2D = new DenseDoubleMatrix2D(m);
        cern.colt.matrix.linalg.EigenvalueDecomposition eigenDecomposition = new cern.colt.matrix.linalg.EigenvalueDecomposition(matrix2D);
        System.out.println(Arrays.toString(eigenDecomposition.getRealEigenvalues().toArray()));*/

        //ejml
        /*org.ejml.data.DenseMatrix64F matrix = new org.ejml.data.DenseMatrix64F(m);
        SimpleEVD eigen = new SimpleEVD(matrix);
        for(int i = eigen.getIndexMin(); i <= eigen.getIndexMax(); i++) {
            if(eigen.getEigenvalue(i).isReal())
                System.out.println(eigen.getEigenvalue(i).getReal());
        }*/

        //


        System.out.println(System.currentTimeMillis() - time);

    }

}