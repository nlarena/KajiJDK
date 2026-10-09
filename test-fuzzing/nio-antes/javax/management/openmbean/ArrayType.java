package javax.management.openmbean;

import java.util.HashMap;
import java.util.Map;

/**
 * The type of an {@code n}-dimensional array whose elements are of another open type.
 *
 * <p>There are two array forms and the difference shows when reading a value:
 *
 * <ul>
 * <li>The <b>reference</b> one ({@code new ArrayType<>(1, SimpleType.INTEGER)}) has {@code Integer}
 *     elements, so an element may be null. Its {@code className} is {@code
 *     [Ljava.lang.Integer;}.</li>
 * <li>The <b>primitive</b> one ({@code new ArrayType<>(SimpleType.INTEGER, true)}) has {@code int}
 *     elements, so none can be null and it takes less space. Its {@code className} is {@code
 *     [I}.</li>
 * </ul>
 *
 * <p>Both declare {@code SimpleType.INTEGER} as {@link #getElementOpenType}, because the open type
 * of an {@code int} <b>is</b> {@code SimpleType.INTEGER} -- there are no primitive open types. What
 * tells them apart is {@link #isPrimitiveArray}, and that is why that method exists: without it,
 * two types that accept different values would be indistinguishable.
 *
 * <p>Only the eight wrappers have a primitive form; asking for a primitive array of {@code String}
 * is an error, and so is asking for one of {@code Void} (an earlier note included it).
 */
public class ArrayType<T> extends OpenType<T> {

    private static final long serialVersionUID = 720504429830309770L;

    // From the wrapper's name to the descriptor and to the primitive's name. It is the table of
    // JVMS 4.3.2, which is where the names `[I`, `[Z` and so on come from.
    private static final Map<String, String> DESCRIPTOR = descriptors();
    private static final Map<String, String> PRIMITIVE_NAME = primitiveNames();
    private static final Map<String, SimpleType<?>> OPEN_TYPE_OF_PRIMITIVE = openTypesOfPrimitives();

    private static Map<String, String> descriptors() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("java.lang.Boolean", "Z");
        m.put("java.lang.Character", "C");
        m.put("java.lang.Byte", "B");
        m.put("java.lang.Short", "S");
        m.put("java.lang.Integer", "I");
        m.put("java.lang.Long", "J");
        m.put("java.lang.Float", "F");
        m.put("java.lang.Double", "D");
        return m;
    }

    private static Map<String, String> primitiveNames() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("java.lang.Boolean", "boolean");
        m.put("java.lang.Character", "char");
        m.put("java.lang.Byte", "byte");
        m.put("java.lang.Short", "short");
        m.put("java.lang.Integer", "int");
        m.put("java.lang.Long", "long");
        m.put("java.lang.Float", "float");
        m.put("java.lang.Double", "double");
        return m;
    }

    private static Map<String, SimpleType<?>> openTypesOfPrimitives() {
        Map<String, SimpleType<?>> m = new HashMap<String, SimpleType<?>>();
        m.put("boolean", SimpleType.BOOLEAN);
        m.put("char", SimpleType.CHARACTER);
        m.put("byte", SimpleType.BYTE);
        m.put("short", SimpleType.SHORT);
        m.put("int", SimpleType.INTEGER);
        m.put("long", SimpleType.LONG);
        m.put("float", SimpleType.FLOAT);
        m.put("double", SimpleType.DOUBLE);
        return m;
    }

    private final int dimension;
    private final OpenType<?> elementType;
    private final boolean primitiveArray;

    private transient int hash;

    /**
     * A reference array of {@code dimension} dimensions.
     *
     * <p>If {@code elementType} is already an {@link ArrayType}, the dimensions are <b>added</b>: a
     * one-dimensional array of a two-dimensional one is a three-dimensional one, not a
     * one-dimensional one whose elements are arrays. It is what makes {@code [[[I} have a single
     * representation.
     *
     * @throws OpenDataException if {@code dimension} is less than 1 or greater than 15
     * @throws IllegalArgumentException if {@code elementType} is null
     */
    public ArrayType(int dimension, OpenType<?> elementType) throws OpenDataException {
        super(arrayClassName(dimension, elementType), arrayClassName(dimension, elementType),
                arrayDescription(dimension, elementType), true);
        if (elementType == null) {
            throw new IllegalArgumentException("the element type cannot be null");
        }
        if (dimension < 1) {
            throw new IllegalArgumentException("the dimension must be 1 or more: " + dimension);
        }
        if (elementType instanceof ArrayType) {
            ArrayType<?> a = (ArrayType<?>) elementType;
            this.dimension = dimension + a.getDimension();
            this.elementType = a.getElementOpenType();
            this.primitiveArray = a.isPrimitiveArray();
        } else {
            this.dimension = dimension;
            this.elementType = elementType;
            this.primitiveArray = false;
        }
        if (this.dimension > 15) {
            throw new IllegalArgumentException(
                    "an array cannot have more than 15 dimensions: " + this.dimension);
        }
    }

    /**
     * A one-dimensional array, primitive or not.
     *
     * <p>With {@code primitiveArray} false it is the same as {@code new ArrayType<>(1,
     * elementType)}.
     *
     * @throws OpenDataException if a primitive one is asked for of a type with no primitive form
     * @throws IllegalArgumentException if {@code elementType} is null
     */
    public ArrayType(SimpleType<?> elementType, boolean primitiveArray) throws OpenDataException {
        super(simpleArrayClassName(elementType, primitiveArray), simpleArrayClassName(elementType, primitiveArray),
                simpleArrayDescription(elementType, primitiveArray), true);
        if (elementType == null) {
            throw new IllegalArgumentException("the element type cannot be null");
        }
        if (primitiveArray && !DESCRIPTOR.containsKey(elementType.getClassName())) {
            throw new OpenDataException(
                    elementType.getClassName() + " has no primitive form");
        }
        this.dimension = 1;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    // The package-private constructor: it skips validation because the caller already did it.
    ArrayType(String className, String typeName, String description, int dimension,
            OpenType<?> elementType, boolean primitiveArray) {
        super(className, typeName, description, true);
        this.dimension = dimension;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    // The names are built before calling `super`, so they cannot look at the fields. Hence them
    // being static and repeating the dimension flattening the constructor does.
    private static String arrayClassName(int dimension, OpenType<?> elementType) {
        if (elementType == null || dimension < 1) {
            // The constructor is going to throw anyway; here it is only a matter of returning
            // something that does not break `super`, which validates on its own.
            return "[Ljava.lang.Object;";
        }
        int d = dimension;
        OpenType<?> base = elementType;
        if (elementType instanceof ArrayType) {
            d = dimension + ((ArrayType<?>) elementType).getDimension();
            base = ((ArrayType<?>) elementType).getElementOpenType();
            if (((ArrayType<?>) elementType).isPrimitiveArray()) {
                return brackets(d) + DESCRIPTOR.get(base.getClassName());
            }
        }
        return brackets(d) + "L" + base.getClassName() + ";";
    }

    private static String simpleArrayClassName(SimpleType<?> elementType, boolean primitiveArray) {
        if (elementType == null) {
            return "[Ljava.lang.Object;";
        }
        if (primitiveArray) {
            String d = DESCRIPTOR.get(elementType.getClassName());
            // A type without a primitive form: the constructor rejects it; here the reference name
            // is returned only so that `super` does not fail earlier with a worse message.
            return d == null ? "[L" + elementType.getClassName() + ";" : "[" + d;
        }
        return "[L" + elementType.getClassName() + ";";
    }

    private static String arrayDescription(int dimension, OpenType<?> elementType) {
        if (elementType == null || dimension < 1) {
            return "arreglo";
        }
        int d = dimension;
        OpenType<?> base = elementType;
        if (elementType instanceof ArrayType) {
            d = dimension + ((ArrayType<?>) elementType).getDimension();
            base = ((ArrayType<?>) elementType).getElementOpenType();
        }
        return d + "-dimension array of " + base.getClassName();
    }

    private static String simpleArrayDescription(SimpleType<?> elementType, boolean primitiveArray) {
        if (elementType == null) {
            return "arreglo";
        }
        if (primitiveArray) {
            String n = PRIMITIVE_NAME.get(elementType.getClassName());
            return "1-dimension array of " + (n == null ? elementType.getClassName() : n);
        }
        return "1-dimension array of " + elementType.getClassName();
    }

    private static String brackets(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append("[");
        }
        return sb.toString();
    }

    /** How many dimensions it has. */
    public int getDimension() {
        return this.dimension;
    }

    /** The open type of the elements at the bottom, with no dimensions left. */
    public OpenType<?> getElementOpenType() {
        return this.elementType;
    }

    /** Whether its elements are primitives and not wrappers. */
    public boolean isPrimitiveArray() {
        return this.primitiveArray;
    }

    /**
     * Whether {@code obj} is an array of this type.
     *
     * <p>The object's <b>class name</b> is compared against this type's, which is exactly the
     * question: {@code [I} and {@code [Ljava.lang.Integer;} are different classes, and that is the
     * difference between a primitive array and a reference one.
     */
    public boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().getName().equals(this.getClassName());
    }

    /** Equality by dimension, element type and whether it is primitive. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ArrayType)) {
            return false;
        }
        ArrayType<?> other = (ArrayType<?>) obj;
        return this.dimension == other.dimension
                && this.primitiveArray == other.primitiveArray
                && this.elementType.equals(other.elementType);
    }

    public int hashCode() {
        if (this.hash == 0) {
            int h = this.dimension + this.elementType.hashCode();
            if (this.primitiveArray) {
                h = h + Boolean.TRUE.hashCode();
            }
            this.hash = h;
        }
        return this.hash;
    }

    public String toString() {
        return ArrayType.class.getName() + "(name=" + this.getTypeName()
                + ",dimension=" + this.dimension
                + ",elementType=" + this.elementType.toString()
                + ",primitiveArray=" + this.primitiveArray + ")";
    }

    /**
     * A one-dimensional array of that type.
     *
     * <p>It exists besides the constructors because it <b>keeps the type parameter</b>: given an
     * {@code OpenType<Integer>} it returns an {@code ArrayType<Integer[]>}, which a constructor
     * cannot express.
     *
     * @throws OpenDataException if the type does not admit arrays
     */
    public static <E> ArrayType<E[]> getArrayType(OpenType<E> elementType)
            throws OpenDataException {
        ArrayType<E[]> a = new ArrayType<E[]>(1, elementType);
        return a;
    }

    /**
     * The type of the primitive array of that class, for example {@code int[].class}.
     *
     * @throws IllegalArgumentException if the class is not a one-dimensional primitive array
     */
    public static <T> ArrayType<T> getPrimitiveArrayType(Class<T> arrayClass) {
        if (arrayClass == null || !arrayClass.isArray()) {
            throw new IllegalArgumentException("not an array class: " + arrayClass);
        }
        String name = arrayClass.getName();
        int d = 0;
        while (d < name.length() && name.charAt(d) == '[') {
            d = d + 1;
        }
        String base = name.substring(d);
        SimpleType<?> element = null;
        for (Map.Entry<String, String> e : DESCRIPTOR.entrySet()) {
            if (e.getValue().equals(base)) {
                element = OPEN_TYPE_OF_PRIMITIVE.get(PRIMITIVE_NAME.get(e.getKey()));
            }
        }
        if (element == null) {
            throw new IllegalArgumentException(
                    "not an array of primitives: " + arrayClass.getName());
        }
        String description = d + "-dimension array of "
                + PRIMITIVE_NAME.get(element.getClassName());
        ArrayType<T> a = new ArrayType<T>(name, name, description, d, element, true);
        return a;
    }
}
