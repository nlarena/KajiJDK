package java.nio.channels;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousByteChannel — reads and writes bytes without waiting.
 *
 * <p>Each operation comes in **two forms**, and the choice is not a matter of taste:
 *
 * <ul>
 * <li>The one that returns a {@link Future} serves when the caller wants to wait for the result at
 *     some moment, in their own thread.</li>
 * <li>The one that takes a {@link CompletionHandler} serves when there is nobody to make wait: the
 *     notice arrives by itself, in the thread the channel chooses.</li>
 * </ul>
 *
 * <p>A channel admits **one read and one write** under way at a time; asking for a second one
 * throws {@link ReadPendingException} or {@link WritePendingException}. It is not a limitation of
 * the implementation but part of the contract: two simultaneous reads over the same channel would
 * have no defined order for the bytes.
 */
public interface AsynchronousByteChannel extends AsynchronousChannel {

    /** Reads, giving notice through the handler. */
    <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler);

    /** Reads, handing over a `Future` with the amount read. */
    Future<Integer> read(ByteBuffer dst);

    /** Writes, giving notice through the handler. */
    <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler);

    /** Writes, handing over a `Future` with the amount written. */
    Future<Integer> write(ByteBuffer src);
}
