package edu.group20.chromflow.graph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import edu.group20.chromflow.TestApp;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public class GraphStructures {

    private final static double EPSILON = 1E-14;

    public static boolean isPlanar(Graph graph) {

        if(isTooBig(graph)) return false;

        //http://www.cs.yale.edu/homes/spielman/561/lect20-15.pdf
        // http://cs-www.cs.yale.edu/homes/spielman/TALKS/blyth1.pdf -> page 61

        double[] eig =  new Matrix(graph.toLaplacianMatrix()).eig().getRealEigenvalues();
        Arrays.sort(eig);

        double secondSmallest = eig[1];
        double thirdSmallest = eig[2];

        final double maxDegree = graph.getEdges().values().stream().mapToInt(Map::size).max().getAsInt();
        return (secondSmallest <= (8 *  maxDegree) / graph.getNodes().size() && thirdSmallest <= Math.ceil(maxDegree / graph.getNodes().size()));
    }

    public static int lowerBoundEigenValue(Graph graph) {

        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (int) Math.ceil(1 + (eig[eig.length-1]/-eig[0]));
    }

    public static int upperBoundEigenValue(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (int) Math.ceil(1+ eig[eig.length-1]);
    }

    public static boolean isBipartiteEigenValue(Graph graph) {

        if(isTooBig(graph)) return false;

        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        final double EPSILON = 1E-14;
        return (Math.abs(eig[0]+eig[eig.length-1]) <= EPSILON);
    }

    public static boolean isCompleteEigenValue(Graph graph) {

        if(isTooBig(graph)) return false;

        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (Math.abs(eig[eig.length-2]+1) <= EPSILON);
    }

    private static boolean isTooBig(Graph g) {
        if(g.getNodes().size() > 1000) {
            TestApp.debug("GraphStructure >> Rejected because the graph has more than 1000 nodes.%n");
            return true;
        }
        return false;
    }

}
