package javax.security.auth;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.security.auth.PrivateCredentialPermission -- permission to read a private
 * credential of a {@link Subject}.
 *
 * <p>The permission's name is a small grammar and it is worth writing it down because the parsing
 * is almost the whole class:
 *
 * <pre>
 *   CredentialClass PrincipalClass "PrincipalName" [PrincipalClass "PrincipalName"]*
 * </pre>
 *
 * <p>For example {@code "java.lang.String javax.security.auth.x500.X500Principal \"cn=john\""}. The
 * principal names go in quotes <b>always</b>, even the wildcard, and there has to be at least one
 * pair: a loose credential class does not say whose the credential is, and that is the datum that
 * decides whether it can be read or not.
 *
 * <h2>The wildcards and the rule that binds them</h2>
 *
 * <p>Both the credential class and each principal pair accept {@code *}. But there is a forbidden
 * combination: a wildcard principal <b>class</b> with a concrete <b>name</b>. It makes sense --
 * "any principal class named john" is a condition that cannot be evaluated, because the name only
 * means something within a name space. The JDK rejects it in the constructor and so does this.
 *
 * <h2>How implies reads</h2>
 *
 * <p>{@code a.implies(b)} asks whether having {@code a} is enough for what {@code b} asks for, and
 * the direction surprises: a permission with <b>fewer</b> principals implies one with more, not the
 * other way round. The reason is that each principal is an additional condition on the same Subject
 * -- asking for "a credential of a Subject that is P1 <i>and also</i> P2" is asking for less than
 * "a credential of a Subject that is P1" --, so whoever has the looser permission also has the
 * stricter one.
 *
 * <p>A note on what it serves today: like {@link AuthPermission}, no check in the library consults
 * it because the security manager can no longer be enabled. The class exists because its form --
 * parsing, {@code implies}, {@code equals} -- is part of the API.
 */
public final class PrivateCredentialPermission extends Permission {

    private static final long serialVersionUID = 5284372143517237068L;

    private static final String WILDCARD = "*";

    private final String credentialClass;
    // (class, name) pairs, in the order they appeared. The order is kept and not a set because
    // `getPrincipals()` and `getName()` return it, although `implies` does not look at it.
    private final String[][] principals;
    private final String actions;

    public PrivateCredentialPermission(String name, String actions) {
        super(name);
        // "read" is the only action that exists. Anything else --null included-- is rejected
        // instead of ignored: a permission built with "write" would read as if it granted
        // permission to read.
        if (actions == null || !actions.equalsIgnoreCase("read")) {
            throw new IllegalArgumentException("actions can only be 'read'");
        }
        this.actions = "read";
        if (name == null) {
            throw new NullPointerException("invalid null name");
        }
        if (name.trim().length() == 0) {
            throw new IllegalArgumentException("invalid empty name");
        }
        List<String[]> pairs = new ArrayList<String[]>();
        this.credentialClass = parse(name, pairs);
        this.principals = pairs.toArray(new String[pairs.size()][]);
    }

    // Returns the credential class and fills `pairs`. The text is walked by hand and not with a
    // space separator because the names go in quotes and may carry spaces inside --`"cn=John
    // Smith"` is a single name--.
    private static String parse(String name, List<String[]> pairs) {
        int i = 0;
        int n = name.length();
        String cls = null;
        String principalClass = null;
        while (i < n) {
            while (i < n && isBlank(name.charAt(i))) {
                i = i + 1;
            }
            if (i >= n) {
                break;
            }
            if (name.charAt(i) == '"') {
                int close = name.indexOf('"', i + 1);
                if (close < 0) {
                    throw invalid(name, "Principal Name must be surrounded by quotes");
                }
                if (principalClass == null) {
                    throw invalid(name,
                        "Credential Class not followed by a Principal Class and Name");
                }
                String principalName = name.substring(i + 1, close);
                // See the class note: a wildcard class with a concrete name cannot be evaluated, so
                // it is rejected instead of accepted and never met.
                if (WILDCARD.equals(principalClass) && !WILDCARD.equals(principalName)) {
                    throw new IllegalArgumentException("PrivateCredentialPermission Principal "
                        + "Class can not be a wildcard (*) value if Principal Name is not a "
                        + "wildcard (*) value");
                }
                pairs.add(new String[] {principalClass, principalName});
                principalClass = null;
                i = close + 1;
                continue;
            }
            int from = i;
            while (i < n && !isBlank(name.charAt(i))) {
                i = i + 1;
            }
            String word = name.substring(from, i);
            if (cls == null) {
                cls = word;
            } else if (principalClass == null) {
                principalClass = word;
            } else {
                // Two classes in a row with no name in between: the pair is missing.
                throw invalid(name, "Principal Name must be surrounded by quotes");
            }
        }
        if (cls == null) {
            throw new IllegalArgumentException("invalid empty name");
        }
        if (principalClass != null) {
            throw invalid(name, "Principal Name must be surrounded by quotes");
        }
        if (pairs.isEmpty()) {
            throw invalid(name, "Credential Class not followed by a Principal Class and Name");
        }
        return cls;
    }

    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    private static IllegalArgumentException invalid(String name, String what) {
        return new IllegalArgumentException("permission name [" + name + "] syntax invalid: " + what);
    }

    /** The credential's class, or {@code "*"}. */
    public String getCredentialClass() {
        return this.credentialClass;
    }

    /**
     * The principal's (class, name) pairs. A copy: touching what comes out of here does not change
     * the permission.
     */
    public String[][] getPrincipals() {
        String[][] copyOf = new String[this.principals.length][];
        int i = 0;
        while (i < this.principals.length) {
            copyOf[i] = new String[] {this.principals[i][0], this.principals[i][1]};
            i = i + 1;
        }
        return copyOf;
    }

    /** Always {@code "read"}. */
    @Override
    public String getActions() {
        return this.actions;
    }

    /**
     * Whether having this permission is enough for what {@code p} asks for. See the class note
     * about the direction, which is the opposite of what one expects.
     */
    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof PrivateCredentialPermission)) {
            return false;
        }
        PrivateCredentialPermission other = (PrivateCredentialPermission) p;
        if (!WILDCARD.equals(this.credentialClass)
                && !this.credentialClass.equals(other.credentialClass)) {
            return false;
        }
        // Each condition of this permission has to be covered by some condition of the other. If
        // this one has no condition left uncovered, then the other asks for at least the same.
        int i = 0;
        while (i < this.principals.length) {
            boolean covered = false;
            int j = 0;
            while (j < other.principals.length) {
                if (covers(this.principals[i], other.principals[j])) {
                    covered = true;
                    break;
                }
                j = j + 1;
            }
            if (!covered) {
                return false;
            }
            i = i + 1;
        }
        return other.principals.length > 0;
    }

    private static boolean covers(String[] mio, String[] theirs) {
        if (!WILDCARD.equals(mio[0]) && !mio[0].equals(theirs[0])) {
            return false;
        }
        return WILDCARD.equals(mio[1]) || mio[1].equals(theirs[1]);
    }

    /**
     * Two permissions are the same if each implies the other.
     *
     * <p>It is defined this way and not by comparing the names because the order of the pairs means
     * nothing: {@code "C P1 \"a\" P2 \"b\""} and {@code "C P2 \"b\" P1 \"a\""} are the
     * same permission written two ways.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PrivateCredentialPermission)) {
            return false;
        }
        PrivateCredentialPermission other = (PrivateCredentialPermission) obj;
        return this.implies(other) && other.implies(this);
    }

    /**
     * Only the credential class.
     *
     * <p>It is a poor hash on purpose: it has to be consistent with an {@code equals} that ignores
     * the order of the pairs and handles wildcards, and the credential class is the only thing two
     * equal permissions always share.
     */
    @Override
    public int hashCode() {
        return this.credentialClass.hashCode();
    }

    /**
     * Null, as in the JDK: there is no specialised collection for this permission, so whoever
     * stores it has to use the generic one.
     */
    @Override
    public PermissionCollection newPermissionCollection() {
        return null;
    }
}
