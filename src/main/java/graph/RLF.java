package graph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class RLF {

    //>> https://imada.sdu.dk/~marco/Publications/Files/MIC2011-ChiGalGua.pdf
    //Comparing Algos >> http://dergipark.gov.tr/download/article-file/254140 (according to the paper RLF & WP are the best
    // across all benchmarked graphs)



    public static int recursiveLargetFirst(Graph graph) {

        // TODO we obviously wouldn't wanna start at 0, but whatever
        int k = 0;

        while (graph.getNodes().size() > 0) {
            k++;
            findStableSet(graph, k);
        }

        return k;

    }

    private static void findStableSet(Graph graph, int k) {

        List<Node> P = new ArrayList<Node>(graph.getNodes().values());
        List<Node> U = new LinkedList<>();
        while (!P.isEmpty()) {

        }


    }

}
