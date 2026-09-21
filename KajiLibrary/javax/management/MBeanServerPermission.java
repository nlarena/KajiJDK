package javax.management;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Permission over the operations of {@link MBeanServerFactory}.
 *
 * <p>Two things set it apart from an ordinary {@link BasicPermission}:
 *
 * <ul>
 *   <li><b>the name can be a comma-separated list</b> --{@code
 *       "createMBeanServer,findMBeanServer"}--, which the wildcard syntax of {@code
 *       BasicPermission} cannot do;
 *   <li><b>{@code createMBeanServer} implies {@code newMBeanServer}.</b> It is not an arbitrary
 *       rule: creating registers the agent in the factory, and whoever can do the most --leave an
 *       agent findable by anyone-- can do the least --make themselves a private one. The other way
 *       round does not hold.
 * </ul>
 *
 * <p>That is why everything is resolved over a bit mask and not over strings: the implication
 * between {@code create} and {@code new} is a relation between sets, and written as text comparison
 * it would be a nest of special cases.
 */
public class MBeanServerPermission extends BasicPermission {

    private static final long serialVersionUID = -5661980843569388590L;

    private static final int CREATE = 1;
    private static final int FIND = 2;
    private static final int NEW_SERVER = 4;
    private static final int RELEASE = 8;
    private static final int ALL = CREATE | FIND | NEW_SERVER | RELEASE;

    /** The order the canonical name is rebuilt in; fixed, so that {@code equals} is stable. */
    private static final String[] NAMES = { "createMBeanServer", "findMBeanServer",
                                              "newMBeanServer", "releaseMBeanServer" };
    private static final int[] BITS = { CREATE, FIND, NEW_SERVER, RELEASE };

    /** Derived from the name, which is the only thing serialized: it does not need to be stored. */
    private transient int mask;

    /** @throws IllegalArgumentException if the name is empty or brings something unknown */
    public MBeanServerPermission(String name) {
        this(name, null);
    }

    /**
     * @param actions has to be {@code null} or empty: this permission has no actions
     * @throws IllegalArgumentException if the name is not valid or if actions come
     */
    public MBeanServerPermission(String name, String actions) {
        super(canonical(name), actions);
        this.mask = maskOf(name);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException(
                "MBeanServerPermission takes no actions: " + actions);
        }
    }

    private static int maskOf(String name) {
        if (name == null) {
            throw new NullPointerException("The name cannot be null");
        }
        String n = name.trim();
        if (n.equals("*")) {
            return ALL;
        }
        if (n.length() == 0) {
            throw new IllegalArgumentException("The name cannot be empty");
        }
        int m = 0;
        for (String part : n.split(",", -1)) {
            String p = part.trim();
            int bit = 0;
            for (int i = 0; i < NAMES.length; i++) {
                if (NAMES[i].equals(p)) {
                    bit = BITS[i];
                    break;
                }
            }
            if (bit == 0) {
                throw new IllegalArgumentException("Invalid name: " + p);
            }
            m |= bit;
        }
        // It is here and not in `implies` that the implication is closed. Putting it in the mask
        // makes it automatic for `equals`, `hashCode` and the collections, instead of a loose case.
        if ((m & CREATE) != 0) {
            m |= NEW_SERVER;
        }
        return m;
    }

    /** The normalized name: same set of operations, always the same text. */
    private static String canonical(String name) {
        return fromMask(maskOf(name));
    }

    private static String fromMask(int m) {
        if (m == ALL) {
            return "*";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NAMES.length; i++) {
            if ((m & BITS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(NAMES[i]);
            }
        }
        return sb.toString();
    }

    /** Over the mask: it is the set of operations that decides. */
    public int hashCode() {
        return mask;
    }

    /** This permission covers {@code p} if its mask wholly contains {@code p}'s. */
    public boolean implies(Permission p) {
        if (!(p instanceof MBeanServerPermission)) {
            return false;
        }
        MBeanServerPermission other = (MBeanServerPermission) p;
        return (mask & other.mask) == other.mask;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MBeanServerPermission)) {
            return false;
        }
        return mask == ((MBeanServerPermission) obj).mask;
    }

    /**
     * A collection that gathers permissions <b>by union of masks</b>.
     *
     * <p>It is what makes {@code {createMBeanServer} + {findMBeanServer}} imply
     * {@code createMBeanServer,findMBeanServer}, which no generic collection can deduce: the
     * heterogeneous one asks permission by permission and neither of the two, alone, is enough.
     */
    public PermissionCollection newPermissionCollection() {
        return new MBeanServerPermissionCollection();
    }
}

/**
 * Package-private on purpose: it is a detail of {@link MBeanServerPermission} and the JDK does not
 * expose it either.
 */
class MBeanServerPermissionCollection extends PermissionCollection {

    private static final long serialVersionUID = -5661980843569388591L;

    private MBeanServerPermission accumulated = null;

    public synchronized void add(Permission permission) {
        if (!(permission instanceof MBeanServerPermission)) {
            throw new IllegalArgumentException("Not an MBeanServerPermission: " + permission);
        }
        if (isReadOnly()) {
            throw new SecurityException("The collection is read-only");
        }
        MBeanServerPermission p = (MBeanServerPermission) permission;
        if (accumulated == null) {
            accumulated = p;
        } else if (!accumulated.implies(p)) {
            // The union is built by name: the constructor knows how to read the comma list, so the
            // mask does not need to be exposed.
            String union = accumulated.getName().equals("*") || p.getName().equals("*")
                    ? "*" : accumulated.getName() + "," + p.getName();
            accumulated = new MBeanServerPermission(union);
        }
    }

    public synchronized boolean implies(Permission permission) {
        return accumulated != null && accumulated.implies(permission);
    }

    public synchronized Enumeration<Permission> elements() {
        final MBeanServerPermission p = accumulated;
        return new Enumeration<Permission>() {
            private boolean granted = false;

            public boolean hasMoreElements() {
                return p != null && !granted;
            }

            public Permission nextElement() {
                if (p == null || granted) {
                    throw new NoSuchElementException();
                }
                granted = true;
                return p;
            }
        };
    }
}
