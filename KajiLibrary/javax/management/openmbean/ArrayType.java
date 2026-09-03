package javax.management.openmbean;

import java.util.HashMap;
import java.util.Map;

/**
 * El tipo de un arreglo de `n` dimensiones cuyos elementos son de otro tipo abierto.
 *
 * <p>Hay dos formas de arreglo y la diferencia se ve al leer un valor:
 *
 * <ul>
 * <li>El **de referencias** (`new ArrayType&lt;&gt;(1, SimpleType.INTEGER)`) tiene elementos
 *     `Integer`, así que un elemento puede ser nulo. Su `className` es `[Ljava.lang.Integer;`.</li>
 * <li>El **de primitivos** (`new ArrayType&lt;&gt;(SimpleType.INTEGER, true)`) tiene elementos
 *     `int`, así que ninguno puede ser nulo y ocupa menos. Su `className` es `[I`.</li>
 * </ul>
 *
 * <p>Los dos declaran `SimpleType.INTEGER` como {@link #getElementOpenType}, porque el tipo abierto
 * de un `int` **es** `SimpleType.INTEGER` -- no hay tipos abiertos primitivos. Lo que los distingue
 * es {@link #isPrimitiveArray}, y por eso ese método existe: sin él, dos tipos que aceptan valores
 * distintos serían indistinguibles.
 *
 * <p>Sólo los ocho envoltorios más `Void` tienen forma primitiva; pedir un arreglo primitivo de
 * `String` es un error.
 */
public class ArrayType<T> extends OpenType<T> {

    private static final long serialVersionUID = 720504429830309770L;

    // Del nombre del envoltorio al descriptor y al nombre del primitivo. Es la tabla del §4.3.2 del
    // JVMS, que es de donde salen los nombres `[I`, `[Z`, etcétera.
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
     * Un arreglo de referencias de `dimension` dimensiones.
     *
     * <p>Si `elementType` ya es un {@link ArrayType}, las dimensiones se **suman**: un arreglo de
     * una dimensión de un arreglo de dos es uno de tres, no uno de una cuyos elementos son
     * arreglos. Es lo que hace que `[[[I` tenga una sola representación.
     *
     * @throws OpenDataException si `dimension` es menor que 1 o mayor que 15
     * @throws IllegalArgumentException si `elementType` es nulo
     */
    public ArrayType(int dimension, OpenType<?> elementType) throws OpenDataException {
        super(arrayClassName(dimension, elementType), arrayClassName(dimension, elementType),
                arrayDescription(dimension, elementType), true);
        if (elementType == null) {
            throw new IllegalArgumentException("el tipo de los elementos no puede ser nulo");
        }
        if (dimension < 1) {
            throw new IllegalArgumentException("la dimensión tiene que ser 1 o más: " + dimension);
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
                    "un arreglo no puede tener más de 15 dimensiones: " + this.dimension);
        }
    }

    /**
     * Un arreglo de una dimensión, primitivo o no.
     *
     * <p>Con `primitiveArray` en `false` es lo mismo que `new ArrayType&lt;&gt;(1, elementType)`.
     *
     * @throws OpenDataException si se pide primitivo de un tipo que no tiene forma primitiva
     * @throws IllegalArgumentException si `elementType` es nulo
     */
    public ArrayType(SimpleType<?> elementType, boolean primitiveArray) throws OpenDataException {
        super(simpleArrayClassName(elementType, primitiveArray), simpleArrayClassName(elementType, primitiveArray),
                simpleArrayDescription(elementType, primitiveArray), true);
        if (elementType == null) {
            throw new IllegalArgumentException("el tipo de los elementos no puede ser nulo");
        }
        if (primitiveArray && !DESCRIPTOR.containsKey(elementType.getClassName())) {
            throw new OpenDataException(
                    elementType.getClassName() + " no tiene forma primitiva");
        }
        this.dimension = 1;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    // El constructor de paquete: se lo saltea la validación porque quien lo llama ya la hizo.
    ArrayType(String className, String typeName, String description, int dimension,
            OpenType<?> elementType, boolean primitiveArray) {
        super(className, typeName, description, true);
        this.dimension = dimension;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    // Los nombres se arman antes de llamar a `super`, así que no pueden mirar los campos. De ahí
    // que sean estáticos y repitan el aplanado de dimensiones que hace el constructor.
    private static String arrayClassName(int dimension, OpenType<?> elementType) {
        if (elementType == null || dimension < 1) {
            // El constructor va a tirar igual; acá sólo hay que devolver algo que no rompa a
            // `super`, que valida por su cuenta.
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
            // Un tipo sin forma primitiva: el constructor lo rechaza; acá se devuelve el nombre de
            // referencias sólo para que `super` no falle antes con un mensaje peor.
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

    /** Cuántas dimensiones tiene. */
    public int getDimension() {
        return this.dimension;
    }

    /** El tipo abierto de los elementos del fondo, ya sin dimensiones. */
    public OpenType<?> getElementOpenType() {
        return this.elementType;
    }

    /** Si sus elementos son primitivos y no envoltorios. */
    public boolean isPrimitiveArray() {
        return this.primitiveArray;
    }

    /**
     * Si `obj` es un arreglo de este tipo.
     *
     * <p>Se compara el **nombre de clase** del objeto contra el de este tipo, que es exactamente la
     * pregunta: `[I` y `[Ljava.lang.Integer;` son clases distintas, y ésa es la diferencia entre un
     * arreglo primitivo y uno de referencias.
     */
    public boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().getName().equals(this.getClassName());
    }

    /** Igualdad por dimensión, tipo de elemento y si es primitivo. */
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
     * Un arreglo de una dimensión de ese tipo.
     *
     * <p>Existe además de los constructores porque **conserva el parámetro de tipo**: dado un
     * `OpenType&lt;Integer&gt;` devuelve un `ArrayType&lt;Integer[]&gt;`, que es lo que un
     * constructor no puede expresar.
     *
     * @throws OpenDataException si el tipo no admite arreglos
     */
    public static <E> ArrayType<E[]> getArrayType(OpenType<E> elementType)
            throws OpenDataException {
        ArrayType<E[]> a = new ArrayType<E[]>(1, elementType);
        return a;
    }

    /**
     * El tipo del arreglo primitivo de esa clase, por ejemplo `int[].class`.
     *
     * @throws IllegalArgumentException si la clase no es un arreglo de primitivos de una dimensión
     */
    public static <T> ArrayType<T> getPrimitiveArrayType(Class<T> arrayClass) {
        if (arrayClass == null || !arrayClass.isArray()) {
            throw new IllegalArgumentException("no es una clase de arreglo: " + arrayClass);
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
                    "no es un arreglo de primitivos: " + arrayClass.getName());
        }
        String description = d + "-dimension array of "
                + PRIMITIVE_NAME.get(element.getClassName());
        ArrayType<T> a = new ArrayType<T>(name, name, description, d, element, true);
        return a;
    }
}
