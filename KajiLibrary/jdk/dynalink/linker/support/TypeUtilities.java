package jdk.dynalink.linker.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Las reglas de conversion entre tipos de Java, escritas para que un enlazador pueda consultarlas.
 *
 * <h2>Por que hace falta preguntarselo a alguien</h2>
 *
 * <p>Porque el compilador de Java aplica estas reglas en tiempo de compilacion y despues no quedan
 * en ningun lado. Un enlazador dinamico decide en tiempo de ejecucion si un argumento entra en un
 * parametro, y para eso necesita las mismas reglas como <strong>datos</strong>.
 * {@code Class.isAssignableFrom} solo cubre las referencias; todo lo que tenga que ver con
 * primitivos, encajonado o ampliacion numerica queda afuera.
 *
 * <h2>Las tres preguntas, que no son la misma</h2>
 *
 * <p>{@link #isSubtype} es la relacion de subtipo de la JLS 4.10: incluye los primitivos, donde
 * {@code int} es subtipo de {@code long}, pero <strong>no</strong> incluye el encajonado — un
 * {@code int} no es subtipo de {@code Integer}.
 *
 * <p>{@link #isMethodInvocationConvertible} es la de la JLS 5.3, que es la que decide si una
 * llamada compila: agrega encajonar y desencajonar. Es mas permisiva que la anterior.
 *
 * <p>{@link #isConvertibleWithoutLoss} es mas <strong>restrictiva</strong> que las dos: pregunta si
 * el valor sobrevive intacto. {@code long} a {@code double} es conversion valida y pierde
 * informacion, porque la mantisa de un {@code double} tiene 53 bits y un {@code long} tiene 64.
 *
 * <p>Las tres se necesitan para cosas distintas: la segunda para saber si una sobrecarga es
 * aplicable, la tercera para decidir si conviene recortar el valor de retorno de un enlace.
 *
 * @since 9
 */
public final class TypeUtilities {

    /** De primitivo a su caja. Incluye {@code void}, que tiene {@code Void}. */
    private static final Map<Class<?>, Class<?>> CAJAS;
    /** La inversa. */
    private static final Map<Class<?>, Class<?>> PRIMITIVOS;
    /** Por nombre: {@code "int"} a {@code int.class}. */
    private static final Map<String, Class<?>> POR_NOMBRE;

    static {
        // IdentityHashMap y no HashMap: las claves son objetos Class, que son unicos por
        // definicion. Comparar por identidad evita llamar a hashCode y equals de Class.
        final Map<Class<?>, Class<?>> cajas = new IdentityHashMap<Class<?>, Class<?>>(9);
        cajas.put(Void.TYPE, Void.class);
        cajas.put(Boolean.TYPE, Boolean.class);
        cajas.put(Byte.TYPE, Byte.class);
        cajas.put(Character.TYPE, Character.class);
        cajas.put(Short.TYPE, Short.class);
        cajas.put(Integer.TYPE, Integer.class);
        cajas.put(Long.TYPE, Long.class);
        cajas.put(Float.TYPE, Float.class);
        cajas.put(Double.TYPE, Double.class);
        CAJAS = Collections.unmodifiableMap(cajas);

        final Map<Class<?>, Class<?>> primitivos = new IdentityHashMap<Class<?>, Class<?>>(9);
        final Map<String, Class<?>> porNombre = new HashMap<String, Class<?>>(9);
        for (final Map.Entry<Class<?>, Class<?>> e : cajas.entrySet()) {
            primitivos.put(e.getValue(), e.getKey());
            porNombre.put(e.getKey().getName(), e.getKey());
        }
        PRIMITIVOS = Collections.unmodifiableMap(primitivos);
        POR_NOMBRE = Collections.unmodifiableMap(porNombre);
    }

    private TypeUtilities() {
    }

    /**
     * Si un valor de {@code sourceType} se puede pasar donde se espera {@code targetType}.
     *
     * <p>Es la conversion por invocacion de metodo de la JLS 5.3: identidad, ampliacion de
     * primitivo, ampliacion de referencia, encajonado y desencajonado.
     *
     * @param sourceType el tipo del valor
     * @param targetType el tipo del parametro
     * @return si la llamada es legal
     */
    public static boolean isMethodInvocationConvertible(final Class<?> sourceType,
            final Class<?> targetType) {
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }
        if (sourceType.isPrimitive()) {
            if (targetType.isPrimitive()) {
                return esSubtipoPropio(sourceType, targetType);
            }
            return encajonaYAmplia(sourceType, targetType);
        }
        if (targetType.isPrimitive()) {
            // Desencajonar y despues ampliar el primitivo. Solo sale de una caja exacta: un
            // Number generico no se desencajona, porque en tiempo de ejecucion podria ser
            // cualquiera de las ocho.
            final Class<?> desencajonado = PRIMITIVOS.get(sourceType);
            return desencajonado != null
                    && (desencajonado == targetType || esSubtipoPropio(desencajonado, targetType));
        }
        return false;
    }

    /**
     * Si un valor de {@code sourceType} entra en {@code targetType} sin perder informacion.
     *
     * @param sourceType el tipo del valor
     * @param targetType el tipo de llegada
     * @return si la conversion es exacta
     */
    public static boolean isConvertibleWithoutLoss(final Class<?> sourceType,
            final Class<?> targetType) {
        // Que el destino sea void basta: el valor se descarta, y descartarlo no pierde nada que
        // alguien vaya a mirar despues. Vale hasta para boolean, que no se convierte a nada mas.
        if (targetType.isAssignableFrom(sourceType) || targetType == void.class) {
            return true;
        }
        if (sourceType.isPrimitive()) {
            if (sourceType == void.class) {
                // Al reves no: de void solo sale el null, y el unico tipo que lo recibe entero
                // es Object. Ni siquiera Void, que ademas de null admitiria una instancia.
                return targetType == Object.class;
            }
            if (targetType.isPrimitive()) {
                return ensanchaSinPerdida(sourceType, targetType);
            }
            return encajonaYAmplia(sourceType, targetType);
        }
        // De referencia a primitivo nunca, aunque la caja sea la exacta: el null no tiene donde ir.
        return false;
    }

    /**
     * La relacion de subtipo de la JLS 4.10, primitivos incluidos.
     *
     * <p>No incluye el encajonado: {@code int} no es subtipo de {@code Integer}. Para eso esta
     * {@link #isMethodInvocationConvertible}.
     *
     * @param subType el candidato a subtipo
     * @param superType el candidato a supertipo
     * @return si el primero es subtipo del segundo
     */
    public static boolean isSubtype(final Class<?> subType, final Class<?> superType) {
        // Cubre clases, interfaces y arreglos, y tambien la identidad entre primitivos.
        if (superType.isAssignableFrom(subType)) {
            return true;
        }
        if (superType.isPrimitive() && subType.isPrimitive()) {
            return esSubtipoPropio(subType, superType);
        }
        return false;
    }

    /**
     * Encajonar y despues ampliar la referencia, que la JLS 5.3 cuenta como un solo paso.
     *
     * <p>El origen ya se sabe primitivo cuando se llega aca, asi que la caja existe siempre.
     */
    private static boolean encajonaYAmplia(final Class<?> sourceType, final Class<?> targetType) {
        return targetType.isAssignableFrom(CAJAS.get(sourceType));
    }

    /**
     * El subtipado entre primitivos de la JLS 4.10.1, sin la identidad.
     *
     * <p>La cadena es {@code double > float > long > int > {char, short} > byte}, cerrada por
     * transitividad, y coincide con la ampliacion de primitivo de la JLS 5.1.2 — por eso un solo
     * metodo contesta las dos preguntas.
     *
     * <p>Los llamadores ya descartaron la identidad antes de llegar, asi que este metodo no la
     * vuelve a mirar. Eso tiene una consecuencia visible: con un tipo que no esta en la cadena,
     * como {@code void}, las ramas escritas por negacion contestan que si. {@code byte} resulta
     * subtipo de {@code void} y {@code int} no. Es un artefacto de como esta escrita la tabla, y
     * se reproduce a proposito porque es lo que el JDK contesta.
     */
    private static boolean esSubtipoPropio(final Class<?> subType, final Class<?> superType) {
        if (superType == boolean.class || subType == boolean.class) {
            return false;
        }
        // Los tres chicos se escriben por lo que NO alcanzan, y los tres grandes por lo que si.
        // No es capricho: byte no llega a char porque char no tiene signo, y char no llega a
        // byte ni a short porque ellos no llegan a 65535. Fuera de ese triangulo, todo sube.
        if (subType == byte.class) {
            return superType != char.class;
        }
        if (subType == char.class) {
            return superType != short.class && superType != byte.class;
        }
        if (subType == short.class) {
            return superType != char.class && superType != byte.class;
        }
        if (subType == int.class) {
            return superType == long.class || superType == float.class
                    || superType == double.class;
        }
        if (subType == long.class) {
            return superType == float.class || superType == double.class;
        }
        if (subType == float.class) {
            return superType == double.class;
        }
        return false;
    }

    /**
     * Las ampliaciones que la JLS 5.1.2 marca como exactas.
     *
     * <p>Son las de {@link #esSubtipoPropio} menos tres: {@code int} a {@code float},
     * {@code long} a {@code float} y {@code long} a {@code double}. En esos casos la mantisa del
     * destino no alcanza para todos los valores del origen y el resultado se redondea — sigue
     * siendo una conversion legal, pero ya no es el mismo numero.
     */
    private static boolean ensanchaSinPerdida(final Class<?> de, final Class<?> a) {
        if (a == boolean.class || de == boolean.class) {
            return false;
        }
        // char queda afuera en las dos direcciones, y esa es la sorpresa de esta tabla: char a
        // int conserva todos los bits. Lo que no conserva es el significado — un caracter pasa a
        // ser el numero de su punto de codigo — y esta pregunta es sobre el valor, no sobre los
        // bits. La JLS llama a esa conversion ampliacion; el JDK no la llama exacta.
        if (a == char.class || de == char.class) {
            return false;
        }
        if (de == byte.class) {
            return true;
        }
        if (de == short.class) {
            return a != byte.class;
        }
        if (de == int.class) {
            // int a float no: la mantisa de un float tiene 24 bits y el int tiene 32.
            return a == long.class || a == double.class;
        }
        if (de == float.class) {
            return a == double.class;
        }
        // long no llega exacto ni a float ni a double: le sobran bits contra las dos mantisas.
        return false;
    }

    /**
     * El primitivo que se llama asi, o {@code null}.
     *
     * <p>{@code "void"} cuenta: es el nombre de {@code void.class}.
     *
     * @param name el nombre, por ejemplo {@code "int"}
     * @return el {@code Class} del primitivo, o {@code null} si el nombre no es de uno
     */
    public static Class<?> getPrimitiveTypeByName(final String name) {
        return POR_NOMBRE.get(name);
    }

    /**
     * El primitivo que hay dentro de una caja, o {@code null} si no es una caja.
     *
     * @param wrapperType la caja, por ejemplo {@code Integer.class}
     * @return el primitivo, o {@code null}
     */
    public static Class<?> getPrimitiveType(final Class<?> wrapperType) {
        return PRIMITIVOS.get(wrapperType);
    }

    /**
     * La caja de un primitivo, o {@code null} si el tipo no es primitivo.
     *
     * @param primitiveType el primitivo, por ejemplo {@code int.class}
     * @return la caja, o {@code null}
     */
    public static Class<?> getWrapperType(final Class<?> primitiveType) {
        return CAJAS.get(primitiveType);
    }

    /**
     * Si el tipo es una de las nueve cajas.
     *
     * @param type el tipo
     * @return si es una caja
     */
    public static boolean isWrapperType(final Class<?> type) {
        return PRIMITIVOS.containsKey(type);
    }
}
