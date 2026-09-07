package jdk.dynalink.linker.support;

import java.util.Objects;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.LinkRequest;

/**
 * The obvious implementation of {@link LinkRequest}: it keeps what it is given.
 *
 * <p>It is immutable, and that is why {@link #getArguments} copies the array on the way out just as
 * the constructor copies it on the way in. Without both copies the immutability would be a pretence:
 * whoever built the request, or whoever read the arguments, could change them afterwards under the
 * linker that is looking at them.
 *
 * @since 9
 */
public class SimpleLinkRequest implements LinkRequest {

    private final CallSiteDescriptor callSiteDescriptor;
    private final Object[] arguments;
    private final boolean callSiteUnstable;

    /**
     * A new request.
     *
     * @param callSiteDescriptor the site's descriptor
     * @param callSiteUnstable whether the site has already been relinked too many times
     * @param arguments the invocation's arguments
     */
    public SimpleLinkRequest(final CallSiteDescriptor callSiteDescriptor,
            final boolean callSiteUnstable, final Object... arguments) {
        this.callSiteDescriptor = Objects.requireNonNull(callSiteDescriptor);
        this.callSiteUnstable = callSiteUnstable;
        this.arguments = Objects.requireNonNull(arguments).clone();
    }

    /** {@inheritDoc} */
    public Object[] getArguments() {
        return arguments.clone();
    }

    /** {@inheritDoc} */
    public Object getReceiver() {
        return arguments.length > 0 ? arguments[0] : null;
    }

    /** {@inheritDoc} */
    public CallSiteDescriptor getCallSiteDescriptor() {
        return callSiteDescriptor;
    }

    /** {@inheritDoc} */
    public boolean isCallSiteUnstable() {
        return callSiteUnstable;
    }

    /** {@inheritDoc} */
    public LinkRequest replaceArguments(final CallSiteDescriptor newCallSiteDescriptor,
            final Object... newArguments) {
        return new SimpleLinkRequest(newCallSiteDescriptor, callSiteUnstable, newArguments);
    }
}
