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
        return binarySearch(a, 0, a.length, key);
    }

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un "no lo
    // encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar manteniendo el
    // orden sin buscar de nuevo.
    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        rangoValido(a.length, fromIndex, toIndex);
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



    // ---- comparacion elemento a elemento -----------------------------------------------------
    //
    // Todo lo que sigue —sort, binarySearch, compare, mismatch— se apoya en el `compare` del
    // wrapper y no en `<`. Para los enteros da lo mismo; para `float` y `double` NO: `<` dice que
    // NaN no es menor ni mayor que nada y que -0.0 == 0.0, y con eso un sort no termina de ordenar
    // y un binarySearch se pierde. `Double.compare` define el orden total que la especificacion de
    // `Arrays` exige: -0.0 antes que 0.0, y NaN al final.

    // Ordena `a` entero, ascendente.
    public static void sort(byte[] a) {
        sort(a, 0, a.length);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(byte[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        byte[] scratch = new byte[toIndex - fromIndex];
        mergeSortByte(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(byte[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(byte[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(char[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        char[] scratch = new char[toIndex - fromIndex];
        mergeSortCharacter(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(char[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(char[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena `a` entero, ascendente.
    public static void sort(short[] a) {
        sort(a, 0, a.length);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(short[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        short[] scratch = new short[toIndex - fromIndex];
        mergeSortShort(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(short[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(short[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(int[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        int[] scratch = new int[toIndex - fromIndex];
        mergeSortInteger(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(int[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(int[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena `a` entero, ascendente.
    public static void sort(long[] a) {
        sort(a, 0, a.length);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(long[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        long[] scratch = new long[toIndex - fromIndex];
        mergeSortLong(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(long[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(long[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena `a` entero, ascendente.
    public static void sort(float[] a) {
        sort(a, 0, a.length);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(float[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        float[] scratch = new float[toIndex - fromIndex];
        mergeSortFloat(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(float[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(float[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena `a` entero, ascendente.
    public static void sort(double[] a) {
        sort(a, 0, a.length);
    }

    // Ordena [fromIndex, toIndex) de `a`, ascendente.
    public static void sort(double[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        double[] scratch = new double[toIndex - fromIndex];
        mergeSortDouble(a, scratch, fromIndex, toIndex);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial, sin fork/join.
    public static void parallelSort(double[] a) {
        sort(a, 0, a.length);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static void parallelSort(double[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex);
    }

    // Ordena por el orden natural de los elementos.
    public static void sort(java.lang.Object[] a) {
        sort(a, 0, a.length);
    }

    public static void sort(java.lang.Object[] a, int fromIndex, int toIndex) {
        rangoValido(a.length, fromIndex, toIndex);
        java.lang.Object[] scratch = new java.lang.Object[toIndex - fromIndex];
        mergeSortObj(a, scratch, fromIndex, toIndex, null);
    }

    public static <T> void sort(T[] a, java.util.Comparator<? super T> c) {
        sort(a, 0, a.length, c);
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, java.util.Comparator<? super T> c) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(char[] a, int fromIndex, int toIndex, char key) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(short[] a, int fromIndex, int toIndex, short key) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(long[] a, int fromIndex, int toIndex, long key) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(float[] a, int fromIndex, int toIndex, float key) {
        rangoValido(a.length, fromIndex, toIndex);
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

    // El indice de `key`, o `-(punto de insercion) - 1` si no esta. El negativo no es un
    // "no lo encontre" a secas: codifica DONDE habria ido, que es lo que permite insertar
    // manteniendo el orden sin buscar de nuevo.
    public static int binarySearch(double[] a, int fromIndex, int toIndex, double key) {
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
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
        rangoValido(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    public static void fill(java.lang.Object[] a, int fromIndex, int toIndex, java.lang.Object val) {
        rangoValido(a.length, fromIndex, toIndex);
        int i = fromIndex;
        while (i < toIndex) {
            a[i] = val;
            i = i + 1;
        }
    }

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static boolean[] copyOfRange(boolean[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        boolean[] out = new boolean[largo];
        int n = Math.min(original.length - from, largo);
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

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static byte[] copyOfRange(byte[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        byte[] out = new byte[largo];
        int n = Math.min(original.length - from, largo);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static char[] copyOfRange(char[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        char[] out = new char[largo];
        int n = Math.min(original.length - from, largo);
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

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static short[] copyOfRange(short[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        short[] out = new short[largo];
        int n = Math.min(original.length - from, largo);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static int[] copyOfRange(int[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        int[] out = new int[largo];
        int n = Math.min(original.length - from, largo);
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

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static long[] copyOfRange(long[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        long[] out = new long[largo];
        int n = Math.min(original.length - from, largo);
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

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static float[] copyOfRange(float[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        float[] out = new float[largo];
        int n = Math.min(original.length - from, largo);
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

    // La copia de [from, to). `to` PUEDE pasarse del largo del original: lo que sobra queda
    // en el valor por defecto. Es deliberado en el JDK — permite copiar y agrandar de una.
    public static double[] copyOfRange(double[] original, int from, int to) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        double[] out = new double[largo];
        int n = Math.min(original.length - from, largo);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return out;
    }

    public static boolean equals(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Igual que `compare` pero leyendo cada elemento como SIN signo: para un `byte`,
    // 0xFF vale 255 y no -1. Es lo que hace falta cuando el arreglo lleva bytes crudos.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Igual que `compare` pero leyendo cada elemento como SIN signo: para un `byte`,
    // 0xFF vale 255 y no -1. Es lo que hace falta cuando el arreglo lleva bytes crudos.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Igual que `compare` pero leyendo cada elemento como SIN signo: para un `byte`,
    // 0xFF vale 255 y no -1. Es lo que hace falta cuando el arreglo lleva bytes crudos.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Igual que `compare` pero leyendo cada elemento como SIN signo: para un `byte`,
    // 0xFF vale 255 y no -1. Es lo que hace falta cuando el arreglo lleva bytes crudos.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Compara los dos arreglos **lexicograficamente**: en el primer indice donde difieren
    // gana esa comparacion; si uno es prefijo del otro, gana el mas corto. Es el mismo
    // orden que usa un diccionario, y por eso sirve para ordenar arreglos entre si.
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
        int n = Math.min(aToIndex - aFromIndex, bToIndex - bFromIndex);
        int i = 0;
        while (i < n) {
            int c = compararNatural(a[aFromIndex + i], b[bFromIndex + i]);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(boolean[] a, boolean[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(byte[] a, byte[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(char[] a, char[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(short[] a, short[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(long[] a, long[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(float[] a, float[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // El primer indice donde los dos arreglos difieren, o -1 si el prefijo comun los agota
    // a los dos. Cuando uno es prefijo del otro, devuelve el largo del mas corto — o sea
    // "hasta aca son iguales", que es informacion util y no un error.
    public static int mismatch(double[] a, double[] b) {
        return mismatch(a, 0, a.length, b, 0, b.length);
    }

    public static int mismatch(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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
        rangoValido(a.length, aFromIndex, aToIndex);
        rangoValido(b.length, bFromIndex, bToIndex);
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

    // Igualdad que **baja por los arreglos anidados**. `equals(Object[], Object[])` compara los
    // elementos con su `equals`, y el `equals` de un arreglo es identidad — asi que dos matrices
    // con el mismo contenido dan `false` alli y `true` aca. Esa es toda la diferencia.
    public static boolean deepEquals(java.lang.Object[] a1, java.lang.Object[] a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null || a1.length != a2.length) {
            return false;
        }
        int i = 0;
        while (i < a1.length) {
            if (!hondoIguales(a1[i], a2[i])) {
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
            h = 31 * h + hondoHash(a[i]);
            i = i + 1;
        }
        return h;
    }

    public static java.lang.String deepToString(java.lang.Object[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        hondoTexto(a, b);
        return b.toString();
    }

    public static java.util.stream.IntStream stream(int[] array) {
        // Inline y no `stream(array, 0, array.length)`: con las cuatro sobrecargas de tres
        // argumentos en juego la resolucion no elige bien, y el cuerpo son dos lineas.
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        int[] trozo = copyOfRange(array, 0, array.length);
        return IntStream.of(trozo);
    }

    public static java.util.stream.IntStream stream(int[] array, int startInclusive, int endExclusive) {
        rangoValido(array.length, startInclusive, endExclusive);
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        int[] trozo = copyOfRange(array, startInclusive, endExclusive);
        return IntStream.of(trozo);
    }

    public static java.util.stream.LongStream stream(long[] array) {
        // Inline y no `stream(array, 0, array.length)`: con las cuatro sobrecargas de tres
        // argumentos en juego la resolucion no elige bien, y el cuerpo son dos lineas.
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        long[] trozo = copyOfRange(array, 0, array.length);
        return LongStream.of(trozo);
    }

    public static java.util.stream.LongStream stream(long[] array, int startInclusive, int endExclusive) {
        rangoValido(array.length, startInclusive, endExclusive);
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        long[] trozo = copyOfRange(array, startInclusive, endExclusive);
        return LongStream.of(trozo);
    }

    public static java.util.stream.DoubleStream stream(double[] array) {
        // Inline y no `stream(array, 0, array.length)`: con las cuatro sobrecargas de tres
        // argumentos en juego la resolucion no elige bien, y el cuerpo son dos lineas.
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        double[] trozo = copyOfRange(array, 0, array.length);
        return DoubleStream.of(trozo);
    }

    public static java.util.stream.DoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        rangoValido(array.length, startInclusive, endExclusive);
        // Local tipado en medio: la llamada anidada como argumento no resuelve
        // (#285), y ademas el destino es varargs.
        double[] trozo = copyOfRange(array, startInclusive, endExclusive);
        return DoubleStream.of(trozo);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] array) {
        // Sin delegar en `stream(array, 0, array.length)`: entre las cuatro sobrecargas de tres
        // argumentos la resolucion no elige la generica, y el cuerpo entero son dos lineas.
        Object[] trozo = copyOfRange((Object[]) array, 0, array.length);
        return (Stream<T>) Stream.of(trozo);
    }

    public static <T> java.util.stream.Stream<T> stream(T[] array, int startInclusive, int endExclusive) {
        rangoValido(array.length, startInclusive, endExclusive);
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

    // Reemplaza cada elemento por la acumulacion de todos los anteriores y el: para la suma,
    // `[1,2,3]` queda `[1,3,6]`. Es la suma prefija, y sirve para pasar de "cuanto hay en
    // cada casilla" a "cuanto hay hasta aca" en una sola pasada.
    //
    // A KajiLibrary subset: secuencial. La version del JDK divide el arreglo y lo hace en
    // dos barridas paralelas; el resultado es identico.
    public static void parallelPrefix(int[] array, java.util.function.IntBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(int[] array, int fromIndex, int toIndex, java.util.function.IntBinaryOperator op) {
        rangoValido(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.applyAsInt(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // Reemplaza cada elemento por la acumulacion de todos los anteriores y el: para la suma,
    // `[1,2,3]` queda `[1,3,6]`. Es la suma prefija, y sirve para pasar de "cuanto hay en
    // cada casilla" a "cuanto hay hasta aca" en una sola pasada.
    //
    // A KajiLibrary subset: secuencial. La version del JDK divide el arreglo y lo hace en
    // dos barridas paralelas; el resultado es identico.
    public static void parallelPrefix(long[] array, java.util.function.LongBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(long[] array, int fromIndex, int toIndex, java.util.function.LongBinaryOperator op) {
        rangoValido(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.applyAsLong(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // Reemplaza cada elemento por la acumulacion de todos los anteriores y el: para la suma,
    // `[1,2,3]` queda `[1,3,6]`. Es la suma prefija, y sirve para pasar de "cuanto hay en
    // cada casilla" a "cuanto hay hasta aca" en una sola pasada.
    //
    // A KajiLibrary subset: secuencial. La version del JDK divide el arreglo y lo hace en
    // dos barridas paralelas; el resultado es identico.
    public static void parallelPrefix(double[] array, java.util.function.DoubleBinaryOperator op) {
        parallelPrefix(array, 0, array.length, op);
    }

    public static void parallelPrefix(double[] array, int fromIndex, int toIndex, java.util.function.DoubleBinaryOperator op) {
        rangoValido(array.length, fromIndex, toIndex);
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
        rangoValido(array.length, fromIndex, toIndex);
        int i = fromIndex + 1;
        while (i < toIndex) {
            array[i] = op.apply(array[i - 1], array[i]);
            i = i + 1;
        }
    }

    // La lista de esos elementos.
    //
    // **Divergencia deliberada**: la del JDK es una VISTA sobre el arreglo —escribir con `set`
    // escribe en el arreglo— y aca es una copia inmutable. La vista pide una clase propia que
    // delegue de vuelta; cuando algo la necesite de verdad, se cambia. Lo que NO cambia es que
    // las dos rechazan `add` y `remove`: el largo es fijo en ambas.
    public static <T> java.util.List<T> asList(T... a) {
        Object[] copia = copyOfRange((Object[]) a, 0, a.length);
        return new FixedList<T>(copia);
    }

    // Igual que `sort`. A KajiLibrary subset: secuencial.
    public static <T extends java.lang.Comparable<? super T>> void parallelSort(T[] a) {
        // El local tipado es el rodeo de #279: con el argumento como `T[]` la llamada se declara
        // ambigua; nombrando `Object[]` elige bien.
        Object[] ensanchado = a;
        sort(ensanchado, 0, ensanchado.length);
    }

    public static <T extends java.lang.Comparable<? super T>> void parallelSort(T[] a, int fromIndex, int toIndex) {
        Object[] ensanchado = a;
        sort(ensanchado, fromIndex, toIndex);
    }

    /**
     * Una copia de `original` con largo `newLength`, del MISMO tipo dinamico.
     *
     * <p>Ese "mismo tipo dinamico" es todo el punto: quien copia un `String[]` espera un
     * `String[]` de vuelta, no un `Object[]` que reviente con ArrayStoreException al primer
     * guardado. El tipo solo se conoce en runtime, asi que hace falta reflexion — es la misma
     * razon por la que `Collection.toArray(T[])` la necesita.
     */
    public static <T> T[] copyOf(T[] original, int newLength) {
        return copyOfRange(original, 0, newLength);
    }

    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        return (T[]) copyOfRange(original, from, to, original.getClass());
    }

    /** Una copia con largo `newLength`, del tipo de arreglo que pida `newType`. */
    public static <T, U> T[] copyOf(U[] original, int newLength, java.lang.Class<? extends T[]> newType) {
        return copyOfRange(original, 0, newLength, newType);
    }

    public static <T, U> T[] copyOfRange(U[] original, int from, int to, java.lang.Class<? extends T[]> newType) {
        if (from < 0 || from > original.length) {
            throw new ArrayIndexOutOfBoundsException(from);
        }
        int largo = to - from;
        if (largo < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        java.lang.Object[] out = (java.lang.Object[]) Array.newInstance(
            newType.getComponentType(), largo);
        int n = Math.min(original.length - from, largo);
        int i = 0;
        while (i < n) {
            out[i] = original[from + i];
            i = i + 1;
        }
        return (T[]) out;
    }

    // ---- helpers internos ---------------------------------------------------------------------

    /**
     * Valida un rango [from, to) contra un largo, con los mismos tres errores que el JDK.
     *
     * <p>Son tres y no uno porque dicen cosas distintas: `from > to` es un rango dado vuelta
     * (culpa del llamador al calcularlo), y salirse de [0, length] es un indice fuera del arreglo.
     * Colapsarlos en un solo mensaje haria perder cual de los dos pasó.
     */
    private static void rangoValido(int length, int fromIndex, int toIndex) {
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

    /** El `compareTo` de un elemento, para los `compare` de orden natural. */
    private static int compararNatural(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
    }

    // Merge sort sobre [lo, hi) de un boolean[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un byte[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un char[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un short[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un int[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un long[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un float[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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

    // Merge sort sobre [lo, hi) de un double[]. Estable y O(n log n) garantizado, que es lo que
    // hace falta: el quicksort de dos pivotes del JDK es mas rapido en el caso medio pero se
    // degrada a O(n²) con entradas adversarias, y aca la simplicidad vale mas que ese margen.
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
     * Merge sort sobre objetos, con `cmp` o por orden natural si es null.
     *
     * <p>La **estabilidad** no es un detalle aca: el contrato de `Arrays.sort(Object[])` la exige,
     * y es lo que permite ordenar por un criterio y despues por otro sin perder el primero. La
     * garantiza el `<= 0` de la comparacion: ante un empate gana el de la mitad izquierda, que es
     * el que venia antes.
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
            int c = cmp == null ? compararNatural(a[i], a[j]) : cmp.compare(a[i], a[j]);
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

    // ---- el trio "hondo": baja por los arreglos anidados ---------------------------------------
    //
    // Los tres despachan sobre el tipo dinamico del elemento porque en Java no hay otra forma: un
    // `Object` que resulta ser `int[]` no comparte nada con uno que resulta ser `Object[]`, y el
    // `equals` de los dos es identidad. Sin este despacho, dos matrices iguales darian distinto.

    private static boolean hondoIguales(Object a, Object b) {
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

    private static int hondoHash(Object e) {
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

    private static void hondoTexto(Object[] a, StringBuilder b) {
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
                hondoTexto((Object[]) e, b);
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
