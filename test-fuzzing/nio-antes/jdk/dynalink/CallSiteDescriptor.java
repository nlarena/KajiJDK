package jdk.dynalink;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Everything known about a dynamic call site at link time: who wrote it (the {@link Lookup}), what it
 * wants to do (the {@link Operation}) and with what signature (the {@link MethodType}).
 *
 * <p>It is a **value type**: immutable, comparable, and with "modifiers" that return a new instance.
 * The public/protected pair of each modifier is not duplication: the public `final`
 * ({@link #changeMethodType}) holds the invariants, and the `protected` one
 * ({@link #changeMethodTypeInternal}) is the one a subclass redefines to preserve its own fields.
 * The subclass cannot skip the check, and the check cannot stop it from extending.
 *
 * <p>The invariants are only verified when `getClass() != CallSiteDescriptor.class`, that is, only
 * against subclasses: the base implementation meets them by construction and paying the cost on the
 * common path would make no sense.
 *
 * @since 9
 */
public class CallSiteDescriptor extends SecureLookupSupplier {

    private final Operation operation;
    private final MethodType methodType;

    public CallSiteDescriptor(final Lookup lookup, final Operation operation, final MethodType methodType) {
        super(lookup);
        this.operation = Objects.requireNonNull(operation, "name");
        this.methodType = Objects.requireNonNull(methodType, "methodType");
    }

    public final Operation getOperation() {
        return operation;
    }

    public final MethodType getMethodType() {
        return methodType;
    }

    /**
     * The same descriptor with another signature.
     *
     * @throws AssertionError if a subclass redefined {@link #changeMethodTypeInternal} in a way that
     *         changes the class, the lookup or the operation.
     */
    public final CallSiteDescriptor changeMethodType(final MethodType newMethodType) {
        final CallSiteDescriptor changed = changeMethodTypeInternal(newMethodType);
        if (getClass() != CallSiteDescriptor.class) {
            assertChangeInvariants(changed, "changeMethodTypeInternal");
            alwaysAssert(operation == changed.operation,
                    () -> "changeMethodTypeInternal must not change the descriptor's operation");
            alwaysAssert(newMethodType == changed.methodType,
                    () -> "changeMethodTypeInternal didn't set the correct new method type");
        }
        return changed;
    }

    /** The extension point of {@link #changeMethodType}; a subclass copies its fields here. */
    protected CallSiteDescriptor changeMethodTypeInternal(final MethodType newMethodType) {
        return new CallSiteDescriptor(getLookupPrivileged(), operation, newMethodType);
    }

    /**
     * The same descriptor with another operation.
     *
     * @throws AssertionError if a subclass redefined {@link #changeOperationInternal} in a way that
     *         changes the class, the lookup or the signature.
     */
    public final CallSiteDescriptor changeOperation(final Operation newOperation) {
        getLookup();
        final CallSiteDescriptor changed = changeOperationInternal(newOperation);
        if (getClass() != CallSiteDescriptor.class) {
            assertChangeInvariants(changed, "changeOperationInternal");
            alwaysAssert(methodType == changed.methodType,
                    () -> "changeOperationInternal must not change the descriptor's method type");
            alwaysAssert(newOperation == changed.operation,
                    () -> "changeOperationInternal didn't set the correct new operation");
        }
        return changed;
    }

    /** The extension point of {@link #changeOperation}. */
    protected CallSiteDescriptor changeOperationInternal(final Operation newOperation) {
        return new CallSiteDescriptor(getLookupPrivileged(), newOperation, methodType);
    }

    /**
     * Equality by value, with the exact class as part of the contract: a subclass's descriptor is
     * never equal to a base one, because the subclass may carry state of its own.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj.getClass() != getClass()) {
            return false;
        }
        final CallSiteDescriptor other = (CallSiteDescriptor) obj;
        return operation.equals(other.operation)
                && methodType.equals(other.methodType)
                && lookupsEqual(getLookupPrivileged(), other.getLookupPrivileged());
    }

    // Two lookups are the same if they give the same access from the same class; `Lookup` does not
    // define `equals`, so the comparison has to be explicit.
    private static boolean lookupsEqual(final Lookup l1, final Lookup l2) {
        return l1.lookupClass() == l2.lookupClass() && l1.lookupModes() == l2.lookupModes();
    }

    @Override
    public int hashCode() {
        return operation.hashCode() + 31 * methodType.hashCode()
                + 31 * 31 * lookupHashCode(getLookupPrivileged());
    }

    private static int lookupHashCode(final Lookup lookup) {
        return lookup.lookupClass().hashCode() + 31 * lookup.lookupModes();
    }

    @Override
    public String toString() {
        final String mt = methodType.toString();
        final String l = getLookupPrivileged().toString();
        final String o = operation.toString();
        return o + mt + '@' + l;
    }

    private void assertChangeInvariants(final CallSiteDescriptor changed, final String caller) {
        alwaysAssert(changed != null, () -> caller + " must not return null.");
        alwaysAssert(getClass() == changed.getClass(),
                () -> caller + " must not change the descriptor's class");
        alwaysAssert(lookupsEqual(getLookupPrivileged(), changed.getLookupPrivileged()),
                () -> caller + " must not change the descriptor's lookup");
    }

    // A real assert, not the `-ea` kind: these invariants protect a linker from someone else's
    // badly written subclass, and switching them off would turn the error into silent corruption.
    private static void alwaysAssert(final boolean cond, final Supplier<String> errorMessage) {
        if (!cond) {
            throw new AssertionError(errorMessage.get());
        }
    }
}
