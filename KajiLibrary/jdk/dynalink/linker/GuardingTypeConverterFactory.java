package jdk.dynalink.linker;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

/**
 * Contributes a language's own type conversions.
 *
 * <h2>Why the conversion carries a guard too</h2>
 *
 * <p>Because it almost never depends on the classes alone. "Convert to a number" may hold for the
 * strings that look like numbers and not for the others, even though both are {@code String}. The
 * result is a {@link GuardedInvocation}: the conversion plus the condition under which it is that
 * one and not another. When the guard fails, the caller tries the next alternative.
 *
 * <h2>Why the lookup arrives as a supplier and not directly</h2>
 *
 * <p>Because a {@code Lookup} is a credential and handing it over unasked would be giving it away.
 * The {@link Supplier} forces a call to obtain it, which is where the control can happen; a
 * converter that needs no privileged access simply never invokes it.
 *
 * @since 9
 */
public interface GuardingTypeConverterFactory {

    /**
     * How to convert from one type to another, with the condition under which it holds.
     *
     * @param sourceType the type to start from
     * @param targetType the type to arrive at
     * @param lookupSupplier the site's lookup, if it is needed
     * @return the conversion with its guard, or {@code null} if this factory cannot make it
     * @throws Exception if the conversion cannot be built
     */
    GuardedInvocation convertToType(Class<?> sourceType, Class<?> targetType,
            Supplier<MethodHandles.Lookup> lookupSupplier) throws Exception;
}
