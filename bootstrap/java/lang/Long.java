package java.lang;

// java.lang.Long — the long box (JLS §5.1.7). Same cache contract as Integer:
// valueOf must hand back the identical object for -128..127.
public final class Long {
    public static final Class<Long> TYPE = (Class<Long>) Class.getPrimitiveClass("long");

    private final long value;

    public Long(long value) {
        this.value = value;
    }

    // Populated LAZILY, unlike IntegerCache: the JLS doesn't require boxed identity for
    // long at all (§5.1.7 lists int/short/char/byte/boolean only), and the eager 256-Long
    // fill deterministically trips an open os-parallel GC bug (collection landing inside
    // this <clinit> while promoting 8-byte-field objects). Lazy per-slot fill keeps the
    // repeat-boxing identity (`Long a = 7L, b = 7L; a == b`) without the mass allocation.
    private static class LongCache {
        static final Long[] cache = new Long[256];
    }

    public static Long valueOf(long l) {
        if (l >= -128L && l <= 127L) {
            int index = (int) l + 128;
            Long boxed = LongCache.cache[index];
            if (boxed == null) {
                boxed = new Long(l);
                LongCache.cache[index] = boxed;
            }
            return boxed;
        }
        return new Long(l);
    }

    public long longValue() {
        return value;
    }

    public boolean equals(Object o) {
        if (o instanceof Long) {
            return value == ((Long) o).value;
        }
        return false;
    }

    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }
}
