package javax.security.auth.kerberos;

import java.io.Serializable;
import java.security.Principal;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosPrincipal -- a Kerberos name.
 *
 * <p>It has the form {@code component/component@REALM}: a user is {@code ana@COMPANY.COM}, a
 * service is {@code host/server.company.com@COMPANY.COM}. The realm goes in upper case by
 * convention, but this class does not impose it: it is a name, not a rule.
 *
 * <h2>The default realm</h2>
 *
 * <p>A name without {@code @REALM} takes the realm of the Kerberos configuration. KajiJDK does not
 * read {@code krb5.conf}; it takes the {@code java.security.krb5.realm} property if it is set, and
 * otherwise fails with {@link IllegalArgumentException}, like the JDK without configuration. It is
 * the most common cause of a program that works on one machine not working on another: it is not
 * the code, it is that the other one has no realm configured.
 *
 * <h2>The name type does not count for equality</h2>
 *
 * <p>{@link #getNameType} says whether it is a user, a service, a host. Two principals with the
 * same name and a different type are <b>equal</b> and have the same hash; the type is a hint for
 * the KDC, not part of the identity.
 *
 * <h2>The at sign can be escaped</h2>
 *
 * <p>{@code a\\@b@REALM} is the user {@code a@b} of the realm {@code REALM}: the first at sign is
 * preceded by a backslash and separates nothing. This class cuts at the first <b>unescaped</b> at
 * sign and leaves the name as it is, with its backslashes.
 */
public final class KerberosPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -7374788026156829911L;

    /** Unknown type. */
    public static final int KRB_NT_UNKNOWN = 0;

    /** A user, or a service with a name of its own. */
    public static final int KRB_NT_PRINCIPAL = 1;

    /** A service with an instance: {@code service/instance}. */
    public static final int KRB_NT_SRV_INST = 2;

    /** A host's service: {@code service/host}. */
    public static final int KRB_NT_SRV_HST = 3;

    /** A host's service, with the host as separate components. */
    public static final int KRB_NT_SRV_XHST = 4;

    /** A numeric identifier. */
    public static final int KRB_NT_UID = 5;

    /** An enterprise name, of the {@code user@domain} style. */
    public static final int KRB_NT_ENTERPRISE = 10;

    /** The full name, with the realm. */
    private final String fullName;

    /** The realm. */
    private final String realm;

    /** Which type it is. */
    private final int nameType;

    /**
     * A principal of type {@link #KRB_NT_PRINCIPAL}.
     *
     * @throws IllegalArgumentException if the name is null, empty, malformed, or has no realm and
     *     there is no default realm
     */
    public KerberosPrincipal(String name) {
        this(name, KRB_NT_PRINCIPAL);
    }

    /**
     * A principal of that type.
     *
     * @throws IllegalArgumentException if the name does not serve, or if the type is not one of the
     *     seven
     */
    public KerberosPrincipal(String name, int nameType) {
        if (name == null) {
            throw new IllegalArgumentException("Null name not allowed");
        }
        if (!isLegalType(nameType)) {
            throw new IllegalArgumentException("Illegal name type");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty nameString not allowed");
        }
        int at = unescapedAt(name);
        String namePart;
        String realmPart;
        if (at < 0) {
            namePart = name;
            realmPart = defaultRealm();
        } else {
            namePart = name.substring(0, at);
            realmPart = name.substring(at + 1);
        }
        checkNamePart(namePart);
        checkRealm(realmPart);
        this.fullName = namePart + "@" + realmPart;
        this.realm = realmPart;
        this.nameType = nameType;
    }

    /** The realm. */
    public String getRealm() {
        return this.realm;
    }

    /** The hash of the full name; the type does not count. See the class note. */
    @Override
    public int hashCode() {
        return this.fullName.hashCode();
    }

    /** Equal if the full name is the same; the type does not count. See the class note. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosPrincipal)) {
            return false;
        }
        return this.fullName.equals(((KerberosPrincipal) other).fullName);
    }

    /** The full name, with the realm. */
    @Override
    public String getName() {
        return this.fullName;
    }

    /** Which type it is. */
    public int getNameType() {
        return this.nameType;
    }

    /** The full name. */
    @Override
    public String toString() {
        return this.fullName;
    }

    /** Whether it is one of the seven types. */
    private static boolean isLegalType(int nameType) {
        return (nameType >= KRB_NT_UNKNOWN && nameType <= KRB_NT_UID)
            || nameType == KRB_NT_ENTERPRISE;
    }

    /** The position of the first unescaped at sign, or -1. See the class note. */
    private static int unescapedAt(String name) {
        int i = 0;
        while (i < name.length()) {
            char c = name.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '@') {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /** That the name part has components and none is empty in the middle. */
    private static void checkNamePart(String namePart) {
        if (namePart.isEmpty()) {
            throw new IllegalArgumentException("Empty nameStrings not allowed");
        }
        // An empty component in the middle --"host//x"-- is not a name; a slash at the end is
        // tolerated, as in the JDK.
        int i = 0;
        while (i < namePart.length()) {
            char c = namePart.charAt(i);
            if (c == '\\') {
                i = i + 2;
                continue;
            }
            if (c == '/' && i + 1 < namePart.length() && namePart.charAt(i + 1) == '/') {
                throw new IllegalArgumentException("Empty nameString not allowed");
            }
            i = i + 1;
        }
    }

    /** That the realm is not empty and has no characters the protocol does not admit. */
    private static void checkRealm(String realm) {
        if (realm.isEmpty()) {
            throw new IllegalArgumentException("empty realm part not allowed");
        }
        int i = 0;
        while (i < realm.length()) {
            char c = realm.charAt(i);
            if (c == '/' || c == ':' || c == '@' || c == '\0') {
                throw new IllegalArgumentException(
                    "Illegal character in realm name; one of: '/', ':', '@' (600)");
            }
            i = i + 1;
        }
    }

    /** The default realm. See the class note. */
    private static String defaultRealm() {
        String configured = System.getProperty("java.security.krb5.realm");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        throw new IllegalArgumentException("KrbException: Cannot locate default realm");
    }
}
