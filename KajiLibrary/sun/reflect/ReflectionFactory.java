package sun.reflect;

import java.io.Externalizable;
import java.io.ObjectStreamField;
import java.io.OptionalDataException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Los ganchos de serializacion que necesita una biblioteca que serializa por su cuenta.
 *
 * <p>Existe para una sola clase de cliente: el que reimplementa el formato de
 * {@code ObjectOutputStream} --un ORB de CORBA, un marco de persistencia-- y necesita hacer las
 * mismas cosas que hace el JDK y que **ningun** API publico permite: construir un objeto sin correr
 * su constructor, llamar a un {@code readObject} privado, saber si una clase tiene inicializador
 * estatico.
 *
 * <p>Por eso el paquete se llama `sun.` y sigue exportado: no es API para cualquiera, pero
 * quitarlo romperia a esos clientes, que no tienen sustituto.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>La mayor parte de esta clase **no se puede implementar desde Java**. Los tres agujeros:
 *
 * <ul>
 *   <li>Los `newConstructorForSerialization` fabrican un {@link Constructor} que, al invocarse,
 *       reserva una instancia de una clase y corre el constructor de **otra**. Eso lo hace la VM
 *       con un accesor generado; no hay forma de expresarlo en Java.
 *   <li>Los que devuelven {@link MethodHandle} necesitan una busqueda con acceso privado sobre una
 *       clase ajena ({@code MethodHandles.privateLookupIn}), que es justamente el permiso que el
 *       lenguaje no da.
 *   <li>{@link #hasStaticInitializerForSerialization} pregunta si una clase tiene {@code <clinit>},
 *       que la reflexion no expone: {@code <clinit>} no es un {@link java.lang.reflect.Method}.
 * </ul>
 *
 * <p>Esos diez lanzan {@link UnsupportedOperationException} con el motivo. Devolver `null` --que es
 * lo que el JDK devuelve cuando la clase de verdad no tiene el metodo-- seria peor: el cliente lo
 * leeria como "esta clase no define `readObject`" y seguiria de largo serializando mal, en vez de
 * enterarse.
 *
 * <p>Los tres que si se pueden estan hechos de verdad:
 * {@link #getReflectionFactory}, {@link #newConstructorForExternalization} --que es solo buscar el
 * constructor publico sin argumentos-- y {@link #serialPersistentFields}, que es leer un campo
 * estatico.
 */
public class ReflectionFactory {

    private static final ReflectionFactory soleInstance = new ReflectionFactory();

    /**
     * La de adentro, la que en el JDK hace el trabajo. Aca no tiene los metodos de serializacion
     * --ver la nota de la clase-- pero se conserva el campo porque es donde iria el puente el dia
     * que los tenga.
     */
    private static final jdk.internal.reflect.ReflectionFactory delegate =
            jdk.internal.reflect.ReflectionFactory.getReflectionFactory();

    private ReflectionFactory() {
    }

    /** La unica instancia. */
    public static ReflectionFactory getReflectionFactory() {
        return soleInstance;
    }

    /**
     * Un constructor que reserva una instancia de `cl` y corre el cuerpo de `constructorToCall`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public Constructor<?> newConstructorForSerialization(Class<?> cl,
            Constructor<?> constructorToCall) {
        throw new UnsupportedOperationException(
                "cannot synthesize a serialization constructor for " + nombre(cl)
                + ": allocating an instance without running its constructor needs VM support");
    }

    /**
     * Como {@link #newConstructorForSerialization(Class, Constructor)}, tomando el constructor sin
     * argumentos de la superclase no serializable.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final Constructor<?> newConstructorForSerialization(Class<?> cl) {
        throw new UnsupportedOperationException(
                "cannot synthesize a serialization constructor for " + nombre(cl)
                + ": allocating an instance without running its constructor needs VM support");
    }

    /**
     * El constructor publico sin argumentos que {@link Externalizable} exige.
     *
     * <p>Este si esta hecho: no tiene nada de magico, es el constructor que la propia clase
     * declara. El JDK lo devuelve accesible aunque la clase no lo sea, y eso tambien.
     *
     * @return el constructor, o `null` si la clase no lo tiene
     * @throws NullPointerException si `cl` es nulo
     */
    public final Constructor<?> newConstructorForExternalization(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        if (!Externalizable.class.isAssignableFrom(cl)) {
            return null;
        }
        try {
            Constructor<?> c = cl.getDeclaredConstructor();
            c.setAccessible(true);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Un asa al {@code readObject} privado de `cl`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle readObjectForSerialization(Class<?> cl) {
        throw noHayAsa("readObject", cl);
    }

    /**
     * Un asa al {@code readObjectNoData} privado de `cl`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle readObjectNoDataForSerialization(Class<?> cl) {
        throw noHayAsa("readObjectNoData", cl);
    }

    /**
     * Un asa que lee los campos por omision de `cl` desde el flujo.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle defaultReadObjectForSerialization(Class<?> cl) {
        throw noHayAsa("defaultReadObject", cl);
    }

    /**
     * Un asa al {@code writeObject} privado de `cl`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle writeObjectForSerialization(Class<?> cl) {
        throw noHayAsa("writeObject", cl);
    }

    /**
     * Un asa que escribe los campos por omision de `cl` al flujo.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle defaultWriteObjectForSerialization(Class<?> cl) {
        throw noHayAsa("defaultWriteObject", cl);
    }

    /**
     * Un asa al {@code readResolve} de `cl`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle readResolveForSerialization(Class<?> cl) {
        throw noHayAsa("readResolve", cl);
    }

    /**
     * Un asa al {@code writeReplace} de `cl`.
     *
     * <p><b>No implementado.</b> Ver la nota de la clase.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final MethodHandle writeReplaceForSerialization(Class<?> cl) {
        throw noHayAsa("writeReplace", cl);
    }

    /**
     * Si `cl` tiene inicializador estatico.
     *
     * <p><b>No implementado.</b> {@code <clinit>} no es un {@link java.lang.reflect.Method} y la
     * reflexion no lo lista. Devolver `false` seria una respuesta concreta y equivocada: el que
     * pregunta lo usa para decidir si la deserializacion tiene que forzar la inicializacion de la
     * clase, y contestarle que no cuando si la deja a medio inicializar.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final boolean hasStaticInitializerForSerialization(Class<?> cl) {
        throw new UnsupportedOperationException(
                "cannot tell whether " + nombre(cl) + " has a static initializer: <clinit> is not "
                + "reachable through java.lang.reflect");
    }

    /**
     * Una {@link OptionalDataException} con la bandera de fin de datos puesta.
     *
     * <p><b>No implementado.</b> Los dos constructores de `OptionalDataException` son de paquete
     * --a proposito: solo la maquinaria de serializacion tiene por que fabricarla-- y desde
     * `sun.reflect` no se los ve.
     *
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public final OptionalDataException newOptionalDataExceptionForSerialization(boolean bool) {
        throw new UnsupportedOperationException(
                "cannot build an OptionalDataException: its constructors are package-private to "
                + "java.io");
    }

    /**
     * Los {@code serialPersistentFields} declarados por `cl`, o `null` si no declara ninguno.
     *
     * <p>Este si esta hecho: es leer un campo estatico. Solo cuenta si esta declarado como manda la
     * especificacion --{@code private static final ObjectStreamField[]}-- porque un campo con ese
     * nombre y otros modificadores no es el contrato y la serializacion del JDK tampoco lo mira.
     *
     * @throws NullPointerException si `cl` es nulo
     */
    public final ObjectStreamField[] serialPersistentFields(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException("cl");
        }
        try {
            Field f = cl.getDeclaredField("serialPersistentFields");
            int m = f.getModifiers();
            if (!Modifier.isPrivate(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)) {
                return null;
            }
            if (f.getType() != ObjectStreamField[].class) {
                return null;
            }
            f.setAccessible(true);
            ObjectStreamField[] campos = (ObjectStreamField[]) f.get(null);
            return campos == null ? null : campos.clone();
        } catch (Exception e) {
            return null;
        }
    }

    /** El mensaje que comparten los siete que devuelven un asa. */
    private static UnsupportedOperationException noHayAsa(String metodo, Class<?> cl) {
        return new UnsupportedOperationException(
                "cannot bind a MethodHandle to " + nombre(cl) + "." + metodo
                + ": a private lookup into another class is not available in this library");
    }

    /** El nombre de la clase, tolerando el nulo: esto es para un mensaje de error. */
    private static String nombre(Class<?> cl) {
        return cl == null ? "null" : cl.getName();
    }
}
