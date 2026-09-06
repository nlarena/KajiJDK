package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardedInvocationTransformer;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.support.SimpleLinkRequest;

/**
 * The linker: what decides, at each invocation, where it goes.
 *
 * <h2>The problem it solves</h2>
 *
 * <p>In a dynamic language, {@code a.b(c)} cannot be compiled into a call: what {@code a} is is not
 * known until the program runs. The way out is to leave the place empty -- a call site -- and fill it
 * in the first time control passes through, with a cheap check that says "as long as {@code a} stays
 * of this class, this will do". That is a guarded invocation.
 *
 * <h2>Why linking beats simply looking up every time</h2>
 *
 * <p>Because nearly always the same site sees the same type. A loop calling {@code list.size()} a
 * million times calls it on the same class all million times, so the lookup happens once and the
 * other 999,999 are a class comparison and a jump. That bet is the whole performance of a dynamic
 * language on the virtual machine.
 *
 * <h2>When the bet fails</h2>
 *
 * <p>A site that sees many different types -- {@code megamorphic} -- piles up guards that fail one
 * after another, and chaining more makes it worse. Hence a threshold: past so many relinks, the site
 * is declared unstable and from then on it is replaced whole instead of chained.
 * {@link RelinkableCallSite#resetAndRelink} is how that is asked for.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>{@link #getLinkerServices} works, and with it the half of the system that decides about types:
 * whether a conversion is possible, which of two targets is better, which linker serves which class.
 *
 * <p>{@link #link} cannot. Linking means building a method handle, and this virtual machine has no
 * method handles -- see {@code java.lang.invoke.MethodHandles}, which says so in its note. The
 * construction is written exactly as it goes, so what comes out is the
 * {@link UnsupportedOperationException} from the handle factory and not an error invented here.
 *
 * @since 9
 */
public final class DynamicLinker {

    private final LinkerServices services;
    private final GuardedInvocationTransformer prelink;
    private final boolean synchronize;
    private final int unstableThreshold;

    DynamicLinker(final LinkerServices services, final GuardedInvocationTransformer prelink,
            final boolean synchronize, final int unstableThreshold) {
        this.services = services;
        this.prelink = prelink;
        this.synchronize = synchronize;
        this.unstableThreshold = unstableThreshold;
    }

    /**
     * Links that call site.
     *
     * <p>It installs a handle that, the first time it is invoked, looks up where the call goes,
     * leaves the result installed, and carries on with the invocation. From then on the site goes
     * straight through for as long as the guard holds.
     *
     * @param <T> the type of the site
     * @param callSite the site
     * @return the same site, so calls can be chained
     * @throws NullPointerException if the site is {@code null}
     * @throws UnsupportedOperationException if the virtual machine has no method handles, which is
     *     the case for this one
     */
    public <T extends RelinkableCallSite> T link(final T callSite) {
        final MethodType type = callSite.getDescriptor().getMethodType();
        callSite.initialize(relinkAndInvoke(callSite, type, 0));
        return callSite;
    }

    /**
     * What the linker lends to the linkers.
     *
     * @return the services
     */
    public LinkerServices getLinkerServices() {
        return this.services;
    }

    /**
     * Where the site being relinked right now is.
     *
     * <p>It lets a language say "I cannot find method {@code b}" naming the line of the user's
     * program and not the linker's.
     *
     * @return always {@code null}: there can be no relink in progress, because {@link #link} never
     *     gets as far as installing anything. The JDK returns {@code null} in the same situation
     */
    public static StackTraceElement getLinkedCallSiteLocation() {
        return null;
    }

    /**
     * The handle that relinks the site and then invokes whatever ended up linked.
     *
     * <p>It is four combinators and the order matters: one that gathers every argument into an array
     * and calls {@link #relink}, and {@code foldArguments} so that what that returns -- another
     * handle -- is invoked with the original arguments. That way the first invocation pays for the
     * lookup and the following ones do not.
     */
    private MethodHandle relinkAndInvoke(final RelinkableCallSite callSite, final MethodType type,
            final int count) {
        final MethodHandle relink;
        try {
            relink = MethodHandles.lookup().findVirtual(DynamicLinker.class, "relink",
                    MethodType.methodType(MethodHandle.class, RelinkableCallSite.class, int.class,
                            Object[].class));
        } catch (final NoSuchMethodException e) {
            // The method is in this very class: if it cannot be found, the file is corrupt.
            throw new AssertionError(e);
        } catch (final IllegalAccessException e) {
            throw new AssertionError(e);
        }
        final MethodHandle bound =
                MethodHandles.insertArguments(relink, 0, this, callSite, Integer.valueOf(count));
        final MethodHandle gathering = bound.asCollector(Object[].class, type.parameterCount());
        return MethodHandles.foldArguments(MethodHandles.exactInvoker(type),
                gathering.asType(type.changeReturnType(MethodHandle.class)));
    }

    /**
     * Looks up where the site goes for these arguments, leaves it installed, and returns what to
     * invoke.
     *
     * <p>It is what the handle from {@link #relinkAndInvoke} runs. Past the threshold the site is
     * taken to be unstable: it is asked to replace everything instead of chaining, and the linker is
     * told as well -- through {@link LinkRequest#isCallSiteUnstable} -- because a linker that knows
     * the site is unstable can return a more general invocation with no guard.
     *
     * @param callSite the site
     * @param count how many times it has been relinked
     * @param arguments the arguments of this invocation
     * @return the handle to invoke
     * @throws Exception whatever the linker throws
     * @throws NoSuchDynamicMethodException if no linker knows how to serve the operation
     */
    MethodHandle relink(final RelinkableCallSite callSite, final int count,
            final Object[] arguments) throws Exception {
        final CallSiteDescriptor descriptor = callSite.getDescriptor();
        final boolean unstable = count >= this.unstableThreshold;
        final LinkRequest request = new SimpleLinkRequest(descriptor, unstable, arguments);
        GuardedInvocation invocation = this.services.getGuardedInvocation(request);
        if (invocation == null) {
            throw new NoSuchDynamicMethodException(descriptor.toString());
        }
        if (this.prelink != null) {
            invocation = this.prelink.filter(invocation, request, this.services);
        }
        final MethodHandle next = relinkAndInvoke(callSite, descriptor.getMethodType(), count + 1);
        if (this.synchronize) {
            synchronized (callSite) {
                install(callSite, invocation, next, unstable);
            }
        } else {
            install(callSite, invocation, next, unstable);
        }
        return invocation.getInvocation();
    }

    private static void install(final RelinkableCallSite callSite,
            final GuardedInvocation invocation, final MethodHandle next, final boolean unstable) {
        if (unstable) {
            callSite.resetAndRelink(invocation, next);
        } else {
            callSite.relink(invocation, next);
        }
    }
}
