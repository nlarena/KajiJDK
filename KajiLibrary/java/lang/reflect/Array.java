package java.lang.reflect;

/**
 * Static reflective access to Java arrays: create them, measure them, read and write their elements.
 *
 * <p>This is the one class in {@code java.lang.reflect} that is not <em>about</em> a program
 * element — it is about a value. It exists because arrays are the one kind of object the language
 * can create only with syntax ({@code new int[n]}, {@code a[i]}), never with a method call. Every
 * other object has a constructor reflection can find; an array has none, so reflection needs a
 * back door, and this class is it.
 *
 * <p>The shape of the API follows from the JVM's array bytecodes rather than from any generic
 * design: there is a {@code getInt}/{@code setInt} pair for each primitive because {@code iaload}
 * and {@code daload} are different instructions operating on different stack shapes, and there is
 * no way to write one generic accessor over them. {@link #get} and {@link #set} paper over that by
 * boxing, at the cost of allocating on every element access.
 *
 * <h2>KajiLibrary status</h2>
 *
 * <p>Everything here except {@link #newInstance(Class, int...)} is {@code native}: the operations
 * are array bytecodes, and nothing in Java source can express "index an array whose component type
 * I only learn at runtime". The VM does not implement these intrinsics yet (see the package notes),
 * so calling them fails to link; the declarations are correct and the class is the right place for
 * the VM work to land.
 */
public final class Array {

    // Not instantiable: every member is static. Matches the JDK, which declares the same private
    // no-arg constructor for exactly this reason.
    private Array() {
    }

    /**
     * Creates a new array with the given component type and length.
     *
     * @param componentType the {@code Class} of the array's elements
     * @param length the length of the new array
     * @return the new array
     * @throws NegativeArraySizeException if {@code length} is negative
     */
    public static Object newInstance(Class<?> componentType, int length) throws NegativeArraySizeException {
        return newArray(componentType, length);
    }

    /**
     * Creates a new multidimensional array.
     *
     * <p>The component type applies to the innermost dimension: {@code newInstance(int.class, 2, 3)}
     * builds an {@code int[2][3]}, whose own class is {@code int[][]} — the component type of the
     * result is one array level deeper than what was passed in.
     *
     * @param componentType the {@code Class} of the innermost elements
     * @param dimensions the length of each dimension
     * @return the new array
     * @throws IllegalArgumentException if {@code dimensions} is empty or has more than 255 entries
     * @throws NegativeArraySizeException if any entry of {@code dimensions} is negative
     */
    public static Object newInstance(Class<?> componentType, int... dimensions)
            throws IllegalArgumentException, NegativeArraySizeException {
        // The JDK does this check in native code; doing it here keeps the diagnostic in Java and
        // means the intrinsic below only ever sees a well-formed request. 255 is the JVMS limit on
        // the `dimensions` operand of multianewarray, not an arbitrary cap.
        if (dimensions.length == 0 || dimensions.length > 255) {
            throw new IllegalArgumentException("wrong number of array dimensions");
        }
        return multiNewArray(componentType, dimensions);
    }

    /**
     * Returns the length of the given array.
     *
     * @param array the array
     * @return its length
     * @throws IllegalArgumentException if {@code array} is not an array
     */
    public static native int getLength(Object array) throws IllegalArgumentException;

    /**
     * Returns the element at {@code index}, boxing it if the component type is primitive.
     *
     * @param array the array
     * @param index the index
     * @return the element, boxed if primitive
     * @throws IllegalArgumentException if {@code array} is not an array
     * @throws ArrayIndexOutOfBoundsException if {@code index} is out of range
     */
    public static native Object get(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code boolean} at {@code index}. @see #get */
    public static native boolean getBoolean(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code byte} at {@code index}, widening if needed. @see #get */
    public static native byte getByte(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code char} at {@code index}. @see #get */
    public static native char getChar(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code short} at {@code index}, widening if needed. @see #get */
    public static native short getShort(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code int} at {@code index}, widening if needed. @see #get */
    public static native int getInt(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code long} at {@code index}, widening if needed. @see #get */
    public static native long getLong(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code float} at {@code index}, widening if needed. @see #get */
    public static native float getFloat(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Returns the {@code double} at {@code index}, widening if needed. @see #get */
    public static native double getDouble(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /**
     * Stores {@code value} at {@code index}, unboxing it if the component type is primitive.
     *
     * @param array the array
     * @param index the index
     * @param value the new value
     * @throws IllegalArgumentException if {@code array} is not an array, or {@code value} does not
     *         convert to the component type
     * @throws ArrayIndexOutOfBoundsException if {@code index} is out of range
     */
    public static native void set(Object array, int index, Object value)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code boolean} at {@code index}. @see #set */
    public static native void setBoolean(Object array, int index, boolean z)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code byte} at {@code index}, widening if needed. @see #set */
    public static native void setByte(Object array, int index, byte b)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code char} at {@code index}, widening if needed. @see #set */
    public static native void setChar(Object array, int index, char c)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code short} at {@code index}, widening if needed. @see #set */
    public static native void setShort(Object array, int index, short s)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores an {@code int} at {@code index}, widening if needed. @see #set */
    public static native void setInt(Object array, int index, int i)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code long} at {@code index}, widening if needed. @see #set */
    public static native void setLong(Object array, int index, long l)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code float} at {@code index}, widening if needed. @see #set */
    public static native void setFloat(Object array, int index, float f)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    /** Stores a {@code double} at {@code index}. @see #set */
    public static native void setDouble(Object array, int index, double d)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException;

    // The two allocation intrinsics, private exactly as in the JDK: the public newInstance overloads
    // above are the validated front door. `newArray` is `anewarray`/`newarray` with a runtime class;
    // `multiNewArray` is `multianewarray`.
    private static native Object newArray(Class<?> componentType, int length)
            throws NegativeArraySizeException;

    private static native Object multiNewArray(Class<?> componentType, int[] dimensions)
            throws IllegalArgumentException, NegativeArraySizeException;
}
