package java.nio.channels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

// This library's selector: `poll` over the sockets of the registered channels.
//
// ===============================================================================================
// WHAT MAKES A SELECTOR POSSIBLE AT ALL
// ===============================================================================================
//
// One system call: the one that answers "which of these has something" **without reading**. Every
// other way of finding out consumes what it finds, and a selector that consumed would hand the
// program a ready channel with the data already gone. That call is `poll`, and until the VM had it
// this class could not exist -- see `jdk.internal.net.Net.poll`.
//
// ===============================================================================================
// WAKING UP
// ===============================================================================================
//
// A `select()` with no timeout waits until something happens, and sometimes nothing will: the
// program wants to shut down, or has just registered a new channel that the poll already in flight
// knows nothing about. `wakeup()` is the way out, and the way it works is the classic one -- a pipe
// whose read end is polled along with everything else, and a byte written into its write end. That
// is why the selector opens a pipe of its own in its constructor.
//
// ===============================================================================================
// THE THREE SETS
// ===============================================================================================
//
// `keys()` is everything registered, `selectedKeys()` is what had activity, and the cancelled ones
// are internal. **`selectedKeys()` has to be drained by the program**: the selector adds to it and
// never removes, so a key left in it comes back next turn even with nothing to report and the loop
// spins. It is the commonest bug with this API and it is the JDK's design, not ours.
final class KajiSelector extends AbstractSelector {

    private final Set<SelectionKey> keys = new HashSet<SelectionKey>();
    private final Set<SelectionKey> selected = new HashSet<SelectionKey>();
    private final Set<SelectionKey> keysView = Collections.unmodifiableSet(this.keys);

    /** The pipe `wakeup` writes into, and whose read end is polled with everything else. */
    private final SocketChannel wakeupRead;
    private final SocketChannel wakeupWrite;

    /** Set when a wake-up is seen, cleared by the waiting loop. */
    private boolean wokenUp;

    KajiSelector(SelectorProvider provider) throws IOException {
        super(provider);
        final SocketChannel[] pair = LoopbackPair.open();
        this.wakeupRead = pair[0];
        this.wakeupWrite = pair[1];
        this.wakeupRead.configureBlocking(false);
        this.wakeupWrite.configureBlocking(false);
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        final KajiSelectionKey k = new KajiSelectionKey(ch, this, ops);
        k.attach(att);
        synchronized (this.keys) {
            this.keys.add(k);
        }
        return k;
    }

    @Override
    public Set<SelectionKey> keys() {
        ensureOpen();
        return this.keysView;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        ensureOpen();
        return this.selected;
    }

    @Override
    public int selectNow() throws IOException {
        return doSelect(0);
    }

    @Override
    public int select(long timeout) throws IOException {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout");
        }
        return doSelect(timeout == 0 ? -1 : timeout);
    }

    @Override
    public int select() throws IOException {
        return doSelect(-1);
    }

    /**
     * One turn: drop what was cancelled, ask the system, and mark what came back.
     *
     * @param timeout milliseconds, 0 to ask and return, negative to wait as long as it takes
     * @return how many keys had activity that they were not already reporting
     */
    private int doSelect(long timeout) throws IOException {
        ensureOpen();
        final long deadline = timeout < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeout;
        while (true) {
            final int n = oneRound();
            if (n > 0 || timeout == 0 || System.currentTimeMillis() >= deadline || woken()) {
                return n;
            }
        }
    }

    /**
     * Whether a wake-up arrived and was consumed, which ends the wait whatever else happened.
     *
     * <p>It is read and cleared here rather than in the round itself so that a wake-up delivered
     * during the last round still ends the loop.
     */
    private boolean woken() {
        synchronized (this) {
            final boolean w = this.wokenUp;
            this.wokenUp = false;
            return w;
        }
    }

    /**
     * One turn of the wait.
     *
     * <p>The wait is sliced because **no native of this VM blocks for long**: a native that parks
     * holds every other Java thread down, including the one that was going to call
     * {@link #wakeup()}. `Net.poll` therefore returns after at most a slice, and the loop above is
     * what turns those slices back into the timeout the caller asked for.
     */
    private int oneRound() throws IOException {
        dropCancelled();

        final List<KajiSelectionKey> watched = new ArrayList<KajiSelectionKey>();
        final List<Integer> handles = new ArrayList<Integer>();
        final List<Integer> wanted = new ArrayList<Integer>();
        synchronized (this.keys) {
            for (final SelectionKey k : this.keys) {
                final KajiSelectionKey kk = (KajiSelectionKey) k;
                if (!kk.isValid()) {
                    continue;
                }
                final int h = handleOf(kk.channel());
                if (h < 0) {
                    continue;
                }
                final int events = pollEventsFor(kk.channel(), kk.rawInterestOps());
                if (events == 0) {
                    continue;
                }
                watched.add(kk);
                handles.add(Integer.valueOf(h));
                wanted.add(Integer.valueOf(events));
            }
        }

        // The wake-up end goes last so that its answer is easy to find, and it is always watched:
        // that is the whole point of it.
        final int n = watched.size();
        final int[] hs = new int[n + 1];
        final int[] es = new int[n + 1];
        final int[] rs = new int[n + 1];
        for (int i = 0; i < n; i++) {
            hs[i] = handles.get(i).intValue();
            es[i] = wanted.get(i).intValue();
        }
        hs[n] = handleOf(this.wakeupRead);
        es[n] = jdk.internal.net.Net.POLL_READ;

        begin();
        final int ready;
        try {
            // A slice, not the caller's timeout: see the note on this method.
            ready = jdk.internal.net.Net.poll(hs, es, rs, SLICE_MS);
        } finally {
            end();
        }
        if (ready < 0) {
            throw new IOException("poll failed");
        }

        if ((rs[n] & jdk.internal.net.Net.POLL_READ) != 0) {
            drainWakeup();
            synchronized (this) {
                this.wokenUp = true;
            }
        }

        int updated = 0;
        for (int i = 0; i < n; i++) {
            final KajiSelectionKey k = watched.get(i);
            final int ops = opsFrom(k.channel(), k.rawInterestOps(), rs[i]);
            if (ops == 0) {
                continue;
            }
            synchronized (this.selected) {
                if (this.selected.contains(k)) {
                    // Already reported and not yet drained: the ops are merged, and the key does
                    // not count again. That is what the JDK's return value means -- new keys, not
                    // ready ones.
                    k.readyOps(k.readyOps() | ops);
                } else {
                    k.readyOps(ops);
                    this.selected.add(k);
                    updated++;
                }
            }
        }
        dropCancelled();
        return updated;
    }

    /** How long one round waits. The native caps it at the same figure; see its note. */
    private static final int SLICE_MS = 50;

    /**
     * What to ask the system for, given what the program is interested in.
     *
     * <p>Four interest bits map onto two system ones: accepting is reading, and finishing a connect
     * is writing. That is not an approximation -- it is what the system reports.
     */
    private static int pollEventsFor(SelectableChannel ch, int ops) {
        int e = 0;
        if ((ops & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) {
            e |= jdk.internal.net.Net.POLL_READ;
        }
        if ((ops & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
            e |= jdk.internal.net.Net.POLL_WRITE;
        }
        return e;
    }

    /** The way back: system bits into the interest bits the program asked about. */
    private static int opsFrom(SelectableChannel ch, int interest, int revents) {
        int ops = 0;
        if ((revents & jdk.internal.net.Net.POLL_READ) != 0) {
            ops |= interest & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
        }
        if ((revents & jdk.internal.net.Net.POLL_WRITE) != 0) {
            ops |= interest & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
        }
        if ((revents & jdk.internal.net.Net.POLL_ERROR) != 0) {
            // An error is reported as every readiness the program asked about: whichever operation
            // it goes on to attempt is the one that will tell it what happened, which is better
            // than a channel that never comes up again.
            ops |= interest;
        }
        return ops;
    }

    /** The VM handle behind a channel, or -1 when it has none. */
    private static int handleOf(SelectableChannel ch) {
        if (ch instanceof KajiSocketChannel) {
            return ((KajiSocketChannel) ch).pollHandle();
        }
        if (ch instanceof KajiServerSocketChannel) {
            return ((KajiServerSocketChannel) ch).pollHandle();
        }
        if (ch instanceof KajiDatagramChannel) {
            return ((KajiDatagramChannel) ch).pollHandle();
        }
        if (ch instanceof KajiPipe.Source) {
            return handleOf(((KajiPipe.Source) ch).inner());
        }
        if (ch instanceof KajiPipe.Sink) {
            return handleOf(((KajiPipe.Sink) ch).inner());
        }
        return -1;
    }

    @Override
    public Selector wakeup() {
        try {
            this.wakeupWrite.write(java.nio.ByteBuffer.wrap(new byte[] {1}));
        } catch (IOException ignored) {
            // A wake-up that cannot be delivered leaves the select waiting, which is bad, but there
            // is nothing to report it to: `wakeup` does not throw and its callers are shutting
            // down.
        }
        return this;
    }

    /** Empties the wake-up pipe so the next select does not find it ready straight away. */
    private void drainWakeup() {
        final java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(64);
        try {
            while (this.wakeupRead.read(b) > 0) {
                b.clear();
            }
        } catch (IOException ignored) {
            // Same: nothing to report it to, and the next turn will find it again.
        }
    }

    /** Takes the cancelled keys out of every set, which is where a cancellation actually lands. */
    private void dropCancelled() {
        final Set<SelectionKey> cancelled = cancelledKeys();
        synchronized (cancelled) {
            if (cancelled.isEmpty()) {
                return;
            }
            for (final SelectionKey k : cancelled) {
                synchronized (this.keys) {
                    this.keys.remove(k);
                }
                synchronized (this.selected) {
                    this.selected.remove(k);
                }
                // Through `deregister`, which is how the JDK does it (`AbstractSelector.deregister`):
                // `removeKey` is package-private and lives in `java.nio.channels.spi`, so calling it from
                // here is not valid Java. `deregister` does exactly that line.
                if (k instanceof AbstractSelectionKey
                        && k.channel() instanceof AbstractSelectableChannel) {
                    deregister((AbstractSelectionKey) k);
                }
            }
            cancelled.clear();
        }
    }

    @Override
    protected void implCloseSelector() throws IOException {
        // Waking up first: a thread parked in `select()` has to come out before the sockets under
        // it are closed, or it would find them gone mid-poll.
        wakeup();
        synchronized (this.keys) {
            for (final SelectionKey k : new ArrayList<SelectionKey>(this.keys)) {
                k.cancel();
            }
        }
        dropCancelled();
        this.wakeupRead.close();
        this.wakeupWrite.close();
    }

    private void ensureOpen() {
        if (!this.isOpen()) {
            throw new ClosedSelectorException();
        }
    }
}
