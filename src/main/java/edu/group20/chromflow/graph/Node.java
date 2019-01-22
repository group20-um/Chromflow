package edu.group20.chromflow.graph;

/**
 * A node represents a vertex in a graph.
 */
public class Node {

    private final int id;
    private int value;

    /**
     * @param id The unique id of the node.
     * @param value The associated value.
     */
    public Node(int id, int value) {
        this.id = id;
        this.value = value;
    }

    public int getId() {
        return id;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Represents the edge between two Nodes.
     */
    public static class Edge {

        private Node from;
        private Node to;

        public Edge(Node from, Node to) {
            this.from = from;
            this.to = to;
        }

        public Node getFrom() {
            return this.from;
        }

        public Node getTo() {
            return this.to;
        }

    }

    @Override
    public String toString() {
        return String.format("[Node;id=%d,value=%d]", this.id, this.value);
    }
}
