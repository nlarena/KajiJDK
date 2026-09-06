package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;

import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * What to do when a member the class does not have is asked for.
 *
 * <h2>Why it is configurable</h2>
 *
 * <p>Because each language answers {@code obj.doesNotExist} differently. Java does not compile.
 * JavaScript returns {@code undefined}. Another may want an exception with a message in its own
 * language, or to consult a prototype object before giving up.
 *
 * <p>Without this hook, {@link BeansLinker} would have to pick one of those answers for everybody,
 * and whichever it picked would be wrong for most.
 *
 * <h2>Returning {@code null} is not the same as failing</h2>
 *
 * <p>{@code null} means "I have nothing to contribute for this case", and linking carries on its
 * normal course — which ends in a {@link jdk.dynalink.NoSuchDynamicMethodException}. Returning a
 * handle, on the other hand, leaves the site linked to it, and that is how
 * {@code obj.doesNotExist} comes to be worth something instead of blowing up.
 *
 * @since 9
 */
@FunctionalInterface
public interface MissingMemberHandlerFactory {

    /**
     * The method to answer a member that does not exist with.
     *
     * @param linkRequest the request that could not be satisfied
     * @param linkerServices the host's services
     * @return the method, or {@code null} to let it fail as usual
     * @throws Exception if the construction fails
     */
    MethodHandle createMissingMemberHandler(LinkRequest linkRequest, LinkerServices linkerServices)
            throws Exception;
}
