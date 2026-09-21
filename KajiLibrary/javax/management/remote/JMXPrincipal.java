package javax.management.remote;

import java.io.Serializable;
import java.security.Principal;

/**
 * KajiLibrary's javax.management.remote.JMXPrincipal -- a JMX client's identity.
 *
 * <p>A name and nothing else. A {@link JMXAuthenticator} produces it and it goes inside the
 * {@code Subject} that one returns; from there the server takes it to decide what that client may
 * do.
 *
 * <p>It is immutable, and {@link #equals} compares only the name: two instances with the same
 * name are the same identity, no matter who created them.
 */
public class JMXPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4184480100214577411L;

    /** The name. */
    private String name;

    /**
     * @throws NullPointerException if the name is null
     */
    public JMXPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("illegal null input");
        }
        this.name = name;
    }

    /** The name. */
    public String getName() {
        return this.name;
    }

    /** {@code "JMXPrincipal:  "} and the name; the two spaces are the JDK's. */
    @Override
    public String toString() {
        return "JMXPrincipal:  " + this.name;
    }

    /** Only the name. */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof JMXPrincipal)) {
            return false;
        }
        return this.name.equals(((JMXPrincipal) o).getName());
    }

    /** The name's. */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
