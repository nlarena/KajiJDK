package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * The Unix UID: the number that really identifies a user.
 *
 * <h2>Why the number and not the name</h2>
 *
 * <p>Because the name is a label that lives in {@code /etc/passwd} and the kernel does not know
 * it: all the file system's permissions are resolved against the UID. Two names may point at
 * the same UID -- and there they are the same identity, even though they are written
 * differently; a recycled name points at a new UID.
 *
 * <p>Hence this principal exists apart from {@link UnixPrincipal}: one is what it is called, the
 * other is who it is.
 *
 * <p>The two constructors are the same datum in two forms, and {@link #getName} returns the text
 * while {@link #longValue} returns the number -- useful because the UID arrives as a string from
 * almost everywhere.
 */
public class UnixNumericUserPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = -4329764253802397821L;

    private final String name;

    /**
     * @throws NullPointerException if it is {@code null}
     * @throws NumberFormatException if it is not a number
     */
    public UnixNumericUserPrincipal(String name) {
        if (name == null) {
            throw new NullPointerException("the UID cannot be null");
        }
        Long.parseLong(name);
        this.name = name;
    }

    /** From the number. */
    public UnixNumericUserPrincipal(long name) {
        this.name = Long.toString(name);
    }

    /** The UID as text. */
    public String getName() {
        return this.name;
    }

    /** The UID as a number. */
    public long longValue() {
        return Long.parseLong(this.name);
    }

    public String toString() {
        return "UnixNumericUserPrincipal: " + this.name;
    }

    /** By exact class and UID. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.name.equals(((UnixNumericUserPrincipal) o).getName());
    }

    public int hashCode() {
        return this.name.hashCode();
    }
}
