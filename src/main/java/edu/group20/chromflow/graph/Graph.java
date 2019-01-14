package edu.group20.chromflow.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static edu.group20.chromflow.graph.Node.Edge;

public class Graph implements Cloneable {

    private Meta meta = new Meta();

    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();

    //
    private int sizeCircle=0;
    private ArrayList<Integer> sizes=new ArrayList<>();


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
        this.meta = new Meta();
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

        if(bidirectional && hasEdge(from, to)) return;

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

    public void setSizeCircle(int sizeCircle) {
        this.sizeCircle = sizeCircle;
    }

    public boolean izCycle(Node source, Boolean visited[], Node parent ){


        //if (source.getValue() != -1) return true;

        visited[source.getId()-1]=true;

        List<Node> neighbours=this.getNeighbours(source);

        for(int i=0; i<neighbours.size(); i++ ){

            if (!visited[neighbours.get(i).getId()-1]){

                sizeCircle++;

                return izCycle( neighbours.get(i) , visited, source);

            }

            else if( neighbours.get(i)!= parent ){

                sizeCircle++;
                int cnt = 0;
                sizes.add(cnt, sizeCircle);
                return true;
            }

            else{
                sizeCircle--;
            }

        }

        return false;

    }

    public int getSizeCircle() {
        return sizeCircle;
    }

    public int cycleSize(Node source, int size) {

        if (source.getValue() != -1) return size;
        else if (this.getNextAvailableNode(source) == null) return 0;
        else {
            source.setValue(1);
            size++;
            return cycleSize(this.getNextAvailableNode(source), size);
        }
    }

    public boolean isBipartite( List<Node> parent){

        if(this.getNodes().size()<2){ //probably check somewhere else
            return false;
        }

        if(this.isColored()){
            return true;
        }

        colourNeighbours( parent);
        List<Node> newParents=new ArrayList<>();
        for(int i=0; i<parent.size();i++){

            if(hasNeighbourSameColour( parent.get(i))){
                return false;
            }
            newParents.addAll(this.getNeighbours(parent.get(i)));
        }

        return isBipartite( newParents);

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

    public boolean isPath () { //doesn't work perfectly, no need
        int size = nodes.size();
        for (int i = 1; i <= size; i ++){
            if (this.getNeighbours(nodes.get(i)).size() > 2) return false;
        }
        return true;
    }

    public boolean isComplete(){
        return nodes.keySet().stream().noneMatch(id -> edges.get(id).size() != nodes.size() - 1);
    }

    public boolean hasCycle(){
        this.isComplete();

        Boolean[] visited=new Boolean[this.getNodes().size()];

        for(int i=0; i<visited.length; i++){
            visited[i]=false;
        }

        boolean cyclic=false;

        for( int i=1; i<= this.getNodes().size(); i++){

            if(!visited[i-1]){

                if(this.izCycle(this.getNode(i), visited, new Node(-1, -1))){

                    cyclic=true;

                }
            }

        }

        return cyclic;
    }

    public boolean hasOnlyEvenCycles(){

        boolean even=true;

        for(int i=0; i<sizes.size(); i++){

            if(sizes.get(i)%2!=0){
                even=false;
            }
        }

        return even;
    }

    public int findBiggestCycle(){

        int max=0;

        for(int i=0; i<sizes.size(); i++){

            if(sizes.get(i)>max){
                max=sizes.get(i);
            }
        }

        return max;

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
