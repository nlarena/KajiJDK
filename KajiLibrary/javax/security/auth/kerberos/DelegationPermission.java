package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Objects;

/**
 * KajiLibrary's javax.security.auth.kerberos.DelegationPermission -- permission to delegate
 * credentials.
 *
 * <p>It is the permission for a service to use a client's ticket to talk to <b>another</b> service
 * on its behalf. The name carries the two principals in quotes, separated by a space: {@code
 * "\"host/web@REALM\" \"krbtgt/REALM@REALM\""} is "the web service may ask for tickets on the
 * client's behalf". The first is the subordinate --who delegates-- and the second the target.
 *
 * <p>The format is strict: both parts go in quotes, with at least one space between them and
 * nothing around. Each error has its own message --{@code improperly quoted}, {@code not enough
 * input}, {@code extra input}--, because the name comes from a hand-written policy file and whoever
 * wrote it has to be able to see what they left out.
 *
 * <p>There are no actions. Two permissions are equal if their two principals match; the space
 * between the quotes does not count.
 *
 * @deprecated the JDK marks it for removal together with the security manager; it is still here
 *     because code that instantiates it has to be able to compile and run
 */
@Deprecated(since = "17", forRemoval = true)
public final class DelegationPermission extends BasicPermission implements Serializable {

    private static final long serialVersionUID = 883133252142523922L;

    /** Who delegates. */
    private transient String subordinate;

    /** To whom. */
    private transient String service;

    /**
     * With that name. See the class note on the format.
     *
     * @throws NullPointerException if it is null
     * @throws IllegalArgumentException if it is empty or malformed
     */
    public DelegationPermission(String principals) {
        super(principals);
        init(principals);
    }

    /** Likewise; the actions are ignored. */
    public DelegationPermission(String principals, String actions) {
        super(principals, actions);
        init(principals);
    }

    /** Splits the two principals. See the class note. */
    private void init(String target) {
        if (target == null) {
            throw new NullPointerException("name can't be null");
        }
        if (target.isEmpty()) {
            throw new IllegalArgumentException("name can't be empty");
        }
        if (!target.startsWith("\"")) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        int subordinateEnd = target.indexOf('"', 1);
        if (subordinateEnd < 0) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        String rest = target.substring(subordinateEnd + 1);
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: not enough input");
        }
        int at = 0;
        while (at < rest.length() && Character.isWhitespace(rest.charAt(at))) {
            at = at + 1;
        }
        if (at == 0) {
            throw new IllegalArgumentException("Illegal input [" + target
                + "]: improperly separated");
        }
        rest = rest.substring(at);
        if (rest.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: not enough input");
        }
        if (!rest.startsWith("\"")) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        int serviceEnd = rest.indexOf('"', 1);
        if (serviceEnd < 0) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: improperly quoted");
        }
        if (serviceEnd + 1 != rest.length()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: extra input");
        }
        this.subordinate = target.substring(1, subordinateEnd);
        this.service = rest.substring(1, serviceEnd);
        if (this.subordinate.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target
                + "]: bad subordinate name");
        }
        if (this.service.isEmpty()) {
            throw new IllegalArgumentException("Illegal input [" + target + "]: bad service name");
        }
    }

    /** Whether it is the same pair of principals. There are no wildcards. */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof DelegationPermission)) {
            return false;
        }
        DelegationPermission that = (DelegationPermission) p;
        return this.subordinate.equals(that.subordinate) && this.service.equals(that.service);
    }

    /** Equal if their two principals match. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DelegationPermission)) {
            return false;
        }
        return implies((DelegationPermission) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.subordinate, this.service);
    }

    /** A collection of these. */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new KrbDelegationPermissionCollection();
    }

    /** When read from a stream the principals are split again: they are not serialized. */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(getName());
    }
}
