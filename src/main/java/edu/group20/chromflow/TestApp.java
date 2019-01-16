package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.graph.GraphStructures;
import edu.group20.chromflow.util.GraphReader;

public class TestApp {

    public final static boolean KELK_MODE = false;
    public static boolean OUTPUT_ENABLED = true;

    public static void main(String[] args) {

        //2,3,4,7,9,
        // planar, very high chance, 13,14,
        // planar graph: 13, 14, 16
        // 15,

        //graph04.txt breaks isPlanar
        args = new String[] {"src/main/java/data/block3_2018_graph16.txt"};
        Graph graph = GraphReader.parseGraph(args[0]);

        debug("Result>> %s%n", ChromaticNumber.computeExact(graph, true));

    }

    public static void debug(String format, Object... vars) {
         if(!KELK_MODE && OUTPUT_ENABLED) System.out.printf(format, vars);
    }

    public static void debugln(String message) {
        if(!KELK_MODE && OUTPUT_ENABLED) System.out.printf("%s%n", message);
    }

    public static void kelkOutput(String format, Object... vars) {
        if(KELK_MODE && OUTPUT_ENABLED) System.out.printf(format, vars);
    }


}
