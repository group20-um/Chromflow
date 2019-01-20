package edu.group20.chromflow.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.group20.chromflow.graph.Node.Edge;

public class Graph implements Cloneable {

    private Meta meta = new Meta();

    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();

    public Graph() {}

    public Meta getMeta() {
        return this.meta;
    }

    public int getEdgeCount() {
        return edges.values().stream().mapToInt(Map::size).sum();
    }

    public int getDegree(int node) {
        return this.edges.get(node).size();
    }

    public double getDensity() {
        return getEdgeCount() / Math.pow(this.nodes.size(), 2);
    }

    public void reset() {
        this.nodes.values().forEach(e -> e.setValue(-1));
    }

    public boolean addNode(int id, int value) {
        if(!(this.nodes.containsKey(id))) {
            this.nodes.put(id, new Node(id, value));
            this.edges.put(id, new HashMap<>());
            return true;
        }
        return false;
    }

    public void addEdge(int from, int to, boolean bidirectional) {

        if(bidirectional && hasEdge(from, to)) return;
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
        return this.edges.get(node);
    }

    public Map<Integer, Node> getNodes() {
        return this.nodes;
    }

    public Map<Integer, Map<Integer, Edge>> getEdges() {
        return this.edges;
    }


    public int getMaxNodeId() {
        int max = Integer.MIN_VALUE;
        for(Node n : nodes.values()) {
            max = Math.max(n.getId(), max);
        }
        return max;
        //return this.nodes.values().stream().max(Comparator.comparingInt(Node::getId)).get().getId();
    }

    public int getMinNodeId() {
        int min = Integer.MAX_VALUE;
        for(Node n : nodes.values()) {
            min = Math.min(n.getId(), min);
        }
        return min;
        //return this.nodes.values().stream().min(Comparator.comparingInt(Node::getId)).get().getId();
    }

    public boolean hasEdge(int from, int to) {
        return this.edges.containsKey(from) && this.edges.get(from).containsKey(to);
    }

    public boolean hasNode(int node) {
        return this.nodes.containsKey(node);
    }

    @Override
    public Graph clone() {
        Graph clone = new Graph();
        this.nodes.forEach((k, v) -> clone.addNode(k, v.getValue()));
        this.edges.forEach((k, v) -> v.forEach((id, edge) -> clone.addEdge(edge.getFrom().getId(), edge.getTo().getId(), true)));
        return clone;
    }

    //---
    public List<Node> getNeighbours(Node node){
        return this.edges.get(node.getId()).values().stream().map(Edge::getTo).collect(Collectors.toList());
    }

    public boolean isColored(){
        return nodes.values().stream().noneMatch(n -> n.getValue() == -1);
    }

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

    public boolean isComplete(){
        return nodes.keySet().stream().allMatch(id -> edges.get(id).size() == nodes.size() - 1);
    }

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
