import edu.group20.chromflow.TestApp;
import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.util.GraphReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

public class TestGraphs {

    @BeforeAll
    public static void setup() {
        TestApp.OUTPUT_ENABLED = false;
    }

    @Test
    public void testAllGraphsFromBlock1() {

        int[] results = new int[] {
                5, 10, 2, 11, 4, 54, 7, 2, 16, 5, 15, 5, 31, 4, 4, 7, 3, 8, 31, 4
        };
        HashSet<Integer> ignore = new HashSet<>(Arrays.asList(
                1, 7, 11, 12, 16, 18
        ));

        for (int i = 1; i <= 20; i++) {

            if(ignore.contains(i))continue;

            Graph g = GraphReader.parseGraph(String.format("src/main/java/data/block1/graph%02d.txt", i));
            Assertions.assertNotNull(g);

            ChromaticNumber.Result r = ChromaticNumber.computeExact(g, true);
            Assertions.assertEquals(results[i-1], r.getExact(), String.format("Graph %02d", i));

        }
    }

    @Test
    public void testBenchmarkGraphs() {

        final String[] graphs = new String[] {
                "1-FullIns_3", "2-FullIns_3", "2-Insertions_3", "anna", "david", "fpsol2.i.3", "games120", "homer", "huck", "jean", "le450_25a", "le450_25b", "miles1500", "miles250", "miles500", "mulsol.i.1", "mulsol.i.2", "mulsol.i.3", "mulsol.i.4", "mulsol.i.5", "myciel3", "myciel4", "queen5_5", "queen6_6", "r125.1", "r250.1", "zeroin.i.1", "zeroin.i.2", "zeroin.i.3", "1-FullIns_4"
        };

        //24
        final int[] exactValues = {
                4, 5, 4, 11, 11, 30, 9, 13, 11, 10, 25, 25, 73, 8, 20, 49, 31, 31, 31, 31, 4, 5, 5, 7, 5, 8, 49, 30, 30, 5
        };

        for (int i = 0; i < graphs.length; i++) {
            final String graph = graphs[i];

            Graph g = GraphReader.parseGraph(String.format("src/main/java/data/benchmark/%s.col", graph));
            Assertions.assertNotNull(g);

            ChromaticNumber.Result r = ChromaticNumber.computeExact(g, true);
            Assertions.assertEquals(exactValues[i], r.getExact(), String.format("Graph %s", graph));

        }

    }

    @Test
    public void testGraphsFromBlock3() {
        int[] results = new int[] {
            3, -1, -1, -1, 2, 3, -1, 98, -1, 3, 15, 2, -1, 4, -1, -1, 8, 10, 11, 9
        };
        HashSet<Integer> ignore = new HashSet<>(Arrays.asList(
                2,3,4,7,9,13,15,16
        ));

        for (int i = 1; i <= 20; i++) {

            if(ignore.contains(i))continue;

            Graph g = GraphReader.parseGraph(String.format("src/main/java/data/block3/block3_2018_graph%02d.txt", i));
            Assertions.assertNotNull(g);

            ChromaticNumber.Result r = ChromaticNumber.computeExact(g, true);
            Assertions.assertEquals(results[i-1], r.getExact(), String.format("Graph %02d", i));

        }
    }

}
