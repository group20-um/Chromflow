package edu.group20.chromflow.graph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Code heavily inspired by https://www.geeksforgeeks.org/merge-sort/ and only upgraded
// to suit our requirements
public class Mergesort {

    private static <T> void merge(Comparator<T> comparator, T[] array, int l, int m, int r)  {

        int n1 = m - l + 1;
        int n2 = r - m;

        T[] L = (T[]) new Object[n1];
        T[] R = (T[]) new Object[n2];

        System.arraycopy(array, l, L, 0, n1);
        System.arraycopy(array, m + 1, R, 0, n2);

        int i = 0, j = 0;
        int k = l;
        while (i < n1 && j < n2) {
            if (comparator.compare(L[i], R[j]) <= 0) {
                array[k] = L[i];
                i++;
            } else {
                array[k] = R[j];
                j++;
            }
            k++;
        }

        while (i < n1) {
            array[k] = L[i];
            i++;
            k++;
        }

        while (j < n2) {
            array[k] = R[j];
            j++;
            k++;
        }

    }

    private static <T> void sort(Comparator<T> comparator, T[] array, int l, int r) {
        if (l < r) {
            int m = l + (r - l) / 2;
            sort(comparator, array, l, m);
            sort(comparator, array, m+1, r);
            merge(comparator, array, l, m, r);
        }
    }

    public static <T> LinkedList<T> sort(List<T> list, Comparator<T> comparator) {
        T[] array = (T[]) list.toArray();
        sort(comparator, array, 0, array.length - 1);
        return Stream.of(array).collect(Collectors.toCollection(LinkedList::new));
    }

}
