package java.nio.channels;

import java.io.Closeable;

// KajiLibrary's java.nio.channels.Channel -- the root of the NIO channel hierarchy: something that
// is open or closed and can be closed. KajiJDK models no real channels; this interface exists so
// that the one method returning it (System.inheritedChannel(), which always answers null) has a
// type to name.
//
public interface Channel extends Closeable {

    /** Whether this channel is open. */
    boolean isOpen();

    /** Closes this channel. */
    void close() throws java.io.IOException;
}
