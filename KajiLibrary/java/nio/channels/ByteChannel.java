package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ByteChannel — a channel that reads **and** writes.
 *
 * <p>It declares nothing of its own: it is the union of {@link ReadableByteChannel} and {@link
 * WritableByteChannel}, and its value is exactly that. It lets a signature ask for "a bidirectional
 * channel" with a single type, instead of one parameter for each half or an intersection written by
 * hand in each place.
 */
public interface ByteChannel extends ReadableByteChannel, WritableByteChannel {
}
