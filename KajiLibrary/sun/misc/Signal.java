package sun.misc;

/**
 * A signal of the operating system, so that it can be attended to from Java.
 *
 * <h2>What it is used for</h2>
 *
 * <p>Almost always for the same thing: finding out that somebody asked for the process to end
 * --{@code SIGTERM} from a {@code kill}, {@code SIGINT} from a Ctrl-C-- and closing down in order.
 * Also for {@code SIGHUP}, which by convention means "reread your configuration".
 *
 * <p>It is {@code sun.misc} and was never public API; what there is for this in the official API is
 * {@code Runtime.addShutdownHook}, which covers the common case and allows neither telling which
 * signal arrived nor ignoring it.
 *
 * <h2>Why the number cannot be invented</h2>
 *
 * <p>The name of a signal is portable and its number is not. {@code SIGUSR1} is 10 on Linux on x86
 * and 30 on macOS; on Windows most of them simply do not exist. The number comes from asking the
 * operating system for the name, and it is the first thing the constructor of the JDK does.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The constructor throws {@link UnsupportedOperationException}. It is the same decision as in
 * {@code com.sun.security.auth.module.UnixSystem}: when only the operating system has the datum and
 * this VM cannot ask for it, inventing it is worse than failing.
 *
 * <p>Here the damage would be concrete. A wrong signal number does not give an error: it installs
 * the handler on <strong>another</strong> signal. A program that believes it is attending to
 * {@code SIGTERM} and is really attending to {@code SIGSEGV} behaves inexplicably, and the symptom
 * does not point anywhere near the cause.
 *
 * @since 1.2
 */
public final class Signal {

    private static final String NOT_THERE =
            "the operating system assigns the number of a signal to its name, and this VM cannot "
            + "ask it; an invented number would install the handler on another signal";

    /**
     * The signal with that name, without the {@code SIG} prefix.
     *
     * @param name the name, for example {@code "TERM"} or {@code "INT"}
     * @throws IllegalArgumentException if the operating system does not know that signal
     * @throws UnsupportedOperationException in this VM always; see the note of the class
     */
    public Signal(String name) {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The number the operating system gives this signal.
     *
     * @return the number
     */
    public int getNumber() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * The name of the signal, without the {@code SIG} prefix.
     *
     * @return the name
     */
    public String getName() {
        throw new UnsupportedOperationException(NOT_THERE);
    }

    /**
     * Whether the two are the same signal.
     *
     * <p>The note said "the same name and the same number", which is the JDK's rule; this one
     * compares <strong>identity</strong>. They cannot disagree here, because the constructor always
     * throws and no {@code Signal} can exist to compare -- but the day the constructor works, this
     * has to become the JDK's rule.
     *
     * @param other the other one
     * @return whether they are the same signal
     */
    public boolean equals(Object other) {
        return this == other;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Signal";
    }

    /**
     * It installs a handler for that signal and returns the one that was there.
     *
     * <p>Returning the previous one is not a detail: it is what allows chaining. A polite handler
     * does its own thing and then calls the one that was there, because it may have been the VM
     * that installed it --for {@code SIGQUIT}, for example, which is what produces the thread
     * dump--.
     *
     * @param sig the signal
     * @param handler the handler, or {@link SignalHandler#SIG_DFL} / {@link SignalHandler#SIG_IGN}
     * @return the handler that was there
     * @throws IllegalArgumentException if the signal cannot be attended to
     * @throws UnsupportedOperationException in this VM always
     */
    public static synchronized SignalHandler handle(Signal sig, SignalHandler handler)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException(
                "installing a signal handler needs the VM to register itself with the operating "
                + "system, and this VM does not");
    }

    /**
     * It sends that signal to the process itself.
     *
     * @param sig the signal
     * @throws IllegalArgumentException if the signal cannot be raised
     * @throws UnsupportedOperationException in this VM always
     */
    public static void raise(Signal sig) throws IllegalArgumentException {
        throw new UnsupportedOperationException(
                "raising a signal needs the operating system, and this VM does not reach it");
    }
}
