package com.sun.tools.attach;

/**
 * The agent was loaded but its {@code agentmain} failed.
 *
 * <h2>Why it is different from {@link AgentLoadException}</h2>
 *
 * <p>Because the agent <strong>is already inside</strong> the target VM. With an
 * {@code AgentLoadException} nothing happened; with this one, the agent's code ran and threw
 * something, or returned a code other than zero. The target VM was left with whatever that
 * agent managed to do before failing, which is not the same as being left intact.
 *
 * <p>{@link #returnValue} is what a native agent returned. For one written in Java it is always
 * {@code 0}: there the failure arrives as an exception and not as a code.
 */
public class AgentInitializationException extends Exception {

    private static final long serialVersionUID = -1508756333332806353L;

    private final int returnValue;

    /** With no detail. */
    public AgentInitializationException() {
        super();
        this.returnValue = 0;
    }

    /** With a message. */
    public AgentInitializationException(String s) {
        super(s);
        this.returnValue = 0;
    }

    /** With a message and the code a native agent returned. */
    public AgentInitializationException(String s, int returnValue) {
        super(s);
        this.returnValue = returnValue;
    }

    /** The code the native {@code Agent_OnAttach} returned; {@code 0} for a Java agent. */
    public int returnValue() {
        return this.returnValue;
    }
}
