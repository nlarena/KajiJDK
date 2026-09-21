package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

// KajiLibrary's java.nio.channels.ReadableByteChannel -- a channel that can be read into a buffer.
//
// A single method, and that is the whole interface. What makes it useful is that the source is
// provided by **the caller**: `new Scanner(channel)` does not need the library to know how to open
// files or sockets, because the channel comes already open from outside. That is why it can really
// be implemented in KajiJDK, which has no access to the file system, while `new Scanner(File)`
// cannot.
public interface ReadableByteChannel extends Channel {

    /**
     * Reads a sequence of bytes into `dst`, and returns **how many** it read.
     *
     * <p>It returns `-1` at end of stream, `0` if `dst` had no room (or if a non-blocking channel
     * had nothing ready), and between 1 and `dst.remaining()` in the normal case. The three are
     * legitimate and different results: a reader that treats the `0` as end of stream either hangs
     * or stops early, depending on the channel.
     *
     * <p>A channel admits **one** read at a time: if another thread is reading, this call blocks
     * until the first one finishes.
     */
    int read(ByteBuffer dst) throws IOException;
}
