package java.nio.channels.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.InterruptibleChannel;

/**
 * KajiLibrary's java.nio.channels.spi.AbstractInterruptibleChannel — the base of every channel.
 *
 * <p>It solves a single thing, and that is why it exists: that **closing be idempotent and happen
 * once**. The public `close()` is `final` and keeps the bookkeeping of the open bit; what each
 * channel has of its own goes in {@link #implCloseChannel()}, which is called exactly once per
 * channel even if it is closed ten times from five threads. Without this separation each channel
 * would reimplement the same `if (alreadyClosed) return;` and one of them would do it wrongly.
 *
 * <h2>What `begin()`/`end()` do here, and what they do not</h2>
 *
 * <p>In the JDK this pair wraps each blocking operation and serves for **aborting it from
 * outside**: `begin()` enrols an interruptor in the thread, and if somebody interrupts it while it
 * is inside, the channel is closed underneath it and the call blows up on the spot with
 * {@link ClosedByInterruptException}.
 *
 * <p>Here the detection is **on the way out, not in the middle**, and it is best said without
 * adornment: `end()` looks at whether the channel was closed or whether the thread was left
 * interrupted during the operation and only then throws. The reason is that this VM does not expose
 * the hook the JDK uses to unblock a thread stopped in a syscall. The difference does not show in
 * the channels this library makes with its own hands --the file ones, where no operation really
 * blocks-- but it would show in a network one, and that is why it is written down.
 *
 * <p>The contract that **is** fulfilled whole: if the channel was closed asynchronously while the
 * operation was running, the operation does not return a half result but
 * {@link AsynchronousCloseException}; and if the thread was interrupted, the channel is left closed
 * and {@link ClosedByInterruptException} comes out. A partial result of an abandoned operation is
 * exactly what these exceptions exist not to hand over.
 *
 * <h2>`close()` without `throws IOException`</h2>
 *
 * <p>The JDK declares it; here it cannot be done. This library's {@link Channel} inherits from
 * `java.io.Closeable`, whose `close()` does not declare it, and §8.4.8.3 forbids an override to
 * widen the checked exceptions. The divergence is born in `Closeable` and is dragged as far as
 * here. So as not to lose the reason, whatever {@link #implCloseChannel()} throws as an {@link
 * IOException} comes out wrapped in {@link UncheckedIOException}: the error is not swallowed, it
 * changes shape.
 */
public abstract class AbstractInterruptibleChannel implements Channel, InterruptibleChannel {

    // Without `volatile` on purpose: this VM does not guarantee that the keyword means what the JMM
    // says, and a `volatile` that orders nothing is worse than its absence because it invites
    // trust. What is guaranteed is the idempotence under the latch below.
    private boolean openFlag = true;

    // A latch of its own and not `this`: if the lock were the channel, anybody who synchronised on
    // somebody else's channel could block its closing.
    private final Object latch = new Object();

    // Mark of an asynchronous close that happened while there was an operation inside. It is what
    // separates "I was closed" from "I finished normally" when `end()` has to decide what to throw.
    private boolean closedDuringOp = false;

    protected AbstractInterruptibleChannel() {
    }

    /**
     * Closes the channel.
     *
     * <p>It is `final` because the point of the class is that nobody skip the bookkeeping; each
     * channel's own part goes in {@link #implCloseChannel()}.
     *
     * @throws UncheckedIOException if the concrete closing fails; see the note of the class
     */
    public final void close() {
        synchronized (this.latch) {
            if (!this.openFlag) {
                return;
            }
            this.openFlag = false;
            this.closedDuringOp = true;
        }
        try {
            this.implCloseChannel();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The concrete closing of this channel, called a single time.
     *
     * <p>When it runs, the channel appears closed already: whoever asks {@link #isOpen()} from
     * inside is going to see `false`, which is right --the closing has been decided, this is only
     * carrying it out--.
     */
    protected abstract void implCloseChannel() throws IOException;

    public final boolean isOpen() {
        synchronized (this.latch) {
            return this.openFlag;
        }
    }

    /**
     * Marks the start of an operation that could block.
     *
     * <p>It always goes in a pair with {@link #end}, and the `end` goes in a `finally`; if not, an
     * exception in the middle leaves the mark set and the next operation inherits a state that is
     * not its own.
     */
    protected final void begin() {
        synchronized (this.latch) {
            this.closedDuringOp = !this.openFlag;
        }
    }

    /**
     * Closes the pair of {@link #begin}.
     *
     * @param completed `true` if the operation got as far as completing
     * @throws AsynchronousCloseException if the channel was closed while the operation was running
     * @throws ClosedByInterruptException if the thread was left interrupted; the channel is left
     *     closed
     */
    protected final void end(boolean completed) throws AsynchronousCloseException {
        boolean closed;
        synchronized (this.latch) {
            closed = this.closedDuringOp || !this.openFlag;
        }
        // The interrupted one is looked at first because it is the cause and the closing is its
        // consequence: the other way round, an interruption would be reported as an anonymous close
        // and the why would be lost.
        if (Thread.currentThread().isInterrupted()) {
            this.close();
            throw new ClosedByInterruptException();
        }
        if (closed && !completed) {
            throw new AsynchronousCloseException();
        }
    }
}
