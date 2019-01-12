package graph;

import java.util.List;
import java.util.ArrayList;

public class TestLowerBound {

    private static ArrayList<Node> list;
    private static int n;
    private static int maxSize = 0;


    public static int search(Graph g){
        list = new ArrayList<>(g.getNodes().values());
        n = list.size();
        List<Node> C = new ArrayList<>();
        expand(g, C, list);
        return maxSize;

    }

    public static void expand(Graph graph, List<Node> C, List<Node> list){
        for (int i=list.size()-1;i>=0;i--){
            if (C.size()+list.size()<=maxSize) {
                return;
            }
            Node v = list.get(i);
            C.add(v);
            List<Node> newList = new ArrayList<Node>();
            for (Node w : list){
                if(graph.hasEdge(v.getId(), w.getId())) {
                    newList.add(w);
                }
            }
            if (newList.isEmpty()&&C.size()>maxSize){
                maxSize = Math.max(maxSize, C.size());
            }

            if(!newList.isEmpty()){
                expand(graph, C, newList);
            }
            C.remove(v);
            list.remove(v);
        }

    }

}
