package com.sun.tools.attach;

/**
 * The agent could not be loaded into the target VM.
 *
 * <p>The typical causes are three, and all of them of the agent's side and not of the channel's.
 * That the JAR does not exist; that it exists but does not declare {@code Agent-Class} in its
 * manifest; or that the class it declares does not have the {@code agentmain} that is asked of
 * it.
 *
 * <p>When this happens the target VM is left <strong>intact</strong>: the agent did not get to
 * run. If it did get to run and failed, the exception is {@link AgentInitializationException},
 * and there the target was indeed left with what the agent managed to do.
 */
public class AgentLoadException extends Exception {

    private static final long serialVersionUID = -688265984016827025L;

    /** With no detail. */
    public AgentLoadException() {
        super();
    }

    /** With a message that explains the case. */
    public AgentLoadException(String s) {
        super(s);
    }
}
