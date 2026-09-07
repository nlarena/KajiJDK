package jdk.dynalink.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SwitchPoint;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;

/**
 * A method handle plus the conditions under which it goes on being the right one.
 *
 * <h2>Why a link is this and not a bare method handle</h2>
 *
 * <p>Because linking is expensive and calling is cheap. If the linker's answer were only "for these
 * arguments, call this", it would have to be asked on every invocation and linking would be worth
 * nothing. The useful answer is "as long as this holds, call this", and the three ways of saying "as
 * long as this holds" are this class's three optional parts.
 *
 * <h2>The three conditions, and why they are three and not one</h2>
 *
 * <p><strong>The guard</strong> is a method handle returning {@code boolean} and taking the same
 * arguments (or a prefix of them). It is evaluated on <strong>every</strong> invocation. It is the
 * condition that depends on the values: "the receiver is still of this class".
 *
 * <p><strong>The switch points</strong> are never evaluated: they are a global switch someone throws
 * exactly once, and until then the JIT can erase the check entirely. It is the condition that depends
 * on the world and not on the arguments — "nobody has redefined this method yet". Zero cost while
 * nothing changes, which is the reason they exist apart from the guard.
 *
 * <p><strong>The exception</strong> is the condition that is only discovered by trying. If the
 * invocation throws that class of exception, the link is taken to have been invalid and the slow path
 * is retried. It serves for what would be far too expensive to check up front.
 *
 * <p>All three are optional and they compose in the reverse of the order they are listed in: switch
 * points first, the exception catch inside them, and the guard further in. It can be seen in
 * {@link #compose}.
 *
 * <h2>Immutable</h2>
 *
 * <p>Every method that looks like a mutator —{@link #asType}, {@link #addSwitchPoint},
 * {@link #dropArguments}— returns a new instance. It has to be that way because one linked invocation
 * is shared between sites and between threads.
 *
 * @since 9
 */
public class GuardedInvocation {

    private final MethodHandle invocation;
    private final MethodHandle guard;
    private final SwitchPoint[] switchPoints;
    private final Class<? extends Throwable> exception;

    /**
     * An invocation with no conditions: it always holds.
     *
     * @param invocation the method handle
     */
    public GuardedInvocation(final MethodHandle invocation) {
        this(invocation, null, (SwitchPoint[]) null, null);
    }

    /**
     * With a guard.
     *
     * @param invocation the method handle
     * @param guard the guard, or {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard) {
        this(invocation, guard, (SwitchPoint[]) null, null);
    }

    /**
     * With a switch point.
     *
     * @param invocation the method handle
     * @param switchPoint the switch, or {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final SwitchPoint switchPoint) {
        this(invocation, null, switchPoint, null);
    }

    /**
     * With a guard and a switch point.
     *
     * @param invocation the method handle
     * @param guard the guard, or {@code null}
     * @param switchPoint the switch, or {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint switchPoint) {
        this(invocation, guard, switchPoint, null);
    }

    /**
     * With all three conditions, in the single-switch-point form.
     *
     * @param invocation the method handle
     * @param guard the guard, or {@code null}
     * @param switchPoint the switch, or {@code null}
     * @param exception the exception that invalidates the link, or {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint switchPoint, final Class<? extends Throwable> exception) {
        this(invocation, guard,
                switchPoint == null ? null : new SwitchPoint[] { switchPoint }, exception);
    }

    /**
     * With all three conditions and several switch points.
     *
     * <p>It is the constructor every other one calls.
     *
     * @param invocation the method handle; it cannot be {@code null}
     * @param guard the guard, or {@code null}
     * @param switchPoints the switches, or {@code null}
     * @param exception the exception that invalidates the link, or {@code null}
     */
    public GuardedInvocation(final MethodHandle invocation, final MethodHandle guard,
            final SwitchPoint[] switchPoints, final Class<? extends Throwable> exception) {
        this.invocation = Objects.requireNonNull(invocation);
        this.guard = guard;
        // Copied on the way in and on the way out: the array belongs to whoever passed it and we do
        // not want a later change to it to alter a link that is already in use.
        this.switchPoints = switchPoints == null ? null : switchPoints.clone();
        this.exception = exception;
    }

    /** The method handle to invoke. */
    public MethodHandle getInvocation() {
        return invocation;
    }

    /** The guard, or {@code null} if there is none. */
    public MethodHandle getGuard() {
        return guard;
    }

    /** The switches, or {@code null} if there are none. It is a copy. */
    public SwitchPoint[] getSwitchPoints() {
        return switchPoints == null ? null : switchPoints.clone();
    }

    /** The exception that invalidates the link, or {@code null}. */
    public Class<? extends Throwable> getException() {
        return exception;
    }

    /**
     * Whether any of the switches has already been thrown.
     *
     * <p>It serves to discard up front a link known to be dead, without getting as far as building it.
     */
    public boolean hasBeenInvalidated() {
        if (switchPoints == null) {
            return false;
        }
        for (final SwitchPoint sp : switchPoints) {
            if (sp.hasBeenInvalidated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The same invocation with another method handle and another guard, keeping the other conditions.
     *
     * @param newInvocation the new method handle
     * @param newGuard the new guard, or {@code null}
     * @return the derived invocation
     */
    public GuardedInvocation replaceMethods(final MethodHandle newInvocation,
            final MethodHandle newGuard) {
        return new GuardedInvocation(newInvocation, newGuard, switchPoints, exception);
    }

    /**
     * One more switch.
     *
     * @param newSwitchPoint the switch, or {@code null} to add nothing
     * @return the derived invocation, or {@code this} if there was nothing to add
     */
    public GuardedInvocation addSwitchPoint(final SwitchPoint newSwitchPoint) {
        if (newSwitchPoint == null) {
            return this;
        }
        final SwitchPoint[] updated;
        if (switchPoints == null) {
            updated = new SwitchPoint[] { newSwitchPoint };
        } else {
            updated = Arrays.copyOf(switchPoints, switchPoints.length + 1);
            updated[switchPoints.length] = newSwitchPoint;
        }
        return new GuardedInvocation(invocation, guard, updated, exception);
    }

    /**
     * Adapted to another signature, with Java's conversions.
     *
     * @param newType the requested signature
     * @return the adapted invocation
     */
    public GuardedInvocation asType(final MethodType newType) {
        return replaceMethods(invocation.asType(newType),
                guard == null ? null : guard.asType(guardType(guard, newType)));
    }

    /**
     * Adapted to another signature, with the languages' conversions as well.
     *
     * @param linkerServices the services that contribute those conversions
     * @param newType the requested signature
     * @return the adapted invocation
     */
    public GuardedInvocation asType(final LinkerServices linkerServices, final MethodType newType) {
        return replaceMethods(linkerServices.asType(invocation, newType),
                guard == null ? null
                        : linkerServices.asType(guard, guardType(guard, newType)));
    }

    /**
     * Like {@link #asType(LinkerServices, MethodType)}, but without degrading the return value.
     *
     * @param linkerServices the services
     * @param newType the requested signature
     * @return the adapted invocation, perhaps with a wider return than requested
     */
    public GuardedInvocation asTypeSafeReturn(final LinkerServices linkerServices,
            final MethodType newType) {
        return replaceMethods(linkerServices.asTypeLosslessReturn(invocation, newType),
                guard == null ? null
                        : linkerServices.asType(guard, guardType(guard, newType)));
    }

    /**
     * Adapted to a call site's signature.
     *
     * @param desc the site's descriptor
     * @return the adapted invocation
     */
    public GuardedInvocation asType(final CallSiteDescriptor desc) {
        return asType(desc.getMethodType());
    }

    /**
     * The signature the guard takes inside an invocation of signature {@code type}.
     *
     * <p>It is the invocation's, cut down to the parameters the guard looks at, and returning
     * {@code boolean}. The guard may take fewer parameters than the invocation —usually it looks only
     * at the receiver— and there would be no sense in forcing it to declare the ones it ignores.
     */
    private static MethodType guardType(final MethodHandle guard, final MethodType type) {
        return type.dropParameterTypes(guard.type().parameterCount(), type.parameterCount())
                .changeReturnType(boolean.class);
    }

    /**
     * With filters applied to some arguments.
     *
     * <p>The same filters go to the method handle and to the guard: if the incoming argument is
     * filtered, the guard has to have its opinion about the filtered value and not the original.
     *
     * @param pos the position of the first argument to filter
     * @param filters the filters
     * @return the derived invocation
     */
    public GuardedInvocation filterArguments(final int pos, final MethodHandle... filters) {
        return replaceMethods(MethodHandles.filterArguments(invocation, pos, filters),
                guard == null ? null : MethodHandles.filterArguments(guard, pos, filters));
    }

    /**
     * With extra arguments that are ignored.
     *
     * @param pos where to insert them
     * @param valueTypes the types of the ignored arguments
     * @return the derived invocation
     */
    public GuardedInvocation dropArguments(final int pos, final List<Class<?>> valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null ? null : MethodHandles.dropArguments(guard, pos, valueTypes));
    }

    /**
     * With extra arguments that are ignored.
     *
     * @param pos where to insert them
     * @param valueTypes the types of the ignored arguments
     * @return the derived invocation
     */
    public GuardedInvocation dropArguments(final int pos, final Class<?>... valueTypes) {
        return replaceMethods(MethodHandles.dropArguments(invocation, pos, valueTypes),
                guard == null ? null : MethodHandles.dropArguments(guard, pos, valueTypes));
    }

    /**
     * The final method handle: the invocation with its three conditions in place, and one and the
     * same fallback path for all three.
     *
     * @param fallback where to go when some condition does not hold
     * @return the composed method handle
     */
    public MethodHandle compose(final MethodHandle fallback) {
        return compose(fallback, fallback, fallback);
    }

    /**
     * The final method handle, with a different fallback for each condition.
     *
     * <p>The assembly goes from the inside out, and the order is not arbitrary. The guard sits
     * innermost because it is the one always evaluated and it has to come first. Then the exception
     * catch, which wraps both the invocation and the guard. The switch points end up outside
     * everything because they are the cheapest: if the switch has been thrown there is nothing to
     * enter at all.
     *
     * @param switchpointFallback where to go if a switch was thrown
     * @param guardFallback where to go if the guard returned {@code false}
     * @param catchFallback where to go if the exception was raised
     * @return the composed method handle
     */
    public MethodHandle compose(final MethodHandle switchpointFallback,
            final MethodHandle guardFallback, final MethodHandle catchFallback) {
        final MethodHandle guarded = guard == null ? invocation
                : MethodHandles.guardWithTest(guard, invocation, guardFallback);
        // catchException's handler receives the exception as its first argument, and the fallback does
        // not expect it: dropArguments adds that parameter in front so that it discards it.
        final MethodHandle caught = exception == null ? guarded
                : MethodHandles.catchException(guarded, exception,
                        MethodHandles.dropArguments(catchFallback, 0, exception));
        if (switchPoints == null) {
            return caught;
        }
        MethodHandle result = caught;
        for (final SwitchPoint sp : switchPoints) {
            result = sp.guardWithTest(result, switchpointFallback);
        }
        return result;
    }
}
