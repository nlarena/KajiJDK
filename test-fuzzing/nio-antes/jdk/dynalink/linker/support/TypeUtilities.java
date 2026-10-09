package jdk.dynalink.linker.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Java's conversion rules between types, written down so that a linker can consult them.
 *
 * <h2>Why they have to be asked of someone</h2>
 *
 * <p>Because the Java compiler applies these rules at compile time and afterwards they are left
 * nowhere. A dynamic linker decides at run time whether an argument fits a parameter, and for that it
 * needs the same rules as <strong>data</strong>. {@code Class.isAssignableFrom} only covers
 * references; everything to do with primitives, boxing or numeric widening is left out.
 *
 * <h2>The three questions, which are not the same one</h2>
 *
 * <p>{@link #isSubtype} is JLS 4.10's subtype relation: it includes primitives, where {@code int} is
 * a subtype of {@code long}, but it does <strong>not</strong> include boxing — an {@code int} is not
 * a subtype of {@code Integer}.
 *
 * <p>{@link #isMethodInvocationConvertible} is JLS 5.3's, the one that decides whether a call
 * compiles: it adds boxing and unboxing. It is more permissive than the previous one.
 *
 * <p>{@link #isConvertibleWithoutLoss} is more <strong>restrictive</strong> than both: it asks
 * whether the value survives intact. {@code long} to {@code double} is a valid conversion and it
 * loses information, because a {@code double}'s mantissa has 53 bits and a {@code long} has 64.
 *
 * <p>All three are needed for different things: the second to know whether an overload is applicable,
 * the third to decide whether narrowing a link's return value is a good idea.
 *
 * @since 9
 */
public final class TypeUtilities {

    /** From primitive to its wrapper. It includes {@code void}, which has {@code Void}. */
    private static final Map<Class<?>, Class<?>> WRAPPERS;
    /** The inverse. */
    private static final Map<Class<?>, Class<?>> PRIMITIVES;
    /** By name: {@code "int"} to {@code int.class}. */
    private static final Map<String, Class<?>> BY_NAME;

    static {
        // IdentityHashMap and not HashMap: the keys are Class objects, which are unique by
        // definition. Comparing by identity avoids calling Class's hashCode and equals.
        final Map<Class<?>, Class<?>> wrappers = new IdentityHashMap<Class<?>, Class<?>>(9);
        wrappers.put(Void.TYPE, Void.class);
        wrappers.put(Boolean.TYPE, Boolean.class);
        wrappers.put(Byte.TYPE, Byte.class);
        wrappers.put(Character.TYPE, Character.class);
        wrappers.put(Short.TYPE, Short.class);
        wrappers.put(Integer.TYPE, Integer.class);
        wrappers.put(Long.TYPE, Long.class);
        wrappers.put(Float.TYPE, Float.class);
        wrappers.put(Double.TYPE, Double.class);
        WRAPPERS = Collections.unmodifiableMap(wrappers);

        final Map<Class<?>, Class<?>> primitives = new IdentityHashMap<Class<?>, Class<?>>(9);
        final Map<String, Class<?>> byName = new HashMap<String, Class<?>>(9);
        for (final Map.Entry<Class<?>, Class<?>> e : wrappers.entrySet()) {
            primitives.put(e.getValue(), e.getKey());
            byName.put(e.getKey().getName(), e.getKey());
        }
        PRIMITIVES = Collections.unmodifiableMap(primitives);
        BY_NAME = Collections.unmodifiableMap(byName);
    }

    private TypeUtilities() {
    }

    /**
     * Whether a value of {@code sourceType} can be passed where {@code targetType} is expected.
     *
     * <p>It is JLS 5.3's method invocation conversion: identity, primitive widening, reference
     * widening, boxing and unboxing.
     *
     * @param sourceType the value's type
     * @param targetType the parameter's type
     * @return whether the call is legal
     */
    public static boolean isMethodInvocationConvertible(final Class<?> sourceType,
            final Class<?> targetType) {
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }
        if (sourceType.isPrimitive()) {
            if (targetType.isPrimitive()) {
                return isProperSubtype(sourceType, targetType);
            }
            return boxAndWiden(sourceType, targetType);
        }
        if (targetType.isPrimitive()) {
            // Unbox and then widen the primitive. It only comes out of an exact wrapper: a generic
            // Number is not unboxed, because at run time it could be any of the eight.
            final Class<?> unboxed = PRIMITIVES.get(sourceType);
            return unboxed != null
                    && (unboxed == targetType || isProperSubtype(unboxed, targetType));
        }
        return false;
    }

    /**
     * Whether a value of {@code sourceType} fits {@code targetType} without losing information.
     *
     * @param sourceType the value's type
     * @param targetType the type to arrive at
     * @return whether the conversion is exact
     */
    public static boolean isConvertibleWithoutLoss(final Class<?> sourceType,
            final Class<?> targetType) {
        // The target being void is enough: the value is discarded, and discarding it loses nothing
        // anyone is going to look at later. It holds even for boolean, which converts to nothing else.
        if (targetType.isAssignableFrom(sourceType) || targetType == void.class) {
            return true;
        }
        if (sourceType.isPrimitive()) {
            if (sourceType == void.class) {
                // Not the other way round: out of void comes only null, and the only type that
                // receives it whole is Object. Not even Void, which besides null would admit an
                // instance.
                return targetType == Object.class;
            }
            if (targetType.isPrimitive()) {
                return widensWithoutLoss(sourceType, targetType);
            }
            return boxAndWiden(sourceType, targetType);
        }
        // From reference to primitive never, even with the exact wrapper: the null has nowhere to go.
        return false;
    }

    /**
     * JLS 4.10's subtype relation, primitives included.
     *
     * <p>It does not include boxing: {@code int} is not a subtype of {@code Integer}. That is what
     * {@link #isMethodInvocationConvertible} is for.
     *
     * @param subType the subtype candidate
     * @param superType the supertype candidate
     * @return whether the first is a subtype of the second
     */
    public static boolean isSubtype(final Class<?> subType, final Class<?> superType) {
        // It covers classes, interfaces and arrays, and identity between primitives too.
        if (superType.isAssignableFrom(subType)) {
            return true;
        }
        if (superType.isPrimitive() && subType.isPrimitive()) {
            return isProperSubtype(subType, superType);
        }
        return false;
    }

    /**
     * Box and then widen the reference, which JLS 5.3 counts as a single step.
     *
     * <p>The source is already known to be primitive by the time this is reached, so the wrapper
     * always exists.
     */
    private static boolean boxAndWiden(final Class<?> sourceType, final Class<?> targetType) {
        return targetType.isAssignableFrom(WRAPPERS.get(sourceType));
    }

    /**
     * JLS 4.10.1's subtyping between primitives, without identity.
     *
     * <p>The chain is {@code double > float > long > int > {char, short} > byte}, closed under
     * transitivity, and it coincides with JLS 5.1.2's primitive widening — which is why a single
     * method answers both questions.
     *
     * <p>The callers have already ruled out identity before getting here, so this method does not
     * look at it again. That has one visible consequence: with a type outside the chain, such as
     * {@code void}, the branches written by negation answer yes. {@code byte} comes out a subtype of
     * {@code void} and {@code int} does not. It is an artefact of how the table is written, and it is
     * reproduced on purpose because it is what the JDK answers.
     */
    private static boolean isProperSubtype(final Class<?> subType, final Class<?> superType) {
        if (superType == boolean.class || subType == boolean.class) {
            return false;
        }
        // The three small ones are written by what they do NOT reach, and the three big ones by what
        // they do. It is not a whim: byte does not reach char because char is unsigned, and char does
        // not reach byte or short because those do not reach 65535. Outside that triangle, all go up.
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
     * The widenings JLS 5.1.2 marks as exact.
     *
     * <p>They are {@link #isProperSubtype}'s minus three: {@code int} to {@code float}, {@code long}
     * to {@code float} and {@code long} to {@code double}. In those cases the target's mantissa is
     * not enough for every value of the source and the result is rounded — it is still a legal
     * conversion, but it is no longer the same number.
     */
    private static boolean widensWithoutLoss(final Class<?> from, final Class<?> to) {
        if (to == boolean.class || from == boolean.class) {
            return false;
        }
        // char is left out in both directions, and that is this table's surprise: char to int keeps
        // every bit. What it does not keep is the meaning — a character becomes the number of its
        // code point — and this question is about the value, not about the bits. The JLS calls that
        // conversion a widening; the JDK does not call it exact.
        if (to == char.class || from == char.class) {
            return false;
        }
        if (from == byte.class) {
            return true;
        }
        if (from == short.class) {
            return to != byte.class;
        }
        if (from == int.class) {
            // int to float no: a float's mantissa has 24 bits and the int has 32.
            return to == long.class || to == double.class;
        }
        if (from == float.class) {
            return to == double.class;
        }
        // long reaches neither float nor double exactly: it has bits to spare against both mantissas.
        return false;
    }

    /**
     * The primitive with that name, or {@code null}.
     *
     * <p>{@code "void"} counts: it is the name of {@code void.class}.
     *
     * @param name the name, for instance {@code "int"}
     * @return the primitive's {@code Class}, or {@code null} if the name is not one
     */
    public static Class<?> getPrimitiveTypeByName(final String name) {
        return BY_NAME.get(name);
    }

    /**
     * The primitive inside a wrapper, or {@code null} if it is not a wrapper.
     *
     * @param wrapperType the wrapper, for instance {@code Integer.class}
     * @return the primitive, or {@code null}
     */
    public static Class<?> getPrimitiveType(final Class<?> wrapperType) {
        return PRIMITIVES.get(wrapperType);
    }

    /**
     * The wrapper of a primitive, or {@code null} if the type is not primitive.
     *
     * @param primitiveType the primitive, for instance {@code int.class}
     * @return the wrapper, or {@code null}
     */
    public static Class<?> getWrapperType(final Class<?> primitiveType) {
        return WRAPPERS.get(primitiveType);
    }

    /**
     * Whether the type is one of the nine wrappers.
     *
     * @param type the type
     * @return whether it is a wrapper
     */
    public static boolean isWrapperType(final Class<?> type) {
        return PRIMITIVES.containsKey(type);
    }
}
