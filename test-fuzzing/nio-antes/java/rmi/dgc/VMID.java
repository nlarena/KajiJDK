package java.rmi.dgc;

import java.io.Serializable;
import java.rmi.server.UID;
import java.security.SecureRandom;

/**
 * A virtual machine identifier, unique across all VMs.
 *
 * <p>A {@link UID} is only unique **within** its VM: two VMs started at the same time can issue
 * the same one. The distributed collector needs to tell apart clients living in different
 * processes, which is why `VMID` prepends to the `UID` a block of random bytes fixed once per
 * process: two VMs match only if they match in the eight bytes **and** in the `UID`.
 *
 * <p>For years the JDK used the machine's network address instead of the random block. It changed
 * that because it leaked the host's IP in a serialised object that travels over the network, and
 * because eight bytes from a cryptographic generator collide less than an IP that repeats behind
 * every NAT. We do the same, for the same two reasons.
 *
 * @see java.rmi.dgc.DGC
 */
public final class VMID implements Serializable {

    private static final long serialVersionUID = -538642295484486218L;

    /**
     * The eight bytes of the process. They are drawn only once: if they were drawn per instance,
     * two `VMID`s from the same VM would look like they came from different VMs, which is exactly
     * the opposite of what the class exists for.
     */
    private static final byte[] randomBytes;

    static {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        randomBytes = bytes;
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** The bytes of the process; the name comes from when they were the network address. */
    private byte[] addr;

    /** The identifier within this VM. */
    private UID uid;

    /** A new identifier, distinct from all of this VM's and (almost surely) from other VMs'. */
    public VMID() {
        this.addr = randomBytes;
        this.uid = new UID();
    }

    /**
     * Whether this `VMID` is unique across all VMs.
     *
     * @deprecated It always gives `true`. It existed to warn when the identifier came from a
     *     network address that could not be determined; since it comes from a cryptographic
     *     generator there is no case in which it is not unique, and the method has nothing to say.
     * @return `true`, always
     */
    @Deprecated
    public static boolean isUnique() {
        return true;
    }

    public int hashCode() {
        return this.uid.hashCode();
    }

    /**
     * Whether the other is a `VMID` with the same process block and the same {@link UID}.
     *
     * <p>Both have to match: the `UID` on its own repeats across VMs, and the process block on its
     * own does not tell apart two objects from the same VM.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VMID)) {
            return false;
        }
        VMID other = (VMID) obj;
        if (!this.uid.equals(other.uid)) {
            return false;
        }
        if (this.addr == null || other.addr == null) {
            return this.addr == other.addr;
        }
        if (this.addr.length != other.addr.length) {
            return false;
        }
        for (int i = 0; i < this.addr.length; i++) {
            if (this.addr[i] != other.addr[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.addr != null) {
            for (int i = 0; i < this.addr.length; i++) {
                int b = this.addr[i] & 0xFF;
                sb.append(HEX[b >> 4]).append(HEX[b & 0xF]);
            }
        }
        sb.append(':').append(this.uid);
        return sb.toString();
    }
}
