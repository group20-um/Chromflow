package edu.group20.chromflow.graph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.util.Arrays;

public class GraphStructures {

    private final static double EPSILON = 1E-14;

    public static int lowerBoundEigenValue(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (int) (1+ (eig[eig.length-1]/-eig[0]));
    }

    public static int upperBoundEigenValue(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (int) (1+ eig[eig.length-1]);
    }

    public static boolean isBipartiteEigenValue(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        final double EPSILON = 1E-14;
        return (Math.abs(eig[0]+eig[eig.length-1]) <= EPSILON);
    }

    public static boolean isCompleteEigenValue(Graph graph) {
        Matrix matrix = new Matrix(graph.toAdjacentMatrix());
        EigenvalueDecomposition eigenvalues = matrix.eig();
        double[] eig = eigenvalues.getRealEigenvalues();
        Arrays.sort(eig);
        return (Math.abs(eig[eig.length-2]+1) <= EPSILON);
    }

}
