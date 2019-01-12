package edu.group20.chromflow.graph;

import java.util.*;
import java.util.stream.Collectors;

public class RLF {

    //>> https://imada.sdu.dk/~marco/Publications/Files/MIC2011-ChiGalGua.pdf
    //Comparing Algos >> http://dergipark.gov.tr/download/article-file/254140 (according to the paper RLF & WP are the best
    // across all benchmarked graphs)

    // friday:
    // - https://arxiv.org/pdf/1505.05825.pdf (a lot of algos)

    //--- NORMAL
    public static int recursiveLargetFirst(Graph graph) {
        graph.reset();

        // TODO we obviously wouldn't wanna start at 0, but whatever
        int k = 0;

        while (graph.getNodes().values().stream().anyMatch(e -> e.getValue() == -1)) {
            k++;
            findStableSet(graph, k);
        }

        return k;

    }

    private static void findStableSet(Graph graph, int k) {

        Map<Integer, Integer> ignoreEdges = new HashMap<>();
        Map<Integer, Integer> degrees = new HashMap<>();

        List<Node> P = graph.getNodes().values().stream().filter(e -> e.getValue() == -1).collect(Collectors.toList());
        List<Node> U = new LinkedList<>();
        while (P.stream().anyMatch(e -> e.getValue() == -1)) {

            Node max = P.stream().filter(e -> e.getValue() == -1).max(new Comparator<Node>() {
                @Override
                public int compare(Node o1, Node o2) {
                    return -Integer.compare(getDegree(o1), getDegree(o2));
                }

                public int getDegree(Node n) {
                    List<Node> neighbours = graph.getNeighbours(n);
                    int i = 0;
                    for (Node neighbour : neighbours) {
                        if (U.contains(neighbour)) {
                            i++;
                        }
                    }
                    return i;
                }
            }).get();
            //Node max = U.stream().max((o1, o2) -> -Integer.compare(degrees.getOrDefault(o1.getId(), 0), degrees.getOrDefault(o2.getId(), 0))).get();
            max.setValue(k);


            Collection<Node.Edge> edges = graph.getEdges(max.getId()).values();
            edges.forEach(edge -> {
                Node neighbour = edge.getTo();

                if (P.contains(neighbour)) { // TODO: replace P with HashMap to get constant lookup time
                    P.remove(neighbour);
                    U.add(neighbour);
                }

                ignoreEdges.put(edge.getFrom().getId(), edge.getTo().getId());
                ignoreEdges.put(edge.getTo().getId(), edge.getFrom().getId());

                graph.getEdges(neighbour.getId()).entrySet().stream()
                        .filter(e -> !(ignoreEdges.getOrDefault(neighbour.getId(), -1).intValue() == e.getKey()))
                        .filter(e -> P.contains(e.getValue().getTo()))
                        .forEach(e -> degrees.put(e.getValue().getTo().getId(), degrees.getOrDefault(e.getValue().getTo().getId(), 0) + 1));
            });


        }

    }

}