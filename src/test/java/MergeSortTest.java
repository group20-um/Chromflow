import edu.group20.chromflow.graph.Mergesort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MergeSortTest {

    @Test
    public void testSortingCorrectlyAscending() {
        List<Integer> list = IntStream.range(-10000, 9999).boxed().collect(Collectors.toCollection(LinkedList::new));
        Collections.shuffle(list);
        Assertions.assertFalse(isInOrder(list), "The list should not be in order!");
        list = Mergesort.sort(list, Comparator.naturalOrder());
        Assertions.assertTrue(isInOrder(list), "The list is not in order!");
    }

    private boolean isInOrder(List<Integer> list) {
        return IntStream.range(0, list.size() - 1).noneMatch(i -> list.get(i) > list.get(i + 1));
    }

}
