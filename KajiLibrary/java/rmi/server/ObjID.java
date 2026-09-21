package java.rmi.server;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The identifier of a remote object within its VM.
 *
 * <h2>The three well-known identifiers</h2>
 *
 * <p>{@link #REGISTRY_ID}, {@link #ACTIVATOR_ID} and {@link #DGC_ID} are fixed, and they have to
 * be: a client looking for the registry cannot ask anyone what its identifier is, because asking
 * already requires the registry. It is the cold-start problem, and it is solved by fixing in
 * advance the three identifiers that are needed before anything can be found out.
 *
 * <p>All the others come from the no-argument constructor, which uses a {@link UID} so as not to
 * repeat itself.
 */
public final class ObjID implements Serializable {

    private static final long serialVersionUID = -6386392263968365220L;

    /** The RMI registry. */
    public static final int REGISTRY_ID = 0;

    /** The activator. */
    public static final int ACTIVATOR_ID = 1;

    /** The distributed garbage collector. */
    public static final int DGC_ID = 2;

    private static final AtomicLong NEXT = new AtomicLong(0);

    private final long objNum;
    private final UID space;

    /** A new, unique one. */
    public ObjID() {
        this.objNum = NEXT.getAndIncrement();
        this.space = new UID();
    }

    /**
     * One of the well-known ones.
     *
     * <p>The {@link UID} it carries is the "well-known" one —the one from {@link UID#UID(short)}
     * with zero— and not a new one: that is what makes two different VMs build the same identifier.
     */
    public ObjID(int num) {
        this.objNum = num;
        this.space = new UID((short) 0);
    }

    private ObjID(long objNum, UID space) {
        this.objNum = objNum;
        this.space = space;
    }

    /** It writes it in the format {@link #read} expects. */
    public void write(ObjectOutput out) throws IOException {
        out.writeLong(this.objNum);
        this.space.write(out);
    }

    /** It reads it from the format {@link #write} writes. */
    public static ObjID read(ObjectInput in) throws IOException {
        long num = in.readLong();
        UID space = UID.read(in);
        return new ObjID(num, space);
    }

    public int hashCode() {
        return (int) this.objNum;
    }

    public boolean equals(Object obj) {
        if (obj instanceof ObjID) {
            ObjID o = (ObjID) obj;
            return this.objNum == o.objNum && this.space.equals(o.space);
        }
        return false;
    }

    public String toString() {
        return "[" + this.space.toString() + ", " + String.valueOf(this.objNum) + "]";
    }
}
