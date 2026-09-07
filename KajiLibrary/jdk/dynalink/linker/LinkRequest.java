package jdk.dynalink.linker;

import jdk.dynalink.CallSiteDescriptor;

/**
 * What a linker is asked for: the call site plus the actual arguments.
 *
 * <h2>Why the arguments are needed, if the site already has the types</h2>
 *
 * <p>Because the site's static types are the calling language's, not the receiving object's. At a
 * {@code (Object,Object)Object} site the descriptor says nothing useful; what decides the link is
 * that the receiver is, at run time, an instance of such and such a class. This interface is what
 * exposes that fact.
 *
 * <h2>Why instability travels in here</h2>
 *
 * <p>{@link #isCallSiteUnstable} warns the linker that this site has already changed its mind too
 * many times — it is megamorphic. A linker that knows can return something more generic and cheaper
 * instead of a specialised invocation that will be invalidated straight away. Without this fact the
 * only possible strategy would be to always specialise, which is the worst one for the megamorphic
 * case.
 *
 * @since 9
 */
public interface LinkRequest {

    /** The call site's descriptor. */
    CallSiteDescriptor getCallSiteDescriptor();

    /**
     * The arguments of the invocation that triggered the link.
     *
     * <p>Returns a copy: they are mutable and the linker should not be able to touch the originals.
     */
    Object[] getArguments();

    /**
     * The first argument, or {@code null} if there is none.
     *
     * <p>It is a shortcut for the overwhelmingly commonest case, which is looking at the receiver. It
     * also avoids copying the whole array to read a single position.
     */
    Object getReceiver();

    /** Whether the site has been relinked so often that specialising is not worth it. */
    boolean isCallSiteUnstable();

    /**
     * The same request with another descriptor and other arguments.
     *
     * <p>It is used by a linker that decomposes one operation into another --resolving a method's
     * name and then delegating the invocation, say-- without losing the instability mark.
     *
     * @param newCallSiteDescriptor the new descriptor
     * @param newArguments the new arguments
     * @return the derived request
     */
    LinkRequest replaceArguments(CallSiteDescriptor newCallSiteDescriptor, Object... newArguments);
}
