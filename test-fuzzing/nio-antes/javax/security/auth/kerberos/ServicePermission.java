package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;

/**
 * KajiLibrary's javax.security.auth.kerberos.ServicePermission -- permission to use a Kerberos
 * service.
 *
 * <p>The name is the service's principal, or {@code "*"} for all. There are two actions:
 * {@code initiate} --asking for a ticket for that service, that is acting as client-- and
 * {@code accept} --receiving tickets for that service, that is being the service--. A server needs
 * {@code accept} on its own principal; a client needs {@code initiate} on the server's.
 *
 * <p>The canonical form is {@code "initiate,accept"}, in that order, without spaces and in lower
 * case; when building, upper case and spaces around each action are accepted.
 *
 * @deprecated the JDK marks it for removal together with the security manager; it is still here
 *     because code that instantiates it has to be able to compile and run
 */
@Deprecated(since = "17", forRemoval = true)
public final class ServicePermission extends Permission implements Serializable {

    private static final long serialVersionUID = -1227585031618624935L;

    /** Act as client. */
    private static final int INITIATE = 0x1;

    /** Act as service. */
    private static final int ACCEPT = 0x2;

    /** Both. */
    private static final int ALL = INITIATE | ACCEPT;

    /** Which actions are permitted. */
    private transient int mask;

    /** The canonical form, built on demand. */
    private String actions;

    /**
     * That permission on that service.
     *
     * @param servicePrincipal the service's principal, or {@code "*"}
     * @param action {@code initiate}, {@code accept} or both separated by a comma
     * @throws NullPointerException if either is null
     * @throws IllegalArgumentException if the actions are empty or one does not exist
     */
    public ServicePermission(String servicePrincipal, String action) {
        super(servicePrincipal);
        if (servicePrincipal == null) {
            throw new NullPointerException("service principal can't be null");
        }
        init(action);
    }

    /** With the mask already built; for the collection. */
    ServicePermission(String servicePrincipal, int mask) {
        super(servicePrincipal);
        this.mask = mask & ALL;
    }

    /** Interprets the actions. */
    private void init(String action) {
        if (action == null) {
            throw new NullPointerException("action can't be null");
        }
        if (action.isEmpty()) {
            throw new IllegalArgumentException("action can't be empty");
        }
        this.mask = getMask(action);
    }

    /**
     * Whether this permission is enough for what the other asks for: the same service or {@code
     * "*"}, and all its actions.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof ServicePermission)) {
            return false;
        }
        ServicePermission that = (ServicePermission) p;
        return (this.mask & that.mask) == that.mask && impliesIgnoreMask(that);
    }

    /** Whether the name is enough, without looking at the actions. */
    boolean impliesIgnoreMask(ServicePermission p) {
        return getName().equals("*") || getName().equals(p.getName());
    }

    /** Equal if they name the same service and permit the same actions. */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ServicePermission)) {
            return false;
        }
        ServicePermission that = (ServicePermission) obj;
        return this.mask == that.mask && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode() ^ this.mask;
    }

    /** The canonical form of those bits. */
    static String getActions(int mask) {
        StringBuilder text = new StringBuilder();
        if ((mask & INITIATE) == INITIATE) {
            text.append("initiate");
        }
        if ((mask & ACCEPT) == ACCEPT) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append("accept");
        }
        return text.toString();
    }

    /** The actions in canonical form. See the class note. */
    @Override
    public String getActions() {
        if (this.actions == null) {
            this.actions = getActions(this.mask);
        }
        return this.actions;
    }

    /** A collection that merges the permissions of the same service. */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new KrbServicePermissionCollection();
    }

    /** The bits. */
    int getMask() {
        return this.mask;
    }

    /**
     * The bits of that list of actions.
     *
     * <p>Each action is trimmed and compared case-insensitively; an empty one --from an extra
     * comma-- is as invalid as one that does not exist, and the message repeats the whole list.
     */
    private static int getMask(String action) {
        int mask = 0;
        String[] pieces = action.split(",", -1);
        int i = 0;
        while (i < pieces.length) {
            String piece = pieces[i].trim();
            if (piece.equalsIgnoreCase("initiate")) {
                mask = mask | INITIATE;
            } else if (piece.equalsIgnoreCase("accept")) {
                mask = mask | ACCEPT;
            } else {
                throw new IllegalArgumentException("invalid permission: " + action);
            }
            i = i + 1;
        }
        return mask;
    }

    /**
     * When read from a stream the canonical form is interpreted again: the mask is not serialized.
     */
    private void readObject(java.io.ObjectInputStream in)
            throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(this.actions == null ? "" : this.actions);
    }
}
