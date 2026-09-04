package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.dynalink.linker.LinkerServices;

/**
 * Fabrica las guardas mas comunes: "el receptor es de esta clase", "no es nulo", "es un arreglo".
 *
 * <h2>Que es una guarda, en concreto</h2>
 *
 * <p>Un metodo que devuelve {@code boolean} y toma un prefijo de los argumentos de la invocacion
 * que protege. Se evalua en cada llamada, asi que tiene que ser barato: comparar un puntero a
 * {@code Class}, o comparar contra {@code null}. Cualquier cosa mas cara que eso deberia ser un
 * switch point y no una guarda.
 *
 * <h2>Las guardas que no hacen falta</h2>
 *
 * <p>Varios metodos de aca miran la firma del sitio y descubren que la pregunta ya esta contestada.
 * Si el sitio declara el parametro como {@code String} y se pide una guarda de "es un
 * {@code String}", el chequeo sobra: el verificador de la JVM ya lo garantiza. Si se pide una
 * guarda de "es un {@code Integer}" sobre un parametro declarado {@code String}, la guarda nunca
 * puede dar verdadero.
 *
 * <p>En los dos casos devuelven una constante en vez de un chequeo, y dejan un aviso en el
 * registro. La constante es la respuesta correcta; el aviso esta porque casi siempre significa que
 * el enlazador que la pidio se equivoco de firma, y sin el aviso eso no se notaria nunca.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Las decisiones de arriba son reales y ocurren. Lo que todavia no hay es el fabricante de
 * handles: {@code MethodHandles} no puede construir uno sin soporte de la VM, asi que cualquier
 * metodo de aca termina en {@link UnsupportedOperationException} al llegar a ese punto.
 *
 * <p>Los handles de los chequeos se resuelven la primera vez que se los usa y no en el
 * inicializador estatico, que es donde los resuelve el JDK. La diferencia es solo cual excepcion
 * sale: asi sale la que nombra lo que falta, en lugar de un {@code ExceptionInInitializerError}
 * que la envuelve y hace que la clase quede inutilizable.
 *
 * @since 9
 */
public final class Guards {

    private static final Logger LOG = Logger.getLogger(Guards.class.getName());

    private Guards() {
    }

    /**
     * Los handles de los chequeos, resueltos una sola vez y a demanda.
     *
     * <p>Es el modismo del titular de inicializacion bajo demanda: la JVM garantiza que esta clase
     * se inicializa la primera vez que se la toca y solo una vez, sin candado explicito.
     */
    private static final class Handles {
        static final MethodHandle IS_OF_CLASS = propio("esDeClase", Class.class, Object.class);
        static final MethodHandle IS_INSTANCE = propio("esInstancia", Class.class, Object.class);
        static final MethodHandle IS_ARRAY = propio("esArreglo", Object.class);
        static final MethodHandle IS_NULL = propio("esNulo", Object.class);
        static final MethodHandle IS_NOT_NULL = propio("noEsNulo", Object.class);
        static final MethodHandle IS_IDENTICAL = propio("esElMismo", Object.class, Object.class);

        private static MethodHandle propio(final String nombre, final Class<?>... params) {
            return Lookup.findOwnStatic(MethodHandles.lookup(), nombre, Boolean.TYPE, params);
        }
    }

    // Los chequeos propiamente dichos. Son metodos comunes: lo unico que Guards agrega es
    // envolverlos en un handle de la forma que el sitio de invocacion necesita.

    @SuppressWarnings("unused")
    private static boolean esDeClase(final Class<?> clazz, final Object obj) {
        return obj != null && obj.getClass() == clazz;
    }

    @SuppressWarnings("unused")
    private static boolean esInstancia(final Class<?> clazz, final Object obj) {
        return clazz.isInstance(obj);
    }

    @SuppressWarnings("unused")
    private static boolean esArreglo(final Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    @SuppressWarnings("unused")
    private static boolean esNulo(final Object obj) {
        return obj == null;
    }

    @SuppressWarnings("unused")
    private static boolean noEsNulo(final Object obj) {
        return obj != null;
    }

    @SuppressWarnings("unused")
    private static boolean esElMismo(final Object obj1, final Object obj2) {
        return obj1 == obj2;
    }

    /**
     * Una guarda de "el primer argumento es exactamente de esta clase", no de una subclase.
     *
     * @param clazz la clase exacta
     * @param type la firma del sitio
     * @return la guarda, o una constante si la respuesta ya se sabe
     */
    public static MethodHandle isOfClass(final Class<?> clazz, final MethodType type) {
        final Class<?> declarado = type.parameterType(0);
        if (clazz == declarado) {
            avisar("la guarda de clase exacta sobre {0} siempre da verdadero en {1}", clazz, type);
            return constante(true, type);
        }
        if (!declarado.isAssignableFrom(clazz)) {
            avisar("la guarda de clase exacta sobre {0} nunca puede dar verdadero en {1}",
                    clazz, type);
            return constante(false, type);
        }
        return ligada(Handles.IS_OF_CLASS, clazz, 0, type);
    }

    /**
     * Una guarda de "el primer argumento es instancia de esta clase", subclases incluidas.
     *
     * @param clazz la clase
     * @param type la firma del sitio
     * @return la guarda, o una constante si la respuesta ya se sabe
     */
    public static MethodHandle isInstance(final Class<?> clazz, final MethodType type) {
        return isInstance(clazz, 0, type);
    }

    /**
     * Una guarda de "el argumento en esa posicion es instancia de esta clase".
     *
     * @param clazz la clase
     * @param pos la posicion del argumento
     * @param type la firma del sitio
     * @return la guarda, o una constante si la respuesta ya se sabe
     */
    public static MethodHandle isInstance(final Class<?> clazz, final int pos,
            final MethodType type) {
        final Class<?> declarado = type.parameterType(pos);
        if (clazz.isAssignableFrom(declarado)) {
            avisar("la guarda de instancia de {0} siempre da verdadero en {1}", clazz, type);
            return constante(true, type);
        }
        if (!declarado.isAssignableFrom(clazz)) {
            avisar("la guarda de instancia de {0} nunca puede dar verdadero en {1}", clazz, type);
            return constante(false, type);
        }
        return ligada(Handles.IS_INSTANCE, clazz, pos, type);
    }

    /**
     * Una guarda de "el argumento en esa posicion es un arreglo".
     *
     * @param pos la posicion del argumento
     * @param type la firma del sitio
     * @return la guarda, o una constante si la respuesta ya se sabe
     */
    public static MethodHandle isArray(final int pos, final MethodType type) {
        final Class<?> declarado = type.parameterType(pos);
        if (declarado.isArray()) {
            avisar("la guarda de arreglo siempre da verdadero en la posicion {0} de {1}",
                    Integer.valueOf(pos), type);
            return constante(true, type);
        }
        // Object[] se usa como piso: si ni siquiera un arreglo de objetos entra en el tipo
        // declarado, ningun arreglo puede llegar ahi.
        if (!declarado.isAssignableFrom(Object[].class)) {
            avisar("la guarda de arreglo nunca puede dar verdadero en la posicion {0} de {1}",
                    Integer.valueOf(pos), type);
            return constante(false, type);
        }
        return enPosicion(Handles.IS_ARRAY, pos, type);
    }

    /**
     * Adapta una guarda a la firma de un sitio.
     *
     * <p>La firma que le corresponde es la del sitio recortada a los parametros que la guarda mira
     * —puede mirar menos— y devolviendo {@code boolean}.
     *
     * @param test la guarda
     * @param type la firma del sitio
     * @return la guarda adaptada
     */
    public static MethodHandle asType(final MethodHandle test, final MethodType type) {
        return test.asType(tipoDeGuarda(test, type));
    }

    /**
     * Adapta una guarda a la firma de un sitio, con las conversiones de los lenguajes.
     *
     * @param linkerServices los servicios que aportan esas conversiones
     * @param test la guarda
     * @param type la firma del sitio
     * @return la guarda adaptada
     */
    public static MethodHandle asType(final LinkerServices linkerServices, final MethodHandle test,
            final MethodType type) {
        return linkerServices.asType(test, tipoDeGuarda(test, type));
    }

    /**
     * Una guarda de "es exactamente de esta clase", de firma {@code (Object)boolean}.
     *
     * @param clazz la clase
     * @return la guarda
     */
    public static MethodHandle getClassGuard(final Class<?> clazz) {
        return Handles.IS_OF_CLASS.bindTo(clazz);
    }

    /**
     * Una guarda de "es instancia de esta clase", de firma {@code (Object)boolean}.
     *
     * @param clazz la clase
     * @return la guarda
     */
    public static MethodHandle getInstanceOfGuard(final Class<?> clazz) {
        return Handles.IS_INSTANCE.bindTo(clazz);
    }

    /**
     * Una guarda de "es este objeto y no otro", por identidad.
     *
     * <p>Sirve para enlazar contra un objeto en particular en vez de contra una clase, que es lo
     * que hace falta en un lenguaje donde los metodos viven en la instancia.
     *
     * @param obj el objeto
     * @return la guarda
     */
    public static MethodHandle getIdentityGuard(final Object obj) {
        return Handles.IS_IDENTICAL.bindTo(obj);
    }

    /**
     * Una guarda de "es nulo", de firma {@code (Object)boolean}.
     *
     * @return la guarda
     */
    public static MethodHandle isNull() {
        return Handles.IS_NULL;
    }

    /**
     * Una guarda de "no es nulo", de firma {@code (Object)boolean}.
     *
     * @return la guarda
     */
    public static MethodHandle isNotNull() {
        return Handles.IS_NOT_NULL;
    }

    private static MethodType tipoDeGuarda(final MethodHandle test, final MethodType type) {
        return type.dropParameterTypes(test.type().parameterCount(), type.parameterCount())
                .changeReturnType(Boolean.TYPE);
    }

    /** Fija la clase como primer argumento del chequeo y lo coloca en la posicion pedida. */
    private static MethodHandle ligada(final MethodHandle test, final Class<?> clazz,
            final int pos, final MethodType type) {
        return enPosicion(test.bindTo(clazz), pos, type);
    }

    /**
     * Un chequeo unario puesto a mirar el argumento {@code pos} de un sitio de firma {@code type}.
     *
     * <p>El reordenamiento es lo que hace el trabajo: {@code permuteArguments} con el arreglo
     * {@code {pos}} dice que el unico parametro del chequeo se alimenta del argumento {@code pos},
     * y que todos los demas se descartan.
     */
    private static MethodHandle enPosicion(final MethodHandle test, final int pos,
            final MethodType type) {
        return MethodHandles.permuteArguments(
                test.asType(test.type().changeParameterType(0, type.parameterType(pos))),
                type.changeReturnType(Boolean.TYPE), new int[] { pos });
    }

    /** Una guarda que ignora sus argumentos y siempre contesta lo mismo. */
    private static MethodHandle constante(final boolean valor, final MethodType type) {
        return MethodHandles.permuteArguments(
                MethodHandles.constant(Boolean.TYPE, Boolean.valueOf(valor)),
                type.changeReturnType(Boolean.TYPE), new int[0]);
    }

    private static void avisar(final String mensaje, final Object a, final Object b) {
        if (LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, mensaje, new Object[] { a, b });
        }
    }
}
