package com.sun.jdi.connect;

/**
 * The VM was launched but the debugging connection was not established.
 *
 * <p>What makes it useful is that it **brings the process**. When a
 * {@link LaunchingConnector} fails, the reason is almost always in the error output of the VM
 * that has just been launched -- a misspelt `-agentlib:jdwp` option, a busy port -- and without
 * the `Process` that output is lost along with the orphaned process.
 *
 * <p>Whoever catches this exception then has two duties: to read the process's streams in order
 * to know what happened, and to end it.
 */
public class VMStartException extends Exception {

    private static final long serialVersionUID = 6408644824640801020L;

    /** The launched VM's process. Package-private, as in the JDK. */
    Process process;

    /**
     * A failure about that process, with no detail.
     *
     * @param process the VM that was launched
     */
    public VMStartException(Process process) {
        super();
        this.process = process;
    }

    /**
     * A failure about that process, with a detail.
     *
     * @param s the detail
     * @param process the VM that was launched
     */
    public VMStartException(String s, Process process) {
        super(s);
        this.process = process;
    }

    /** The launched VM's process; see the class note about what to do with it. */
    public Process process() {
        return this.process;
    }
}
