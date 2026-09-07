package jdk.dynalink.linker.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.dynalink.linker.LinkerServices;

/**
 * Builds the commonest guards: "the receiver is of this class", "it is not null", "it is an array".
 *
 * <h2>What a guard is, concretely</h2>
 *
 * <p>A method handle returning {@code boolean} and taking a prefix of the arguments of the invocation
 * it protects. It is evaluated on every call, so it has to be cheap: comparing a pointer to a
 * {@code Class}, or comparing against {@code null}. Anything dearer than that should be a switch
 * point and not a guard.
 *
 * <h2>The guards that are not needed</h2>
 *
 * <p>Several methods here look at the site's signature and find the question already answered. If the
 * site declares the parameter as {@code String} and a guard of "it is a {@code String}" is asked for,
 * the check is redundant: the JVM's verifier already guarantees it. If a guard of "it is an
 * {@code Integer}" is asked for over a parameter declared {@code String}, the guard can never be
 * true.
 *
 * <p>In both cases they return a constant instead of a check, and leave a warning in the log. The
 * constant is the right answer; the warning is there because it almost always means the linker that
 * asked for it got the signature wrong, and without the warning that would never be noticed.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The decisions above are real and they happen. What there is not yet is the handle factory:
 * {@code MethodHandles} cannot build one without VM support, so any method here ends in
 * {@link UnsupportedOperationException} on reaching that point.
 *
 * <p>The checks' handles are resolved the first time they are used and not in the static
 * initializer, which is where the JDK resolves them. The difference is only which exception comes
 * out: this way the one that names what is missing comes out, instead of an
 * {@code ExceptionInInitializerError} wrapping it and leaving the class unusable.
 *
 * @since 9
 */
public final class Guards {

    private static final Logger LOG = Logger.getLogger(Guards.class.getName());

    private Guards() {
    }

    /**
     * The checks' handles, resolved once and on demand.
     *
     * <p>It is the initialization-on-demand holder idiom: the JVM guarantees that this class is
     * initialized the first time it is touched and only once, with no explicit lock.
     */
    private static final class Handles {
        static final MethodHandle IS_OF_CLASS = own("checkClass", Class.class, Object.class);
        static final MethodHandle IS_INSTANCE = own("checkInstance", Class.class, Object.class);
        static final MethodHandle IS_ARRAY = own("checkArray", Object.class);
        static final MethodHandle IS_NULL = own("checkNull", Object.class);
        static final MethodHandle IS_NOT_NULL = own("checkNotNull", Object.class);
        static final MethodHandle IS_IDENTICAL = own("checkIdentical", Object.class, Object.class);

        private static MethodHandle own(final String name, final Class<?>... params) {
            return Lookup.findOwnStatic(MethodHandles.lookup(), name, Boolean.TYPE, params);
        }
    }

    // The checks themselves. They are ordinary methods: all Guards adds is wrapping them in a handle
    // of the shape the call site needs.

    @SuppressWarnings("unused")
    private static boolean checkClass(final Class<?> clazz, final Object obj) {
        return obj != null && obj.getClass() == clazz;
    }

    @SuppressWarnings("unused")
    private static boolean checkInstance(final Class<?> clazz, final Object obj) {
        return clazz.isInstance(obj);
    }

    @SuppressWarnings("unused")
    private static boolean checkArray(final Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    @SuppressWarnings("unused")
    private static boolean checkNull(final Object obj) {
        return obj == null;
    }

    @SuppressWarnings("unused")
    private static boolean checkNotNull(final Object obj) {
        return obj != null;
    }

    @SuppressWarnings("unused")
    private static boolean checkIdentical(final Object obj1, final Object obj2) {
        return obj1 == obj2;
    }

    /**
     * A guard of "the first argument is exactly of this class", not of a subclass.
     *
     * @param clazz the exact class
     * @param type the site's signature
     * @return the guard, or a constant if the answer is already known
     */
    public static MethodHandle isOfClass(final Class<?> clazz, final MethodType type) {
        final Class<?> declared = type.parameterType(0);
        if (clazz == declared) {
            warn("the exact-class guard on {0} is always true in {1}", clazz, type);
            return constant(true, type);
        }
        if (!declared.isAssignableFrom(clazz)) {
            warn("the exact-class guard on {0} can never be true in {1}", clazz, type);
            return constant(false, type);
        }
        return bound(Handles.IS_OF_CLASS, clazz, 0, type);
    }

    /**
     * A guard of "the first argument is an instance of this class", subclasses included.
     *
     * @param clazz the class
     * @param type the site's signature
     * @return the guard, or a constant if the answer is already known
     */
    public static MethodHandle isInstance(final Class<?> clazz, final MethodType type) {
        return isInstance(clazz, 0, type);
    }

    /**
     * A guard of "the argument at that position is an instance of this class".
     *
     * @param clazz the class
     * @param pos the argument's position
     * @param type the site's signature
     * @return the guard, or a constant if the answer is already known
     */
    public static MethodHandle isInstance(final Class<?> clazz, final int pos,
            final MethodType type) {
        final Class<?> declared = type.parameterType(pos);
        if (clazz.isAssignableFrom(declared)) {
            warn("the instance-of-{0} guard is always true in {1}", clazz, type);
            return constant(true, type);
        }
        if (!declared.isAssignableFrom(clazz)) {
            warn("the instance-of-{0} guard can never be true in {1}", clazz, type);
            return constant(false, type);
        }
        return bound(Handles.IS_INSTANCE, clazz, pos, type);
    }

    /**
     * A guard of "the argument at that position is an array".
     *
     * @param pos the argument's position
     * @param type the site's signature
     * @return the guard, or a constant if the answer is already known
     */
    public static MethodHandle isArray(final int pos, final MethodType type) {
        final Class<?> declared = type.parameterType(pos);
        if (declared.isArray()) {
            warn("the array guard is always true at position {0} of {1}",
                    Integer.valueOf(pos), type);
            return constant(true, type);
        }
        // Object[] is used as a floor: if not even an array of objects fits the declared type, no
        // array can get there.
        if (!declared.isAssignableFrom(Object[].class)) {
            warn("the array guard can never be true at position {0} of {1}",
                    Integer.valueOf(pos), type);
            return constant(false, type);
        }
        return atPosition(Handles.IS_ARRAY, pos, type);
    }

    /**
     * Adapts a guard to a site's signature.
     *
     * <p>The signature it takes is the site's, cut down to the parameters the guard looks at --it may
     * look at fewer-- and returning {@code boolean}.
     *
     * @param test the guard
     * @param type the site's signature
     * @return the adapted guard
     */
    public static MethodHandle asType(final MethodHandle test, final MethodType type) {
        return test.asType(guardType(test, type));
    }

    /**
     * Adapts a guard to a site's signature, with the languages' conversions.
     *
     * @param linkerServices the services that contribute those conversions
     * @param test the guard
     * @param type the site's signature
     * @return the adapted guard
     */
    public static MethodHandle asType(final LinkerServices linkerServices, final MethodHandle test,
            final MethodType type) {
        return linkerServices.asType(test, guardType(test, type));
    }

    /**
     * A guard of "it is exactly of this class", of signature {@code (Object)boolean}.
     *
     * @param clazz the class
     * @return the guard
     */
    public static MethodHandle getClassGuard(final Class<?> clazz) {
        return Handles.IS_OF_CLASS.bindTo(clazz);
    }

    /**
     * A guard of "it is an instance of this class", of signature {@code (Object)boolean}.
     *
     * @param clazz the class
     * @return the guard
     */
    public static MethodHandle getInstanceOfGuard(final Class<?> clazz) {
        return Handles.IS_INSTANCE.bindTo(clazz);
    }

    /**
     * A guard of "it is this object and no other", by identity.
     *
     * <p>It serves to link against one particular object instead of against a class, which is what is
     * needed in a language where methods live in the instance.
     *
     * @param obj the object
     * @return the guard
     */
    public static MethodHandle getIdentityGuard(final Object obj) {
        return Handles.IS_IDENTICAL.bindTo(obj);
    }

    /**
     * A guard of "it is null", of signature {@code (Object)boolean}.
     *
     * @return the guard
     */
    public static MethodHandle isNull() {
        return Handles.IS_NULL;
    }

    /**
     * A guard of "it is not null", of signature {@code (Object)boolean}.
     *
     * @return the guard
     */
    public static MethodHandle isNotNull() {
        return Handles.IS_NOT_NULL;
    }

    private static MethodType guardType(final MethodHandle test, final MethodType type) {
        return type.dropParameterTypes(test.type().parameterCount(), type.parameterCount())
                .changeReturnType(Boolean.TYPE);
    }

    /** Fixes the class as the check's first argument and places it at the requested position. */
    private static MethodHandle bound(final MethodHandle test, final Class<?> clazz,
            final int pos, final MethodType type) {
        return atPosition(test.bindTo(clazz), pos, type);
    }

    /**
     * A unary check set to look at argument {@code pos} of a site of signature {@code type}.
     *
     * <p>The reordering is what does the work: {@code permuteArguments} with the array {@code {pos}}
     * says that the check's only parameter is fed from argument {@code pos}, and that all the others
     * are discarded.
     */
    private static MethodHandle atPosition(final MethodHandle test, final int pos,
            final MethodType type) {
        return MethodHandles.permuteArguments(
                test.asType(test.type().changeParameterType(0, type.parameterType(pos))),
                type.changeReturnType(Boolean.TYPE), new int[] { pos });
    }

    /** A guard that ignores its arguments and always answers the same. */
    private static MethodHandle constant(final boolean value, final MethodType type) {
        return MethodHandles.permuteArguments(
                MethodHandles.constant(Boolean.TYPE, Boolean.valueOf(value)),
                type.changeReturnType(Boolean.TYPE), new int[0]);
    }

    private static void warn(final String message, final Object a, final Object b) {
        if (LOG.isLoggable(Level.WARNING)) {
            LOG.log(Level.WARNING, message, new Object[] { a, b });
        }
    }
}
