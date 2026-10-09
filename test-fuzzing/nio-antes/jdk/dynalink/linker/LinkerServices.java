package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

import jdk.dynalink.SecureLookupSupplier;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * What a linker can ask of its host while linking.
 *
 * <h2>Why a linker needs anything from outside</h2>
 *
 * <p>Because it is not alone. A linker for Java objects has to convert an argument to the
 * parameter's type, and that conversion may belong to <strong>another</strong> language running at
 * the same site: passing a scripting language's function to a method expecting a {@code Runnable},
 * for instance. The only one who knows the whole chain is whoever composed it, and this interface is
 * how it lends it out.
 *
 * <p>That is why {@link #getGuardedInvocation} is here: a linker can delegate a request to the whole
 * chain, itself included, without knowing it.
 *
 * @since 9
 */
public interface LinkerServices {

    /**
     * Adapts a method handle to another signature, using the languages' conversions as well.
     *
     * <p>It is the replacement for {@code MethodHandle.asType}, which only does Java's.
     *
     * @param handle the method handle
     * @param fromType the requested signature
     * @return the adapted method handle
     */
    MethodHandle asType(MethodHandle handle, MethodType fromType);

    /**
     * Like {@link #asType}, but without degrading the return value.
     *
     * <p>The difference matters in a chain of invocations. If the method returns {@code long} and the
     * site declares {@code int}, {@code asType} truncates — and truncates <strong>before</strong>
     * anyone can look at the value. This method instead leaves the return wider when the conversion
     * would lose information, and the narrowing is left for the very end, where the caller can decide
     * otherwise.
     *
     * @param handle the method handle
     * @param fromType the requested signature
     * @return the adapted method handle, perhaps with a wider return than requested
     */
    default MethodHandle asTypeLosslessReturn(final MethodHandle handle, final MethodType fromType) {
        final Class<?> returnType = handle.type().returnType();
        return asType(handle, TypeUtilities.isConvertibleWithoutLoss(returnType, fromType.returnType())
                ? fromType : fromType.changeReturnType(returnType));
    }

    /**
     * The method handle that converts from one type to another, or {@code null} if there is none.
     *
     * @param sourceType the type to start from
     * @param targetType the type to arrive at
     * @return the converter, or {@code null}
     */
    MethodHandle getTypeConverter(Class<?> sourceType, Class<?> targetType);

    /**
     * Whether any conversion from one type to another exists.
     *
     * <p>It is not the same as {@link #getTypeConverter} being non-{@code null}: the conversion may
     * exist and be guarded, so that it is only known with the value in hand.
     *
     * @param from the type to start from
     * @param to the type to arrive at
     * @return whether the conversion is possible
     */
    boolean canConvert(Class<?> from, Class<?> to);

    /**
     * Delegates a request to the whole chain of linkers.
     *
     * @param linkRequest the request
     * @return the linked invocation, or {@code null} if nobody knew how
     * @throws Exception if linking fails
     */
    GuardedInvocation getGuardedInvocation(LinkRequest linkRequest) throws Exception;

    /**
     * Asks the chain's {@link ConversionComparator}s which target suits.
     *
     * @param sourceType the value's type
     * @param targetType1 the first target
     * @param targetType2 the second target
     * @return the preference, or {@code INDETERMINATE} if none of them has an opinion
     */
    ConversionComparator.Comparison compareConversion(Class<?> sourceType, Class<?> targetType1,
            Class<?> targetType2);

    /**
     * Applies the internal-object filter configured by the host.
     *
     * <p>It keeps one language's own values from escaping into another. If no filter is configured it
     * returns the method handle unchanged.
     *
     * @param target the method handle
     * @return the filtered method handle
     */
    MethodHandle filterInternalObjects(MethodHandle target);

    /**
     * Runs something with a site's {@code Lookup} available.
     *
     * <p>It is how a type converter reaches private members of the calling class: instead of
     * receiving the credential --which it could keep-- it receives a window of time during which the
     * credential is in place. Outside that window it does not have it.
     *
     * @param <T> what the action returns
     * @param operation the action
     * @param lookupSupplier the carrier of the lookup
     * @return whatever the action returned
     */
    <T> T getWithLookup(Supplier<T> operation, SecureLookupSupplier lookupSupplier);
}
