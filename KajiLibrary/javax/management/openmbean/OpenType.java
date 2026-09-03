package javax.management.openmbean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * La raíz de los tipos abiertos: la descripción de un tipo que un cliente de JMX puede entender sin
 * tener la clase.
 *
 * <p>Ése es todo el sentido del paquete y conviene tenerlo presente antes de leer el resto. Un MBean
 * común puede exponer un atributo de cualquier clase, y un cliente remoto que no tenga esa clase en
 * su classpath no puede hacer nada con el valor. Un MBean **abierto** se limita a un puñado de tipos
 * que todo el mundo tiene --los envoltorios, `String`, `Date`, `ObjectName`, `BigDecimal`,
 * `BigInteger`-- y a tres formas de componerlos: arreglos, {@link CompositeType} y
 * {@link TabularType}. Con eso, el valor viaja y el otro lado lo entiende siempre.
 *
 * <p>De ahí que {@link #ALLOWED_CLASSNAMES_LIST} sea una lista cerrada y no una sugerencia: un
 * `className` que no esté ahí no es un tipo abierto, y el constructor lo rechaza.
 *
 * <h2>Los tres nombres</h2>
 *
 * <p>Todo tipo abierto tiene tres cadenas y es fácil confundirlas:
 *
 * <ul>
 * <li>`className` es el nombre binario de la clase de los **valores**. Para un `CompositeType` es
 *     siempre `javax.management.openmbean.CompositeData`, no el nombre de lo que representa.</li>
 * <li>`typeName` identifica al tipo. Para los simples y los arreglos coincide con `className`;
 *     para un compuesto o una tabla lo elige quien lo declara, y es lo que distingue dos tipos
 *     compuestos con la misma forma.</li>
 * <li>`description` es texto para una persona, y no participa de la identidad... salvo que sí:
 *     {@link #equals} de las subclases lo compara. Es una decisión del JDK, no nuestra, y va dicha
 *     porque sorprende.</li>
 * </ul>
 *
 * <h2>Por qué el parámetro de tipo no se usa</h2>
 *
 * <p>`T` no aparece en ningún miembro. No es un descuido: existe para que
 * `SimpleType&lt;Integer&gt;` y `SimpleType&lt;String&gt;` sean tipos distintos en el compilador y
 * `ArrayType.getArrayType(SimpleType.INTEGER)` pueda devolver `ArrayType&lt;Integer[]&gt;`. En
 * ejecución se borra, y por eso {@link #isValue} recibe `Object` y no `T`.
 */
public abstract class OpenType<T> implements Serializable {

    private static final long serialVersionUID = -9195195325186646468L;

    /**
     * Los nombres de clase que un tipo abierto puede tener.
     *
     * <p>Son los ocho envoltorios, `Void`, `String`, `Date`, `BigDecimal`, `BigInteger`,
     * `ObjectName`, y las tres formas compuestas. Es una lista de sólo lectura.
     */
    public static final List<String> ALLOWED_CLASSNAMES_LIST =
            Collections.unmodifiableList(allowedClassNames());

    /**
     * Lo mismo que {@link #ALLOWED_CLASSNAMES_LIST}, como arreglo.
     *
     * <p>Un arreglo público es modificable por quien lo recibe, así que este campo **se puede
     * ensuciar**. Está igual porque el JDK lo declara así y sacarlo sería romper el contrato; el
     * que quiera leer sin riesgo tiene la lista de al lado, que sí es de sólo lectura.
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
     * Un tipo abierto con esos tres nombres.
     *
     * <p>`className` puede llevar corchetes al principio (`[Ljava.lang.String;`) para nombrar un
     * arreglo; lo que tiene que estar en {@link #ALLOWED_CLASSNAMES_LIST} es el tipo del fondo.
     *
     * @throws OpenDataException si `className` no nombra un tipo abierto
     * @throws IllegalArgumentException si alguno de los tres es nulo o vacío
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
            // Un arreglo de referencias se escribe `[[Ljava.lang.String;`; uno de primitivos, `[[I`.
            // Los dos son nombres de clase válidos y hay que aceptarlos, pero el que se compara
            // contra la lista es el tipo del fondo, y sólo el de referencias lo tiene escrito.
            if (base.startsWith("L") && base.endsWith(";")) {
                base = base.substring(1, base.length() - 1);
            } else if (!PRIMITIVES.contains(base)) {
                throw new OpenDataException("no es un name de arreglo válido: " + className);
            }
        }
        if (!PRIMITIVES.contains(base) && !ALLOWED_CLASSNAMES_LIST.contains(base)) {
            throw new OpenDataException(className + " no es un tipo abierto");
        }
        this.className = className;
        this.typeName = typeName;
        this.description = description;
        this.isArray = brackets > 0;
    }

    // El constructor de paquete que usan las subclases que YA saben que su nombre es válido
    // (`SimpleType` con sus quince constantes, `ArrayType` con el que se armó él mismo). Se saltea
    // la validación, no la repite: repetirla sería trabajo y además obligaría a `SimpleType` a
    // declarar una excepción verificada en un inicializador estático, donde no se puede atrapar.
    OpenType(String className, String typeName, String description, boolean isArray) {
        this.className = className;
        this.typeName = typeName;
        this.description = description;
        this.isArray = isArray;
    }

    private static final List<String> PRIMITIVES = Collections.unmodifiableList(
            Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D",
                    "boolean", "char", "byte", "short", "int", "long", "float", "double"));

    private static void requireNonBlank(String s, String cual) {
        if (s == null || s.trim().length() == 0) {
            throw new IllegalArgumentException(cual + " no puede ser nulo ni vacío");
        }
    }

    /** El nombre binario de la clase de los valores de este tipo. */
    public String getClassName() {
        return this.className;
    }

    /** El nombre que identifica a este tipo. */
    public String getTypeName() {
        return this.typeName;
    }

    /** La descripción, para una persona. */
    public String getDescription() {
        return this.description;
    }

    /** Si los valores de este tipo son arreglos. */
    public boolean isArray() {
        return this.isArray;
    }

    /** Si `obj` es un valor de este tipo. Un nulo nunca lo es. */
    public abstract boolean isValue(Object obj);

    public abstract boolean equals(Object obj);

    public abstract int hashCode();

    public abstract String toString();
}
