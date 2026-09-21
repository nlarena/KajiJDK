package jdk.nio.mapmode;

import java.nio.channels.MapModes;
import java.nio.channels.FileChannel;

/**
 * The mapping modes that are not in {@link FileChannel.MapMode}.
 *
 * <p>Both are for **non-volatile** memory: a mapping made with them can be forced to persist with
 * `MappedByteBuffer.force()`, which is what tells them apart from `READ_ONLY` and `READ_WRITE`. The
 * difference lives on the `map()` side, not here: a `MapMode` is a label, and these two are the
 * labels.
 *
 * <p>Whether `map()` accepts them depends on the concrete `FileChannel`. Ours maps already, but
 * **these two it rejects** with `UnsupportedOperationException`: asking for non-volatile memory and
 * receiving an ordinary mapping would be exactly the kind of answer that lies. JDK 25 on Windows
 * answers the same, for the same reason.
 *
 * @since 14
 */
public class ExtendedMapMode {

    /** A read-only mapping over non-volatile memory. */
    public static final FileChannel.MapMode READ_ONLY_SYNC =
            MapModes.of("READ_ONLY_SYNC");

    /** A read-and-write mapping over non-volatile memory. */
    public static final FileChannel.MapMode READ_WRITE_SYNC =
            MapModes.of("READ_WRITE_SYNC");

    private ExtendedMapMode() {
    }
}
