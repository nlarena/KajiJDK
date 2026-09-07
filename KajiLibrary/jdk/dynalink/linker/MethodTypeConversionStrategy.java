package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * How to adapt a method handle to another signature when {@code MethodHandle.asType} is not enough.
 *
 * <h2>When it is not enough</h2>
 *
 * <p>{@code asType} only does Java's conversions: widening a primitive, boxing, widening a
 * reference. A dynamic language almost always has more — number to string, string to number, any
 * object to boolean. The runtime cannot do those on its own, and this interface is where the
 * language contributes them.
 *
 * <p>It is applied <strong>after</strong> Java's conversions, not instead of them: what arrives here
 * is what {@code asType} could not resolve.
 *
 * @since 9
 */
@FunctionalInterface
public interface MethodTypeConversionStrategy {

    /**
     * The method handle adapted to the requested signature.
     *
     * @param target the original method handle
     * @param newType the requested signature
     * @return the adapted one, or the original if there is nothing to do
     */
    MethodHandle asType(MethodHandle target, MethodType newType);
}
