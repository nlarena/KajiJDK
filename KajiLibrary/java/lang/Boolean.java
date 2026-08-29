package java.lang;

// Por import y nombre simple: calificar el tipo en el uso no resuelve desde java.lang
// (finding #210).
import java.io.Serializable;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.Boolean -- the boxed {@code boolean}.
 *
 * <p>It does not extend {@link Number}, because a boolean is not one. And it is the only wrapper
 * whose cache is complete and unconditional: there are two values, so {@link #TRUE} and
 * {@link #FALSE} are all the instances {@link #valueOf(boolean)} will ever hand out.
 *
 * <p>The three logical operations at the end look pointless next to {@code &&} and {@code ||},
 * and they are not: the operators are short-circuiting and these are not, so they can be passed
 * around as method references where an operator cannot go.
 */
public final class Boolean implements Serializable, Comparable<Boolean>,
        java.lang.constant.Constable {

    /** The shared instance holding true. */
    public static final Boolean TRUE = new Boolean(true);

    /** The shared instance holding false. */
    public static final Boolean FALSE = new Boolean(false);

    /**
     * The mirror of the primitive type {@code boolean}.
     *
     * <p>Not {@code Boolean.class}: that one names this class.
     */
    public static final Class<Boolean> TYPE = Class.getPrimitiveClass("boolean");

    private final boolean value;

    /**
     * A Boolean holding {@code value}.
     *
     * @param value the value
     * @deprecated the JDK deprecates every wrapper constructor, and this one hardest: it produces
     *         a third and a fourth instance of a two-valued type, and code that then compares
     *         with {@code ==} silently stops working. Use {@link #valueOf(boolean)}.
     */
    @Deprecated(since = "9")
    public Boolean(boolean value) {
        this.value = value;
    }

    /**
     * A Boolean holding whether {@code s} spells {@code "true"}, ignoring case.
     *
     * @param s the text
     * @deprecated as with the other constructor; use {@link #valueOf(String)}.
     */
    @Deprecated(since = "9")
    public Boolean(String s) {
        this.value = Boolean.parseBoolean(s);
    }

    /**
     * The shared Boolean holding {@code b}. Never allocates.
     *
     * @param b the value
     */
    public static Boolean valueOf(boolean b) {
        if (b) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * The shared Boolean for what {@code s} spells.
     *
     * @param s the text
     */
    public static Boolean valueOf(String s) {
        return Boolean.valueOf(Boolean.parseBoolean(s));
    }

    /**
     * Whether {@code s} spells {@code "true"}, ignoring case.
     *
     * <p>Note what it does NOT do: it never throws. Anything that is not {@code "true"} -- a
     * misspelling, an empty string, {@code null} -- is false, which makes this the wrong parser
     * for input a human typed and the right one for a flag.
     *
     * @param s the text
     */
    public static boolean parseBoolean(String s) {
        if (s == null) {
            return false;
        }
        return s.equalsIgnoreCase("true");
    }

    /** The value this Boolean holds. */
    public boolean booleanValue() {
        return this.value;
    }

    /**
     * Whether the system property {@code name} exists and spells {@code "true"}.
     *
     * @param name the property name
     */
    public static boolean getBoolean(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(System.getProperty(name));
    }

    // ---- comparing ----

    /**
     * Whether {@code obj} is a Boolean holding the same value.
     *
     * @param obj the object to compare against
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Boolean)) {
            return false;
        }
        Boolean other = (Boolean) obj;
        return this.value == other.booleanValue();
    }

    /** 1231 for true and 1237 for false, which are the JDK's numbers. */
    public int hashCode() {
        return Boolean.hashCode(this.value);
    }

    /**
     * The hash a Boolean holding {@code value} would have.
     *
     * <p>1231 and 1237 rather than 1 and 0, and the choice is deliberate: they are primes far
     * apart, so a hash built by combining a few booleans spreads instead of collapsing into a
     * handful of small integers.
     *
     * @param value the value
     */
    public static int hashCode(boolean value) {
        if (value) {
            return 1231;
        }
        return 1237;
    }

    /**
     * Compares two booleans, with false ordered before true.
     *
     * @param x the first
     * @param y the second
     */
    public static int compare(boolean x, boolean y) {
        if (x == y) {
            return 0;
        }
        if (x) {
            return 1;
        }
        return -1;
    }

    /**
     * Compares this Boolean against another.
     *
     * @param b what to compare against
     */
    public int compareTo(Boolean b) {
        return Boolean.compare(this.value, b.booleanValue());
    }

    // ---- the logical operations, which do NOT short-circuit ----

    /**
     * {@code a && b}, evaluating both.
     *
     * @param a the first
     * @param b the second
     */
    public static boolean logicalAnd(boolean a, boolean b) {
        return a && b;
    }

    /**
     * {@code a || b}, evaluating both.
     *
     * @param a the first
     * @param b the second
     */
    public static boolean logicalOr(boolean a, boolean b) {
        return a || b;
    }

    /**
     * {@code a ^ b}: true when exactly one of them is.
     *
     * @param a the first
     * @param b the second
     */
    public static boolean logicalXor(boolean a, boolean b) {
        return a ^ b;
    }

    // ---- printing ----

    /** {@code "true"} or {@code "false"}. */
    public String toString() {
        return Boolean.toString(this.value);
    }

    /**
     * {@code "true"} or {@code "false"}.
     *
     * @param b the value
     */
    public static String toString(boolean b) {
        if (b) {
            return "true";
        }
        return "false";
    }

    /**
     * This value as a constant that can be written into a class file.
     *
     * <p>A dynamic constant, and here the reason is starker than for the numeric wrappers: a
     * class file has no boolean at all. The two descriptors read the {@code TRUE} and
     * {@code FALSE} fields of this very class.
     */
    public Optional<DynamicConstantDesc<Boolean>> describeConstable() {
        if (this.value) {
            return Optional.of(ConstantDescs.TRUE);
        }
        return Optional.of(ConstantDescs.FALSE);
    }
}
