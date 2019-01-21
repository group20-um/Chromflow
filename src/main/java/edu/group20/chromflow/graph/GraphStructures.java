package edu.group20.chromflow.graph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import edu.group20.chromflow.TestApp;

import java.util.*;
import java.util.stream.Collectors;

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
                return false;
            }

            if(graph.isColored()){
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

    public static class Test{

        public static int isWheelCenter(Graph graph, Node node) {

            if(node.getValue() == 0) {
                return -1;
            } else if(node.getValue() == 1) {
                return graph.getEdges(node.getId()).size();
            }

            Map<Integer, Node.Edge> neighbours = graph.getEdges(node.getId());

            int i = 0;
            for(Node.Edge e : neighbours.values()) {
                Node n = e.getTo();
                if(n.getValue() != 1) {
                    for (int a : graph.getEdges(n.getId()).keySet()) {
                        if (n.getId() != a && neighbours.containsKey(a)) {
                            i++;
                        }
                    }
                }
            }

            if(i / 2 == neighbours.size()) {
                neighbours.forEach((id, e) -> e.getTo().setValue(0));
                node.setValue(1);
                return neighbours.size();
            }
            return -1;
        }

        public static boolean isConnected(Graph graph) {
            graph.reset();
            Stack<Node> nodes = new Stack<>();
            nodes.push(graph.getNode(graph.getMinNodeId()));

            while (!(nodes.isEmpty())) {
                Node n = nodes.pop();
                n.setValue(0);
                nodes.addAll(graph.getNeighbours(n).stream().filter(e -> e.getValue() == -1).collect(Collectors.toList()));
            }

            return graph.getNodes().values().stream().noneMatch(e -> e.getValue() == -1);
        }

    }


    public static class Connectivity {
        // Source: https://algs4.cs.princeton.edu/41graph/Biconnected.java.html
        public static class TwoConnectivity {

            // https://www.sanfoundry.com/java-program-check-whether-graph-biconnected/
            // This is based on checking whether or not the graph has articulation vertices,
            // if the graph has no articulation vertices, it is biconnected:
            // "Therefore a biconnected graph has no articulation vertices."
            public static boolean check(Graph graph) {
                if (!GraphStructures.Test.isConnected(graph)) {
                    return false;
                }

                graph.reset();
                Map<Integer, Integer> low = new HashMap<>();

                for (Node n : graph.getNodes().values()) {
                    low.put(n.getId(), -1);
                }

                for (Node n : graph.getNodes().values()) {
                    if (n.getValue() == -1) {
                        if(dfs(graph, n, n, low)) {
                            return true;
                        }
                    }
                }

                return false;
            }

            private static boolean dfs(Graph graph, Node u, Node v, Map<Integer, Integer> low) {
                final Node pre = graph.getNextAvailableNode(u);
                if(pre == null) return false;

                int children = 0;

                v.setValue(pre.getId());
                low.put(v.getId(), pre.getId());

                for (Node w : graph.getNeighbours(v)) {
                    if (w.getValue() == -1) {
                        children++;
                        if(dfs(graph, v, w, low)) {
                            return true;
                        }


                        // update low number
                        low.put(v.getId(), Math.min(low.get(v.getId()), low.get(w.getId())));

                        // non-root of DFS is an articulation point if low[w] >= pre[v]
                        if (low.get(w.getId()) >= v.getValue() && u != v) {
                            return true;
                        }
                    }

                    // update low number - ignore reverse of edge leading to v
                    else if (w != u) {
                        low.put(v.getId(), Math.min(low.get(v.getId()), w.getValue()));
                    }
                }

                // root of DFS is an articulation point if it has more than 1 child
                return u == v && children > 1;

            }
        }


        public static class OneConnectivity {

            public static boolean check(Graph graph) {
                List<Node> nodes = new LinkedList<>(graph.getNodes().values());
                for(int i = 0; i < nodes.size(); i++) {
                    graph.reset();

                    Node n = nodes.get(i);
                    n.setValue(0);

                    if(i == nodes.size() - 1) {
                        if(!isConnected(graph, nodes.get(i - 1), n)) {
                            return true;
                        }
                    } else if(!isConnected(graph, nodes.get(i + 1), n)) {
                        return true;
                    }
                }
                return false;
            }

            private static boolean isConnected(Graph graph, Node start, Node b) {
                Stack<Node> nodes = new Stack<>();
                nodes.push(start);

                while (!(nodes.isEmpty())) {
                    Node n = nodes.pop();
                    n.setValue(0);
                    nodes.addAll(graph.getNeighbours(n).stream().filter(e -> e != start && e.getValue() == -1).collect(Collectors.toList()));
                }

                return graph.getNodes().values().stream().noneMatch(e -> e != start && e.getValue() == -1);
            }

        }

        public static class Points {
            public static Set<Integer> check(Graph graph) {
                if (!GraphStructures.Test.isConnected(graph)) {
                    return new HashSet<>();
                }

                graph.reset();
                Map<Integer, Integer> low = new HashMap<>();
                Set<Integer> points = new HashSet<>();

                for (Node n : graph.getNodes().values()) {
                    low.put(n.getId(), -1);
                }

                for (Node n : graph.getNodes().values()) {
                    if (n.getValue() == -1) {
                        dfs(graph, n, n, low, points);
                    }
                }

                return points;
            }

            private static void dfs(Graph graph, Node u, Node v, Map<Integer, Integer> low, Set<Integer> points) {
                final Node pre = graph.getNextAvailableNode(u);
                if(pre == null) {
                    return;
                }

                int children = 0;

                v.setValue(pre.getId());
                low.put(v.getId(), pre.getId());

                for (Node w : graph.getNeighbours(v)) {
                    if (w.getValue() == -1) {
                        children++;
                        dfs(graph, v, w, low, points);

                        // update low number
                        low.put(v.getId(), Math.min(low.get(v.getId()), low.get(w.getId())));

                        // non-root of DFS is an articulation point if low[w] >= pre[v]
                        if (low.get(w.getId()) >= v.getValue() && u != v) {
                            points.add(v.getId());
                        }
                    }

                    // update low number - ignore reverse of edge leading to v
                    else if (w != u) {
                        low.put(v.getId(), Math.min(low.get(v.getId()), w.getValue()));
                    }
                }

                // root of DFS is an articulation point if it has more than 1 child
                if(u == v && children > 1) {
                    points.add(v.getId());
                }

            }
        }


    }

}
