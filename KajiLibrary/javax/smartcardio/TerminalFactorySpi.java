package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.TerminalFactorySpi -- what a reader provider implements.
 *
 * <p>A provider that wants to give access to readers registers a {@code TerminalFactory} service
 * whose class extends this. {@link TerminalFactory} is the public face; this is the only thing that
 * has to be written.
 *
 * <p>The subclass's constructor receives the parameter passed to {@link
 * TerminalFactory#getInstance}, and that is where the configuration goes --which library to load,
 * which server to connect to--.
 */
public abstract class TerminalFactorySpi {

    /** For subclasses. */
    protected TerminalFactorySpi() {
    }

    /** This provider's readers. */
    protected abstract CardTerminals engineTerminals();
}
