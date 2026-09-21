package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractSelectableChannel — the registration and the blocking
 * mode, solved once and for all.
 *
 * <p>This is the class that makes bringing a transport of one's own to `java.nio` reasonable:
 * everything tangled about the registration is here and is not rewritten. What is left for whoever
 * inherits is two methods --{@link #implCloseSelectableChannel()} and
 * {@link #implConfigureBlocking(boolean)}-- and neither has any bookkeeping.
 *
 * <h2>The keys are kept in an array, not in a map</h2>
 *
 * <p>A channel is registered in one or two selectors; hardly ever in more. At those sizes a
 * `HashMap` costs more than it saves --hash, buckets, an entry object per registration-- and a
 * linear search over an array of three is faster. The JDK does the same and for the same reason.
 * The holes deleting leaves are reused before growing, so a channel that registers and drops out a
 * thousand times makes nothing grow.
 *
 * <h2>Why the blocking mode has a lock of its own</h2>
 *
 * <p>Registering demands that the channel be **not** blocking, and putting it into blocking mode
 * demands that it be **not** registered. Both conditions are looked at and acted on, so without a
 * common lock two threads can pass both checks and leave a blocking channel inside a selector,
 * which is precisely the state the two rules exist to prevent. That lock is {@link
 * #blockingLock()}, and it is exposed because the code outside needs the same guarantee.
 */
public abstract class AbstractSelectableChannel extends SelectableChannel {

    private final SelectorProvider theProvider;

    // The current keys; there may be holes at `null`. See the note of the class.
    private SelectionKey[] keys = null;
    private int count = 0;

    private final Object keysLatch = new Object();
    private final Object blockingLatch = new Object();

    private boolean blocking = true;

    protected AbstractSelectableChannel(SelectorProvider provider) {
        this.theProvider = provider;
    }

    public final SelectorProvider provider() {
        return this.theProvider;
    }

    // ---- keys ----------------------------------------------------------------------------------

    private void addKey(SelectionKey k) {
        synchronized (this.keysLatch) {
            if (this.keys == null) {
                this.keys = new SelectionKey[3];
            }
            int i = 0;
            while (i < this.keys.length) {
                if (this.keys[i] == null) {
                    this.keys[i] = k;
                    this.count = this.count + 1;
                    return;
                }
                i = i + 1;
            }
            // It is doubled instead of growing one at a time: registering in n selectors would cost
            // O(n^2) in copies if the array grew exactly what was needed each time.
            SelectionKey[] more = new SelectionKey[this.keys.length * 2];
            System.arraycopy(this.keys, 0, more, 0, this.keys.length);
            more[this.keys.length] = k;
            this.keys = more;
            this.count = this.count + 1;
        }
    }

    // It is called by `AbstractSelector.deregister`. It invalidates the key **and** takes it out:
    // separating that would leave valid keys pointing at a registration that no longer exists.
    void removeKey(SelectionKey k) {
        synchronized (this.keysLatch) {
            if (this.keys == null) {
                return;
            }
            int i = 0;
            while (i < this.keys.length) {
                if (this.keys[i] == k) {
                    this.keys[i] = null;
                    this.count = this.count - 1;
                    break;
                }
                i = i + 1;
            }
        }
        ((AbstractSelectionKey) k).invalidate();
    }

    public final boolean isRegistered() {
        synchronized (this.keysLatch) {
            return this.count != 0;
        }
    }

    public final SelectionKey keyFor(Selector sel) {
        synchronized (this.keysLatch) {
            if (this.keys == null) {
                return null;
            }
            int i = 0;
            while (i < this.keys.length) {
                SelectionKey k = this.keys[i];
                if (k != null && k.selector() == sel) {
                    return k;
                }
                i = i + 1;
            }
            return null;
        }
    }

    /**
     * Signs the channel up in `sel`.
     *
     * <p>If it was signed up there already it **does not create a new key**: it changes the
     * operations and the attachment of the one that was there. It is what makes registering again
     * safe, and without that a reactor loop that reasserts its interest on every round would pile
     * up registrations until it ran out of memory.
     */
    public final SelectionKey register(Selector sel, int ops, Object att)
            throws ClosedChannelException {
        if (sel == null) {
            throw new NullPointerException();
        }
        if ((ops & ~this.validOps()) != 0) {
            throw new IllegalArgumentException("operation not admitted by this channel");
        }
        synchronized (this.blockingLatch) {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.blocking) {
                throw new IllegalBlockingModeException();
            }
            SelectionKey k = this.keyFor(sel);
            if (k != null) {
                // An `interestOps` over a cancelled key throws, and the cancelling could have
                // happened between the `keyFor` and this: it is let up as it is, because recycling
                // a cancelled key would be resurrecting a registration somebody asked to end.
                k.interestOps(ops);
                k.attach(att);
                return k;
            }
            SelectionKey fresh = ((AbstractSelector) sel).register(this, ops, att);
            this.addKey(fresh);
            return fresh;
        }
    }

    // ---- closing ---------------------------------------------------------------------------------

    /**
     * Closes the channel and cancels all of its keys.
     *
     * <p>The order matters: first the channel's own part, then the keys. The other way round, a
     * selector could wake up for a channel that is closed already and hand over a ready key over
     * nothing.
     */
    protected final void implCloseChannel() throws IOException {
        this.implCloseSelectableChannel();
        synchronized (this.keysLatch) {
            if (this.keys == null) {
                return;
            }
            int i = 0;
            while (i < this.keys.length) {
                SelectionKey k = this.keys[i];
                if (k != null) {
                    k.cancel();
                }
                i = i + 1;
            }
        }
    }

    /** The concrete closing of this channel. */
    protected abstract void implCloseSelectableChannel() throws IOException;

    // ---- blocking mode ---------------------------------------------------------------------------

    public final boolean isBlocking() {
        synchronized (this.blockingLatch) {
            return this.blocking;
        }
    }

    public final Object blockingLock() {
        return this.blockingLatch;
    }

    /**
     * Changes the mode.
     *
     * <p>Going back to blocking while registered is {@link IllegalBlockingModeException}: a
     * selector that watches a blocking channel cannot fulfil what it promises.
     */
    public final SelectableChannel configureBlocking(boolean block) throws IOException {
        synchronized (this.blockingLatch) {
            if (!this.isOpen()) {
                throw new ClosedChannelException();
            }
            if (this.blocking == block) {
                return this;
            }
            if (block && this.isRegistered()) {
                throw new IllegalBlockingModeException();
            }
            this.implConfigureBlocking(block);
            this.blocking = block;
        }
        return this;
    }

    /** The concrete change of mode. It is only called when the mode really changes. */
    protected abstract void implConfigureBlocking(boolean block) throws IOException;
}
