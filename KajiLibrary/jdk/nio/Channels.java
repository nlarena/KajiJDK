package jdk.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * The door for wrapping a borrowed file descriptor in a selectable channel.
 *
 * <h2>Which problem it solves</h2>
 *
 * <p>NIO knows how to make channels over the things it opens itself: a socket, a file, a pipe. What
 * it does not know how to do is take a descriptor <em>somebody else</em> got hold of —a native
 * library, a parent process that passed it down, a device opened through JNI— and put it into a
 * {@link java.nio.channels.Selector}. This method is that seam, and that is why it lives in a module
 * apart and not in {@code java.nio.channels}: it is a service door, not general API.
 *
 * <h2>Why the {@link SelectableChannelCloser} is needed</h2>
 *
 * <p>Because the descriptor <strong>does not belong to whoever wraps it</strong>. An ordinary
 * channel closes its descriptor when it is closed, and that would be a mistake here: the owner may
 * go on using it. Since NIO cannot know which the right policy is, it delegates it — the closing and
 * the releasing are asked of an object the caller provides.
 *
 * <p>That there are <em>two</em> methods and not one is the fine part. {@code implCloseChannel} runs
 * when the channel is closed, but the descriptor may still be in use by an I/O operation that has
 * not returned yet; {@code implReleaseChannel} runs when that last operation finishes and there is
 * really nobody left. A single method would force a choice between closing too early and never
 * closing.
 *
 * <h2>What this VM cannot do</h2>
 *
 * <p>{@link #readWriteSelectableChannel} <strong>is not implemented here</strong> and throws
 * {@link UnsupportedOperationException}. It is not an omission that can be covered by writing more
 * Java: it needs a selectable channel built over a raw descriptor, that is, the machinery that in
 * the JDK lives in {@code sun.nio.ch} and that does not exist in this VM — its own selector only
 * knows the channels it opened. It is left declared, with the exact type of the JDK, and saying that
 * it cannot: it is preferable to pretending to return a channel that would afterwards select
 * nothing.
 */
public final class Channels {

    private Channels() {
    }

    /**
     * It wraps {@code fd} in a selectable channel for reading and writing.
     *
     * @param fd the descriptor, which goes on belonging to whoever passed it
     * @param closer who decides what to do on closing and on releasing
     * @throws UnsupportedOperationException always, in this VM — see the note of the class
     */
    public static SelectableChannel readWriteSelectableChannel(FileDescriptor fd,
            SelectableChannelCloser closer) {
        throw new UnsupportedOperationException(
                "this VM cannot make a selectable channel over a borrowed descriptor");
    }

    /**
     * The closing policy of a channel that wraps somebody else's descriptor.
     *
     * <p>See the description of {@link Channels} for why there are two methods and not one.
     */
    public interface SelectableChannelCloser {

        /**
         * The channel is closed.
         *
         * <p>There may be I/O in flight still, so what goes here is what unblocks whoever is waiting —
         * not necessarily closing the descriptor.
         */
        void implCloseChannel(SelectableChannel sc) throws IOException;

        /**
         * The last I/O operation over the already closed channel has finished.
         *
         * <p>Only here is nobody using the descriptor.
         */
        void implReleaseChannel(SelectableChannel sc) throws IOException;
    }
}
