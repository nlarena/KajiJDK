package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

/**
 * An identity named by its LDAP distinguished name.
 *
 * <h2>Why it compares by {@link LdapName} and not by text</h2>
 *
 * <p>Because the same DN is written in many ways. {@code CN=John, DC=com} and
 * {@code cn=John,dc=com} are <strong>the same identity</strong> -- the types do not tell case
 * apart, the spaces around the comma do not count -- and comparing them as strings would say
 * they are not.
 *
 * <p>A principal that answers "you are not you" to the same person written differently breaks
 * any access policy, and that is why this class parses in the constructor. The name
 * {@link #getName} returns is the original one, unnormalized: the normalization is for
 * comparing, not for showing.
 */
public final class LdapPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 6820120005580754861L;

    private final String nameString;
    private final transient LdapName name;

    /**
     * @throws InvalidNameException if it is not a valid distinguished name
     * @throws NullPointerException if it is {@code null}
     */
    public LdapPrincipal(String name) throws InvalidNameException {
        if (name == null) {
            throw new NullPointerException("the name cannot be null");
        }
        this.name = new LdapName(name);
        this.nameString = name;
    }

    /** The name just as it was written. */
    public String getName() {
        return this.nameString;
    }

    public String toString() {
        return this.nameString;
    }

    /** By distinguished name, not by text; see the class note. */
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LdapPrincipal)) {
            return false;
        }
        return this.name.equals(((LdapPrincipal) object).name);
    }

    /** Over the normalized name, coherent with {@link #equals}. */
    public int hashCode() {
        return this.name.hashCode();
    }
}
