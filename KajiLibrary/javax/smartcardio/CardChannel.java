package javax.smartcardio;

import java.nio.ByteBuffer;

/**
 * KajiLibrary's javax.smartcardio.CardChannel -- the way the commands go.
 *
 * <p>Channel 0 is the basic one and is not closed; the logical ones are opened with
 * {@link Card#openLogicalChannel} and closed with {@link #close}.
 *
 * <p>The two forms of {@link #transmit} do the same: one with objects, the other with buffers for
 * whoever wants to avoid the copy.
 */
public abstract class CardChannel {

    /** For subclasses. */
    protected CardChannel() {
    }

    /** Which card it belongs to. */
    public abstract Card getCard();

    /** Which channel number it is; 0 is the basic one. */
    public abstract int getChannelNumber();

    /**
     * Sends the command and waits for the answer.
     *
     * @throws CardException if the card does not answer
     */
    public abstract ResponseAPDU transmit(CommandAPDU command) throws CardException;

    /**
     * The same, with buffers.
     *
     * @return how many bytes were written into the response buffer
     * @throws CardException if the card does not answer
     */
    public abstract int transmit(ByteBuffer command, ByteBuffer response) throws CardException;

    /**
     * Closes the channel.
     *
     * @throws CardException if it is the basic one, which is not closed, or if it failed
     */
    public abstract void close() throws CardException;
}
