package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.SelectableChannel — a channel a {@link Selector} can watch.
 *
 * <p>The whole idea of `java.nio` is in this class: instead of one thread per connection blocked in
 * its `read`, one thread watches a thousand channels and attends only the ones that have something.
 * To get into that wheel a channel has to be able to do two things: **not block**
 * ({@link #configureBlocking}) and **sign up** ({@link #register}).
 *
 * <p>The two go together and the order matters: registering a channel in blocking mode throws
 * {@link IllegalBlockingModeException}, because it would be asking a selector to give notice when
 * something is ready for a read that was going to sit waiting on its own anyway.
 *
 * <p>One key ({@link SelectionKey}) per channel **and per selector**: registering twice in the same
 * selector does not create a new key, it updates the one that was there. It is what makes
 * `register` idempotent and keeps phantom registrations from piling up.
 *
 * <h2>State in this library</h2>
 *
 * <p>The class is whole --its ten public members-- and there **are** selectable channels to make:
 * this note used to say there were none, because the only ones that are selectable in the JDK are
 * the network ones and this VM had no network natives. It has them, and `SocketChannel`,
 * `ServerSocketChannel` and `DatagramChannel` are selectable. The machinery underneath is here as
 * well: {@link java.nio.channels.spi.AbstractSelectableChannel} really implements the registration,
 * the blocking mode and the handling of keys, so whoever brings their own transport inherits from
 * there and it works for them without writing any of this.
 */
public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

    protected SelectableChannel() {
    }

    /** The provider that made it. */
    public abstract SelectorProvider provider();

    /**
     * The operations this kind of channel admits, in the set of bits of {@link SelectionKey}.
     *
     * <p>A listening channel admits `OP_ACCEPT` and nothing else; a connected one, reading and
     * writing. Registering asking for an operation that is not here is {@link
     * IllegalArgumentException}, and it is better than silence: an `OP_ACCEPT` over a connected
     * socket is never fulfilled, and without this check the symptom would be a selector that never
     * wakes up.
     */
    public abstract int validOps();

    /** Whether it is registered in some selector. */
    public abstract boolean isRegistered();

    /** The key of this channel in `sel`, or `null` if it is not registered there. */
    public abstract SelectionKey keyFor(Selector sel);

    /**
     * Signs the channel up in `sel` for the operations `ops`, with `att` hanging from the key.
     *
     * @throws ClosedChannelException if the channel is closed
     * @throws IllegalBlockingModeException if the channel is in blocking mode
     * @throws IllegalArgumentException if `ops` asks for something outside {@link #validOps()}
     */
    public abstract SelectionKey register(Selector sel, int ops, Object att)
            throws ClosedChannelException;

    /** Like the other one, with nothing hanging. */
    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return this.register(sel, ops, null);
    }

    /**
     * Puts the channel into blocking or non-blocking mode.
     *
     * @throws IllegalBlockingModeException if blocking is asked for while registered in a selector
     */
    public abstract SelectableChannel configureBlocking(boolean block) throws IOException;

    /** Whether it is in blocking mode. */
    public abstract boolean isBlocking();

    /**
     * The object to synchronise on so that the blocking mode does not change.
     *
     * <p>It is exposed and not hidden because the one who needs the guarantee is the code outside:
     * without a public lock, "put into non-blocking, do the operation, restore" is a race with any
     * other thread that touches the same channel.
     */
    public abstract Object blockingLock();
}
