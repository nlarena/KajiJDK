package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.Pipe — two joined channels, one that writes and one that reads.
 *
 * <p>It is the only selectable channel that does not talk to the world: what goes in through the
 * {@link SinkChannel sink} comes out through the {@link SourceChannel source}, inside the same
 * process. Its reason for being is to be able to **wake a selector up from another thread**: the
 * source is registered, and writing a byte into the sink makes the `select` return. It is how
 * {@link Selector#wakeup()} is implemented on several platforms.
 *
 * <h2>Why there is a `Pipe.open()`</h2>
 *
 * <p>This note used to explain why there was none, and the reason was never the one of the network
 * channels: a pipe could be implemented whole in memory --a queue of bytes between the two ends--
 * without touching the system. What stopped it was something else: both ends are
 * {@link AbstractSelectableChannel}, and a selectable channel is only of use if there is a
 * {@link Selector} to register it in. Without selectors a pipe came down to a queue of bytes with an
 * interface much more expensive than that of a queue of bytes, and its only purpose --waking a
 * selector up-- did not exist. Worse: {@link AbstractSelectableChannel#register} demands a
 * {@link Selector} and asks it for the key, so a pipe made here would have compiled, worked for
 * reading and writing, and **thrown at the moment of registering it**, which is just what one asked
 * for it for.
 *
 * <p>There are selectors now --see {@link Selector#open()}--, so `open()` is here and none of the
 * above had to change: the class and its two nested ones always had their types and their hierarchy
 * right.
 */
public abstract class Pipe {

    protected Pipe() {
    }

    /**
     * Opens a pipe.
     *
     * @return the pipe
     * @throws IOException if it cannot be opened
     */
    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }

    /** The end that is read through. */
    public abstract SourceChannel source();

    /** The end that is written through. */
    public abstract SinkChannel sink();

    /**
     * The reading end of a pipe.
     *
     * <p>It is a class and not an interface --in the JDK as well-- because it has to inherit the whole
     * machinery of a selectable channel; the only thing it adds is fixing {@link #validOps()} at
     * reading.
     */
    public abstract static class SourceChannel extends AbstractSelectableChannel
            implements ReadableByteChannel, ScatteringByteChannel {

        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }

        /** Reading only: nothing is ever written through this end. */
        public final int validOps() {
            return SelectionKey.OP_READ;
        }
    }

    /** The writing end of a pipe. */
    public abstract static class SinkChannel extends AbstractSelectableChannel
            implements WritableByteChannel, GatheringByteChannel {

        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }

        /** Writing only. */
        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }
    }
}
