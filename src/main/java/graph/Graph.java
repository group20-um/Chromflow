package graph;

import java.util.*;
import java.util.stream.Collectors;

import static graph.Node.Edge;

public class Graph implements Cloneable {

    private Map<Integer, Node> nodes = new HashMap<>();
    private Map<Integer, Map<Integer, Edge>> edges = new HashMap<>();

    //
    private int sizeCircle=0;
    public int[][] adjMatrix;
    private ArrayList<Integer> sizes=new ArrayList<>();

    public Graph() {}

    public double getDensity() {
        return edges.values().stream().mapToInt(Map::size).sum() / Math.pow(this.nodes.size(), 2);
    }

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
            System.out.println("Is NOT bipartite");
            return false;
        }

        if(this.isColored()){
            System.out.println("Is bipartite");
            return true;
        }

        colourNeighbours( parent);
        List<Node> newParents=new ArrayList<>();
        for(int i=0; i<parent.size();i++){

            if(hasNeighbourSameColour( parent.get(i))){
                System.out.println("Is NOT bipartite");
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

        boolean complete=true;

        for(int i=1; i<=nodes.size();i++){

            if(this.getNeighbours(this.getNode(i)).size()!=nodes.size()-1){
                complete=false;
            }

        }
        if (complete){
            System.out.println("IS complete");
        }
        else {
            System.out.println("Not complete");
        }
        return complete;
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

        if(cyclic){
            System.out.println("IS cyclic");
            return true;
        }
        else{
            System.out.println("Not cyclic");
        }
        return false;
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

    public void createMatrix(){

        this.adjMatrix=new int[nodes.size()][nodes.size()];

        for(int i=0; i<adjMatrix.length; i++){

            List<Node> neighbours=this.getNeighbours(nodes.get(i+1));

            for(int j=0; j<neighbours.size(); j++){

                adjMatrix[i][neighbours.get(j).getId()-1]=1;
                adjMatrix[neighbours.get(j).getId()-1][i]=1;

            }

        }

    }
}
