package java.util;

import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import java.lang.reflect.Array;

// KajiLibrary's java.util.Arrays — static utilities over arrays: content equality, hashing,
// stringification, fill, right-sized copies, and sorting. Non-instantiable, like the JDK's.
// A KajiLibrary subset: covers int/char/boolean/Object arrays (the JDK repeats every method
// across all eight primitive types). Sorting uses insertion sort. Standalone — usable once
// #11 lets other KajiLibrary classes call java.util statics.
public final class Arrays {

    private Arrays() {}

    // --- toString ----------------------------------------------------------------

    public static String toString(int[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(char[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(boolean[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toString(Object[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (a[i] == null) {
                sb.append("null");
            } else {
                sb.append(a[i].toString());
            }
        }
        sb.append(']');
        return sb.toString();
    }

    // --- equals ------------------------------------------------------------------

    public static boolean equals(int[] a, int[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(char[] a, char[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(boolean[] a, boolean[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(Object[] a, Object[] b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            Object x = a[i];
            Object y = b[i];
            if (x == null) {
                if (y != null) {
                    return false;
                }
            } else {
                if (!x.equals(y)) {
                    return false;
                }
            }
        }
        return true;
    }

    // --- hashCode ----------------------------------------------------------------

    public static int hashCode(int[] a) {
        if (a == null) {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < a.length; i++) {
            result = 31 * result + a[i];
        }
        return result;
    }

    public static int hashCode(Object[] a) {
        if (a == null) {
            return 0;
        }
        int result = 1;
        for (int i = 0; i < a.length; i++) {
            int e = a[i] == null ? 0 : a[i].hashCode();
            result = 31 * result + e;
        }
        return result;
    }

    // --- fill --------------------------------------------------------------------

    public static void fill(int[] a, int val) {
        for (int i = 0; i < a.length; i++) {
            a[i] = val;
        }
    }

    public static void fill(char[] a, char val) {
        for (int i = 0; i < a.length; i++) {
            a[i] = val;
        }
    }

    public static void fill(boolean[] a, boolean val) {
        for (int i = 0; i < a.length; i++) {
            a[i] = val;
        }
    }

    public static void fill(Object[] a, Object val) {
        for (int i = 0; i < a.length; i++) {
            a[i] = val;
        }
    }

    // --- copyOf (right-sized copy, truncating or zero/null-padding) ---------------

    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        int n = original.length < newLength ? original.length : newLength;
        for (int i = 0; i < n; i++) {
            copy[i] = original[i];
        }
        return copy;
    }

    public static char[] copyOf(char[] original, int newLength) {
        char[] copy = new char[newLength];
        int n = original.length < newLength ? original.length : newLength;
        for (int i = 0; i < n; i++) {
            copy[i] = original[i];
        }
        return copy;
    }

    public static boolean[] copyOf(boolean[] original, int newLength) {
        boolean[] copy = new boolean[newLength];
        int n = original.length < newLength ? original.length : newLength;
        for (int i = 0; i < n; i++) {
            copy[i] = original[i];
        }
        return copy;
    }



    // --- sort (ascending, insertion sort) ----------------------------------------

    public static void sort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = key;
        }
    }

    public static void sort(char[] a) {
        for (int i = 1; i < a.length; i++) {
            char key = a[i];
            int j = i - 1;
            while (j >= 0 && a[j] > key) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = key;
        }
    }

    // ---- spliterators over an array ----
    //
    // ORDERED and IMMUTABLE, and the second is the one that surprises: the array can be modified,
    // and what the characteristic promises is that **the spliterator will not do it**. It is a
    // promise about the traversal, not about the data.

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     */
    public static <T> Spliterator<T> spliterator(T[] array) {
        // The local changes nothing -- a `T[]` IS an `Object[]` -- and it is here because our javac
        // declares the call ambiguous when the argument is an array of a type VARIABLE. With
        // `String[]` it chooses well; with `T[]` it does not choose (#279). Naming the parameter's
        // type is exactly what it did not deduce.
        Object[] widened = array;
        return Spliterators.spliterator(widened, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over {@code [startInclusive, endExclusive)} of {@code array}.
     *
     * @param array what to traverse
     * @param startInclusive where to start
     * @param endExclusive where to stop, exclusive
     */
    public static <T> Spliterator<T> spliterator(T[] array, int startInclusive,
            int endExclusive) {
        Object[] widened = array;
        return Spliterators.spliterator(widened, startInclusive, endExclusive,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     */
    public static Spliterator.OfInt spliterator(int[] array) {
        return Spliterators.spliterator(array, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over {@code [startInclusive, endExclusive)} of {@code array}.
     *
     * @param array what to traverse
     * @param startInclusive where to start
     * @param endExclusive where to stop, exclusive
     */
    public static Spliterator.OfInt spliterator(int[] array, int startInclusive,
            int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     */
    public static Spliterator.OfLong spliterator(long[] array) {
        return Spliterators.spliterator(array, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over {@code [startInclusive, endExclusive)} of {@code array}.
     *
     * @param array what to traverse
     * @param startInclusive where to start
     * @param endExclusive where to stop, exclusive
     */
    public static Spliterator.OfLong spliterator(long[] array, int startInclusive,
            int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     */
    public static Spliterator.OfDouble spliterator(double[] array) {
        return Spliterators.spliterator(array, Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    /**
     * A spliterator over {@code [startInclusive, endExclusive)} of {@code array}.
     *
     * @param array what to traverse
     * @param startInclusive where to start
     * @param endExclusive where to stop, exclusive
     */
    public static Spliterator.OfDouble spliterator(double[] array, int startInclusive,
            int endExclusive) {
        return Spliterators.spliterator(array, startInclusive, endExclusive,
                Spliterator.ORDERED | Spliterator.IMMUTABLE);
    }

    public static int binarySearch(int[] a, int key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a bare
    // "I did not find it": it encodes WHERE it would have gone, which is what allows inserting in
    // order without searching again.
    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Integer.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return binarySearch(a, 0, a.length, key, c);
    }


    public static int mismatch(int[] a, int[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static <T> int mismatch(T[] a, T[] b, Comparator<? super T> cmp) {
        return mismatch(a, 0, a.length, b, 0, b.length, cmp);
    }

    public static int compare(int[] a, int[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }



    // ---- element-by-element comparison --------------------------------------------------------
    //
    // Everything that follows --sort, binarySearch, compare, mismatch-- leans on the wrapper's
    // `compare` and not on `<`. For the integers it makes no difference; for `float` and `double` it
    // does: `<` says NaN is neither less nor greater than anything and that -0.0 == 0.0, and with
    // that a sort does not finish sorting and a binarySearch gets lost. `Double.compare` defines the
    // total order `Arrays`'s specification demands: -0.0 before 0.0, and NaN at the end.

    // It sorts the whole of `a`, ascending.
    public static void sort(byte[] a) {
        sort(a, 0, a.length);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(byte[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        byte[] scratch = new byte[toIndex - fromIndex];
        mergeSortByte(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(byte[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(byte[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(char[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        char[] scratch = new char[toIndex - fromIndex];
        mergeSortCharacter(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(char[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(char[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts the whole of `a`, ascending.
    public static void sort(short[] a) {
        sort(a, 0, a.length);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(short[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        short[] scratch = new short[toIndex - fromIndex];
        mergeSortShort(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(short[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(short[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(int[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        int[] scratch = new int[toIndex - fromIndex];
        mergeSortInteger(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(int[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(int[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts the whole of `a`, ascending.
    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(long[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        long[] scratch = new long[toIndex - fromIndex];
        mergeSortLong(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(long[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(long[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts the whole of `a`, ascending.
    public static void sort(float[] a) {
        sort(a, 0, a.length);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(float[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        float[] scratch = new float[toIndex - fromIndex];
        mergeSortFloat(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(float[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(float[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts the whole of `a`, ascending.
    public static void sort(double[] a) {
        sort(a, 0, a.length);
    }

    // It sorts [fromIndex, toIndex) of `a`, ascending.
    public static void sort(double[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        double[] scratch = new double[toIndex - fromIndex];
        mergeSortDouble(a, scratch, fromIndex, toIndex);
    }

    // The same as `sort`. A KajiLibrary subset: sequential, with no fork/join.
    public static void parallelSort(double[] a) {
        sort(a, 0, a.length);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static void parallelSort(double[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // It sorts by the elements' natural order.
    public static void sort(java.lang.Object[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(java.lang.Object[] a, int fromIndex, int toIndex) {
        validRange(a.length, fromIndex, toIndex);
        java.lang.Object[] scratch = new java.lang.Object[toIndex - fromIndex];
        mergeSortObj(a, scratch, fromIndex, toIndex, null);
    }

    public static <T> void sort(T[] a, java.util.Comparator<? super T> c) {
        sort(a, 0, a.length, c);
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, java.util.Comparator<? super T> c) {
        validRange(a.length, fromIndex, toIndex);
        java.lang.Object[] scratch = new java.lang.Object[toIndex - fromIndex];
        mergeSortObj(a, scratch, fromIndex, toIndex, c);
    }

    public static <T> void parallelSort(T[] a, java.util.Comparator<? super T> cmp) {
        sort(a, 0, a.length, cmp);
    }

    public static <T> void parallelSort(T[] a, int fromIndex, int toIndex, java.util.Comparator<? super T> cmp) {
        sort(a, fromIndex, toIndex, cmp);
    }

    public static int binarySearch(byte[] a, byte key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Byte.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(char[] a, char key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(char[] a, int fromIndex, int toIndex, char key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Character.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(short[] a, short key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(short[] a, int fromIndex, int toIndex, short key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Short.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(long[] a, long key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(long[] a, int fromIndex, int toIndex, long key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Long.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(float[] a, float key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(float[] a, int fromIndex, int toIndex, float key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Float.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(double[] a, double key) {
        return binarySearch(a, 0, a.length, key);
    }

    // The index of `key`, or `-(insertion point) - 1` if it is not there. The negative is not a
    // bare "I did not find it": it encodes WHERE it would have gone, which is what allows
    // inserting in order without searching again.
    public static int binarySearch(double[] a, int fromIndex, int toIndex, double key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = Double.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static int binarySearch(java.lang.Object[] a, java.lang.Object key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(java.lang.Object[] a, int fromIndex, int toIndex, java.lang.Object key) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = ((java.lang.Comparable) a[mid]).compareTo(key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, java.util.Comparator<? super T> c) {
        validRange(a.length, fromIndex, toIndex);
        int lo = fromIndex;
        int hi = toIndex - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = c == null ? ((java.lang.Comparable<? super T>) a[mid]).compareTo(key)
                                : c.compare(a[mid], key);
            if (cmp < 0) {
                lo = mid + 1;
            } else if (cmp > 0) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -(lo + 1);
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(byte[] a, byte val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(short[] a, short val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(short[] a, int fromIndex, int toIndex, short val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(long[] a, long val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(long[] a, int fromIndex, int toIndex, long val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(float[] a, float val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(float[] a, int fromIndex, int toIndex, float val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(double[] a, double val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(double[] a, int fromIndex, int toIndex, double val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(java.lang.Object[] a, int fromIndex, int toIndex, java.lang.Object val) {
        validRange(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static boolean[] copyOfRange(boolean[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        boolean[] out = new boolean[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static byte[] copyOf(byte[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] out = new byte[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static char[] copyOfRange(char[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        char[] out = new char[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static short[] copyOf(short[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static short[] copyOfRange(short[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        short[] out = new short[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static int[] copyOfRange(int[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        int[] out = new int[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static long[] copyOf(long[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static long[] copyOfRange(long[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        long[] out = new long[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static float[] copyOf(float[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static float[] copyOfRange(float[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        float[] out = new float[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static double[] copyOf(double[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    // The copy of [from, to). `to` MAY go past the original's length: what is left over is filled
    // with the default value. It is deliberate in the JDK -- it allows copying and growing at once.
    public static double[] copyOfRange(double[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        double[] out = new double[count];
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static boolean equals(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(byte[] a, byte[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length);
    }

    public static boolean equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Byte.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Character.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(short[] a, short[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length);
    }

    public static boolean equals(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Short.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Integer.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(long[] a, long[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length);
    }

    public static boolean equals(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Long.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(float[] a, float[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length);
    }

    public static boolean equals(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Float.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(double[] a, double[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length);
    }

    public static boolean equals(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (Double.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static boolean equals(java.lang.Object[] a, int aFromIndex, int aToIndex, java.lang.Object[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (!Objects.equals(a[aFromIndex + i], b[bFromIndex + i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static <T> boolean equals(T[] a, T[] a2, java.util.Comparator<? super T> cmp) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return equals(a, 0, a.length, a2, 0, a2.length, cmp);
    }

    public static <T> boolean equals(T[] a, int aFromIndex, int aToIndex, T[] b, int bFromIndex, int bToIndex, java.util.Comparator<? super T> cmp) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        if (aToIndex - aFromIndex != bToIndex - bFromIndex) {
            return false;
        }
        int i = 0;
        while (i < aToIndex - aFromIndex) {
            if (cmp.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(boolean[] a, boolean[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Byte.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // The same as `compare` but reading each element as UNSIGNED: for a `byte`, 0xFF is 255 and
    // not -1. It is what is needed when the array carries raw bytes.
    public static int compareUnsigned(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compareUnsigned(a, 0, a.length, b, 0, b.length);
    }

    public static int compareUnsigned(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Byte.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(char[] a, char[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Character.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(short[] a, short[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Short.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // The same as `compare` but reading each element as UNSIGNED: for a `byte`, 0xFF is 255 and
    // not -1. It is what is needed when the array carries raw bytes.
    public static int compareUnsigned(short[] a, short[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compareUnsigned(a, 0, a.length, b, 0, b.length);
    }

    public static int compareUnsigned(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Short.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    public static int compare(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Integer.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // The same as `compare` but reading each element as UNSIGNED: for a `byte`, 0xFF is 255 and
    // not -1. It is what is needed when the array carries raw bytes.
    public static int compareUnsigned(int[] a, int[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compareUnsigned(a, 0, a.length, b, 0, b.length);
    }

    public static int compareUnsigned(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Integer.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(long[] a, long[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Long.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // The same as `compare` but reading each element as UNSIGNED: for a `byte`, 0xFF is 255 and
    // not -1. It is what is needed when the array carries raw bytes.
    public static int compareUnsigned(long[] a, long[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compareUnsigned(a, 0, a.length, b, 0, b.length);
    }

    public static int compareUnsigned(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Long.compareUnsigned(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(float[] a, float[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Float.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // It compares the two arrays **lexicographically**: at the first index where they differ that
    // comparison wins; if one is a prefix of the other, the shorter wins. It is the same order a
    // dictionary uses, and that is why it serves for ordering arrays against each other.
    public static int compare(double[] a, double[] b) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length);
    }

    public static int compare(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = Double.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    public static <T extends java.lang.Comparable<? super T>> int compare(T[] a, int aFromIndex, int aToIndex, T[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = compareNatural(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    public static <T> int compare(T[] a, T[] b, java.util.Comparator<? super T> cmp) {
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }
        return compare(a, 0, a.length, b, 0, b.length, cmp);
    }

    public static <T> int compare(T[] a, int aFromIndex, int aToIndex, T[] b, int bFromIndex, int bToIndex, java.util.Comparator<? super T> cmp) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = cmp.compare(a[aFromIndex + i], b[bFromIndex + i]);
            if (c != 0) {
                return c;
            }
            i = i + 1;
        }
        return (aToIndex - aFromIndex) - (bToIndex - bFromIndex);
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(boolean[] a, boolean[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Boolean.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(byte[] a, byte[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Byte.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(char[] a, char[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Character.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(short[] a, short[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Short.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    public static int mismatch(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Integer.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(long[] a, long[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Long.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(float[] a, float[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Float.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    // The first index where the two arrays differ, or -1 if the common prefix exhausts both. When
    // one is a prefix of the other, it returns the shorter one's length -- that is, "they are equal
    // this far", which is useful information and not an error.
    public static int mismatch(double[] a, double[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (Double.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    public static int mismatch(java.lang.Object[] a, java.lang.Object[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(java.lang.Object[] a, int aFromIndex, int aToIndex, java.lang.Object[] b, int bFromIndex, int bToIndex) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (!Objects.equals(a[aFromIndex + i], b[bFromIndex + i])) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    public static <T> int mismatch(T[] a, int aFromIndex, int aToIndex, T[] b, int bFromIndex, int bToIndex, java.util.Comparator<? super T> cmp) {
        validRange(a.length, aFromIndex, aToIndex);
        validRange(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            if (cmp.compare(a[aFromIndex + i], b[bFromIndex + i]) != 0) {
                return i;
            }
            i = i + 1;
        }
        if ((aToIndex - aFromIndex) != (bToIndex - bFromIndex)) {
            return n;
        }
        return -1;
    }

    public static int hashCode(boolean[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Boolean.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static int hashCode(byte[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Byte.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String toString(byte[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(a[i]);
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    public static int hashCode(char[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Character.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static int hashCode(short[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Short.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String toString(short[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(a[i]);
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    public static int hashCode(long[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Long.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String toString(long[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(a[i]);
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    public static int hashCode(float[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Float.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String toString(float[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(a[i]);
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    public static int hashCode(double[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + Double.hashCode(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String toString(double[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(a[i]);
            i = i + 1;
        }
        b.append(']');
        return b.toString();
    }

    // Equality that **descends into the nested arrays**. `equals(Object[], Object[])` compares the
    // elements with their `equals`, and an array's `equals` is identity -- so two matrices with the
    // same content give `false` there and `true` here. That is the whole difference.
    public static boolean deepEquals(java.lang.Object[] a1, java.lang.Object[] a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null || a1.length != a2.length) {
            return false;
        }
        int i = 0;
        while (i < a1.length) {
            if (!deepEqual(a1[i], a2[i])) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public static int deepHashCode(java.lang.Object[] a) {
        if (a == null) {
            return 0;
        }
        int h = 1;
        int i = 0;
        while (i < a.length) {
            h = 31 * h + deepHashOf(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String deepToString(java.lang.Object[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        deepText(a, b);
        return b.toString();
    }

    public static java.util.stream.IntStream stream(int[] array) {
        // Inlined and not `stream(array, 0, array.length)`: with the four three-argument overloads
        // in play the resolution does not choose well, and the body is two lines.
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        int[] chunk = copyOfRange(array, 0, array.length);
        return IntStream.of(chunk);
    }

    public static java.util.stream.IntStream stream(int[] array, int startInclusive, int endExclusive) {
        validRange(array.length, startInclusive, endExclusive);
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        int[] chunk = copyOfRange(array, startInclusive, endExclusive);
        return IntStream.of(chunk);
    }

    public static java.util.stream.LongStream stream(long[] array) {
        // Inlined and not `stream(array, 0, array.length)`: with the four three-argument overloads
        // in play the resolution does not choose well, and the body is two lines.
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        long[] chunk = copyOfRange(array, 0, array.length);
        return LongStream.of(chunk);
    }

    public static java.util.stream.LongStream stream(long[] array, int startInclusive, int endExclusive) {
        validRange(array.length, startInclusive, endExclusive);
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        long[] chunk = copyOfRange(array, startInclusive, endExclusive);
        return LongStream.of(chunk);
    }

    public static java.util.stream.DoubleStream stream(double[] array) {
        // Inlined and not `stream(array, 0, array.length)`: with the four three-argument overloads
        // in play the resolution does not choose well, and the body is two lines.
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        double[] chunk = copyOfRange(array, 0, array.length);
        return DoubleStream.of(chunk);
    }

    public static java.util.stream.DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        validRange(array.length, startInclusive, endExclusive);
        // A typed local in between: the nested call as an argument does not resolve
        // (#285), and the target is varargs besides.
        double[] chunk = copyOfRange(array, startInclusive, endExclusive);
        return DoubleStream.of(chunk);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] array) {
        // Without delegating to `stream(array, 0, array.length)`: among the four three-argument
        // overloads the resolution does not choose the generic one, and the whole body is two lines.
        Object[] chunk = copyOfRange((Object[]) array, 0, array.length);
        return (Stream<T>) Stream.of(chunk);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        validRange(array.length, startInclusive, endExclusive);
        return (java.util.stream.Stream<T>) Stream.of(
            copyOfRange((java.lang.Object[]) array, startInclusive, endExclusive));
    }

    public static void setAll(int[] array, java.util.function.IntUnaryOperator generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsInt(i);
            i = i + 1;
        }
    }

    public static void parallelSetAll(int[] array, java.util.function.IntUnaryOperator generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsInt(i);
            i = i + 1;
        }
    }

    public static void setAll(long[] array, java.util.function.IntToLongFunction generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsLong(i);
            i = i + 1;
        }
    }

    public static void parallelSetAll(long[] array, java.util.function.IntToLongFunction generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsLong(i);
            i = i + 1;
        }
    }

    public static void setAll(double[] array, java.util.function.IntToDoubleFunction generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsDouble(i);
            i = i + 1;
        }
    }

    public static void parallelSetAll(double[] array, java.util.function.IntToDoubleFunction generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.applyAsDouble(i);
            i = i + 1;
        }
    }

    public static <T> void setAll(T[] array, java.util.function.IntFunction<? extends T> generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.apply(i);
            i = i + 1;
        }
    }

    public static <T> void parallelSetAll(T[] array, java.util.function.IntFunction<? extends T> generator) {
        int i = 0;
        while (i < array.length) {
            array[i] = generator.apply(i);
            i = i + 1;
        }
    }

    // It replaces each element by the accumulation of all the previous ones and itself: for the
    // sum, `[1,2,3]` becomes `[1,3,6]`. It is the prefix sum, and it serves to go from "how much is
    // in each cell" to "how much there is up to here" in a single pass.
    //
    // A KajiLibrary subset: sequential. The JDK's version splits the array and does it in two
    // parallel sweeps; the result is identical.
    public static void parallelPrefix(int[] array, java.util.function.IntBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(int[] array, int fromIndex, int toIndex, java.util.function.IntBinaryOperator op) {
        validRange(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.applyAsInt(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // It replaces each element by the accumulation of all the previous ones and itself: for the
    // sum, `[1,2,3]` becomes `[1,3,6]`. It is the prefix sum, and it serves to go from "how much is
    // in each cell" to "how much there is up to here" in a single pass.
    //
    // A KajiLibrary subset: sequential. The JDK's version splits the array and does it in two
    // parallel sweeps; the result is identical.
    public static void parallelPrefix(long[] array, java.util.function.LongBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(long[] array, int fromIndex, int toIndex, java.util.function.LongBinaryOperator op) {
        validRange(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.applyAsLong(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // It replaces each element by the accumulation of all the previous ones and itself: for the
    // sum, `[1,2,3]` becomes `[1,3,6]`. It is the prefix sum, and it serves to go from "how much is
    // in each cell" to "how much there is up to here" in a single pass.
    //
    // A KajiLibrary subset: sequential. The JDK's version splits the array and does it in two
    // parallel sweeps; the result is identical.
    public static void parallelPrefix(double[] array, java.util.function.DoubleBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(double[] array, int fromIndex, int toIndex, java.util.function.DoubleBinaryOperator op) {
        validRange(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.applyAsDouble(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    public static <T> void parallelPrefix(T[] array, java.util.function.BinaryOperator<T> op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static <T> void parallelPrefix(T[] array, int fromIndex, int toIndex, java.util.function.BinaryOperator<T> op) {
        validRange(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.apply(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // The list of those elements, as a fixed-size **view** over the array itself: `set` writes
    // through to `a`, and a write to `a` shows up in the list.
    //
    // It used to return a copy, with a note saying a inner view needed a class of its own and would
    // be written when something needed it. `ArrayAsList` is that class. `add` and `remove` still
    // refuse either way -- an array's length is fixed.
    public static <T> java.util.List<T> asList(T... a) {
        return new ArrayAsList<T>((Object[]) a);
    }

    // The same as `sort`. A KajiLibrary subset: sequential.
    public static <T extends java.lang.Comparable<? super T>> void parallelSort(T[] a) {
        // The typed local is #279's way round: with the argument as `T[]` the call is declared
        // ambiguous; naming `Object[]` picks correctly.
        Object[] widenedArray = a;
        sort(widenedArray, 0, widenedArray.length);
    }

    public static <T extends java.lang.Comparable<? super T>> void parallelSort(T[] a, int fromIndex, int toIndex) {
        Object[] widenedArray = a;
        sort(widenedArray, fromIndex, toIndex);
    }

    /**
     * A copy of `original` of length `newLength`, of the SAME dynamic type.
     *
     * <p>That "same dynamic type" is the whole point: whoever copies a `String[]` expects a
     * `String[]` back, not an `Object[]` that blows up with ArrayStoreException at the first store.
     * The type is only known at run time, so reflection is needed -- it is the same reason
     * `Collection.toArray(T[])` needs it.
     */
    public static <T> T[] copyOf(T[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return (T[]) copyOfRange(original, from, to, original.getClass());
    }

    /** A copy of length `newLength`, of whatever array type `newType` asks for. */
    public static <T, U> T[] copyOf(U[] original, int newLength, java.lang.Class<? extends T[]> newType) {
        return copyOfRange(original, 0, newLength, newType);
    }

    public static <T, U> T[] copyOfRange(U[] original, int from, int to, java.lang.Class<? extends T[]> newType) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int count = to - from;
        if (count < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        java.lang.Object[] out = (java.lang.Object[]) Array.newInstance(
            newType.getComponentType(), count);
        int n = Math.min(original.length - from, count);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return (T[]) out;
    }

    // ---- internal helpers -----------------------------------------------------------------------

    /**
     * It validates a range [from, to) against a length, with the JDK's same three errors.
     *
     * <p>They are three and not one because they say different things: `from > to` is a range the
     * wrong way round (the caller's fault in computing it), and going outside [0, length] is an index
     * off the end of the array. Collapsing them into one message would lose which of the two
     * happened.
     */
    private static void validRange(int length, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > length) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    /** An element's `compareTo`, for the natural-order `compare`s. */
    private static int compareNatural(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
    }

    // Merge sort over [lo, hi) of a boolean[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortBoolean(boolean[] a, boolean[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortBoolean(a, scratch, lo, mid);
        mergeSortBoolean(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Boolean.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a byte[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortByte(byte[] a, byte[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortByte(a, scratch, lo, mid);
        mergeSortByte(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Byte.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a char[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortCharacter(char[] a, char[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortCharacter(a, scratch, lo, mid);
        mergeSortCharacter(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Character.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a short[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortShort(short[] a, short[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortShort(a, scratch, lo, mid);
        mergeSortShort(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Short.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a int[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortInteger(int[] a, int[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortInteger(a, scratch, lo, mid);
        mergeSortInteger(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Integer.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a long[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortLong(long[] a, long[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortLong(a, scratch, lo, mid);
        mergeSortLong(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Long.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a float[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortFloat(float[] a, float[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortFloat(a, scratch, lo, mid);
        mergeSortFloat(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Float.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // Merge sort over [lo, hi) of a double[]. Stable and guaranteed O(n log n), which is what
    // is needed: the JDK's dual-pivot quicksort is faster in the average case but degrades to
    // O(n²) on adversarial input, and here simplicity is worth more than that margin.
    private static void mergeSortDouble(double[] a, double[] scratch, int lo, int hi) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortDouble(a, scratch, lo, mid);
        mergeSortDouble(a, scratch, mid, hi);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            if (Double.compare(a[j], a[i]) < 0) {
                scratch[k] = a[j];
                j = j + 1;
            } else {
                scratch[k] = a[i];
                i = i + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    /**
     * Merge sort over objects, with `cmp` or by natural order if it is null.
     *
     * <p>**Stability** is not a detail here: `Arrays.sort(Object[])`'s contract demands it, and it is
     * what allows sorting by one criterion and then by another without losing the first. The
     * comparison's `<= 0` guarantees it: on a tie the one from the left half wins, and that is the
     * one that came before.
     */
    private static void mergeSortObj(Object[] a, Object[] scratch, int lo, int hi, Comparator cmp) {
        if (hi - lo < 2) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortObj(a, scratch, lo, mid, cmp);
        mergeSortObj(a, scratch, mid, hi, cmp);
        int i = lo;
        int j = mid;
        int k = 0;
        while (i < mid && j < hi) {
            int c = cmp == null ? compareNatural(a[i], a[j]) : cmp.compare(a[i], a[j]);
            if (c <= 0) {
                scratch[k] = a[i];
                i = i + 1;
            } else {
                scratch[k] = a[j];
                j = j + 1;
            }
            k = k + 1;
        }
        while (i < mid) {
            scratch[k] = a[i];
            i = i + 1;
            k = k + 1;
        }
        while (j < hi) {
            scratch[k] = a[j];
            j = j + 1;
            k = k + 1;
        }
        int m = 0;
        while (m < k) {
            a[lo + m] = scratch[m];
            m = m + 1;
        }
    }

    // ---- the "deep" trio: it descends into the nested arrays -----------------------------------
    //
    // All three dispatch on the element's dynamic type because in Java there is no other way: an
    // `Object` that turns out to be an `int[]` shares nothing with one that turns out to be an
    // `Object[]`, and both of their `equals` is identity. Without this dispatch, two equal matrices
    // would compare different.

    private static boolean deepEqual(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return deepEquals((Object[]) a, (Object[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[]) {
            return equals((long[]) a, (long[]) b);
        }
        if (a instanceof double[] && b instanceof double[]) {
            return equals((double[]) a, (double[]) b);
        }
        if (a instanceof float[] && b instanceof float[]) {
            return equals((float[]) a, (float[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return equals((char[]) a, (char[]) b);
        }
        if (a instanceof short[] && b instanceof short[]) {
            return equals((short[]) a, (short[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[]) {
            return equals((boolean[]) a, (boolean[]) b);
        }
        return a.equals(b);
    }

    private static int deepHashOf(Object e) {
        if (e == null) {
            return 0;
        }
        if (e instanceof Object[]) {
            return deepHashCode((Object[]) e);
        }
        if (e instanceof int[]) {
            return hashCode((int[]) e);
        }
        if (e instanceof long[]) {
            return hashCode((long[]) e);
        }
        if (e instanceof double[]) {
            return hashCode((double[]) e);
        }
        if (e instanceof float[]) {
            return hashCode((float[]) e);
        }
        if (e instanceof char[]) {
            return hashCode((char[]) e);
        }
        if (e instanceof short[]) {
            return hashCode((short[]) e);
        }
        if (e instanceof byte[]) {
            return hashCode((byte[]) e);
        }
        if (e instanceof boolean[]) {
            return hashCode((boolean[]) e);
        }
        return e.hashCode();
    }

    private static void deepText(Object[] a, StringBuilder b) {
        b.append('[');
        int i = 0;
        while (i < a.length) {
            if (i > 0) {
                b.append(", ");
            }
            Object e = a[i];
            if (e == null) {
                b.append("null");
            } else if (e instanceof Object[]) {
                deepText((Object[]) e, b);
            } else if (e instanceof int[]) {
                b.append(toString((int[]) e));
            } else if (e instanceof long[]) {
                b.append(toString((long[]) e));
            } else if (e instanceof double[]) {
                b.append(toString((double[]) e));
            } else if (e instanceof float[]) {
                b.append(toString((float[]) e));
            } else if (e instanceof char[]) {
                b.append(toString((char[]) e));
            } else if (e instanceof short[]) {
                b.append(toString((short[]) e));
            } else if (e instanceof byte[]) {
                b.append(toString((byte[]) e));
            } else if (e instanceof boolean[]) {
                b.append(toString((boolean[]) e));
            } else {
                b.append(e.toString());
            }
            i = i + 1;
        }
        b.append(']');
    }
}
