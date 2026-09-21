package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.LockInfo -- which lock a thread is waiting for.
 *
 * <p>Two data: the class of the object acting as the lock, and its
 * {@code System.identityHashCode}. It does not keep the object: if it did, a thread dump would stop
 * the collector taking away everything somebody was waiting on.
 *
 * <p>The identity code is what allows cross-referencing: two threads waiting for the <b>same</b> lock
 * show the same class-and-code pair. That is how a wait cycle is spotted by hand.
 *
 * <p>It is not a perfect identifier -- two objects can share an identity code-- but for a dump it is
 * enough.
 *
 * <p>It covers both monitors and {@code java.util.concurrent}'s locks; {@link MonitorInfo} is the
 * subclass adding what only monitors have.
 */
public class LockInfo {

    /** The lock's class. */
    private final String className;

    /** Its identity code. */
    private final int identityHashCode;

    /**
     * @throws NullPointerException if the class name is null
     */
    public LockInfo(String className, int identityHashCode) {
        if (className == null) {
            throw new NullPointerException("Parameter className cannot be null");
        }
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /** The lock's class. */
    public String getClassName() {
        return this.className;
    }

    /** Its identity code. */
    public int getIdentityHashCode() {
        return this.identityHashCode;
    }

    /** The class, an at sign, and the code in hexadecimal. Just like {@code Object.toString}. */
    @Override
    public String toString() {
        return this.className + '@' + Integer.toHexString(this.identityHashCode);
    }

    /**
     * The same, read out of a {@link CompositeData}.
     *
     * @return the object, or null if the datum is null
     * @throws IllegalArgumentException if the datum does not describe a {@code LockInfo}
     */
    public static LockInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new LockInfo(CompositeItems.string(cd, "className", "LockInfo"),
                            CompositeItems.integer(cd, "identityHashCode", "LockInfo"));
    }
}
