package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * The call-site side that {@link DynamicLinker} knows how to drive.
 *
 * <p>The life cycle has an order the interface cannot enforce but the contract does:
 * {@link #initialize} exactly once, from {@link DynamicLinker#link}, and after that {@link #relink}
 * or {@link #resetAndRelink} as many times as needed. The difference between the last two is what
 * defines the caching strategy: `relink` may pile the new invocation on top of the earlier ones (a
 * polymorphic chain), `resetAndRelink` forces them to be thrown away — it is what the linker asks
 * for when it has decided the site is **unstable** and chaining would only waste memory.
 *
 * <p>Extending {@code jdk.dynalink.support.AbstractRelinkableCallSite} is almost always preferable
 * to implementing this from scratch.
 *
 * @since 9
 */
public interface RelinkableCallSite {

    /** Installs the initial invocation; {@link DynamicLinker#link} calls it exactly once. */
    void initialize(MethodHandle relinkAndInvoke);

    /** The site's descriptor; it does not change for its whole life. */
    CallSiteDescriptor getDescriptor();

    /** Adds a linked invocation, keeping whatever was there. */
    void relink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);

    /** Replaces everything linked: the site was deemed unstable and chaining does not pay. */
    void resetAndRelink(GuardedInvocation guardedInvocation, MethodHandle relinkAndInvoke);
}
