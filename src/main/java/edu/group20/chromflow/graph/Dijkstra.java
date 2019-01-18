package edu.group20.chromflow.graph;

import java.util.*;

public class Dijkstra {

    public static Map<Integer, Integer> buildPaths(Graph graph, final int start) {

        graph.getNodes().forEach((k, node) -> node.setValue(Integer.MAX_VALUE));
        graph.getNode(start).setValue(0);

        PriorityQueue<Node> vertices = new PriorityQueue<>(Comparator.comparingInt(Node::getValue));
        vertices.add(graph.getNode(start));

        Map<Integer, Integer> previous = new HashMap<>();

        while (!(vertices.isEmpty())) {

            Node current = vertices.poll();

            Collection<Node.Edge> edges = graph.getEdges(current.getId()).values();
            edges.forEach(edge -> {

                Node neighbour = edge.getTo();
                int distance = neighbour.getValue() + 1; // 1 -> constant distance because we have an unweighted graph
                if(distance < neighbour.getValue()) {
                    neighbour.setValue(distance);
                    previous.put(neighbour.getId(), current.getId());
                    vertices.add(neighbour);
                }

            });

        }

        return previous;
    }

}
