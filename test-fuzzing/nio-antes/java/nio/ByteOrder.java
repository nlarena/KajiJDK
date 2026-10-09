package java.nio;

/**
 * Which end of a multi-byte value comes first.
 *
 * <p>Not an enum: the class predates them, and the surface reflects that — two constants and a
 * factory for the platform's own order.
 *
 * <p>It exists as a first-class notion because a buffer is a <em>view</em> over bytes: the same
 * eight bytes are a different {@code long} depending on this, and the choice belongs to the
 * format being parsed rather than to the machine doing the parsing. This project contains both
 * conventions — the class file reader is big-endian, the zip format is little-endian.
 */
public final class ByteOrder {

    /** The order in which the most significant byte comes first. */
    public static final ByteOrder BIG_ENDIAN = new ByteOrder("BIG_ENDIAN");

    /** The order in which the least significant byte comes first. */
    public static final ByteOrder LITTLE_ENDIAN = new ByteOrder("LITTLE_ENDIAN");

    /**
     * The machine's own order, resolved once.
     *
     * <p>Private and separate from {@link #LITTLE_ENDIAN} even though it holds the same object:
     * the two say different things. {@code LITTLE_ENDIAN} is a constant of the format world and
     * would keep its meaning on a big-endian machine; this one is a fact about the machine, and
     * naming it is what lets {@link #nativeOrder()} be a field read rather than a decision taken
     * again on every call.
     */
    private static final ByteOrder NATIVE_ORDER = LITTLE_ENDIAN;

    private final String name;

    private ByteOrder(String name) {
        this.name = name;
    }

    /**
     * Returns the byte order of the underlying machine.
     *
     * <p>The JDK asks the VM. KajiLibrary has no way to, and every platform this project targets
     * is little-endian, so that is what this answers — stated here rather than discovered.
     *
     * @return {@link #LITTLE_ENDIAN}
     */
    public static ByteOrder nativeOrder() {
        return NATIVE_ORDER;
    }

    /**
     * Returns the name of this order.
     *
     * @return either {@code "BIG_ENDIAN"} or {@code "LITTLE_ENDIAN"}
     */
    public String toString() {
        return name;
    }
}
