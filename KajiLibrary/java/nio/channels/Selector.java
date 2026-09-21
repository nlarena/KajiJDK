package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.function.Consumer;

/**
 * KajiLibrary's java.nio.channels.Selector — the one that watches many channels at a time.
 *
 * <p>A thread blocked in {@link #select()} wakes up when **one** of the registered channels has
 * something ready. That is what allows ten thousand connections to be attended with a handful of
 * threads instead of with ten thousand, which is the reason the whole of `java.nio` exists.
 *
 * <p>There are three sets of keys and confusing them is the classic mistake:
 *
 * <ul>
 *   <li>{@link #keys()} — everything registered. It is not touched from outside;
 *   <li>{@link #selectedKeys()} — what had activity. <strong>It has to be emptied by hand</strong>:
 *       the selector adds there but never removes, so a key that is not removed appears again in
 *       the next round even though it has nothing any more, and the loop spins empty for ever. It
 *       is the number one bug of whoever starts with selectors;
 *   <li>the cancelled ones — internal, they clean themselves up in the next selection.
 * </ul>
 *
 * <p>{@link #wakeup()} exists because a `select()` can stay still indefinitely and sometimes it has
 * to be got out of there without any channel having anything: shutting the server down, for
 * example. It is the only operation of the selector that can safely be called from another thread.
 *
 * <h2>State in this library</h2>
 *
 * <p><strong>There is a `Selector.open()`.</strong> This note used to say there was none, because
 * that static asks the system provider for the selector and this VM had no system provider for want
 * of network natives. It has them now: `open()` goes through {@link
 * java.nio.channels.spi.SelectorProvider#provider()}, which hands over the installed provider if
 * there is one and the in-house one if not.
 *
 * <p>Everything else is here, including the three forms with {@link Consumer} --which in the JDK
 * are not abstract either-- expressed in terms of the abstract ones. Whoever implements a selector
 * of their own inherits from {@link java.nio.channels.spi.AbstractSelector}, puts in their part,
 * and these work for them free of charge.
 */
public abstract class Selector implements Closeable {

    protected Selector() {
    }

    /**
     * Opens a selector.
     *
     * @return the selector
     * @throws IOException if it cannot be opened
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /** Whether the selector is still open. */
    public abstract boolean isOpen();

    /** The provider that made it. */
    public abstract SelectorProvider provider();

    /** Every registered key. The set cannot be modified from outside. */
    public abstract Set<SelectionKey> keys();

    /** The keys with activity. **It is emptied by hand**; see the note of the class. */
    public abstract Set<SelectionKey> selectedKeys();

    /** It looks and returns on the spot, whether or not there is something ready. */
    public abstract int selectNow() throws IOException;

    /**
     * Waits until there is something ready or until `timeout` milliseconds have passed.
     *
     * @param timeout `0` means waiting with no limit, not "do not wait"; that is {@link
     *     #selectNow()}
     */
    public abstract int select(long timeout) throws IOException;

    /** Waits with no limit. */
    public abstract int select() throws IOException;

    /**
     * Like {@link #select(long)}, but it runs `action` for each ready key instead of leaving them
     * in {@link #selectedKeys()}.
     *
     * <p>It is the form that cannot be used wrongly: the set of selected ones does not take part,
     * so there is nothing one can forget to empty.
     *
     * @return how many times `action` was run, which can be more than the number of keys if one
     *         became ready again during the same selection
     */
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout negativo");
        }
        return this.walk(action, this.select(timeout));
    }

    /** Like the other one, with no limit to the wait. */
    public int select(Consumer<SelectionKey> action) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        return this.walk(action, this.select());
    }

    /** Like the other one, without waiting at all. */
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        if (action == null) {
            throw new NullPointerException();
        }
        return this.walk(action, this.selectNow());
    }

    // The three forms with `Consumer` lean on the abstract ones and then empty the set: it is what
    // makes there be nothing the caller can forget to clean up.
    private int walk(Consumer<SelectionKey> action, int n) {
        if (n == 0) {
            return 0;
        }
        Set<SelectionKey> ready = this.selectedKeys();
        int runs = 0;
        // It is copied before walking it: `action` has the right to cancel keys, and cancelling
        // while the live set is being iterated is a `ConcurrentModificationException` waiting its
        // turn.
        Object[] copied = ready.toArray();
        ready.clear();
        int i = 0;
        while (i < copied.length) {
            action.accept((SelectionKey) copied[i]);
            runs = runs + 1;
            i = i + 1;
        }
        return runs;
    }

    /**
     * Wakes a blocked `select` up, or makes the next one not get as far as blocking.
     *
     * <p>The second matters as much as the first: if the notice were only good for a `select` that
     * had started already, whoever called just before it started would lose the waking and the
     * thread would stay asleep all the same.
     */
    public abstract Selector wakeup();

    /**
     * Closes the selector; the keys are left invalid and the channels are unregistered.
     *
     * <p>Without `throws IOException`, and the JDK declares it: this library's `java.io.Closeable`
     * does not declare it and §8.4.8.3 forbids widening. The divergence is born in `Closeable`; it
     * is noted the same way in {@link Channel}.
     */
    public abstract void close();
}
