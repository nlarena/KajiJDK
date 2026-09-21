package javax.management.openmbean;

import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.management.ObjectName;

/**
 * The fourteen open types that are composed of nothing: the wrappers, {@code String},
 * {@code Date}, {@code BigDecimal}, {@code BigInteger}, {@code ObjectName} and {@code Void}. (An
 * earlier note said fifteen.)
 *
 * <p>There is no public constructor and there cannot be: the constants here <b>are</b> all the
 * simple types that exist, and allowing one more to be built would permit two different objects for
 * {@code java.lang.Integer}. That matters because the rest of the package compares them by identity
 * on the fast paths, even though {@link #equals} also works.
 *
 * <p>Hence {@code readResolve}: on deserialization a constant would come back as a new object and
 * identity would break silently --which is the worst way to break. {@code readResolve} returns the
 * matching constant and the property is kept.
 *
 * <p>{@code VOID} is there for completeness of the enumeration: it is the return type of an
 * operation that returns nothing. No value is of type {@code VOID}, so its {@link #isValue} is
 * always {@code false} -- which is correct and not an unimplemented case.
 */
public final class SimpleType<T> extends OpenType<T> {

    private static final long serialVersionUID = 2215577471957694503L;

    /** The type of what has no value. */
    public static final SimpleType<Void> VOID =
            new SimpleType<Void>("java.lang.Void");

    /** `java.lang.Boolean`. */
    public static final SimpleType<Boolean> BOOLEAN =
            new SimpleType<Boolean>("java.lang.Boolean");

    /** `java.lang.Character`. */
    public static final SimpleType<Character> CHARACTER =
            new SimpleType<Character>("java.lang.Character");

    /** `java.lang.Byte`. */
    public static final SimpleType<Byte> BYTE =
            new SimpleType<Byte>("java.lang.Byte");

    /** `java.lang.Short`. */
    public static final SimpleType<Short> SHORT =
            new SimpleType<Short>("java.lang.Short");

    /** `java.lang.Integer`. */
    public static final SimpleType<Integer> INTEGER =
            new SimpleType<Integer>("java.lang.Integer");

    /** `java.lang.Long`. */
    public static final SimpleType<Long> LONG =
            new SimpleType<Long>("java.lang.Long");

    /** `java.lang.Float`. */
    public static final SimpleType<Float> FLOAT =
            new SimpleType<Float>("java.lang.Float");

    /** `java.lang.Double`. */
    public static final SimpleType<Double> DOUBLE =
            new SimpleType<Double>("java.lang.Double");

    /** `java.lang.String`. */
    public static final SimpleType<String> STRING =
            new SimpleType<String>("java.lang.String");

    /** `java.math.BigDecimal`. */
    public static final SimpleType<BigDecimal> BIGDECIMAL =
            new SimpleType<BigDecimal>("java.math.BigDecimal");

    /** `java.math.BigInteger`. */
    public static final SimpleType<BigInteger> BIGINTEGER =
            new SimpleType<BigInteger>("java.math.BigInteger");

    /** `java.util.Date`. */
    public static final SimpleType<Date> DATE =
            new SimpleType<Date>("java.util.Date");

    /** `javax.management.ObjectName`. */
    public static final SimpleType<ObjectName> OBJECTNAME =
            new SimpleType<ObjectName>("javax.management.ObjectName");

    // The order matters: `readResolve` walks this array, so a constant missing here would come back
    // from deserialization as an object different from the one that was serialized.
    private static final SimpleType<?>[] ALL = new SimpleType<?>[] {
        VOID, BOOLEAN, CHARACTER, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING,
        BIGDECIMAL, BIGINTEGER, DATE, OBJECTNAME };

    // The three names of a simple type are the same one: there is nothing to choose, and that is
    // why the constructor takes only one.
    private SimpleType(String className) {
        super(className, className, className, false);
    }

    /** Whether {@code obj} is an instance of this type's class. A null never is. */
    public boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().getName().equals(this.getClassName());
    }

    /**
     * Equality by class name.
     *
     * <p>That is enough because the other two names of a simple type are equal to the first.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimpleType)) {
            return false;
        }
        return this.getClassName().equals(((SimpleType<?>) obj).getClassName());
    }

    public int hashCode() {
        return this.getClassName().hashCode();
    }

    public String toString() {
        return SimpleType.class.getName() + "(name=" + this.getTypeName() + ")";
    }

    /**
     * Returns the matching constant instead of the just-deserialized object.
     *
     * <p>See the class note: without this, a {@code SimpleType} that travels through serialization
     * stops being identical to the constant and identity comparisons start failing silently.
     */
    public Object readResolve() throws ObjectStreamException {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].getClassName().equals(this.getClassName())) {
                return ALL[i];
            }
        }
        return this;
    }
}
