package edu.group20.chromflow.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestLowerBound {


    public static int search(Graph g){
        HashSet<Node> list = new HashSet<>(g.getNodes().values());
        HashSet<Node> C = new HashSet<>();

        AtomicInteger maxSize = new AtomicInteger(Integer.MIN_VALUE);
        expand(g, C, list, maxSize);
        return maxSize.get();

    }

    private static void expand(Graph graph, HashSet<Node> C, HashSet<Node> set, AtomicInteger maxSize){
        Iterator<Node> iterator = set.iterator();
        while (iterator.hasNext()){
            if (C.size() + set.size() <= maxSize.get()) {
                break;
            }

            Node node = iterator.next();
            C.add(node);
            HashSet<Node> newList = set.stream()
                    .filter(w -> graph.hasEdge(node.getId(), w.getId()))
                    .collect(Collectors.toCollection(HashSet::new));

            if (newList.isEmpty()) {
                maxSize.set(Math.max(maxSize.get(), C.size()));
            } else {
                expand(graph, C, newList, maxSize);
            }

            C.remove(node);
            iterator.remove();

        }
    }

}
