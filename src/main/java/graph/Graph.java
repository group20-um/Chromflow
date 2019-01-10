package graph;

import java.util.*;

import static graph.Node.*;

public class Graph implements Cloneable {

    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();

    public Graph() {}

    public void reset() {
        this.nodes.values().forEach(e -> e.setValue(-1));
    }

    public boolean addNode(int id, int value) {
        if(!(this.nodes.containsKey(id))) {
            this.nodes.put(id, new Node(id, value));
            return true;
        }
        return false;
    }

    public void addEdge(int from, int to, boolean bidirectional) {
        if(!(this.edges.containsKey(from))) {
            this.edges.put(from, new HashMap<>());
        }
        this.edges.get(from).put(to, new Edge(this.getNode(from), this.getNode(to)));

        if(bidirectional) {
            addEdge(to, from, false);
        }
    }

    public Node getNode(int i) {
        if(this.nodes.containsKey(i)) {
            return this.nodes.get(i);
        }
        throw new IllegalArgumentException();
    }

    public Node getNextAvailableNode(Node start) {
        int maxNodeId = getMaxNodeId();
        for(int i = start.getId() + 1; i <= maxNodeId; i++) {
            if(this.nodes.containsKey(i)) {
                return this.nodes.get(i);
            }
        }
        return null;
    }

    public Map<Integer, Edge> getEdges(int node) {
        return this.edges.getOrDefault(node, new HashMap<>());
    }

    public Map<Integer, Node> getNodes() {
        return this.nodes;
    }

    public Map<Integer, Map<Integer, Edge>> getEdges() {
        return this.edges;
    }


    public int getMaxNodeId() {
        return this.nodes.values().stream().max(Comparator.comparingInt(Node::getId)).get().getId();
    }

    public int getMinNodeId() {
        return this.nodes.values().stream().min(Comparator.comparingInt(Node::getId)).get().getId();
    }

    public boolean hasEdge(int from, int to) {
        return this.edges.containsKey(from) && this.edges.get(from).containsKey(to);
    }

    @Override
    public Graph clone() {
        Graph clone = new Graph();
        this.nodes.forEach((k, v) -> clone.addNode(k, v.getValue()));
        this.edges.forEach((k, v) -> v.forEach((id, edge) -> clone.addEdge(edge.getFrom().getId(), edge.getTo().getId(), true)));
        return clone;
    }
}
