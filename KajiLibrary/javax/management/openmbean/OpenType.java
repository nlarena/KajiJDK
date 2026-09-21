package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The root of the open types: the description of a type a JMX client can understand without having
 * the class.
 *
 * <p>That is the whole point of the package and it is worth keeping in mind before reading the
 * rest. An ordinary MBean can expose an attribute of any class, and a remote client that does not
 * have that class on its class path can do nothing with the value. An <b>open</b> MBean limits
 * itself to a handful of types everybody has --the wrappers, {@code String}, {@code Date},
 * {@code ObjectName}, {@code BigDecimal}, {@code BigInteger}-- and to three ways of composing them:
 * arrays, {@link CompositeType} and {@link TabularType}. With that, the value travels and the other
 * side always understands it.
 *
 * <p>Hence {@link #ALLOWED_CLASSNAMES_LIST} being a closed list and not a suggestion: a
 * {@code className} that is not there is not an open type, and the constructor rejects it.
 *
 * <h2>The three names</h2>
 *
 * <p>Every open type has three strings and they are easy to confuse:
 *
 * <ul>
 * <li>{@code className} is the binary name of the class of the <b>values</b>. For a
 *     {@code CompositeType} it is always {@code javax.management.openmbean.CompositeData}, not the
 *     name of what it represents.</li>
 * <li>{@code typeName} identifies the type. For the simple ones and the arrays it matches
 *     {@code className}; for a composite or a table it is chosen by whoever declares it, and it is
 *     what tells two composite types with the same shape apart.</li>
 * <li>{@code description} is text for a person and does not take part in identity. (An earlier note
 *     said the subclasses' {@code equals} compares it; none of the four does --
 *     {@code CompositeType} compares type name and items, {@code SimpleType} the class name,
 *     {@code ArrayType} dimension, element type and primitiveness, and {@code TabularType} type
 *     name, row type and index names.)</li>
 * </ul>
 *
 * <h2>Why the type parameter is not used</h2>
 *
 * <p>{@code T} appears in no member. It is not an oversight: it exists so that {@code
 * SimpleType<Integer>} and {@code SimpleType<String>} are different types to the compiler and
 * {@code ArrayType.getArrayType(SimpleType.INTEGER)} can return {@code ArrayType<Integer[]>}. At
 * run time it is erased, which is why {@link #isValue} takes an {@code Object} and not a {@code T}.
 */
public abstract class OpenType<T> implements Serializable {

    private static final long serialVersionUID = -9195195325186646468L;

    /**
     * The class names an open type may have.
     *
     * <p>They are the eight wrappers, {@code Void}, {@code String}, {@code Date},
     * {@code BigDecimal}, {@code BigInteger}, {@code ObjectName}, and the two composite forms
     * ({@code CompositeData} and {@code TabularData}). It is a read-only list.
     */
    public static final List<String> ALLOWED_CLASSNAMES_LIST =
            Collections.unmodifiableList(allowedClassNames());

    /**
     * The same as {@link #ALLOWED_CLASSNAMES_LIST}, as an array.
     *
     * <p>A public array can be modified by whoever receives it, so this field <b>can be
     * dirtied</b>. It is there anyway because the JDK declares it that way and removing it would
     * break the contract; whoever wants to read without risk has the list next to it, which is
     * read-only.
     */
    public static final String[] ALLOWED_CLASSNAMES =
            ALLOWED_CLASSNAMES_LIST.toArray(new String[0]);

    private static List<String> allowedClassNames() {
        List<String> out = new ArrayList<String>();
        out.add("java.lang.Void");
        out.add("java.lang.Boolean");
        out.add("java.lang.Character");
        out.add("java.lang.Byte");
        out.add("java.lang.Short");
        out.add("java.lang.Integer");
        out.add("java.lang.Long");
        out.add("java.lang.Float");
        out.add("java.lang.Double");
        out.add("java.lang.String");
        out.add("java.math.BigDecimal");
        out.add("java.math.BigInteger");
        out.add("java.util.Date");
        out.add("javax.management.ObjectName");
        out.add(CompositeData.class.getName());
        out.add(TabularData.class.getName());
        return out;
    }

    private final String className;
    private final String typeName;
    private final String description;
    private final transient boolean isArray;

    /**
     * An open type with those three names.
     *
     * <p>{@code className} may carry leading brackets ({@code [Ljava.lang.String;}) to name an
     * array; what has to be in {@link #ALLOWED_CLASSNAMES_LIST} is the type at the bottom.
     *
     * @throws OpenDataException if {@code className} does not name an open type
     * @throws IllegalArgumentException if any of the three is null or empty
     */
    protected OpenType(String className, String typeName, String description)
            throws OpenDataException {
        requireNonBlank(className, "className");
        requireNonBlank(typeName, "typeName");
        requireNonBlank(description, "description");

        String base = className;
        int brackets = 0;
        while (base.startsWith("[")) {
            brackets = brackets + 1;
            base = base.substring(1);
        }
        if (brackets > 0) {
            // A reference array is written `[[Ljava.lang.String;`; a primitive one, `[[I`. Both are
            // valid class names and have to be accepted, but the one compared against the list is
            // the type at the bottom, and only the reference one has it written out.
            if (base.startsWith("L") && base.endsWith(";")) {
                base = base.substring(1, base.length() - 1);
            } else if (!PRIMITIVES.contains(base)) {
                throw new OpenDataException("not a valid array name: " + className);
            }
        }
        if (!PRIMITIVES.contains(base) && !ALLOWED_CLASSNAMES_LIST.contains(base)) {
            throw new OpenDataException(className + " is not an open type");
        }
        this.className = className;
        this.typeName = typeName;
        this.description = description;
        this.isArray = brackets > 0;
    }

    // The package-private constructor used by the subclasses that ALREADY know their name is valid
    // (`SimpleType` with its fourteen constants, `ArrayType` with the one it built itself). It
    // skips the validation, it does not repeat it: repeating would be work and would also force
    // `SimpleType` to declare a checked exception in a static initializer, where it cannot be
    // caught.
    OpenType(String className, String typeName, String description, boolean isArray) {
        this.className = className;
        this.typeName = typeName;
        this.description = description;
        this.isArray = isArray;
    }

    private static final List<String> PRIMITIVES = Collections.unmodifiableList(
            Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D",
                    "boolean", "char", "byte", "short", "int", "long", "float", "double"));

    private static void requireNonBlank(String s, String what) {
        if (s == null || s.trim().length() == 0) {
            throw new IllegalArgumentException(what + " cannot be null or empty");
        }
    }

    /** The binary name of the class of this type's values. */
    public String getClassName() {
        return this.className;
    }

    /** The name that identifies this type. */
    public String getTypeName() {
        return this.typeName;
    }

    /** The description, for a person. */
    public String getDescription() {
        return this.description;
    }

    /** Whether the values of this type are arrays. */
    public boolean isArray() {
        return this.isArray;
    }

    /** Whether {@code obj} is a value of this type. A null never is. */
    public abstract boolean isValue(Object obj);

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    public abstract String toString();
}
