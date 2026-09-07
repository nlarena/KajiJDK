package jdk.dynalink.linker;

/**
 * Sees every already-linked invocation, together with the request that produced it, and may change
 * it.
 *
 * <h2>Why it is not a {@link MethodHandleTransformer}</h2>
 *
 * <p>Because it receives the {@link LinkRequest} and the {@link LinkerServices} besides the method
 * handle. That lets it decide <strong>per site</strong>: wrap property reads only, add an extra
 * guard when the receiver is of a certain class, count invocations per operation. A plain method
 * transformer has nothing to tell them apart with.
 *
 * <p>It is the hook the Dynalink host uses to instrument every link without touching any linker.
 *
 * @since 9
 */
@FunctionalInterface
public interface GuardedInvocationTransformer {

    /**
     * The transformed invocation.
     *
     * @param inv the linked invocation
     * @param linkRequest the request that produced it
     * @param linkerServices the host's services
     * @return the transformed one, or {@code inv} if there is nothing to change
     */
    GuardedInvocation filter(GuardedInvocation inv, LinkRequest linkRequest,
            LinkerServices linkerServices);
}
