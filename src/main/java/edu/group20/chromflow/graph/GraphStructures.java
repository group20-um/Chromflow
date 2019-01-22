package edu.group20.chromflow.graph;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import java.util.*;
import java.util.stream.Collectors;

public class GraphStructures {

    /**
     * All algorithms based on eigenvalues.
     */
    public static class EVBAsed {

        private final static double EPSILON = 1E-14;

        /**
         * Checks if the graph is likely planar buy comparing the second smallest and third smallest eigenvalues of
         * the Laplacian matrix with known values.
         * @param graph
         * @return
         */
        public static boolean isGraphLikelyPlanar(Graph graph) {
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

        /**
         * Calculates a lower bound base on the eigenvalues of the adjacency matrix.
         * @param graph
         * @return
         */
        public static int lowerBoundEigenValue(Graph graph) {

            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (int) Math.ceil(1 + (eig[eig.length - 1] / -eig[0]));
        }

        /**
         * Calculates an upper bound based on the eigenvalues of the adjacency matrix.
         * @param graph
         * @return
         */
        public static int upperBoundEigenValue(Graph graph) {
            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (int) Math.ceil(1 + eig[eig.length - 1]);
        }

        /**
         * Checks if a graph is planar based on the eigenvalues of the adjacency matrix.
         * @param graph
         * @return
         */
        public static boolean isBipartiteEigenValue(Graph graph) {
            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            final double EPSILON = 1E-14;
            return (Math.abs(eig[0] + eig[eig.length - 1]) <= EPSILON);
        }

        /**
         * Checks if the graph is planar based on its eigenvalues of the adjacency matrix.
         * @param graph
         * @return
         */
        public static boolean isCompleteEigenValue(Graph graph) {

            Matrix matrix = new Matrix(graph.toAdjacentMatrix());
            EigenvalueDecomposition eigenvalues = matrix.eig();
            double[] eig = eigenvalues.getRealEigenvalues();
            Arrays.sort(eig);
            return (Math.abs(eig[eig.length - 2] + 1) <= EPSILON);
        }
    }

    public static class Cycle {

        /**
         * Checks if the graph has a cycle.
         * @param graph
         * @return
         */
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

        /**
         * Checks if there is a cycle outgoing from the source node.
         * @param graph
         * @param source
         * @param visited
         * @param parent
         * @return
         */
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

        /**
         * Checks if the graph has an odd cycle.
         * @param graph
         * @return
         */
        public static boolean hasOddCycle(Graph graph){
            List<Node> parent=new ArrayList<>();
            parent.add(graph.getNode(graph.getMinNodeId()));
            return hasCycle(graph) && !isBipartite(graph, parent);
        }

        /**
         * Checks if the graph is bipartite.
         * @param graph
         * @param parent
         * @return
         */
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

        /**
         * Checks if the graph is connected.
         * @param graph
         * @return
         */
        public static boolean isConnected(Graph graph) {
            return isConnected(graph, graph.getNode(graph.getMinNodeId()));
        }

        /**
         * Checks if the graph is connected starting from a certain node.
         * @param graph
         * @param start The node to start at.
         * @return
         */
        public static boolean isConnected(Graph graph, Node start) {
            Stack<Node> nodes = new Stack<>();
            nodes.push(start);

            while (!(nodes.isEmpty())) {
                Node n = nodes.pop();
                n.setValue(0);
                nodes.addAll(graph.getNeighbours(n).stream().filter(e -> e.getValue() == -1).collect(Collectors.toList()));
            }

            return graph.getNodes().values().stream().noneMatch(e -> e.getValue() == -1);
        }

        /**
         * Checks if the node is the center of a wheel in a graph. This is a very hacky implementation and can break
         * under certain conditions. TODO Improve correctness.
         * @param graph
         * @param node
         * @return
         */
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


    }

    public static class Connectivity {
        // Source and full credit goes to: https://algs4.cs.princeton.edu/41graph/Biconnected.java.html

        public static class TwoConnectivity {

            // https://www.sanfoundry.com/java-program-check-whether-graph-biconnected/
            // This is based on checking whether or not the graph has articulation vertices,
            // if the graph has no articulation vertices, it is biconnected:
            // "Therefore a biconnected graph has no articulation vertices."

            /**
             * Checks if the graph is 2-connected by looking for artticulation vertices.
             * @param graph
             * @return True, if it is 2-connected, otherwise false.
             */
            public static boolean check(Graph graph) {
               return Points.check(graph).isEmpty();
            }

        }

        public static class OneConnectivity {

            /**
             * Checks if the graph is one-connected by 'removing' one node at a time and checking if the graph is
             * is still connected.
             * @param graph
             * @return True, if the graph is one-connected.
             */
            public static boolean check(Graph graph) {
                List<Node> nodes = new LinkedList<>(graph.getNodes().values());
                for(int i = 0; i < nodes.size(); i++) {
                    graph.reset();

                    Node n = nodes.get(i);
                    n.setValue(0);

                    if(i == nodes.size() - 1) {
                        if(!Test.isConnected(graph, nodes.get(i - 1))) {
                            return true;
                        }
                    } else if(!Test.isConnected(graph, nodes.get(i + 1))) {
                        return true;
                    }
                }
                return false;
            }

        }

        public static class Points {

            /**
             * Finds articulations points in a graph. This is  the same exact algorithm used as in {@link TwoConnectivity}
             * and full credit goes to the author of the code linked above from Princeton. - I have made only made
             * slight modifications to make it work with our graph structure.
             * @param graph
             * @return
             */
            public static Set<Node> check(Graph graph) {
                if (!GraphStructures.Test.isConnected(graph)) {
                    return new HashSet<>();
                }

                graph.reset();
                Map<Integer, Integer> low = new HashMap<>();
                Set<Node> points = new HashSet<>();

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

            private static void dfs(Graph graph, Node u, Node v, Map<Integer, Integer> low, Set<Node> points) {
                final Node pre = graph.getNextAvailableNode(u);
                if(pre == null) {
                    return;
                }

                int children = 0;

                boolean isArticulation = false;
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
                            points.add(v);
                        }
                    }

                    // update low number - ignore reverse of edge leading to v
                    else if (w != u) {
                        low.put(v.getId(), Math.min(low.get(v.getId()), w.getValue()));
                    }
                }

                // root of DFS is an articulation point if it has more than 1 child
                if(u == v && children > 1) {
                    points.add(v);
                }

            }

            //---
            public static Set<Node> getArticulationPoints(Graph g) {

                if(!GraphStructures.Test.isConnected(g)) {
                    return new HashSet<>();
                }

                g.reset();
                Map<Integer, Integer> low = new HashMap<>();
                Map<Integer, Integer> parent = new HashMap<>();
                Set<Node> articulationPoints = new HashSet<>();

                getArticulationPoints(g, g.getNode(g.getMinNodeId()), 0, articulationPoints, low, parent);
                //sarticulationPoints = getArticulationPoints(g, g.getNode(g.getMinNodeId()));
                return articulationPoints;
            }

            private static Set<Node> getArticulationPoints(Graph g, Node i) {

                Map<Integer, Integer> low = new HashMap<>();
                Map<Integer, Integer> depth = new HashMap<>();
                Map<Integer, Integer> parent = new HashMap<>();
                Set<Node> articulationPoints = new HashSet<>();

                int d = 0;
                Stack<Node> actual = new Stack<>();
                Stack<Node> visit = new Stack<>();
                visit.add(i);
                while (!visit.isEmpty()) {
                    Node n= visit.pop();
                    n.setValue(0);
                    actual.push(n);
                    depth.put(n.getId(), d);
                    low.put(i.getId(), d);
                    d++;
                    visit.addAll(g.getNeighbours(n).stream().filter(e -> e.getValue() == -1).collect(Collectors.toList()));
                }

                while (!actual.isEmpty()) {
                    Node n = actual.pop();
                    n.setValue(0);

                    int childCount = 0;
                    boolean isArticulation = false;

                    for(Node ni : g.getNeighbours(i)) {
                        if(ni.getValue() == -1) {
                            parent.put(ni.getId(), i.getId());
                            childCount++;
                            if(low.get(ni.getId()) >= depth.get(i.getId())) {
                                isArticulation = true;
                            }
                            low.put(i.getId(), Math.min(low.get(i.getId()), low.get(ni.getId())));
                        } else if(ni.getId() != parent.getOrDefault(i.getId(), ni.getId() + 1)) {
                            low.put(i.getId(), Math.min(low.get(i.getId()), depth.get(ni.getId())));
                        }
                    }

                    if((parent.containsKey(i.getId()) && isArticulation) || (!parent.containsKey(i.getId()) && childCount > 1)) {
                        articulationPoints.add(i);
                    }
                }

                return articulationPoints;

            }

            private static void getArticulationPoints(Graph graph, Node node, int d, Set<Node> articulationPoints, Map<Integer, Integer> low, Map<Integer, Integer> parent) {

                node.setValue(d);
                low.put(node.getId(), d);

                int childCount = 0;
                boolean isArticulation = false;

                for(Node ni : graph.getNeighbours(node)) {
                    if(ni.getValue() == -1) {
                        parent.put(ni.getId(), node.getId());
                        getArticulationPoints(graph, ni, d + 1, articulationPoints, low, parent);
                        childCount++;
                        if(low.get(ni.getId()) >= node.getValue()) {
                            isArticulation = true;
                        }
                        low.put(node.getId(), Math.min(low.get(node.getId()), low.get(ni.getId())));
                    } else if(ni.getId() != parent.getOrDefault(node.getId(), ni.getId() + 1)) {
                        low.put(node.getId(), Math.min(low.get(node.getId()), ni.getValue()));
                    }
                }

                if((parent.containsKey(node.getId()) && isArticulation) || (!parent.containsKey(node.getId()) && childCount > 1)) {
                    articulationPoints.add(node);
                }
            }
        }


    }

}
