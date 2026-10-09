package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.MethodHandleTransformer;
import jdk.dynalink.linker.MethodTypeConversionStrategy;
import jdk.dynalink.linker.support.TypeUtilities;

// What `DynamicLinker` lends to the linkers.
//
// Package-private on purpose: in the JDK this class is not public either -- it is called
// `LinkerServicesImpl` there too -- because nobody outside builds one. It is reached through
// `DynamicLinker.getLinkerServices()`.
//
// **Everything decided here is a question about types, not about handles.** Whether a `String` will
// do where an `Object` is asked for, which of two targets is better, which factory knows how to
// convert one into the other: that is arithmetic over `Class` and works in full. What needs a method
// handle built -- `asType`, `getTypeConverter` -- cannot: this virtual machine has no handles, and
// whoever asks for one gets the `UnsupportedOperationException` from `MethodHandles`, which says
// exactly that.
//
// The distinction matters because it is what separates the two halves of the interface: the half
// that settles **whether** a conversion is possible works, and the half that returns **what** to
// convert with does not.
final class LinkerServicesImpl implements LinkerServices {

    private final GuardingDynamicLinker linker;
    private final List<GuardingTypeConverterFactory> factories;
    private final List<ConversionComparator> comparators;
    private final MethodTypeConversionStrategy autoConversion;
    private final MethodHandleTransformer internalFilter;

    LinkerServicesImpl(final GuardingDynamicLinker linker,
            final List<GuardingTypeConverterFactory> factories,
            final List<ConversionComparator> comparators,
            final MethodTypeConversionStrategy autoConversion,
            final MethodHandleTransformer internalFilter) {
        this.linker = linker;
        this.factories = factories;
        this.comparators = comparators;
        this.autoConversion = autoConversion;
        this.internalFilter = internalFilter;
    }

    /** {@inheritDoc} */
    public MethodHandle asType(final MethodHandle handle, final MethodType fromType) {
        // The language's own method-type conversion first, and the hosting language's after it, if
        // it installed one. The order is not negotiable: the custom strategy is a last resort for
        // what Java does not know how to convert, not a replacement for what it does.
        final MethodHandle base = handle.asType(fromType);
        return this.autoConversion == null ? base : this.autoConversion.asType(base, fromType);
    }

    /** {@inheritDoc} */
    public MethodHandle getTypeConverter(final Class<?> sourceType, final Class<?> targetType) {
        for (int i = 0; i < this.factories.size(); i++) {
            final GuardedInvocation g;
            try {
                g = this.factories.get(i).convertToType(sourceType, targetType, LOOKUP);
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            if (g != null) {
                return g.getInvocation();
            }
        }
        if (!TypeUtilities.isMethodInvocationConvertible(sourceType, targetType)) {
            return null;
        }
        // No factory knows how to do it and the language does: the conversion is a type change over
        // identity. Building it needs handles, and that is where the exception comes from.
        return MethodHandles.identity(sourceType).asType(
                MethodType.methodType(targetType, sourceType));
    }

    /** {@inheritDoc} */
    public boolean canConvert(final Class<?> from, final Class<?> to) {
        if (TypeUtilities.isMethodInvocationConvertible(from, to)) {
            return true;
        }
        for (int i = 0; i < this.factories.size(); i++) {
            try {
                if (this.factories.get(i).convertToType(from, to, LOOKUP) != null) {
                    return true;
                }
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest) throws Exception {
        return this.linker.getGuardedInvocation(linkRequest, this);
    }

    /** {@inheritDoc} */
    public ConversionComparator.Comparison compareConversion(final Class<?> sourceType,
            final Class<?> targetType1, final Class<?> targetType2) {
        for (int i = 0; i < this.comparators.size(); i++) {
            final ConversionComparator.Comparison c =
                    this.comparators.get(i).compareConversion(sourceType, targetType1,
                            targetType2);
            if (c != ConversionComparator.Comparison.INDETERMINATE) {
                return c;
            }
        }
        return ConversionComparator.Comparison.INDETERMINATE;
    }

    /** {@inheritDoc} */
    public <T> T getWithLookup(final java.util.function.Supplier<T> operation,
            final SecureLookupSupplier lookupSupplier) {
        // The JDK installs the lookup somewhere on the thread, where `SecureLookupSupplier.getLookup()`
        // takes it from for as long as the action lasts, and removes it on the way out. Here there is
        // nothing to install: there is no lookup to install -- see `MethodHandles` -- and no place for
        // one to come from. The action is run all the same, which is what the JDK does when the
        // supplier is null.
        java.util.Objects.requireNonNull(operation);
        java.util.Objects.requireNonNull(lookupSupplier);
        return operation.get();
    }

    /** {@inheritDoc} */
    public MethodHandle filterInternalObjects(final MethodHandle target) {
        return this.internalFilter == null ? target : this.internalFilter.transform(target);
    }

    /**
     * The lookup handed to the conversion factories.
     *
     * <p>The JDK hands over the one of the site being linked, so that a conversion can reach members
     * only that site sees. Here there is no site -- nothing gets as far as being linked -- and
     * besides {@code MethodHandles.lookup()} does not exist on this virtual machine, so what is
     * handed over is a supplier that throws when it is consulted. A factory that does not need the
     * lookup never notices, which is the case for nearly all of them.
     */
    private static final java.util.function.Supplier<MethodHandles.Lookup> LOOKUP =
            new NoLookup();

    /** See {@link #LOOKUP}. */
    private static final class NoLookup
            implements java.util.function.Supplier<MethodHandles.Lookup> {

        public MethodHandles.Lookup get() {
            return MethodHandles.lookup();
        }
    }
}
