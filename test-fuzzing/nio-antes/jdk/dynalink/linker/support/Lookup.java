package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A {@link MethodHandles.Lookup} with no checked exceptions.
 *
 * <h2>Why it exists</h2>
 *
 * <p>Because a linker looks up methods it <strong>knows</strong> are there: its own, the ones it has
 * just found by reflection. In that use, {@code NoSuchMethodException} is not a condition to handle
 * but a programming error, and forcing a {@code try}/{@code catch} around every lookup only adds
 * noise.
 *
 * <p>So every method here turns the checked exception into the {@code Error} that corresponds to it:
 * {@link NoSuchMethodError}, {@link NoSuchFieldError}, {@link IllegalAccessError}. They are the same
 * ones the JVM would throw if the method were missing from a compiled invocation — the translation
 * invents no new category, it uses the one that already existed for this very failure.
 *
 * <p>The original cause stays chained, so nothing is lost.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The logic here is complete, but the {@code MethodHandles.Lookup} underneath still cannot build
 * handles without VM support. The lookups end in {@link UnsupportedOperationException}, not in a
 * wrong result.
 *
 * @since 9
 */
public final class Lookup {

    private final MethodHandles.Lookup lookup;

    /** One with access to public members only. */
    public static final Lookup PUBLIC = new Lookup(MethodHandles.publicLookup());

    /**
     * Wraps a lookup.
     *
     * @param lookup the lookup to wrap
     */
    public Lookup(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    /**
     * Like {@code Lookup.unreflect}, with no checked exception.
     *
     * @param m the method
     * @return the method handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle unreflect(final Method m) {
        return unreflect(lookup, m);
    }

    /**
     * Like {@code Lookup.unreflect}, with no checked exception.
     *
     * @param lookup the lookup to use
     * @param m the method
     * @return the method handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public static MethodHandle unreflect(final MethodHandles.Lookup lookup, final Method m) {
        try {
            return lookup.unreflect(m);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not get the method handle for " + m, e);
        }
    }

    /**
     * Like {@code Lookup.unreflectGetter}, with no checked exception.
     *
     * @param f the field
     * @return the getter handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle unreflectGetter(final Field f) {
        try {
            return lookup.unreflectGetter(f);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not get the getter for field " + f, e);
        }
    }

    /**
     * Like {@code Lookup.findGetter}, with no checked exceptions.
     *
     * @param refc the class the field is in
     * @param name the field's name
     * @param type the field's type
     * @return the getter handle
     * @throws NoSuchFieldError if the field does not exist
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle findGetter(final Class<?> refc, final String name, final Class<?> type) {
        try {
            return lookup.findGetter(refc, name, type);
        } catch (final NoSuchFieldException e) {
            throw noField("field not found: " + describe(refc, name, type), e);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not read field " + describe(refc, name, type), e);
        }
    }

    /**
     * Like {@code Lookup.unreflectSetter}, with no checked exception.
     *
     * @param f the field
     * @return the setter handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle unreflectSetter(final Field f) {
        try {
            return lookup.unreflectSetter(f);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not get the setter for field " + f, e);
        }
    }

    /**
     * Like {@code Lookup.unreflectConstructor}, with no checked exception.
     *
     * @param c the constructor
     * @return the method handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle unreflectConstructor(final Constructor<?> c) {
        return unreflectConstructor(lookup, c);
    }

    /**
     * Like {@code Lookup.unreflectConstructor}, with no checked exception.
     *
     * @param lookup the lookup to use
     * @param c the constructor
     * @return the method handle
     * @throws IllegalAccessError if the lookup does not reach
     */
    public static MethodHandle unreflectConstructor(final MethodHandles.Lookup lookup,
            final Constructor<?> c) {
        try {
            return lookup.unreflectConstructor(c);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not get the method handle for constructor " + c, e);
        }
    }

    /**
     * Like {@code Lookup.findSpecial}, with no checked exceptions.
     *
     * <p>The special caller is the declaring class itself: this is the lookup that serves to invoke a
     * method <strong>without</strong> virtual dispatch, which is what is needed to call a
     * superclass's implementation.
     *
     * @param declaringClass the class declaring the method
     * @param name the name
     * @param type the signature
     * @return the method handle
     * @throws NoSuchMethodError if the method does not exist
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle findSpecial(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findSpecial(declaringClass, name, type, declaringClass);
        } catch (final NoSuchMethodException e) {
            throw noMethod("special method not found: "
                    + describe(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not invoke special method "
                    + describe(declaringClass, name, type), e);
        }
    }

    /**
     * Like {@code Lookup.findStatic}, with no checked exceptions.
     *
     * @param declaringClass the class declaring the method
     * @param name the name
     * @param type the signature
     * @return the method handle
     * @throws NoSuchMethodError if the method does not exist
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle findStatic(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findStatic(declaringClass, name, type);
        } catch (final NoSuchMethodException e) {
            throw noMethod("static method not found: "
                    + describe(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not invoke static method "
                    + describe(declaringClass, name, type), e);
        }
    }

    /**
     * Like {@code Lookup.findVirtual}, with no checked exceptions.
     *
     * @param declaringClass the class declaring the method
     * @param name the name
     * @param type the signature, without the receiver
     * @return the method handle, which takes the receiver as its first argument
     * @throws NoSuchMethodError if the method does not exist
     * @throws IllegalAccessError if the lookup does not reach
     */
    public MethodHandle findVirtual(final Class<?> declaringClass, final String name,
            final MethodType type) {
        try {
            return lookup.findVirtual(declaringClass, name, type);
        } catch (final NoSuchMethodException e) {
            throw noMethod("virtual method not found: "
                    + describe(declaringClass, name, type), e);
        } catch (final IllegalAccessException e) {
            throw noAccess("could not invoke virtual method "
                    + describe(declaringClass, name, type), e);
        }
    }

    /**
     * A special method of the lookup's own class.
     *
     * <p>It is the shortcut a linker uses to take handles of its own methods: the class is not named
     * because it is the lookup's, and the types are given loose instead of building a
     * {@link MethodType}.
     *
     * @param lookup the lookup, whose class is the one declaring the method
     * @param name the name
     * @param rtype the return type
     * @param ptypes the parameter types
     * @return the method handle
     */
    public static MethodHandle findOwnSpecial(final MethodHandles.Lookup lookup, final String name,
            final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnSpecial(name, rtype, ptypes);
    }

    /**
     * A special method of this lookup's class.
     *
     * @param name the name
     * @param rtype the return type
     * @param ptypes the parameter types
     * @return the method handle
     */
    public MethodHandle findOwnSpecial(final String name, final Class<?> rtype,
            final Class<?>... ptypes) {
        return findSpecial(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }

    /**
     * A static method of the lookup's own class.
     *
     * @param lookup the lookup, whose class is the one declaring the method
     * @param name the name
     * @param rtype the return type
     * @param ptypes the parameter types
     * @return the method handle
     */
    public static MethodHandle findOwnStatic(final MethodHandles.Lookup lookup, final String name,
            final Class<?> rtype, final Class<?>... ptypes) {
        return new Lookup(lookup).findOwnStatic(name, rtype, ptypes);
    }

    /**
     * A static method of this lookup's class.
     *
     * @param name the name
     * @param rtype the return type
     * @param ptypes the parameter types
     * @return the method handle
     */
    public MethodHandle findOwnStatic(final String name, final Class<?> rtype,
            final Class<?>... ptypes) {
        return findStatic(lookup.lookupClass(), name, MethodType.methodType(rtype, ptypes));
    }

    private static String describe(final Class<?> clazz, final String name, final Object type) {
        return clazz.getName() + "." + name + " of type " + type;
    }

    private static NoSuchMethodError noMethod(final String message, final Throwable cause) {
        final NoSuchMethodError e = new NoSuchMethodError(message);
        e.initCause(cause);
        return e;
    }

    private static NoSuchFieldError noField(final String message, final Throwable cause) {
        final NoSuchFieldError e = new NoSuchFieldError(message);
        e.initCause(cause);
        return e;
    }

    private static IllegalAccessError noAccess(final String message, final Throwable cause) {
        final IllegalAccessError e = new IllegalAccessError(message);
        e.initCause(cause);
        return e;
    }
}
