package java.util;

import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;

// The permission to read or write a system property.
//
// It inherits from `BasicPermission` the hierarchical names with a wildcard —`"java.*"` covers
// `"java.home"` and `"java.version"`— and adds the one thing `BasicPermission` does not have:
// **actions**. A property permission is not just "over which", it is "for what".
//
// The actions are `read` and `write`, comma-separated, case-insensitive and in any order.
// `getActions()` returns them **canonical** —always `"read,write"` in that order— because two
// permissions that say the same thing have to be equal and have the same hash; if the original text
// survived, `"write,read"` and `"read,write"` would be different permissions implying exactly the
// same thing.
//
// A note on the state of the model: since JDK 24 the SecurityManager is permanently disabled, so this
// class no longer governs access to `System.getProperty`. It is implemented because it is
// contract.
public final class PropertyPermission extends BasicPermission {

    private static final int READ = 1;
    private static final int WRITE = 2;

    // The actions, as bits. It is how `implies` can ask "does it cover everything needed?" with an
    // `and`, instead of comparing strings.
    private final int mask;

    // A permission over `name` with the given actions.
    public PropertyPermission(String name, String actions) {
        super(name);
        this.mask = parseText(actions);
    }

    // It turns "read", "write", "read,write" —in any order and capitalisation— into bits.
    //
    // An unknown action is IllegalArgumentException and is not ignored in silence: a typo in a
    // security policy that is swallowed without a word is a hole, not a nuisance.
    private static int parseText(String actions) {
        if (actions == null) {
            throw new NullPointerException("actions can't be null");
        }
        int m = 0;
        int i = 0;
        int n = actions.length();
        while (i < n) {
            // Skip whitespace and commas.
            while (i < n && (actions.charAt(i) == ' ' || actions.charAt(i) == ','
                    || actions.charAt(i) == '\t' || actions.charAt(i) == '\n'
                    || actions.charAt(i) == '\r' || actions.charAt(i) == '\f')) {
                i = i + 1;
            }
            if (i >= n) {
                break;
            }
            int start = i;
            while (i < n && actions.charAt(i) != ',') {
                i = i + 1;
            }
            String word = clampTo(actions.substring(start, i));
            if (word.equalsIgnoreCase("read")) {
                m = m | READ;
            } else if (word.equalsIgnoreCase("write")) {
                m = m | WRITE;
            } else if (word.length() > 0) {
                throw new IllegalArgumentException("invalid actions: " + actions);
            }
        }
        if (m == 0) {
            throw new IllegalArgumentException("invalid actions: " + actions);
        }
        return m;
    }

    private static String clampTo(String s) {
        int a = 0;
        int b = s.length();
        while (a < b && isBlankChar(s.charAt(a))) {
            a = a + 1;
        }
        while (b > a && isBlankChar(s.charAt(b - 1))) {
            b = b - 1;
        }
        return s.substring(a, b);
    }

    private static boolean isBlankChar(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    // Whether this permission implies the other: the name has to cover it **and** the actions too.
    //
    // Both conditions are necessary and neither is enough alone: `("java.*", "read")` does not imply
    // `("java.home", "write")` even though the name is wide enough.
    public boolean implies(Permission p) {
        if (!(p instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission that = (PropertyPermission) p;
        if ((this.mask & that.mask) != that.mask) {
            return false;
        }
        return super.implies(that);
    }

    // Equality by name and actions. Two permissions with the same name and different actions are
    // different, even if one implies the other.
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission that = (PropertyPermission) obj;
        return this.mask == that.mask && this.getName().equals(that.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    // The actions in canonical form: "read", "write" or "read,write".
    public String getActions() {
        if (this.mask == (READ | WRITE)) {
            return "read,write";
        }
        if (this.mask == READ) {
            return "read";
        }
        return "write";
    }

    // A collection that accumulates the actions of the permissions covering a name.
    public PermissionCollection newPermissionCollection() {
        return new PropertyPermissionCollection();
    }
}

// PropertyPermission's collection.
//
// Asking each permission one at a time is not enough: holding `("java.*", "read")` and
// `("java.home", "write")` **does** imply `("java.home", "read,write")`, and no single permission
// implies it. The actions of all those covering the name have to be accumulated and only then
// compared. It is the reason `PermissionCollection.implies` exists as an operation of its own and not
// as a loop over `Permission.implies`.
final class PropertyPermissionCollection extends PermissionCollection {

    private final ArrayList<PropertyPermission> permissions = new ArrayList<PropertyPermission>();

    public void add(Permission permission) {
        if (!(permission instanceof PropertyPermission)) {
            throw new IllegalArgumentException("invalid permission: " + permission);
        }
        if (this.isReadOnly()) {
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");
        }
        this.permissions.add((PropertyPermission) permission);
    }

    public boolean implies(Permission permission) {
        if (!(permission instanceof PropertyPermission)) {
            return false;
        }
        PropertyPermission wanted = (PropertyPermission) permission;
        // The actions of all those covering the name are accumulated; it is enough for the union to
        // cover what was asked.
        int accumulated = 0;
        int i = 0;
        while (i < this.permissions.size()) {
            PropertyPermission held = this.permissions.get(i);
            if (coversName(held.getName(), wanted.getName())) {
                // A permission with the same actions and that name: it is asked, since it already
                // knows how to compare masks.
                if (held.implies(new PropertyPermission(wanted.getName(), held.getActions()))) {
                    accumulated = accumulated | maskBits(held.getActions());
                }
            }
            i = i + 1;
        }
        return (accumulated & maskBits(wanted.getActions())) == maskBits(wanted.getActions());
    }

    // Whether `held` covers `wanted` as a name, with the same wildcard rules as BasicPermission. It
    // is reimplemented here because `getCanonicalName()` is package-private to `java.security` and is
    // not visible from `java.util`.
    private static boolean coversName(String held, String wanted) {
        if (held.equals("*")) {
            return true;
        }
        if (held.endsWith(".*")) {
            String prefix = held.substring(0, held.length() - 1);
            return wanted.length() > prefix.length() && wanted.startsWith(prefix);
        }
        return held.equals(wanted);
    }

    private static int maskBits(String actions) {
        int m = 0;
        if (actions.equals("read") || actions.equals("read,write")) {
            m = m | 1;
        }
        if (actions.equals("write") || actions.equals("read,write")) {
            m = m | 2;
        }
        return m;
    }

    public Enumeration<Permission> elements() {
        ArrayList<Permission> copied = new ArrayList<Permission>();
        int i = 0;
        while (i < this.permissions.size()) {
            copied.add(this.permissions.get(i));
            i = i + 1;
        }
        return new PropPermEnum(copied);
    }
}

// An enumeration over a PropertyPermissionCollection's permissions.
final class PropPermEnum implements Enumeration<Permission> {

    private final ArrayList<Permission> list;
    private int cursor;

    PropPermEnum(ArrayList<Permission> list) {
        this.list = list;
    }

    public boolean hasMoreElements() {
        return this.cursor < this.list.size();
    }

    public Permission nextElement() {
        if (this.cursor >= this.list.size()) {
            throw new NoSuchElementException();
        }
        Permission p = this.list.get(this.cursor);
        this.cursor = this.cursor + 1;
        return p;
    }
}
