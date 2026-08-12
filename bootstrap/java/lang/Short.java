package java.lang;

// java.lang.Short — the short box (JLS §5.1.7), cache -128..127.
public final class Short {
    public static final Class<Short> TYPE = (Class<Short>) Class.getPrimitiveClass("short");

    private final short value;

    public Short(short value) {
        this.value = value;
    }

    private static class ShortCache {
        static final Short[] cache = new Short[256];

        static {
            for (int i = 0; i < 256; i++) {
                cache[i] = new Short((short) (i - 128));
            }
        }
    }

    public static Short valueOf(short s) {
        if (s >= -128 && s <= 127) {
            return ShortCache.cache[s + 128];
        }
        return new Short(s);
    }

    public short shortValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof Short) {
            return value == ((Short) o).value;
        }
        return false;
    }

    public int hashCode() {
        return value;
    }
}
