import edu.group20.chromflow.graph.ChromaticNumber;
import edu.group20.chromflow.graph.Graph;
import edu.group20.chromflow.util.GraphReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GraphCleanerTest {

    @Test
    public void testFullyConnectedNodesRemover() {
        {
            Graph g = GraphReader.parseGraph("src/main/java/data/block3_2018_graph08.txt");
            Assertions.assertNotNull(g);
            ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, true);
            Assertions.assertEquals(98, r.getExact());
        }

        {
            Graph g = GraphReader.parseGraph("src/main/java/data/block3_2018_graph08.txt");
            Assertions.assertNotNull(g);
            ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, true);
            Assertions.assertEquals(98, r.getExact());
        }

        {
            Graph g = GraphReader.parseGraph("src/main/java/data/block3_2018_graph20.txt");
            Assertions.assertNotNull(g);
            ChromaticNumber.Result r = ChromaticNumber.compute(ChromaticNumber.Type.EXACT, g, false, true);
            Assertions.assertEquals(9, r.getExact());
        }
    }

}
