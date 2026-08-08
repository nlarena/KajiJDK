package java.util;

// KajiLibrary's java.util.Objects — null-safe static helpers used all over real code:
// equality/hashCode/toString that tolerate null, and the requireNonNull guards that turn a
// silent NPE-later into a loud NPE-here. Non-instantiable, like the JDK's. A KajiLibrary
// subset (the JDK also has compare/hash(varargs)/requireNonNullElse/…).
public final class Objects {

    private Objects() {}

    // Equal if both null, or a.equals(b). Null-safe (a.equals is only called when a != null).
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.equals(b);
    }

    // The object's hashCode, or 0 for null.
    public static int hashCode(Object o) {
        if (o == null) {
            return 0;
        }
        return o.hashCode();
    }

    // The object's toString, or "null".
    public static String toString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString();
    }

    // The object's toString, or `nullDefault` for null.
    public static String toString(Object o, String nullDefault) {
        if (o == null) {
            return nullDefault;
        }
        return o.toString();
    }

    // Return `obj` if non-null, else throw NullPointerException. The standard argument guard.
    public static <T> T requireNonNull(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }
}
