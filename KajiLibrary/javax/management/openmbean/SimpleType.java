package javax.management.openmbean;

import java.io.ObjectStreamException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import javax.management.ObjectName;

/**
 * Los quince tipos abiertos que no se componen de nada: los envoltorios, `String`, `Date`,
 * `BigDecimal`, `BigInteger`, `ObjectName` y `Void`.
 *
 * <p>No hay constructor público y no puede haberlo: las quince constantes de acá **son** todos los
 * tipos simples que existen, y dejar fabricar uno más permitiría dos objetos distintos para
 * `java.lang.Integer`. Eso importa porque el resto del paquete los compara por identidad en los
 * caminos rápidos, aunque {@link #equals} también funcione.
 *
 * <p>De ahí `readResolve`: al deserializar, una constante volvería como un objeto nuevo y la
 * identidad se rompería en silencio --que es la peor forma de romperse--. `readResolve` devuelve la
 * constante que corresponde y la propiedad se mantiene.
 *
 * <p>`VOID` está por completitud de la enumeración: es el tipo de retorno de una operación que no
 * devuelve nada. Ningún valor es de tipo `VOID`, así que su {@link #isValue} es siempre `false` --lo
 * cual es correcto y no un caso sin implementar--.
 */
public final class SimpleType<T> extends OpenType<T> {

    private static final long serialVersionUID = 2215577471957694503L;

    /** El tipo de lo que no tiene valor. */
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

    // El orden importa: `readResolve` recorre este arreglo, así que una constante que falte acá
    // volvería de la deserialización como un objeto distinto del que se serializó.
    private static final SimpleType<?>[] ALL = new SimpleType<?>[] {
        VOID, BOOLEAN, CHARACTER, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, STRING,
        BIGDECIMAL, BIGINTEGER, DATE, OBJECTNAME };

    // Los tres nombres de un tipo simple son el mismo: no hay nada que elegir, y por eso el
    // constructor toma uno solo.
    private SimpleType(String className) {
        super(className, className, className, false);
    }

    /** Si `obj` es una instancia de la clase de este tipo. Un nulo nunca lo es. */
    public boolean isValue(Object obj) {
        if (obj == null) {
            return false;
        }
        return obj.getClass().getName().equals(this.getClassName());
    }

    /**
     * Igualdad por el nombre de clase.
     *
     * <p>Alcanza con eso porque los otros dos nombres de un tipo simple son iguales al primero.
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
     * Devuelve la constante correspondiente en vez del objeto recién deserializado.
     *
     * <p>Ver la nota de la clase: sin esto, un `SimpleType` que viaja por serialización deja de ser
     * idéntico a la constante y las comparaciones por identidad empiezan a fallar en silencio.
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
