package sun.misc;

/**
 * What to do when a signal of the operating system arrives.
 *
 * <h2>The two constants are not ordinary handlers</h2>
 *
 * <p>{@link #SIG_DFL} and {@link #SIG_IGN} run no Java code: they are marks that tell the operating
 * system "do the usual thing for me" and "do not tell me". Passing them to {@link Signal#handle}
 * uninstalls the Java handler instead of installing another one.
 *
 * <p>That is why calling their {@link #handle} directly makes no sense, and that is why the names
 * are those of C --{@code SIG_DFL}, {@code SIG_IGN}-- and not something more Java-like: they
 * represent the same as they represent over there.
 *
 * <h2>What can be done inside</h2>
 *
 * <p>Little. The handler runs in a separate thread the VM raises for this, but the process may be
 * about to die: with {@code SIGTERM} the only reasonable thing is to let somebody know and return
 * quickly. A handler that hangs keeps the process from ending.
 *
 * @since 1.2
 */
public interface SignalHandler {

    /**
     * The handler the operating system had before Java got involved.
     *
     * <p>Installing it returns the signal to its normal behaviour: with {@code SIGINT}, ending the
     * process.
     */
    SignalHandler SIG_DFL = new Mark("SIG_DFL");

    /** The mark of "ignore this signal". */
    SignalHandler SIG_IGN = new Mark("SIG_IGN");

    /**
     * It attends to the signal.
     *
     * @param sig the signal that arrived
     */
    void handle(Signal sig);
}

/**
 * The two marks of {@link SignalHandler}.
 *
 * <p>With a name and not anonymous because of #482: the bytecode generator does not emit an
 * anonymous class that is in the initialiser of a field. Besides, it reads better in a stack dump
 * than a {@code SignalHandler$1}.
 */
final class Mark implements SignalHandler {

    private final String name;

    Mark(String name) {
        this.name = name;
    }

    public void handle(Signal sig) {
        throw new UnsupportedOperationException(
                name + " is a mark for the operating system, not a handler that runs");
    }

    public String toString() {
        return name;
    }
}
