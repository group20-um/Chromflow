package edu.group20.chromflow;

import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.util.GraphReader;

public class TestApp {

    public final static boolean KELK_MODE = false;
    public static boolean OUTPUT_ENABLED = true;

    public static void main(String[] args) {

        // www.cs.uu.nl/education/scripties/pdf.php?SID=INF/SCR-2009-095

        args = new String[] {"src/main/java/data/graph02.txt"};
        long time = System.currentTimeMillis();
        Graph graph = GraphReader.parseGraph(args[0]);
        debug("Build Graph (%dms) >> Graph (%s) parsed %d vertices, %d edges and a density of %.6f%%.%n",
                (System.currentTimeMillis() - time), args[0], graph.getNodes().size(), graph.getEdgeCount(), graph.getDensity() * 100);

        debug("Result>> %s%n", ChromaticNumber.compute(ChromaticNumber.Type.EXACT, graph, false, true));

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
