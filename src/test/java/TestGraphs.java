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
                1, 6, 7, 11, 12, 16, 18
        ));

        for (int i = 1; i <= 20; i++) {

            if(ignore.contains(i))continue;

            Graph g = GraphReader.parseGraph(String.format("src/main/java/data/graph%02d.txt", i));
            Assertions.assertNotNull(g);

            ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, true);
            Assertions.assertEquals(r.getExact(), results[i-1], String.format("Graph %02d", i));


        }
    }


}
