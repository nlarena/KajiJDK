package java.net;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

// "To which hosts and ports, and to do what."
//
// The permission's name is `host:ports` and the actions are a subset of
// connect/listen/accept/resolve. Everything interesting about the class is in `implies`, which is
// what makes a permission written once cover a set: `*.example.org:1-1023` with "connect" implies
// `www.example.org:80` with "connect".
//
// ===========================================================================================
// WHAT THIS CLASS IS WITH NO NETWORK
// ===========================================================================================
//
// A permission does not connect: it describes. The whole class is parsing and set comparison
// --wildcard names, port ranges, bit masks-- and that is computed in full here.
//
// **The one difference from the JDK, and it has to be said:** the JDK's `implies`, when the
// comparison by name is not enough, **resolves both hosts through DNS** and compares IP addresses,
// so that `example.org` implies `93.184.216.34`. That needs a resolver, which does not exist in this
// VM (the why is in `InetAddress`'s header).
//
// Here the comparison is **textual only**: canonical name against canonical name, case-insensitively,
// plus the prefix wildcard. That makes this `implies` **stricter** than the JDK's, never more
// permissive -- it may say "no" where the JDK would say "yes", and not the other way round.
//
// That direction of the error is the one that matters: a permission that denies too much is noticed
// at first use; one that grants too much is never noticed. And since comparison by IP only adds
// cases, no affirmative answer from here is an answer the JDK would not give.
//
// The rest --actions, canonical order, ranges, `equals`, `hashCode`-- is identical to the JDK's, and
// it is verified against the real JDK case by case.
//
// @deprecated The Security Manager is deprecated for removal; these permissions are no longer
// checked.
@Deprecated
public final class SocketPermission extends Permission implements java.io.Serializable {

    private static final long serialVersionUID = -7204263841984476862L;

    private static final int CONNECT = 0x1;
    private static final int LISTEN = 0x2;
    private static final int ACCEPT = 0x4;
    private static final int RESOLVE = 0x8;

    // The host in lower case. If `wildcard` is true, it is the SUFFIX to match (".example.org"), or
    // the empty string for the bare `*`, which matches everything.
    private final String host;
    private final boolean wildcard;
    private final int portMin;
    private final int portMax;
    private final int mask;
    private final String actions;

    /**
     * The permission over {@code host} for {@code action}.
     *
     * @param host {@code hostname[:port|:min-max|:min-|:-max]}; empty means "localhost", and a name
     *     may start with {@code *.} to cover a whole domain
     * @param action a comma-separated list of connect/listen/accept/resolve, case-insensitive
     * @throws NullPointerException if {@code action} is null
     * @throws IllegalArgumentException if {@code action} is empty or holds an unknown name
     */
    public SocketPermission(String host, String action) {
        // The name that is stored is the ALREADY normalized one: an empty host means "localhost", and
        // `getName()` has to return that and not the empty string it was written with. The computation
        // goes inline because `super(...)` has to be the first statement.
        super(host == null || host.length() == 0 ? "localhost" : host);
        String h = host == null || host.length() == 0 ? "localhost" : host;
        int cut = cutPoint(h);
        String name;
        String ports;
        if (cut == -1) {
            name = h;
            ports = null;
        } else {
            name = h.substring(0, cut);
            ports = h.substring(cut + 1);
        }
        name = name.toLowerCase();
        if (name.equals("*")) {
            this.wildcard = true;
            this.host = "";
        } else if (name.startsWith("*.")) {
            this.wildcard = true;
            this.host = name.substring(1);
        } else {
            this.wildcard = false;
            this.host = name;
        }
        int[] range = parsePorts(ports);
        this.portMin = range[0];
        this.portMax = range[1];
        this.mask = parseActions(action);
        this.actions = buildActions(this.mask);
    }

    // The ':' separating the host from the ports. It is the LAST one, and not the first, because a
    // literal IPv6 address comes full of ':' -- but between brackets, so if there is a ']' the split
    // has to be looked for after it.
    private static int cutPoint(String h) {
        int bracket = h.lastIndexOf(']');
        return h.indexOf(':', bracket + 1) == -1 ? -1 : h.lastIndexOf(':');
    }

    // "80" -> [80,80]; "80-90" -> [80,90]; "1024-" -> [1024,65535]; "-100" -> [0,100];
    // absent -> the whole range, which is what makes `*` with "connect" imply any port.
    private static int[] parsePorts(String p) {
        if (p == null || p.length() == 0) {
            return new int[] {0, 65535};
        }
        int dash = p.indexOf('-');
        if (dash == -1) {
            int v = parseInt(p, -1);
            if (v < 0) {
                throw new IllegalArgumentException("invalid port range: " + p);
            }
            return new int[] {v, v};
        }
        String left = p.substring(0, dash);
        String right = p.substring(dash + 1);
        int lo = left.length() == 0 ? 0 : parseInt(left, -1);
        int hi = right.length() == 0 ? 65535 : parseInt(right, -1);
        if (lo < 0 || hi < 0) {
            throw new IllegalArgumentException("invalid port range: " + p);
        }
        return new int[] {lo, hi};
    }

    private static int parseInt(String s, int onFailure) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return onFailure;
        }
    }

    // "resolve" is added on its own whenever there is any other action: connecting to a name implies
    // having been able to resolve it, so separating them would be granting a useless permission.
    private static int parseActions(String action) {
        if (action == null) {
            throw new NullPointerException("action can't be null");
        }
        if (action.length() == 0) {
            throw new IllegalArgumentException("action can't be empty");
        }
        int m = 0;
        int start = 0;
        while (start <= action.length()) {
            int comma = action.indexOf(',', start);
            String tok;
            if (comma == -1) {
                tok = action.substring(start);
                start = action.length() + 1;
            } else {
                tok = action.substring(start, comma);
                start = comma + 1;
            }
            tok = tok.trim().toLowerCase();
            if (tok.length() == 0) {
                continue;
            }
            if (tok.equals("connect")) {
                m = m | CONNECT;
            } else if (tok.equals("listen")) {
                m = m | LISTEN;
            } else if (tok.equals("accept")) {
                m = m | ACCEPT;
            } else if (tok.equals("resolve")) {
                m = m | RESOLVE;
            } else {
                throw new IllegalArgumentException("invalid permission: " + tok);
            }
        }
        if ((m & (CONNECT | LISTEN | ACCEPT)) != 0) {
            m = m | RESOLVE;
        }
        return m;
    }

    // The order is fixed --connect, listen, accept, resolve-- and not the written one: that way two
    // equivalent permissions written differently give the same string.
    private static String buildActions(int m) {
        StringBuilder b = new StringBuilder();
        String sep = "";
        if ((m & CONNECT) != 0) {
            b.append(sep).append("connect");
            sep = ",";
        }
        if ((m & LISTEN) != 0) {
            b.append(sep).append("listen");
            sep = ",";
        }
        if ((m & ACCEPT) != 0) {
            b.append(sep).append("accept");
            sep = ",";
        }
        if ((m & RESOLVE) != 0) {
            b.append(sep).append("resolve");
        }
        return b.toString();
    }

    /**
     * Whether this permission covers {@code p}.
     *
     * <p>Three conditions, all necessary: {@code p}'s actions have to be among this one's, its port
     * range has to fall entirely inside this one's, and its host has to match.
     *
     * <p>On the comparison of hosts, see the file's header: it is textual, with no DNS.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof SocketPermission)) {
            return false;
        }
        SocketPermission that = (SocketPermission) p;
        if ((this.mask & that.mask) != that.mask) {
            return false;
        }
        if (that.portMin < this.portMin || that.portMax > this.portMax) {
            return false;
        }
        return this.matchesHost(that);
    }

    private boolean matchesHost(SocketPermission that) {
        if (this.wildcard) {
            // A bare `*` covers everything; `*.domain` covers the subdomains, NOT the bare domain
            // (`*.example.org` does not imply `example.org`, which is what the JDK does).
            return this.host.length() == 0 || that.host.endsWith(this.host);
        }
        if (that.wildcard) {
            return false;
        }
        return this.host.equals(that.host);
    }

    /** The actions in canonical order. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Same host, same range and same actions.
     *
     * <p>Comparing the **canonicalized** host and not the raw name is what makes
     * {@code HOST.com:80} and {@code host.com:80} the same permission.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SocketPermission)) {
            return false;
        }
        SocketPermission that = (SocketPermission) obj;
        return this.mask == that.mask
                && this.wildcard == that.wildcard
                && this.portMin == that.portMin
                && this.portMax == that.portMax
                && this.host.equals(that.host);
    }

    /**
     * The hash of the NAME, not of the actions.
     *
     * <p>It is what the JDK does, and it is not an oversight: two permissions over the same host with
     * different actions land in the same bucket on purpose, because the lookups always go by host.
     */
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * A collection that knows how to answer {@code implies} over the whole set.
     *
     * <p>One of its own is needed because a set of `SocketPermission`s can imply something no member
     * implies on its own: "connect" to a host plus "resolve" to the same host add up. The collection
     * gathers the masks of the ones matching the host before deciding.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return new SocketPermissionCollection();
    }

    // Package-private, as in the JDK: nobody names it, it is obtained through `newPermissionCollection`.
    static final class SocketPermissionCollection extends PermissionCollection {

        private static final long serialVersionUID = 2787186408602930181L;

        private final List<Permission> perms = new ArrayList<Permission>();

        @Override
        public void add(Permission permission) {
            if (!(permission instanceof SocketPermission)) {
                throw new IllegalArgumentException("invalid permission: " + permission);
            }
            if (this.isReadOnly()) {
                throw new SecurityException(
                        "attempt to add a Permission to a readonly PermissionCollection");
            }
            synchronized (this.perms) {
                this.perms.add(0, permission);
            }
        }

        @Override
        public boolean implies(Permission permission) {
            if (!(permission instanceof SocketPermission)) {
                return false;
            }
            SocketPermission np = (SocketPermission) permission;
            int needed = np.mask;
            int gathered = 0;
            synchronized (this.perms) {
                int i = 0;
                while (i < this.perms.size()) {
                    SocketPermission x = (SocketPermission) this.perms.get(i);
                    // Only the ones that already cover the host and the range add up; otherwise
                    // their actions are about something else and have no reason to count here.
                    if ((x.mask & needed) != 0
                            && np.portMin >= x.portMin
                            && np.portMax <= x.portMax
                            && x.matchesHost(np)) {
                        gathered = gathered | x.mask;
                        if ((gathered & needed) == needed) {
                            return true;
                        }
                    }
                    i = i + 1;
                }
            }
            return false;
        }

        @Override
        public Enumeration<Permission> elements() {
            synchronized (this.perms) {
                return Collections.enumeration(new ArrayList<Permission>(this.perms));
            }
        }
    }
}
