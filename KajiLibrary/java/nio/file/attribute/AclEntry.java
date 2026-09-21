package java.nio.file.attribute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

// An entry of an access control list: to whom, what type, what permissions and how it is inherited.
//
// **It is pure value and that is why it is complete.** No native is needed to build, compare or
// print an `AclEntry`; what KajiJDK cannot do is **read one off the disk or write one**, and that
// lives in `AclFileAttributeView`, which is left without an implementation. The class is useful all
// the same: whoever has ACLs from another source --a configuration file, a protocol-- can model them
// with this.
//
// **Immutable, and built with a `Builder`.** It is four fields of which two are optional sets: a
// four-argument constructor would force writing `Collections.emptySet()` twice in the common case.
// The sets are copied on the way in --the reference the caller gave is not kept-- because otherwise
// the "immutability" would last until the caller touched their own set.
public final class AclEntry {

    private final AclEntryType type;
    private final UserPrincipal who;
    private final Set<AclEntryPermission> perms;
    private final Set<AclEntryFlag> flags;

    // The hash is computed once and kept; 0 means "not yet". That the legitimate value 0 is
    // recomputed each time is cheap and saves an extra boolean field.
    private volatile int hash;

    private AclEntry(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms,
            Set<AclEntryFlag> flags) {
        this.type = type;
        this.who = who;
        this.perms = perms;
        this.flags = flags;
    }

    /** An empty builder, which has to be given at least a type and a principal. */
    public static Builder newBuilder() {
        Set<AclEntryPermission> p = Collections.emptySet();
        Set<AclEntryFlag> f = Collections.emptySet();
        return new Builder(null, null, p, f);
    }

    /** A builder preloaded with `entry`'s values, for copying with one thing changed. */
    public static Builder newBuilder(AclEntry entry) {
        if (entry == null) {
            throw new NullPointerException();
        }
        return new Builder(entry.type, entry.who, entry.perms, entry.flags);
    }

    /** Whether this entry allows, denies, audits or alarms. */
    public AclEntryType type() {
        return this.type;
    }

    /** Whom it applies to. */
    public UserPrincipal principal() {
        return this.who;
    }

    /** The permissions, in a **copy**: modifying it does not touch the entry. */
    public Set<AclEntryPermission> permissions() {
        return new HashSet<AclEntryPermission>(this.perms);
    }

    /** The inheritance flags, in a **copy**. */
    public Set<AclEntryFlag> flags() {
        return new HashSet<AclEntryFlag>(this.flags);
    }

    /** Equal if all four fields agree. */
    public boolean equals(Object ob) {
        if (ob == this) {
            return true;
        }
        if (!(ob instanceof AclEntry)) {
            return false;
        }
        AclEntry other = (AclEntry) ob;
        if (this.type != other.type) {
            return false;
        }
        if (!this.who.equals(other.who)) {
            return false;
        }
        if (!this.perms.equals(other.perms)) {
            return false;
        }
        return this.flags.equals(other.flags);
    }

    private static int hashCodeOf(Set<?> s) {
        // The sum of the elements' hashes: it does not depend on the iteration order, which in a
        // set is undefined.
        int h = 0;
        Iterator<?> it = s.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            h = h + (o == null ? 0 : o.hashCode());
        }
        return h;
    }

    public int hashCode() {
        int h = this.hash;
        if (h != 0) {
            return h;
        }
        h = this.type.hashCode();
        h = h * 127 + this.who.hashCode();
        h = h * 127 + hashCodeOf(this.perms);
        h = h * 127 + hashCodeOf(this.flags);
        this.hash = h;
        return h;
    }

    /** Something like `user:READ_DATA/WRITE_DATA:FILE_INHERIT:ALLOW`. */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.who.getName());
        sb.append(':');
        Iterator<AclEntryPermission> ip = this.perms.iterator();
        while (ip.hasNext()) {
            sb.append(ip.next().name());
            sb.append('/');
        }
        if (!this.perms.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(':');
        Iterator<AclEntryFlag> iflag = this.flags.iterator();
        while (iflag.hasNext()) {
            sb.append(iflag.next().name());
            sb.append('/');
        }
        if (!this.flags.isEmpty()) {
            sb.setLength(sb.length() - 1);
        }
        sb.append(':');
        sb.append(this.type.name());
        return sb.toString();
    }

    /**
     * `AclEntry`'s builder.
     *
     * <p>The `set*` return `this` so they can be chained, and they **mutate** the builder itself:
     * immutability is guaranteed by `build()`, which copies the sets, not by the `Builder`. It is not
     * safe to share a `Builder` between threads.
     */
    public static final class Builder {

        private AclEntryType type;
        private UserPrincipal who;
        private Set<AclEntryPermission> perms;
        private Set<AclEntryFlag> flags;

        private Builder(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms,
                Set<AclEntryFlag> flags) {
            this.type = type;
            this.who = who;
            this.perms = perms;
            this.flags = flags;
        }

        /**
         * It builds the entry.
         *
         * @throws IllegalStateException if the type or the principal is missing -- the only two
         *     fields with no reasonable default: an empty set of permissions means "none", but there
         *     is no entry with no addressee
         */
        public AclEntry build() {
            if (this.type == null) {
                throw new IllegalStateException("missing type component");
            }
            if (this.who == null) {
                throw new IllegalStateException("missing who component");
            }
            return new AclEntry(this.type, this.who,
                    new HashSet<AclEntryPermission>(this.perms),
                    new HashSet<AclEntryFlag>(this.flags));
        }

        /** It sets the type. */
        public Builder setType(AclEntryType type) {
            if (type == null) {
                throw new NullPointerException();
            }
            this.type = type;
            return this;
        }

        /** It sets whom it applies to. */
        public Builder setPrincipal(UserPrincipal who) {
            if (who == null) {
                throw new NullPointerException();
            }
            this.who = who;
            return this;
        }

        /**
         * It sets the permissions.
         *
         * <p>It copies and checks element by element rather than trust the static type: a raw `Set`
         * can carry anything, and the `ClassCastException` would turn up much later, on use.
         */
        public Builder setPermissions(Set<AclEntryPermission> perms) {
            if (perms.isEmpty()) {
                this.perms = Collections.emptySet();
                return this;
            }
            Set<AclEntryPermission> copied = new HashSet<AclEntryPermission>();
            Iterator<AclEntryPermission> it = perms.iterator();
            while (it.hasNext()) {
                AclEntryPermission p = it.next();
                if (p == null) {
                    throw new NullPointerException();
                }
                copied.add(p);
            }
            this.perms = copied;
            return this;
        }

        /** It sets the permissions, loose. */
        public Builder setPermissions(AclEntryPermission... perms) {
            Set<AclEntryPermission> copied = new HashSet<AclEntryPermission>();
            int i = 0;
            while (i < perms.length) {
                if (perms[i] == null) {
                    throw new NullPointerException();
                }
                copied.add(perms[i]);
                i = i + 1;
            }
            this.perms = copied;
            return this;
        }

        /** It sets the inheritance flags. */
        public Builder setFlags(Set<AclEntryFlag> flags) {
            if (flags.isEmpty()) {
                this.flags = Collections.emptySet();
                return this;
            }
            Set<AclEntryFlag> copied = new HashSet<AclEntryFlag>();
            Iterator<AclEntryFlag> it = flags.iterator();
            while (it.hasNext()) {
                AclEntryFlag f = it.next();
                if (f == null) {
                    throw new NullPointerException();
                }
                copied.add(f);
            }
            this.flags = copied;
            return this;
        }

        /** It sets the inheritance flags, loose. */
        public Builder setFlags(AclEntryFlag... flags) {
            Set<AclEntryFlag> copied = new HashSet<AclEntryFlag>();
            int i = 0;
            while (i < flags.length) {
                if (flags[i] == null) {
                    throw new NullPointerException();
                }
                copied.add(flags[i]);
                i = i + 1;
            }
            this.flags = copied;
            return this;
        }
    }
}
