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
}
