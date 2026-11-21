package generics;

import java.util.Collection;
import java.util.List;

/**
 * Utility class demonstrating Generics concepts (Lectures 47-48):
 * - Generic methods: <T> void printCollection(...)
 * - Bounded type parameters: <T extends Comparable<T>>
 * - Wildcards: Collection<?> (unbounded), List<? extends Number>
 */
public class GenericUtils {

    /**
     * Unbounded Wildcard (Collection<?>)
     * Can print any collection regardless of element type.
     */
    public static void printCollection(Collection<?> items, String header) {
        System.out.println("--- " + header + " ---");
        if (items.isEmpty()) {
            System.out.println(" [Empty Collection]");
            return;
        }
        int index = 1;
        for (Object item : items) {
            System.out.println("  " + (index++) + ". " + item);
        }
    }

    /**
     * Bounded Type Parameter (<T extends Comparable<T>>)
     * Finds the maximum element in a list of comparable items.
     */
    public static <T extends Comparable<T>> T findMax(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        T max = list.get(0);
        for (T item : list) {
            if (item.compareTo(max) > 0) {
                max = item;
            }
        }
        return max;
    }

    /**
     * Generic swap method exchanging two positions in an array.
     */
    public static <T> void swap(T[] array, int i, int j) {
        if (array == null || i < 0 || j < 0 || i >= array.length || j >= array.length) {
            return;
        }
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
