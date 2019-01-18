package edu.group20.chromflow;

import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.util.GraphReader;

public class ImageGenerator {

    public static void main(String[] args) {
        for(int i = 1; i <= 20; i++) {
            Graph graph = GraphReader.parseGraph(String.format("src/main/java/data/block3_2018_graph%02d.txt", i));
            GraphCleaner.clean(graph);
            GephiConverter.generateGephiFile(graph, String.format("block3_2018_graph%02d.txt", i));
        }
    }

}
