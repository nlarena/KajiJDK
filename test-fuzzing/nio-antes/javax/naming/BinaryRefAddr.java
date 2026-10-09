package javax.naming;

/**
 * An address made of opaque bytes: a CORBA object identifier, a binary handle.
 *
 * <h2>Why it copies the array and why it redefines all three</h2>
 *
 * <p>The constructor **copies**. A `byte[]` is mutable and the address is kept inside a
 * `Reference` that may stay bound indefinitely: keeping the caller's array would let the address be
 * changed from under it. It is the same reason `NamingException` clones names.
 *
 * <p>And it redefines `equals`/`hashCode`/`toString` because the `RefAddr` ones compare the content
 * with `equals`, and an array's `equals` is identity. Without this, two addresses with the same
 * bytes would be different, which is exactly the opposite of what the `RefAddr` contract says.
 *
 * <p>`toString` cuts at the first 32 bytes: it is a diagnostic dump, and a binary address can be
 * kilobytes long.
 */
public class BinaryRefAddr extends RefAddr {

    private static final long serialVersionUID = -3415254970957330361L;

    private byte[] buf;

    public BinaryRefAddr(String addrType, byte[] src) {
        this(addrType, src, 0, src.length);
    }

    /** Copies `count` bytes from `offset`: the caller's array is not kept referenced. */
    public BinaryRefAddr(String addrType, byte[] src, int offset, int count) {
        super(addrType);
        buf = new byte[count];
        System.arraycopy(src, offset, buf, 0, count);
    }

    @Override
    public Object getContent() {
        return buf;
    }

    /** Byte by byte: the inherited `equals` would use the array's identity. */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BinaryRefAddr) {
            BinaryRefAddr target = (BinaryRefAddr) obj;
            if (addrType.compareTo(target.addrType) != 0) {
                return false;
            }
            if (buf == target.buf) {
                return true;
            }
            if (buf.length != target.buf.length) {
                return false;
            }
            for (int i = 0; i < buf.length; i++) {
                if (buf[i] != target.buf[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = addrType.hashCode();
        for (int i = 0; i < buf.length; i++) {
            hash += buf[i];
        }
        return hash;
    }

    /** Cuts at 32 bytes: this is diagnostic, not a full dump. */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Address Type: ");
        str.append(addrType).append("\n");
        str.append("AddressContents: ");
        for (int i = 0; i < buf.length && i < 32; i++) {
            str.append(buf[i]).append(" ");
        }
        if (buf.length >= 32) {
            str.append(" ...\n");
        }
        return str.toString();
    }
}
