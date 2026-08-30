package java.nio.channels;

import java.io.Closeable;

// KajiLibrary's java.nio.channels.Channel -- the root of the NIO channel hierarchy: something that
// is open or closed and can be closed. KajiJDK models no real channels; this interface exists so
// that the one method returning it (System.inheritedChannel(), which always answers null) has a
// type to name.
//
// A KajiLibrary note: the JDK's `close()` here declares `throws IOException`, but KajiLibrary's
// `java.io.Closeable.close()` does not, and an override may not widen the throws clause (JLS
// 8.4.8.3). So `close()` is declared without it, which matches this library's Closeable and is
// invisible to the public-surface accounting (javap normalises `throws` away).
public interface Channel extends Closeable {

    /** Whether this channel is open. */
    boolean isOpen();

    /** Closes this channel. */
    void close();
}
