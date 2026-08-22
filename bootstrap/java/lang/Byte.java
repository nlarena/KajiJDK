package java.lang;

// java.lang.Byte — the byte box (JLS §5.1.7). Every byte value fits the cache,
// so valueOf NEVER allocates: all 256 boxes are canonical.
public final class Byte {
    public static final Class<Byte> TYPE = (Class<Byte>) Class.getPrimitiveClass("byte");

    private final byte value;

    public Byte(byte value) {
        this.value = value;
    }

    private static class ByteCache {
        static final Byte[] cache = new Byte[256];

        static {
            for (int i = 0; i < 256; i++) {
                cache[i] = new Byte((byte) (i - 128));
            }
        }
    }

    public static Byte valueOf(byte b) {
        return ByteCache.cache[b + 128];
    }

    public byte byteValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof Byte) {
            return value == ((Byte) o).value;
        }
        return false;
    }

    public int hashCode() {
        return value;
    }
}
