package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.List;
import java.util.Comparator;

// KajiLibrary's java.util.Collections — static algorithms over the collection interfaces:
// reverse, swap, fill, and sort (natural order or by a Comparator). Non-instantiable, like
// the JDK's. This subset works through a List's indexed access (get/set/size), so it needs
// no iteration. Standalone and doubly-gated for now: our concretes aren't `List` yet (#9)
// and other KajiLibrary code can't call java.util statics (#11) — but it compiles and is
// ready for when those land. (The JDK also has emptyList/unmodifiable*/nCopies/binarySearch/
// max/min/frequency, deferred: they need concrete backing lists or iteration.)
public final class Collections {

    private Collections() {}

    // Swap the elements at positions `i` and `j`.
    public static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }

    // Reverse the order of the elements in place.
    public static <T> void reverse(List<T> list) {
        int size = list.size();
        for (int i = 0; i < size / 2; i++) {
            Collections.swap(list, i, size - 1 - i);
        }
    }

    // Replace every element with `obj`.
    public static <T> void fill(List<T> list, T obj) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            list.set(i, obj);
        }
    }

    // Sort ascending by a Comparator (stable-ish insertion sort over indexed access).
    public static <T> void sort(List<T> list, Comparator<? super T> c) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && c.compare(list.get(j), key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }

    // Sort ascending by natural order (elements must be Comparable to each other).
    public static <T extends Comparable<? super T>> void sort(List<T> list) {
        int size = list.size();
        for (int i = 1; i < size; i++) {
            T key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).compareTo(key) > 0) {
                list.set(j + 1, list.get(j));
                j = j - 1;
            }
            list.set(j + 1, key);
        }
    }
}
