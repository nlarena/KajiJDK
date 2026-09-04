package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Un {@link MethodHandles.Lookup} sin excepciones verificadas.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Porque un enlazador busca metodos que <strong>sabe</strong> que estan: los suyos propios, los
 * que acaba de encontrar por reflexion. En ese uso, {@code NoSuchMethodException} no es una
 * condicion a manejar sino un error de programa, y obligar a escribir un {@code try}/{@code catch}
 * alrededor de cada busqueda solo agrega ruido.
 *
 * <p>Por eso cada metodo de aca convierte la excepcion verificada en el {@code Error} que le
 * corresponde: {@link NoSuchMethodError}, {@link NoSuchFieldError}, {@link IllegalAccessError}.
 * Son los mismos que tiraria la JVM si el metodo faltara en una invocacion compilada — la
 * traduccion no inventa una categoria nueva, usa la que ya existia para esta misma falla.
 *
 * <p>La causa original queda encadenada, asi que no se pierde nada.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La logica de aca esta completa, pero el {@code MethodHandles.Lookup} que hay debajo todavia
 * no puede fabricar handles sin soporte de la VM. Las busquedas terminan en
 * {@link UnsupportedOperationException}, no en un resultado equivocado.
 *
 * @since 9
 */
public final class Lookup {

    private final MethodHandles.Lookup lookup;

    /** Uno con acceso solo a lo publico. */
    public static final Lookup PUBLIC = new Lookup(MethodHandles.publicLookup());

    /**
     * Envuelve un lookup.
     *
     * @param lookup el lookup a envolver
     */
    public Lookup(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    /**
     * Como {@code Lookup.unreflect}, sin excepcion verificada.
     *
     * @param m el metodo
     * @return el handle
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle unreflect(final Method m) {
        return unreflect(lookup, m);
    }

    /**
     * Como {@code Lookup.unreflect}, sin excepcion verificada.
     *
     * @param lookup el lookup a usar
     * @param m el metodo
     * @return el handle
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public static MethodHandle unreflect(final MethodHandles.Lookup lookup, final Method m) {
        try {
            return lookup.unreflect(m);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo obtener el handle del metodo " + m, e);
        }
    }

    /**
     * Como {@code Lookup.unreflectGetter}, sin excepcion verificada.
     *
     * @param f el campo
     * @return el handle de lectura
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle unreflectGetter(final Field f) {
        try {
            return lookup.unreflectGetter(f);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo obtener el lector del campo " + f, e);
        }
    }

    /**
     * Como {@code Lookup.findGetter}, sin excepciones verificadas.
     *
     * @param refc la clase donde esta el campo
     * @param name el nombre del campo
     * @param type el tipo del campo
     * @return el handle de lectura
     * @throws NoSuchFieldError si el campo no existe
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle findGetter(final Class<?> refc, final String name, final Class<?> type) {
        try {
            return lookup.findGetter(refc, name, type);
        } catch (final NoSuchFieldException e) {
            throw sinCampo("no se encontro el campo " + descripcion(refc, name, type), e);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo leer el campo " + descripcion(refc, name, type), e);
        }
    }

    /**
     * Como {@code Lookup.unreflectSetter}, sin excepcion verificada.
     *
     * @param f el campo
     * @return el handle de escritura
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle unreflectSetter(final Field f) {
        try {
            return lookup.unreflectSetter(f);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo obtener el escritor del campo " + f, e);
        }
    }

    /**
     * Como {@code Lookup.unreflectConstructor}, sin excepcion verificada.
     *
     * @param c el constructor
     * @return el handle
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle unreflectConstructor(final Constructor<?> c) {
        return unreflectConstructor(lookup, c);
    }

    /**
     * Como {@code Lookup.unreflectConstructor}, sin excepcion verificada.
     *
     * @param lookup el lookup a usar
     * @param c el constructor
     * @return el handle
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public static MethodHandle unreflectConstructor(final MethodHandles.Lookup lookup,
            final Constructor<?> c) {
        try {
            return lookup.unreflectConstructor(c);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo obtener el handle del constructor " + c, e);
        }
    }

    /**
     * Como {@code Lookup.findSpecial}, sin excepciones verificadas.
     *
     * <p>El llamador especial es la propia clase declarante: esta es la busqueda que sirve para
     * invocar un metodo <strong>sin</strong> despacho virtual, que es lo que hace falta para
     * llamar a la implementacion de una superclase.
     *
     * @param declaringClass la clase que declara el metodo
     * @param name el nombre
     * @param type la firma
     * @return el handle
     * @throws NoSuchMethodError si el metodo no existe
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle findSpecial(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findSpecial(declaringClass, name, type, declaringClass);
        } catch (final NoSuchMethodException e) {
            throw sinMetodo("no se encontro el metodo especial "
                    + descripcion(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo invocar el metodo especial "
                    + descripcion(declaringClass, name, type), e);
        }
    }

    /**
     * Como {@code Lookup.findStatic}, sin excepciones verificadas.
     *
     * @param declaringClass la clase que declara el metodo
     * @param name el nombre
     * @param type la firma
     * @return el handle
     * @throws NoSuchMethodError si el metodo no existe
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle findStatic(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findStatic(declaringClass, name, type);
        } catch (final NoSuchMethodException e) {
            throw sinMetodo("no se encontro el metodo estatico "
                    + descripcion(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo invocar el metodo estatico "
                    + descripcion(declaringClass, name, type), e);
        }
    }

    /**
     * Como {@code Lookup.findVirtual}, sin excepciones verificadas.
     *
     * @param declaringClass la clase que declara el metodo
     * @param name el nombre
     * @param type la firma, sin el receptor
     * @return el handle, que toma el receptor como primer argumento
     * @throws NoSuchMethodError si el metodo no existe
     * @throws IllegalAccessError si el lookup no alcanza
     */
    public MethodHandle findVirtual(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findVirtual(declaringClass, name, type);
        } catch (final NoSuchMethodException e) {
            throw sinMetodo("no se encontro el metodo virtual "
                    + descripcion(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw sinAcceso("no se pudo invocar el metodo virtual "
                    + descripcion(declaringClass, name, type), e);
        }
    }

    /**
     * Un metodo especial de la clase del propio lookup.
     *
     * <p>Es el atajo que usa un enlazador para tomar handles de sus propios metodos: la clase no
     * se nombra porque es la del lookup, y los tipos se dan sueltos en vez de armar un
     * {@link MethodType}.
     *
     * @param lookup el lookup, cuya clase es la que declara el metodo
     * @param name el nombre
     * @param rtype el tipo de retorno
     * @param ptypes los tipos de los parametros
     * @return el handle
     */
    public static MethodHandle findOwnSpecial(final MethodHandles.Lookup lookup, final String name,
            final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnSpecial(name, rtype, ptypes);
    }

    /**
     * Un metodo especial de la clase de este lookup.
     *
     * @param name el nombre
     * @param rtype el tipo de retorno
     * @param ptypes los tipos de los parametros
     * @return el handle
     */
    public MethodHandle findOwnSpecial(final String name, final Class<?> rtype,
            final Class<?>... ptypes) {
        return findSpecial(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }

    /**
     * Un metodo estatico de la clase del propio lookup.
     *
     * @param lookup el lookup, cuya clase es la que declara el metodo
     * @param name el nombre
     * @param rtype el tipo de retorno
     * @param ptypes los tipos de los parametros
     * @return el handle
     */
    public static MethodHandle findOwnStatic(final MethodHandles.Lookup lookup, final String name,
            final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnStatic(name, rtype, ptypes);
    }

    /**
     * Un metodo estatico de la clase de este lookup.
     *
     * @param name el nombre
     * @param rtype el tipo de retorno
     * @param ptypes los tipos de los parametros
     * @return el handle
     */
    public MethodHandle findOwnStatic(final String name, final Class<?> rtype,
            final Class<?>... ptypes) {
        return findStatic(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }

    private static String descripcion(final Class<?> clazz, final String name, final Object type) {
        return clazz.getName() + "." + name + " de tipo " + type;
    }

    private static NoSuchMethodError sinMetodo(final String mensaje, final Throwable causa) {
        final NoSuchMethodError e = new NoSuchMethodError(mensaje);
        e.initCause(causa);
        return e;
    }

    private static NoSuchFieldError sinCampo(final String mensaje, final Throwable causa) {
        final NoSuchFieldError e = new NoSuchFieldError(mensaje);
        e.initCause(causa);
        return e;
    }

    private static IllegalAccessError sinAcceso(final String mensaje, final Throwable causa) {
        final IllegalAccessError e = new IllegalAccessError(mensaje);
        e.initCause(causa);
        return e;
    }
}
