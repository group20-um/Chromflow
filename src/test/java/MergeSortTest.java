import edu.group20.chromflow.util.Mergesort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MergeSortTest {

    @Test
    public void testSortingCorrectlyAscendingIntegers() {
        List<Integer> list = IntStream.range(-10000, 9999).boxed().collect(Collectors.toCollection(LinkedList::new));
        Collections.shuffle(list);
        list = Mergesort.sort(list, Comparator.naturalOrder());
        Assertions.assertTrue(isInOrder(list), "The list is not in order!");
    }

    @Test
    public void testSortingCorrectlyAscendingDoubles() {
        final List<Double> list = new ArrayList<>();
        IntStream.range(0, 20000).forEach(e -> list.add(Math.random()));

        Collections.shuffle(list);
        List<Double> sorted = Mergesort.sort(list, Comparator.naturalOrder());
        Assertions.assertTrue(isInOrder(sorted), "The list is not in order!");
    }

    @Test
    public void testSortingCorrectlyAscendingCharacters() {
        final List<Character> list = new ArrayList<>();
        IntStream.range(0, 20000).forEach(e -> list.add((char) ('a' + (Math.random() + 26))));

        Collections.shuffle(list);
        List<Character> sorted = Mergesort.sort(list, Comparator.naturalOrder());
        Assertions.assertTrue(isInOrder(sorted), "The list is not in order!");
    }

    private <T extends Comparable<T>> boolean isInOrder(List<T> list) {
        return IntStream.range(0, list.size() - 1).noneMatch(i -> list.get(i).compareTo(list.get(i + 1)) > 0);
    }

}
