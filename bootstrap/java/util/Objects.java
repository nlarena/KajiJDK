package java.util;

// Minimal java.util.Objects — the null-check + value-op helpers. `requireNonNull` also backs the
// synthetic null-check javac emits for a non-static inner class's captured enclosing instance, so
// having it here lets ordinary inner/anonymous classes load.
public final class Objects {
    private Objects() {
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static boolean equals(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }
}
