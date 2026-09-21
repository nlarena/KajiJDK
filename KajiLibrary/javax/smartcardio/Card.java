package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.Card -- a connected card.
 *
 * <p>It is obtained with {@link CardTerminal#connect} and released with {@link #disconnect}. While
 * it lasts, the commands go through a {@link CardChannel}.
 *
 * <h2>The basic channel and the logical ones</h2>
 *
 * <p>{@link #getBasicChannel} is channel 0, the one that always exists. {@link #openLogicalChannel}
 * opens another, with its own file-selection state, so that two parts of the program can use the
 * card without stepping on each other. Not every card supports them.
 *
 * <h2>{@link #beginExclusive}</h2>
 *
 * <p>It locks the reader for this thread. It goes with {@link #endExclusive} in a {@code finally}:
 * otherwise the reader stays taken until the process ends and no other program can use the card.
 */
public abstract class Card {

    /** For subclasses. */
    protected Card() {
    }

    /** What the card answered when it powered up. */
    public abstract ATR getATR();

    /** Which protocol was negotiated: {@code "T=0"} or {@code "T=1"}. */
    public abstract String getProtocol();

    /** Channel 0. See the class note. */
    public abstract CardChannel getBasicChannel();

    /**
     * Opens a logical channel.
     *
     * @throws CardException if the card cannot open it
     */
    public abstract CardChannel openLogicalChannel() throws CardException;

    /**
     * Takes the reader for this thread. See the class note.
     *
     * @throws CardException if it could not
     */
    public abstract void beginExclusive() throws CardException;

    /**
     * Releases it.
     *
     * @throws CardException if this thread did not hold it
     */
    public abstract void endExclusive() throws CardException;

    /**
     * A command for the <b>reader</b>, not for the card.
     *
     * @throws CardException if the reader rejects it
     */
    public abstract byte[] transmitControlCommand(int controlCode, byte[] command)
        throws CardException;

    /**
     * Releases the card.
     *
     * @param reset whether it also has to be reset
     * @throws CardException if it could not
     */
    public abstract void disconnect(boolean reset) throws CardException;
}
