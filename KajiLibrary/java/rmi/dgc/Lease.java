package java.rmi.dgc;

import java.io.Serializable;

/**
 * The permission, with an expiry, that the server gives a client to hold on to a reference.
 *
 * <p>It is the piece that lets the distributed collector tolerate a client dying without notice:
 * instead of waiting for a `clean` that may never arrive, the server hands out the reference for a
 * term, and a client that wants to keep it has to ask for it again before it expires. If the
 * client disappears, the reference is released on its own when the term runs out.
 *
 * <p>It is immutable, and it has to be: it travels serialised and both ends of the connection read
 * it.
 *
 * @see DGC#dirty(java.rmi.server.ObjID[], long, Lease)
 */
public final class Lease implements Serializable {

    private static final long serialVersionUID = -5713411624328831948L;

    /** The VM the permission was given to. */
    private VMID vmid;

    /** How long it lasts, in milliseconds. */
    private long value;

    /**
     * A permission for the given VM, for the given duration.
     *
     * @param id the client's VM; it may be null, and then the server assigns one
     * @param duration the duration in milliseconds
     */
    public Lease(VMID id, long duration) {
        this.vmid = id;
        this.value = duration;
    }

    /** The VM this permission was given to, or `null` if none was assigned. */
    public VMID getVMID() {
        return this.vmid;
    }

    /** The duration of the permission, in milliseconds. */
    public long getValue() {
        return this.value;
    }
}
