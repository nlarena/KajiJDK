package jdk.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * KajiLibrary's jdk.internal.reflect.ReflectionFactory — la fabrica de los tres accesores.
 *
 * <h2>Que hay y por que alcanza</h2>
 *
 * <p>Los tres {@code new*Accessor} estan, y devuelven accesores que <strong>funcionan</strong>: cada
 * uno delega en la maquinaria reflexiva que esta VM ya tiene (los intrinsecos
 * {@code Intrinsic::MethodInvoke} y {@code Intrinsic::ConstructorNewInstance} del interprete, y las
 * costuras nativas de {@link Field}). No son una segunda plomeria en paralelo a la que anda: son la
 * forma en que esa plomeria se deja nombrar desde una interfaz.
 *
 * <p>{@link #newInstance(Constructor, Object[], Class)} esta por lo mismo, y es el unico miembro de
 * esta clase que el JDK escribe en terminos de los accesores en vez de al reves.
 *
 * <h2>Que no esta, y por que no puede estar</h2>
 *
 * <p>Son tres bloqueos, y ninguno se arregla escribiendo mas Java aca.
 *
 * <ul>
 * <li><strong>Las cuatro copias</strong> —{@code copyMethod}, {@code leafCopyMethod},
 *     {@code copyField}, {@code copyConstructor}— y los dos accesos crudos
 *     ({@code getExecutableTypeAnnotationBytes}, {@code getExecutableSharedParameterTypes}). El JDK
 *     los escribe con miembros <em>package-private</em> de {@code java.lang.reflect}
 *     ({@code Method.copy()}, {@code Executable.getSharedParameterTypes()}), que solo existen porque
 *     esa fabrica y esas clases estan del mismo lado de la frontera de paquete. Desde aca no hay
 *     manera publica de fabricar un {@link Method} ni un {@link Field}: los construye el VM. Traerlos
 *     no seria implementar esta clase sino cambiar {@code java.lang.reflect}.</li>
 * <li><strong>Toda la mitad de serializacion</strong> —los siete {@code *ForSerialization} que
 *     devuelven {@link java.lang.invoke.MethodHandle}, los tres
 *     {@code newConstructorFor(Externalization|Serialization)},
 *     {@code hasStaticInitializerForSerialization},
 *     {@code newOptionalDataExceptionForSerialization} y {@code serialPersistentFields}—. Necesita
 *     dos cosas que no estan: un {@code MethodHandles.Lookup} con acceso privado sobre una clase
 *     ajena, que es un privilegio del VM y no un metodo, y {@code java.io.ObjectStreamField}, que no
 *     esta en esta biblioteca. Todos ellos <em>fabrican</em> algo, asi que no hay version honesta que
 *     devuelva menos: o construyen el handle o mienten.</li>
 * <li><strong>{@code parseAccessFlags(int, AccessFlag.Location, Class)}</strong>. Existe la forma de
 *     dos argumentos ({@code AccessFlag.maskToAccessFlags}), pero el tercero no es decorativo: es lo
 *     que desambigua los bits que significan cosas distintas segun que clase los lleve. Delegar en la
 *     de dos y tirar la clase daria la respuesta correcta casi siempre, y "casi siempre" es
 *     exactamente la clase de miembro que no se escribe.</li>
 * </ul>
 *
 * <p>El criterio es el de la casa: un miembro que falta es un subconjunto legal y no compila del otro
 * lado; uno que miente compila y revienta despues.
 */
public class ReflectionFactory {

    // El JDK tambien la esconde: nadie fabrica una fabrica, se pide la que hay. Ser `private` es lo
    // que hace que `getReflectionFactory()` pueda prometer identidad.
    private static final ReflectionFactory LA_UNICA = new ReflectionFactory();

    private ReflectionFactory() {
    }

    /** La fabrica del proceso. Siempre la misma instancia. */
    public static ReflectionFactory getReflectionFactory() {
        return ReflectionFactory.LA_UNICA;
    }

    /**
     * Un accesor para {@code field}.
     *
     * @param field el campo
     * @param override si quien lo pide ya suprimio el control de acceso; con {@code false} un campo
     *                 {@code final} sale de solo lectura y sus escritores tiran
     * @return el accesor
     */
    public FieldAccessor newFieldAccessor(Field field, boolean override) {
        return new AccesorDeCampo(field, override);
    }

    /**
     * Un accesor para {@code method}.
     *
     * @param method el metodo
     * @param callerSensitive si el destino mira quien lo llamo. En esta VM no hay metodos asi —no
     *                        existe el gancho que lo haria posible, ver {@link Reflection}— y el
     *                        accesor no cambia de forma por el.
     * @return el accesor
     */
    public MethodAccessor newMethodAccessor(Method method, boolean callerSensitive) {
        return new AccesorDeMetodo(method);
    }

    /**
     * Un accesor para {@code c}.
     *
     * @param c el constructor
     * @return el accesor
     */
    public ConstructorAccessor newConstructorAccessor(Constructor<?> c) {
        return new AccesorDeConstructor(c);
    }

    /**
     * Construye una instancia con {@code ctor}, diciendo quien llama.
     *
     * <p>El {@code caller} es para el chequeo de acceso, que en el JDK se hace aca y no en el accesor.
     * Esta VM no lo hace en ninguno de los dos lados —{@code Constructor.newInstance} tampoco lo
     * hace—, asi que el argumento se acepta y no cambia nada; es la misma situacion que el
     * {@code caller} de {@link MethodAccessor#invoke(Object, Object[], Class)}.
     *
     * @param <T> el tipo construido
     * @param ctor el constructor
     * @param args los argumentos
     * @param caller quien dice construir
     * @return la instancia
     * @throws IllegalAccessException si el chequeo de acceso fallara
     * @throws InstantiationException si la clase no se puede instanciar
     * @throws InvocationTargetException envolviendo lo que haya tirado el constructor
     */
    public <T> T newInstance(Constructor<T> ctor, Object[] args, Class<?> caller)
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        return (T) this.newConstructorAccessor(ctor).newInstance(args);
    }
}
