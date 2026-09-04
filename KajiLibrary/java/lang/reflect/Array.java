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
 * <h2>Como esta implementada aca</h2>
 *
 * <p>Los diecisiete accesores publicos <b>ya no son nativos</b>: son Java sobre cinco puertas
 * internas --{@code getInt0}, {@code getLong0}, {@code getFloat0}, {@code getDouble0},
 * {@code getRef0} y sus simetricas-- que leen y escriben crudo. Antes estaban declarados
 * {@code native} sin puente en la VM, o sea que <b>todos tiraban {@code UnsatisfiedLinkError}</b>:
 * una clase que medía completa y no servia para nada.
 *
 * <p>Que sean cinco y no diecisiete sale de que el <b>ancho</b> de un elemento lo decide el tipo del
 * arreglo, que la VM ya conoce; lo que elige quien llama es la <b>forma</b> en que quiere el valor.
 * Un {@code getInt0} sobre un {@code byte[]} lee un byte y lo extiende con signo, sobre un
 * {@code char[]} lee dos sin signo.
 *
 * <p>Y los <b>chequeos viven de este lado</b>: el nulo, el indice fuera de rango, el tipo que no
 * corresponde y las conversiones de ensanchamiento. Cada uno tiene su excepcion con su mensaje, y
 * escribirlas en Java es lo que hace que se lean. Las puertas internas solo ven pedidos bien
 * formados -- la misma decision que ya estaba tomada para {@code newArray}.
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
        // El chequeo va aca y no en la puerta interna, como todos los demas de esta clase. El
        // comentario de esa puerta ya decia que este chequeo existia; no existia, y un largo
        // negativo devolvia un arreglo vacio en silencio.
        if (length < 0) {
            throw new NegativeArraySizeException(Integer.toString(length));
        }
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
        // Y ninguna negativa: se chequean **todas antes** de alocar nada, para que una dimension
        // interior mal dada no deje medio arreglo construido en el heap.
        for (int d : dimensions) {
            if (d < 0) {
                throw new NegativeArraySizeException(Integer.toString(d));
            }
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
    public static int getLength(Object array) throws IllegalArgumentException {
        int n = length0(array);
        if (n < 0) {
            // El -1 de la puerta interna es "no es un arreglo"; el nulo se separa antes porque su
            // excepcion es otra.
            if (array == null) {
                throw new NullPointerException();
            }
            throw new IllegalArgumentException("Argument is not an array");
        }
        return n;
    }

    // El componente del arreglo, ya validado. Todo accesor empieza por aca: es donde se deciden de
    // una vez el nulo, el "no es un arreglo" y el rango.
    private static Class<?> componente(Object array, int index) {
        if (array == null) {
            throw new NullPointerException();
        }
        Class<?> c = array.getClass().getComponentType();
        if (c == null) {
            throw new IllegalArgumentException("Argument is not an array");
        }
        int n = length0(array);
        if (index < 0 || index >= n) {
            throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length "
                                                     + n);
        }
        return c;
    }

    /**
     * Returns the element at {@code index}, boxing it if the component type is primitive.
     *
     * @param array the array
     * @param index the index
     * @return the element, boxed if primitive
     * @throws IllegalArgumentException if {@code array} is not an array
     * @throws ArrayIndexOutOfBoundsException if {@code index} is out of range
     */
    public static Object get(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (!c.isPrimitive()) {
            return getRef0(array, index);
        }
        if (c == Boolean.TYPE) {
            return Boolean.valueOf(getInt0(array, index) != 0);
        }
        if (c == Byte.TYPE) {
            return Byte.valueOf((byte) getInt0(array, index));
        }
        if (c == Character.TYPE) {
            return Character.valueOf((char) getInt0(array, index));
        }
        if (c == Short.TYPE) {
            return Short.valueOf((short) getInt0(array, index));
        }
        if (c == Integer.TYPE) {
            return Integer.valueOf(getInt0(array, index));
        }
        if (c == Long.TYPE) {
            return Long.valueOf(getLong0(array, index));
        }
        if (c == Float.TYPE) {
            return Float.valueOf(getFloat0(array, index));
        }
        return Double.valueOf(getDouble0(array, index));
    }

    /** Returns the {@code boolean} at {@code index}. @see #get */
    public static boolean getBoolean(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        exigir(componente(array, index) == Boolean.TYPE, "boolean");
        return getInt0(array, index) != 0;
    }

    /** Returns the {@code byte} at {@code index}. @see #get */
    public static byte getByte(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        exigir(componente(array, index) == Byte.TYPE, "byte");
        return (byte) getInt0(array, index);
    }

    /** Returns the {@code char} at {@code index}. @see #get */
    public static char getChar(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        exigir(componente(array, index) == Character.TYPE, "char");
        return (char) getInt0(array, index);
    }

    /** Returns the {@code short} at {@code index}, widening if needed. @see #get */
    public static short getShort(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        exigir(c == Byte.TYPE || c == Short.TYPE, "short");
        return (short) getInt0(array, index);
    }

    /** Returns the {@code int} at {@code index}, widening if needed. @see #get */
    public static int getInt(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        exigir(c == Byte.TYPE || c == Character.TYPE || c == Short.TYPE || c == Integer.TYPE, "int");
        return getInt0(array, index);
    }

    /** Returns the {@code long} at {@code index}, widening if needed. @see #get */
    public static long getLong(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Long.TYPE) {
            return getLong0(array, index);
        }
        exigir(c == Byte.TYPE || c == Character.TYPE || c == Short.TYPE || c == Integer.TYPE,
               "long");
        return getInt0(array, index);
    }

    /** Returns the {@code float} at {@code index}, widening if needed. @see #get */
    public static float getFloat(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Float.TYPE) {
            return getFloat0(array, index);
        }
        if (c == Long.TYPE) {
            return getLong0(array, index);
        }
        exigir(c == Byte.TYPE || c == Character.TYPE || c == Short.TYPE || c == Integer.TYPE,
               "float");
        return getInt0(array, index);
    }

    /** Returns the {@code double} at {@code index}, widening if needed. @see #get */
    public static double getDouble(Object array, int index)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Double.TYPE) {
            return getDouble0(array, index);
        }
        if (c == Float.TYPE) {
            return getFloat0(array, index);
        }
        if (c == Long.TYPE) {
            return getLong0(array, index);
        }
        exigir(c == Byte.TYPE || c == Character.TYPE || c == Short.TYPE || c == Integer.TYPE,
               "double");
        return getInt0(array, index);
    }

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
    public static void set(Object array, int index, Object value)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (!c.isPrimitive()) {
            if (value != null && !c.isInstance(value)) {
                throw new IllegalArgumentException("argument type mismatch");
            }
            setRef0(array, index, value);
            return;
        }
        if (value == null) {
            // Un nulo no se puede desenvolver en un primitivo, y el contrato lo llama un argumento
            // equivocado y no un `NullPointerException`.
            throw new IllegalArgumentException("argument type mismatch");
        }
        if (value instanceof Boolean) {
            setBoolean(array, index, ((Boolean) value).booleanValue());
        } else if (value instanceof Byte) {
            setByte(array, index, ((Byte) value).byteValue());
        } else if (value instanceof Character) {
            setChar(array, index, ((Character) value).charValue());
        } else if (value instanceof Short) {
            setShort(array, index, ((Short) value).shortValue());
        } else if (value instanceof Integer) {
            setInt(array, index, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            setLong(array, index, ((Long) value).longValue());
        } else if (value instanceof Float) {
            setFloat(array, index, ((Float) value).floatValue());
        } else if (value instanceof Double) {
            setDouble(array, index, ((Double) value).doubleValue());
        } else {
            throw new IllegalArgumentException("argument type mismatch");
        }
    }

    /** Stores a {@code boolean} at {@code index}. @see #set */
    public static void setBoolean(Object array, int index, boolean z)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        exigir(componente(array, index) == Boolean.TYPE, "boolean");
        setInt0(array, index, z ? 1 : 0);
    }

    /** Stores a {@code byte} at {@code index}, widening if needed. @see #set */
    public static void setByte(Object array, int index, byte b)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Byte.TYPE || c == Short.TYPE || c == Integer.TYPE) {
            setInt0(array, index, b);
        } else {
            anchos(array, index, c, b, b, b);
        }
    }

    /** Stores a {@code char} at {@code index}, widening if needed. @see #set */
    public static void setChar(Object array, int index, char ch)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Character.TYPE || c == Integer.TYPE) {
            setInt0(array, index, ch);
        } else {
            anchos(array, index, c, ch, ch, ch);
        }
    }

    /** Stores a {@code short} at {@code index}, widening if needed. @see #set */
    public static void setShort(Object array, int index, short s)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Short.TYPE || c == Integer.TYPE) {
            setInt0(array, index, s);
        } else {
            anchos(array, index, c, s, s, s);
        }
    }

    /** Stores an {@code int} at {@code index}, widening if needed. @see #set */
    public static void setInt(Object array, int index, int i)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Integer.TYPE) {
            setInt0(array, index, i);
        } else {
            anchos(array, index, c, i, i, i);
        }
    }

    /** Stores a {@code long} at {@code index}, widening if needed. @see #set */
    public static void setLong(Object array, int index, long l)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Long.TYPE) {
            setLong0(array, index, l);
        } else if (c == Float.TYPE) {
            setFloat0(array, index, l);
        } else if (c == Double.TYPE) {
            setDouble0(array, index, l);
        } else {
            throw new IllegalArgumentException("argument type mismatch");
        }
    }

    /** Stores a {@code float} at {@code index}, widening if needed. @see #set */
    public static void setFloat(Object array, int index, float f)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        Class<?> c = componente(array, index);
        if (c == Float.TYPE) {
            setFloat0(array, index, f);
        } else if (c == Double.TYPE) {
            setDouble0(array, index, f);
        } else {
            throw new IllegalArgumentException("argument type mismatch");
        }
    }

    /** Stores a {@code double} at {@code index}. @see #set */
    public static void setDouble(Object array, int index, double d)
            throws IllegalArgumentException, ArrayIndexOutOfBoundsException {
        exigir(componente(array, index) == Double.TYPE, "double");
        setDouble0(array, index, d);
    }

    // Los tres destinos anchos --`long`, `float`, `double`-- que aceptan cualquiera de los cuatro
    // tipos enteros angostos. Los destinos de ancho de `int` los decide cada `set`, porque ahi si se
    // diferencian: un `char` entra en un `int[]` pero no en un `short[]`.
    private static void anchos(Object array, int index, Class<?> c, long comoLong, float comoFloat,
                               double comoDouble) {
        if (c == Long.TYPE) {
            setLong0(array, index, comoLong);
        } else if (c == Float.TYPE) {
            setFloat0(array, index, comoFloat);
        } else if (c == Double.TYPE) {
            setDouble0(array, index, comoDouble);
        } else {
            throw new IllegalArgumentException("argument type mismatch");
        }
    }

    private static void exigir(boolean ok, String tipo) {
        if (!ok) {
            throw new IllegalArgumentException("Argument is not an array of type " + tipo);
        }
    }

    // ---- las puertas internas ----------------------------------------------------------------
    //
    // Sin chequeos: los hace todo lo de arriba. Ver la nota de la clase.

    /** El largo, o -1 si no es un arreglo. */
    private static native int length0(Object array);

    private static native int getInt0(Object array, int index);

    private static native long getLong0(Object array, int index);

    private static native float getFloat0(Object array, int index);

    private static native double getDouble0(Object array, int index);

    private static native Object getRef0(Object array, int index);

    private static native void setInt0(Object array, int index, int value);

    private static native void setLong0(Object array, int index, long value);

    private static native void setFloat0(Object array, int index, float value);

    private static native void setDouble0(Object array, int index, double value);

    private static native void setRef0(Object array, int index, Object value);

    // The two allocation intrinsics, private exactly as in the JDK: the public newInstance overloads
    // above are the validated front door. `newArray` is `anewarray`/`newarray` with a runtime class;
    // `multiNewArray` is `multianewarray`.
    private static native Object newArray(Class<?> componentType, int length)
            throws NegativeArraySizeException;

    private static native Object multiNewArray(Class<?> componentType, int[] dimensions)
            throws IllegalArgumentException, NegativeArraySizeException;
}
