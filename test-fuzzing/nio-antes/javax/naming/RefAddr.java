package javax.naming;

/**
 * A communication address, labelled with **what kind of address it is**.
 *
 * <h2>Why an address is not a string</h2>
 *
 * <p>An object may be reachable in several ways at once: the same service has a URL, an RPC port
 * and a mailbox. Storing them as loose strings forces guessing which is which by their shape, which
 * is exactly what breaks when the fourth one appears. Here each address comes with its type
 * --`"URL"`, `"ORB"`, `"LinkAddress"`-- and `Reference.get(String)` asks by type. The type is
 * a `String` and not an `enum` because each provider defines the set, and none knows them all.
 *
 * <p>It is abstract and the subclass supplies the content, because an address can be text
 * (`StringRefAddr`) or opaque bytes only the provider understands (`BinaryRefAddr`). Those two are
 * the only ones in the package, but the class is meant for a provider to add its own.
 *
 * <h2>Equality</h2>
 *
 * <p>Two addresses are equal if **the type and the content** match. Here the content is compared
 * with `equals`, which is right for `String` but not for arrays; that is why `BinaryRefAddr`
 * redefines `equals` and `hashCode` to compare byte by byte.
 *
 * <p>On `Serializable`: the contract says the content has to be serializable too, and that cannot
 * be checked from here. The `serialVersionUID` is the real JDK's so the serial form matches. (An
 * earlier note said this tree had no `ObjectOutputStream`; it has one now.)
 */
public abstract class RefAddr implements java.io.Serializable {

    private static final long serialVersionUID = -1468165120479475415L;

    /** What kind of address it is. `protected` because the subclass reads it for its `toString`. */
    protected String addrType;

    protected RefAddr(String addrType) {
        this.addrType = addrType;
    }

    public String getType() {
        return addrType;
    }

    public abstract Object getContent();

    /**
     * Type and content. The content is compared with `equals`, not by identity, unless it is the
     * same.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RefAddr) {
            RefAddr target = (RefAddr) obj;
            if (addrType.compareTo(target.addrType) == 0) {
                Object thisobj = this.getContent();
                Object thatobj = target.getContent();
                if (thisobj == thatobj) {
                    return true;
                }
                if (thisobj != null) {
                    return thisobj.equals(thatobj);
                }
            }
        }
        return false;
    }

    /** A sum instead of a mix, so that null content does not change the type's hash. */
    @Override
    public int hashCode() {
        return (getContent() == null)
            ? addrType.hashCode()
            : addrType.hashCode() + getContent().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("Type: ");
        str.append(addrType).append("\n");
        str.append("Content: ").append(getContent()).append("\n");
        return str.toString();
    }
}
