package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * The Windows user name.
 *
 * <p>Like every principal of this package: immutable, compared by name and by exact class. The
 * second matters more than it seems -- a {@code UnixPrincipal} and an {@code NTUserPrincipal} with
 * the same text are <strong>not</strong> the same identity, and comparing them only by name would
 * make a policy written for one apply to the other.
 */
public final class NTUserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -8737649811939033735L;

    private final String name;

    /**
     * @throws NullPointerException if the name is {@code null}
     */
    public NTUserPrincipal(String name) {
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
        return "NTUserPrincipal:  " + this.name;
    }

    /** By exact class and name; see the class note. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((NTUserPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
