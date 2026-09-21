package com.sun.net.httpserver;

import java.security.Principal;

/**
 * Who sent an authenticated request: a user inside a realm.
 *
 * <h2>Why the realm is part of the identity</h2>
 *
 * <p>Because the user name alone is not unique. The realm is the scope where that name means
 * something, and the same server may have several -- one for administration, another for the
 * public area -- with a different {@code admin} in each. Hence {@link #getName} returns
 * {@code "realm/user"} and not only the user: comparing identities by the bare name would
 * confuse two different people.
 */
public class HttpPrincipal implements Principal {

    private final String username;
    private final String realm;

    /**
     * @throws NullPointerException if any of them is {@code null}
     */
    public HttpPrincipal(String username, String realm) {
        if (username == null || realm == null) {
            throw new NullPointerException("username and realm may not be null");
        }
        this.username = username;
        this.realm = realm;
    }

    /** Over the two components: see the class note. */
    public boolean equals(Object another) {
        if (this == another) {
            return true;
        }
        if (another instanceof HttpPrincipal) {
            HttpPrincipal o = (HttpPrincipal) another;
            return this.username.equals(o.username) && this.realm.equals(o.realm);
        }
        return false;
    }

    /** The contextualized name: {@code "realm/user"}. */
    public String getName() {
        return this.realm + "/" + this.username;
    }

    /** Only the user. */
    public String getUsername() {
        return this.username;
    }

    /** Only the realm. */
    public String getRealm() {
        return this.realm;
    }

    public int hashCode() {
        return getName().hashCode();
    }

    public String toString() {
        return getName();
    }
}
