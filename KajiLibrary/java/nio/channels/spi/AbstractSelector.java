package java.nio.channels.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelector — the part of a selector that does not
 * depend on the system.
 *
 * <p>It solves three things and leaves the rest to whoever inherits it:
 *
 * <ul>
 *   <li><strong>The closing happens once.</strong> {@link #close()} is `final` and keeps the
 *       bookkeeping; the own part goes in {@link #implCloseSelector()};
 *   <li><strong>The cancelled ones gather in one place.</strong> {@link #cancelledKeys()} is the
 *       postbox where {@link AbstractSelectionKey#cancel()} leaves the keys; whoever implements the
 *       selection empties it at the start of each `select`, which is the only moment when they can
 *       touch their structures without a race;
 *   <li><strong>Dropping a key is a single step.</strong> {@link #deregister} invalidates the key
 *       and takes it out of the channel at the same time. Separating that leaves valid keys over
 *       dropped channels, which is the kind of state nobody understands afterwards.
 * </ul>
 *
 * <p>{@link #begin()} and {@link #end()} wrap the wait so that it can be interrupted. The same
 * caveats as in {@link AbstractInterruptibleChannel} apply here: this VM cannot get a thread out of
 * a started wait, so what there is is the pair and its contract, not the unblocking.
 *
 * <p>`Selector.open()` reaches the in-house selector, or the installed provider's; whoever
 * implements one of their own inherits from here and has the above done. This note used to say
 * there was no `open()` to reach any selector with, and that stopped being true when the VM grew
 * its network seam.
 */
public abstract class AbstractSelector extends Selector {

    private final Set<SelectionKey> cancelled = new HashSet<SelectionKey>();
    private final SelectorProvider theProvider;
    private boolean openFlag = true;

    protected AbstractSelector(SelectorProvider provider) {
        this.theProvider = provider;
    }

    // It is called by `AbstractSelectionKey.cancel()`. Package-private: the postbox is filled by
    // that road and not by any other, or it would stop being true that everything inside it was
    // really cancelled.
    void cancel(SelectionKey k) {
        synchronized (this.cancelled) {
            this.cancelled.add(k);
        }
    }

    /**
     * Closes the selector.
     *
     * <p>Without `throws IOException` --the JDK declares it-- because of the chain that starts in
     * `java.io.Closeable`; whatever {@link #implCloseSelector()} throws comes out wrapped in
     * {@link UncheckedIOException} so as not to lose the reason.
     */
    public final void close() {
        synchronized (this) {
            if (!this.openFlag) {
                return;
            }
            this.openFlag = false;
        }
        try {
            this.implCloseSelector();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The concrete closing, called a single time. */
    protected abstract void implCloseSelector() throws IOException;

    public final boolean isOpen() {
        return this.openFlag;
    }

    public final SelectorProvider provider() {
        return this.theProvider;
    }

    /**
     * The postbox of cancelled keys.
     *
     * <p>The live set is returned, not a copy: whoever implements `select` has to **empty it**
     * after processing it, and over a copy they could not.
     */
    protected final Set<SelectionKey> cancelledKeys() {
        return this.cancelled;
    }

    /**
     * Registers `ch` in this selector.
     *
     * <p>It is called by {@link AbstractSelectableChannel#register} and not by the user: the
     * checking that the channel is open, non-blocking and admits `ops` has happened there already.
     */
    protected abstract SelectionKey register(AbstractSelectableChannel ch, int ops, Object att);

    /**
     * Drops `key`: it invalidates it and takes it out of its channel.
     *
     * <p>Both things together on purpose; see the note of the class.
     */
    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).removeKey(key);
    }

    /** Marks the start of an interruptible wait. It goes in a pair with {@link #end()}. */
    protected final void begin() {
    }

    /** Closes the pair of {@link #begin()}. */
    protected final void end() {
    }
}
