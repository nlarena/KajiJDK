package com.sun.tools.attach;

/**
 * It was not possible to attach.
 *
 * <p>Different from {@link AgentLoadException}, and the difference is where it was cut off. Here
 * it did not even get to talk to the target VM. The three possible causes are that there should
 * be no {@link com.sun.tools.attach.spi.AttachProvider} installed, that none of those installed
 * should recognize that identifier, or that the target should not be a VM that accepts
 * connections.
 *
 * <p>That a provider should throw it is not necessarily a failure of the system, and that is why
 * {@link VirtualMachine#attach(String)} catches it and goes on with the next provider. It is the
 * way a provider has of saying "this identifier is not mine".
 */
public class AttachNotSupportedException extends Exception {

    private static final long serialVersionUID = 3391824968260177264L;

    /** With no detail. */
    public AttachNotSupportedException() {
        super();
    }

    /** With a message that explains the case. */
    public AttachNotSupportedException(String s) {
        super(s);
    }
}
