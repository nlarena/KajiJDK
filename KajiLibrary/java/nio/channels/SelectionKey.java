package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.SelectionKey — the note of a channel in a selector.
 *
 * <p>A key joins three things: **which channel**, **what it is interested in**
 * ({@link #interestOps()}) and **what it has ready** ({@link #readyOps()}). The distinction between
 * the last two is the whole mechanism: one declares what one wants to listen for, the selector
 * answers what happened, and the rest is reading bits.
 *
 * <p>The four bits are not consecutive --1, 4, 8, 16 and not 1, 2, 4, 8-- and it is not a
 * transcription error: it is like that in the JDK from the start and there is code with the numbers
 * written by hand, so changing them now would break it. They are copied as they are.
 *
 * <p>The {@link #attach attachment} exists so as not to have to keep a map from channel to state in
 * parallel: the key travels with the event, so hanging the context of the connection there is what
 * avoids the global `HashMap` and its lock.
 *
 * <h2>State in this library</h2>
 *
 * <p>The class is whole. What is abstract stays abstract --it is put in by whoever implements a
 * selector-- and what is computable is implemented: the four `is*` are a mask over {@link
 * #readyOps()}, the attachment is a field, and {@link #interestOpsOr}/{@link #interestOpsAnd} are
 * the same as the JDK's, read and set again.
 *
 * <p>Instances can be obtained: this note used to say they could not, because there were no
 * selectors to make. There are —see {@link Selector#open()}—, and
 * {@link java.nio.channels.spi.AbstractSelectionKey} goes on inheriting from here with the validity
 * part already solved, which is what whoever brings their own transport needs.
 */
public abstract class SelectionKey {

    /** Ready to read. */
    public static final int OP_READ = 1 << 0;

    /** Ready to write. */
    public static final int OP_WRITE = 1 << 2;

    /** Ready to finish connecting. */
    public static final int OP_CONNECT = 1 << 3;

    /** Ready to accept a connection. */
    public static final int OP_ACCEPT = 1 << 4;

    private Object attached;

    protected SelectionKey() {
    }

    /** The channel of this key. It returns it even if the key is cancelled. */
    public abstract SelectableChannel channel();

    /** The selector of this key. It returns it even if the key is cancelled. */
    public abstract Selector selector();

    /**
     * Whether the key is still valid. It stops being so on cancelling it, on closing the channel
     * or the selector.
     */
    public abstract boolean isValid();

    /**
     * Cancels the registration.
     *
     * <p>The key is left invalid on the spot, but the channel is taken out of the selector only in
     * the next selection: taking it out now would be modifying the set of keys underneath a
     * `select` that could be running in another thread.
     */
    public abstract void cancel();

    /** The operations that are being waited for. */
    public abstract int interestOps();

    /** Changes the operations that are waited for. */
    public abstract SelectionKey interestOps(int ops);

    /**
     * Adds `ops` to what is waited for and returns what was there before.
     *
     * <p>Returning the old value is what makes it useful against reading and setting separately:
     * two threads that add bits at the same time do not step on each other.
     */
    public int interestOpsOr(int ops) {
        synchronized (this) {
            int before = this.interestOps();
            this.interestOps(before | ops);
            return before;
        }
    }

    /** Leaves only the bits that are also in `ops`, and returns what was there before. */
    public int interestOpsAnd(int ops) {
        synchronized (this) {
            int before = this.interestOps();
            this.interestOps(before & ops);
            return before;
        }
    }

    /** The operations the selector found ready. */
    public abstract int readyOps();

    public final boolean isReadable() {
        return (this.readyOps() & OP_READ) != 0;
    }

    public final boolean isWritable() {
        return (this.readyOps() & OP_WRITE) != 0;
    }

    public final boolean isConnectable() {
        return (this.readyOps() & OP_CONNECT) != 0;
    }

    public final boolean isAcceptable() {
        return (this.readyOps() & OP_ACCEPT) != 0;
    }

    /** Hangs `ob` from the key and returns what was hanging before. `null` unhangs. */
    public final Object attach(Object ob) {
        Object before = this.attached;
        this.attached = ob;
        return before;
    }

    /** What hangs from the key, or `null`. */
    public final Object attachment() {
        return this.attached;
    }
}
