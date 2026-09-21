package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An identifier for this VM and its start-up.
 *
 * <h2>How uniqueness is obtained without coordination</h2>
 *
 * <p>This note used to say it was unique within this VM, built from three numbers (the moment the
 * VM started, a discriminant and a counter) whose combination does not repeat <strong>on this
 * machine</strong>, and that an {@link ObjID} adds its own number to make it unique across
 * machines. What the code does: {@code unique} is always 0, so there is no discriminant;
 * {@code time} is the {@code System.currentTimeMillis()} of when this class was initialised, not
 * of VM start-up; and {@code count} is an {@code int} counter cast to {@code short}, so after
 * 65,536 UIDs the values repeat within the same VM. Two VMs that initialise this class in the same
 * millisecond produce the same sequence. {@link ObjID} adds only its own counter, which starts at 0
 * in every VM, so it does not make the identifier unique across machines either (checked by
 * reading the constructors here and in {@code ObjID.java}).
 *
 * <p>That the counter is atomic is not decoration: two threads exporting objects at once would ask
 * for the same number, and two remote objects with the same identifier is exactly the kind of bug
 * that shows up in production and not in testing.
 */
public final class UID implements Serializable {

    private static final long serialVersionUID = 1086053664494604050L;

    private static final AtomicInteger NEXT = new AtomicInteger(0);
    private static final long STARTUP = System.currentTimeMillis();

    private final int unique;
    private final long time;
    private final short count;

    /**
     * A new one. This note used to call it unique in this VM; its {@code count} is an {@code int}
     * counter cast to {@code short}, so it repeats after 65,536 of them (see the class note).
     */
    public UID() {
        this.unique = 0;
        this.time = STARTUP;
        this.count = (short) NEXT.getAndIncrement();
    }

    /**
     * A "well-known" one: the same number always gives the same identifier.
     *
     * <p>It serves the objects that have to be findable without having been announced — the
     * registry, the distributed collector. See {@link ObjID}.
     */
    public UID(short num) {
        this.unique = 0;
        this.time = 0;
        this.count = num;
    }

    private UID(int unique, long time, short count) {
        this.unique = unique;
        this.time = time;
        this.count = count;
    }

    public int hashCode() {
        return (int) this.time + (int) this.count;
    }

    public boolean equals(Object obj) {
        if (obj instanceof UID) {
            UID o = (UID) obj;
            return this.unique == o.unique && this.count == o.count && this.time == o.time;
        }
        return false;
    }

    public String toString() {
        return Integer.toString(this.unique, 16) + ":"
                + Long.toString(this.time, 16) + ":"
                + Integer.toString(this.count, 16);
    }

    /** It writes it in the format {@link #read} expects. */
    public void write(DataOutput out) throws IOException {
        out.writeInt(this.unique);
        out.writeLong(this.time);
        out.writeShort(this.count);
    }

    /** It reads it from the format {@link #write} writes. */
    public static UID read(DataInput in) throws IOException {
        int unique = in.readInt();
        long time = in.readLong();
        short count = in.readShort();
        return new UID(unique, time, count);
    }
}
