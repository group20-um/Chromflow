package edu.group20.chromflow.graph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static edu.group20.chromflow.graph.Node.Edge;

public class Graph implements Cloneable {

    private Meta meta = new Meta();

    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();

    public Graph() {}

    /**
     * Returns some data that contains meta information for the graph that is required for some algorithms but not strictly
     * required for the graph itself.
     * @return Never null.
     */
    public Meta getMeta() {
        return this.meta;
    }

    /**
     * Counts the amount of edges in the graph.
     * @return
     */
    public int getEdgeCount() {
        return edges.values().stream().mapToInt(Map::size).sum();
    }

    /**
     * Returns the degree of a node.
     * @param node
     * @return
     */
    public int getDegree(int node) {
        return this.edges.get(node).size();
    }

    /**
     * Returns the density of the graph based on the edgeCount / maxEdges
     * @return
     */
    public double getDensity() {
        return getEdgeCount() / Math.pow(this.nodes.size(), 2);
    }

    /**
     * Resets the values of all nodes to '-1'.
     */
    public void reset() {
        this.nodes.values().forEach(e -> e.setValue(-1));
    }

    /**
     * Adds a new node if it does not exist yet.
     * @param id The id of the node.
     * @param value The value associated with the node.
     * @return True, if the node was added, false if the id is already part of the graph.
     */
    public boolean addNode(int id, int value) {
        if(!(this.nodes.containsKey(id))) {
            this.nodes.put(id, new Node(id, value));
            this.edges.put(id, new HashMap<>());
            return true;
        }
        return false;
    }

    /**
     * This adds an edge between A -> B.
     * @param from Start node id.
     * @param to End node id.
     * @param bidirectional Adds also a second edge from B -> A.
     */
    public void addEdge(int from, int to, boolean bidirectional) {

        if(bidirectional && hasEdge(from, to)) return;
        this.edges.get(from).put(to, new Edge(this.getNode(from), this.getNode(to)));

        if(bidirectional) {
            addEdge(to, from, false);
        }
    }

    /**
     * Returns a node based on the given id.
     * @param i The id of the nod.
     * @return The node, if it does not exist this throws an {@link IllegalArgumentException}.
     */
    public Node getNode(int i) {
        if(this.nodes.containsKey(i)) {
            return this.nodes.get(i);
        }
        throw new IllegalArgumentException();
    }

    /**
     * Gets the node with the next highest id after the given id.
     * @param start The node to start at.
     * @return Null, if start is the node with the highest id, otherwise the next node.
     */
    public Node getNextAvailableNode(Node start) {
        int maxNodeId = getMaxNodeId();
        for(int i = start.getId() + 1; i <= maxNodeId; i++) {
            if(this.nodes.containsKey(i)) {
                return this.nodes.get(i);
            }
        }
        return null;
    }

    /**
     * All edges assocaited with the node.
     * @param node The id of the node.
     * @return A map of all the associated edges.
     */
    public Map<Integer, Edge> getEdges(int node) {
        return this.edges.get(node);
    }

    /**
     * All nodes of the graph.
     * @return never null.
     */
    public Map<Integer, Node> getNodes() {
        return this.nodes;
    }

    /**
     * All edges of the graph.
     * @return Never null.
     */
    public Map<Integer, Map<Integer, Edge>> getEdges() {
        return this.edges;
    }


    /**
     * Returns the max node id of the graph.
     * @return
     */
    public int getMaxNodeId() {
        int max = Integer.MIN_VALUE;
        for(Node n : nodes.values()) {
            max = Math.max(n.getId(), max);
        }
        return max;
        //return this.nodes.values().stream().max(Comparator.comparingInt(Node::getId)).get().getId();
    }

    /**
     * Returns the min node id of the graph.
     * @return
     */
    public int getMinNodeId() {
        int min = Integer.MAX_VALUE;
        for(Node n : nodes.values()) {
            min = Math.min(n.getId(), min);
        }
        return min;
        //return this.nodes.values().stream().min(Comparator.comparingInt(Node::getId)).get().getId();
    }

    /**
     * Checks if there is an edge from A to B.
     * @param from The start node.
     * @param to The end node.
     * @return True, if there is an edge from A -> B, otherwise false.
     */
    public boolean hasEdge(int from, int to) {
        return this.edges.containsKey(from) && this.edges.get(from).containsKey(to);
    }

    /**
     * Checks if the graph has a node with the given id.
     * @param node The id of the node.
     * @return True, if the node exists, otherwise false.
     */
    public boolean hasNode(int node) {
        return this.nodes.containsKey(node);
    }

    /**
     * Clones the graph by copying all the nodes and edges.
     * @return
     */
    @Override
    public Graph clone() {
        Graph clone = new Graph();
        this.nodes.forEach((k, v) -> clone.addNode(k, v.getValue()));
        this.edges.forEach((k, v) -> v.forEach((id, edge) -> clone.addEdge(edge.getFrom().getId(), edge.getTo().getId(), true)));
        return clone;
    }

    //---

    /**
     * Returns the neighbours of a node.
     * @param node The node.
     * @return A list of all neighbours.
     */
    public List<Node> getNeighbours(Node node){
        List<Node> neighbours = new LinkedList<>();
        for(Node.Edge e : this.edges.get(node.getId()).values()) {
            neighbours.add(e.getTo());
        }
        return neighbours;
        //return this.edges.get(node.getId()).values().stream().map(Edge::getTo).collect(Collectors.toList());
    }

    /**
     * Checks if the graph is coloured by validating that no value of a node == -1 which is commonly associated with the
     * default value of a node. This is not checking if the colouring is valid.
     * @return
     */
    public boolean isColored(){
        return nodes.values().stream().noneMatch(n -> n.getValue() == -1);
    }

    /**
     * Colours all neighbours of a node.
     * @param nodes
     */
    public void colourNeighbours( List<Node> nodes){

        for(int i=0; i<nodes.size();i++){
            List<Node> neighbours=this.getNeighbours(nodes.get(i));
            int colour=0;
            if(nodes.get(i).getValue()==0){
                colour=1;
            }
            for (int j=0; j<neighbours.size(); j++){
                neighbours.get(j).setValue(colour);
            }
        }
    }

    /**
     * Checks if neighbours have the same colour.
     * @param node
     * @return
     */
    public boolean hasNeighbourSameColour( Node node){
        if(node.getValue()!=-1){
            List<Node> neighbours=this.getNeighbours(node);
            for(int i=0; i<neighbours.size(); i++){
                if(node.getValue()==neighbours.get(i).getValue()){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the graph is complete by checking if the degree of every node is n-1 where n = |nodes|.
     * @return
     */
    public boolean isComplete(){
        return nodes.keySet().stream().allMatch(id -> edges.get(id).size() == nodes.size() - 1);
    }

    /**
     * Turns the graph into a adjacency matrix.
     * @return
     */
    public double[][] toAdjacentMatrix(){

        double[][] adjacentMatrix = new double[nodes.size()][nodes.size()];

        Map<Integer, Integer> mapping = new HashMap<>();
        int index = 0;
        for(Node n : nodes.values()) {
            mapping.put(n.getId(), index);
            index++;
        }

        nodes.forEach((from_id, n) -> {
            final int mapFromId = mapping.get(from_id);
            getEdges(n.getId()).forEach((to_id, e) -> {
                final int mapToId = mapping.get(to_id);
                adjacentMatrix[mapFromId][mapToId] = 1;
                adjacentMatrix[mapToId][mapFromId] = 1;
            });
        });

        return adjacentMatrix;
    }

    /**
     * Turns the graph into a Laplacian matrix.
     * @return
     */
    public double[][] toLaplacianMatrix(){

        double[][] laplacianMatrix = new double[nodes.size()][nodes.size()];

        Map<Integer, Integer> mapping = new HashMap<>();
        int index = 0;
        for(Node n : nodes.values()) {
            mapping.put(n.getId(), index);
            index++;
        }

        nodes.forEach((from_id, n) -> {
            final int mapFromId = mapping.get(from_id);

            getEdges(n.getId()).forEach((to_id, e) -> {
                final int mapToId = mapping.get(to_id);
                laplacianMatrix[mapFromId][mapToId] = -1;
                laplacianMatrix[mapToId][mapFromId] = -1;
            });

            laplacianMatrix[mapFromId][mapFromId] = edges.get(from_id).size();

        });

        return laplacianMatrix;
    }

    /**
     * A meta class containing some information that is not directly related to the graph but is used in
     * algorithms related to it.
     */
    public static class Meta {
        private int level = 0;

        public Meta() {}

        public int getLevel() {
            return this.level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

    }

}
