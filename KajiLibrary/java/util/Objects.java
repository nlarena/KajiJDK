package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Arrays;
import java.util.Comparator;

// java.util.Objects — the static helpers that tolerate null: equality, hash and toString that do not
// blow up on an empty reference, and the `requireNonNull` guards that turn a silent, distant NPE into
// a noisy one at the spot. It is not instantiated.
//
// The other half, the one that closes the contract, is the six `check*`: the index and range checking
// each class used to write by hand. They do not look like much until one sees that the reason they
// exist is that **overflow makes them hard to write correctly**: the obvious `from + size <= length`
// gives a false positive when `from + size` overflows, and that is why the version here compares the
// other way round. They have been in the JDK since 9 precisely because everybody wrote them
// differently and some wrote them wrong.
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

    // The two arguments compared by `c`, or 0 if they are the SAME object.
    //
    // The identity shortcut is what allows `null, null` to be passed: it never reaches the comparator.
    // With two different references, on the other hand, tolerating null is `c`'s responsibility.
    public static <T> int compare(T a, T b, Comparator<? super T> c) {
        if (a == b) {
            return 0;
        }
        return c.compare(a, b);
    }

    // **Deep** equality: if both are arrays, they are compared element by element (and if those
    // elements are arrays, it goes down another level). For anything else it is `equals`.
    //
    // It exists because `equals` over an array is identity: `new int[]{1}.equals(new int[]{1})` gives
    // false, and there is no way of fixing that from `Object`.
    public static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Object[] && b instanceof Object[]) {
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }
        if (a instanceof byte[] && b instanceof byte[]) {
            return Arrays.equals((byte[]) a, (byte[]) b);
        }
        if (a instanceof short[] && b instanceof short[]) {
            return Arrays.equals((short[]) a, (short[]) b);
        }
        if (a instanceof int[] && b instanceof int[]) {
            return Arrays.equals((int[]) a, (int[]) b);
        }
        if (a instanceof long[] && b instanceof long[]) {
            return Arrays.equals((long[]) a, (long[]) b);
        }
        if (a instanceof char[] && b instanceof char[]) {
            return Arrays.equals((char[]) a, (char[]) b);
        }
        if (a instanceof float[] && b instanceof float[]) {
            return Arrays.equals((float[]) a, (float[]) b);
        }
        if (a instanceof double[] && b instanceof double[]) {
            return Arrays.equals((double[]) a, (double[]) b);
        }
        if (a instanceof boolean[] && b instanceof boolean[]) {
            return Arrays.equals((boolean[]) a, (boolean[]) b);
        }
        return a.equals(b);
    }

    // The hash of a **sequence** of values, with the formula List's contract fixes.
    //
    // Mind the single-argument case: `hash(x)` is NOT `hashCode(x)`, because the varargs builds an
    // array of one and applies the `31 * 1 + h` to it all the same. It is a known JDK trap and it is
    // replicated as it stands, because the number is part of the contract.
    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    // The representation `Object.toString` gives by default, even if the class has overridden it:
    // class and **identity** hash.
    //
    // It serves what its name says and not what it looks like: when the object's own `toString` lies,
    // or costs a lot, or recurses, this one says who the object is without asking it.
    public static String toIdentityString(Object o) {
        requireNonNull(o);
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    // `requireNonNull` with the message **deferred**: it is only built if there is something to
    // throw.
    //
    // That is the whole difference from the String version, and it is the point: a guard that passes
    // 99.99% of the time has no reason to pay for the message's concatenation.
    public static <T> T requireNonNull(T obj, java.util.function.Supplier<String> messageSupplier) {
        if (obj == null) {
            String m = null;
            if (messageSupplier != null) {
                m = messageSupplier.get();
            }
            throw new NullPointerException(m);
        }
        return obj;
    }

    // The first if it is not null, otherwise the second -- which **does** have to be non-null.
    public static <T> T requireNonNullElse(T obj, T defaultObj) {
        if (obj != null) {
            return obj;
        }
        return requireNonNull(defaultObj, "defaultObj");
    }

    // The same, with the replacement deferred.
    public static <T> T requireNonNullElseGet(T obj,
            java.util.function.Supplier<? extends T> supplier) {
        if (obj != null) {
            return obj;
        }
        T alternate = requireNonNull(supplier, "supplier").get();
        return requireNonNull(alternate, "supplier.get()");
    }

    // ---- index and range checking ---------------------------------------------------------------
    //
    // The six return what they were given when the check passes, so they can be chained inside an
    // expression: `arr[checkIndex(i, arr.length)]`.

    // `index` valid in `[0, length)`.
    public static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    public static long checkIndex(long index, long length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " out of bounds for length " + length);
        }
        return index;
    }

    // The range `[from, to)` inside `[0, length]`. It returns `from`.
    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static long checkFromToIndex(long fromIndex, long toIndex, long length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + toIndex
                    + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    // The range `[from, from + size)` inside `[0, length]`. It returns `from`.
    //
    // The comparison is written as `size > length - from` and **not** as `from + size > length`,
    // which is the obvious form: the obvious one overflows with a large `size`, the sum comes out
    // negative, and the check passes in exactly the case it was there to catch.
    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || length < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + "
                    + size + ") out of bounds for length " + length);
        }
        return fromIndex;
    }

    public static long checkFromIndexSize(long fromIndex, long size, long length) {
        if (fromIndex < 0 || size < 0 || length < 0 || size > length - fromIndex) {
            throw new IndexOutOfBoundsException("Range [" + fromIndex + ", " + fromIndex + " + "
                    + size + ") out of bounds for length " + length);
        }
        return fromIndex;
    }
}
