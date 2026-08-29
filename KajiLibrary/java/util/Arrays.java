package java.util;

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

    public static Object[] copyOf(Object[] original, int newLength) {
        Object[] copy = new Object[newLength];
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
    // ORDERED e IMMUTABLE, y la segunda es la que sorprende: el array se puede modificar, y lo
    // que la caracteristica promete es que **el spliterator no lo va a hacer**. Es una promesa
    // sobre el recorrido, no sobre el dato.

    /**
     * A spliterator over all of {@code array}.
     *
     * @param array what to traverse
     */
    public static <T> Spliterator<T> spliterator(T[] array) {
        // El local no cambia nada -- un `T[]` ES un `Object[]` -- y esta porque nuestro javac
        // declara ambigua la llamada cuando el argumento es un arreglo de una VARIABLE de tipo.
        // Con `String[]` elige bien; con `T[]` no elige (#279). Nombrar el tipo del parametro es
        // justamente lo que no dedujo.
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
        int length = a.length;
        int middle = length / 2;
        int middleValue = a[middle];
        if (middleValue == key) {
            return middleValue;
        }
        if (middleValue < key) {
            return Arrays.binarySearch(a, 0, middle, key);
        }
        else {
            return Arrays.binarySearch(a, middle, length, key);
        }
    }

    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        return 0;
    }

    public static <T> int binarySearch(T[] a, T key, Comparator<? super T> c) {
        return 0;
    }


    public static int mismatch(int[] a, int[] b) {
        return 0;
    }

    public static <T> int mismatch(T[] a, T[] b, Comparator<? super T> cmp) {
        return 0;
    }

    public static int compare(int[] a, int[] b) {
        return 0;
    }

    public static <T extends Comparable<? super T>> int compare(T[] a, T[] b) {
        return 0;
    }


}
