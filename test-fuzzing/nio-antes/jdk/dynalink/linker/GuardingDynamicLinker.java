package jdk.dynalink.linker;

/**
 * A linker: given a call site and the actual arguments, it produces the method to call.
 *
 * <p>It is Dynalink's central interface and the only one a dynamic language has to implement in
 * order to integrate. Everything else in the package exists to serve it.
 *
 * <h2>Why it returns a guarded invocation and not a method handle</h2>
 *
 * <p>Because linking is done once and used many times. The answer cannot be "for these arguments,
 * this method" --that would force asking again on every call-- but "as long as this condition holds,
 * this method". The condition is the {@link GuardedInvocation}'s guard, and it is what turns linking
 * into caching.
 *
 * <h2>What returning null means</h2>
 *
 * <p>That this linker does not know how to handle that site, not that the site is invalid. Linkers
 * are composed into a chain and the {@code null} is what passes the turn to the next one. Throwing
 * an exception, by contrast, breaks the chain.
 *
 * @since 9
 */
@FunctionalInterface
public interface GuardingDynamicLinker {

    /**
     * The method to call, with the condition under which it goes on holding.
     *
     * @param linkRequest the site and the actual arguments of the first invocation
     * @param linkerServices what the linker can ask of its host
     * @return the invocation with its guard, or {@code null} if this linker cannot handle it
     * @throws Exception if linking really fails; it breaks the chain of linkers
     */
    GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices)
            throws Exception;
}
