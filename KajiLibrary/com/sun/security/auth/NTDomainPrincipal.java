package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * The Windows domain the user belongs to.
 *
 * <p>It goes separately from the user because in Windows the identity is the pair: the same
 * account name in two domains are two different people.
 *
 * <p>Like every principal of this package: immutable, compared by name and by exact class. The
 * second matters more than it seems -- a {@code UnixPrincipal} and an {@code NTUserPrincipal} with
 * the same text are <strong>not</strong> the same identity, and comparing them only by name would
 * make a policy written for one apply to the other.
 */
public class NTDomainPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4408637351440771220L;

    private final String name;

    /**
     * @throws NullPointerException if the name is {@code null}
     */
    public NTDomainPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("the name cannot be null");
        }
        this.name = name;
    }

    /** The name. */
    public String getName() {
        return this.name;
    }

    public String toString() {
        return "NTDomainPrincipal: " + this.name;
    }

    /** By exact class and name; see the class note. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((NTDomainPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
