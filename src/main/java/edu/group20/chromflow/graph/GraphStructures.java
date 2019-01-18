package edu.group20.chromflow.graph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import edu.group20.chromflow.TestApp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GraphStructures {

    public static class EVBAsed {

        private final static double EPSILON = 1E-14;

        public static boolean isPlanar(Graph graph) {

            if (isTooBig(graph)) return false;

            //http://www.cs.yale.edu/homes/spielman/561/lect20-15.pdf
            // http://cs-www.cs.yale.edu/homes/spielman/TALKS/blyth1.pdf -> page 61

            double[] eig = new Matrix(graph.toLaplacianMatrix()).eig().getRealEigenvalues();
            if (eig.length < 3) return false;
            Arrays.sort(eig);

            double secondSmallest = eig[1];
            double thirdSmallest = eig[2];

            if (secondSmallest <= 0) return false;

            final double maxDegree = graph.getEdges().values().stream().mapToInt(Map::size).max().getAsInt();
            return (secondSmallest <= (8 * maxDegree) / graph.getNodes().size() &&
                    thirdSmallest <= Math.ceil(maxDegree / graph.getNodes().size()));
        }

        public static int lowerBoundEigenValue(Graph graph) {

            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (int) Math.ceil(1 + (eig[eig.length - 1] / -eig[0]));
        }

        public static int upperBoundEigenValue(Graph graph) {
            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (int) Math.ceil(1 + eig[eig.length - 1]);
        }

        public static boolean isBipartiteEigenValue(Graph graph) {

            if (isTooBig(graph)) return false;

            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            final double EPSILON = 1E-14;
            return (Math.abs(eig[0] + eig[eig.length - 1]) <= EPSILON);
        }

        public static boolean isCompleteEigenValue(Graph graph) {

            if (isTooBig(graph)) return false;

            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (Math.abs(eig[eig.length - 2] + 1) <= EPSILON);
        }

        private static boolean isTooBig(Graph g) {
            if(g.getNodes().size() > 1000) {
                TestApp.debug("GraphStructure >> Rejected because the graph has more than 1000 nodes.%n");
                return true;
            }
            return false;
        }

    }

    public static class Cycle {

        public static boolean hasCycle(Graph graph){
            graph.isComplete();

            Boolean[] visited=new Boolean[graph.getNodes().size()];

            for(int i=0; i<visited.length; i++){
                visited[i]=false;
            }

            boolean cyclic=false;

            for( int i=1; i<= graph.getNodes().size(); i++){

                if(!visited[i-1]){

                    if(izCycle(graph, graph.getNode(i), visited, new Node(-1, -1))){

                        cyclic=true;

                    }

                }

            }

            return cyclic;
        }

        public static boolean izCycle(Graph graph, Node source, Boolean visited[], Node parent ){


            //if (source.getValue() != -1) return true;

            visited[source.getId()-1]=true;

            List<Node> neighbours=graph.getNeighbours(source);

            for(int i=0; i<neighbours.size(); i++ ){

                if (!visited[neighbours.get(i).getId()-1]){

                    return izCycle( graph, neighbours.get(i) , visited, source);

                }

                else if( neighbours.get(i)!= parent ){

                    return true;
                }

            }

            return false;

        }

        public static boolean hasOddCycle(Graph graph){
            List<Node> parent=new ArrayList<>();
            parent.add(graph.getNode(graph.getMinNodeId()));
            return hasCycle(graph) && !isBipartite(graph, parent);
        }

        public static boolean isBipartite( Graph graph, List<Node> parent){

            if(graph.getNodes().size()<2){ //probably check somewhere else
                System.out.println("Is NOT bipartite");
                return false;
            }

            if(graph.isColored()){
                System.out.println("Is bipartite");
                return true;
            }

            graph.colourNeighbours(parent);
            List<Node> newParents=new ArrayList<>();
            for(int i=0; i<parent.size();i++){
                if(graph.hasNeighbourSameColour( parent.get(i))){
                    return false;
                }
                newParents.addAll(graph.getNeighbours(parent.get(i)));
            }

            return isBipartite( graph, newParents);

        }

    }


}
