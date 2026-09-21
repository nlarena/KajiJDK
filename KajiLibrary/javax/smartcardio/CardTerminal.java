package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardTerminal -- a reader.
 *
 * <p>It is obtained through {@link CardTerminals}. What is done with it is wait for a card to
 * appear and connect.
 *
 * <h2>The two {@code waitFor}s</h2>
 *
 * <p>{@link #waitForCardPresent} and {@link #waitForCardAbsent} block until the state changes or
 * the time runs out. A timeout of zero means <b>wait forever</b>, not "do not wait"; it is the
 * convention of {@code Object.wait} and getting it wrong hangs the program.
 */
public abstract class CardTerminal {

    /** For subclasses. */
    protected CardTerminal() {
    }

    /** What the reader is called. */
    public abstract String getName();

    /**
     * Connects to the card that is inserted.
     *
     * @param protocol {@code "T=0"}, {@code "T=1"} or {@code "*"} to negotiate
     * @throws CardNotPresentException if there is no card
     * @throws CardException if it could not connect
     */
    public abstract Card connect(String protocol) throws CardException;

    /**
     * Whether a card is inserted.
     *
     * @throws CardException if it could not be found out
     */
    public abstract boolean isCardPresent() throws CardException;

    /**
     * Waits for one to be inserted. See the class note.
     *
     * @param timeout milliseconds, or zero to wait forever
     * @return whether there is a card on return
     * @throws IllegalArgumentException if the time is negative
     * @throws CardException if it could not wait
     */
    public abstract boolean waitForCardPresent(long timeout) throws CardException;

    /**
     * Waits for it to be removed. See the class note.
     *
     * @param timeout milliseconds, or zero to wait forever
     * @return whether there is no card on return
     * @throws IllegalArgumentException if the time is negative
     * @throws CardException if it could not wait
     */
    public abstract boolean waitForCardAbsent(long timeout) throws CardException;
}
