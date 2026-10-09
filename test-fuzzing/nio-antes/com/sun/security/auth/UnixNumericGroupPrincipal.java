package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * The Unix GID, with a distinction the number alone does not carry: whether it is the user's
 * <strong>primary</strong> group.
 *
 * <h2>Why the distinction matters</h2>
 *
 * <p>A Unix user belongs to one primary group and to as many supplementary ones as are needed.
 * For the read and write permissions the two are worth the same, but <strong>the files it
 * creates inherit the primary group</strong>. Without this datum a policy that depends on that
 * cannot be expressed.
 *
 * <p>That is why the flag is part of the identity and goes into {@link #equals}: the same GID as
 * a primary and as a supplementary one are two different principals.
 */
public class UnixNumericGroupPrincipal implements Principal, Serializable {

    private static final long serialVersionUID = 3941535899328403223L;

    private final String name;
    private final boolean primaryGroup;

    /**
     * @param primaryGroup whether it is the primary group
     * @throws NullPointerException if the GID is {@code null}
     * @throws NumberFormatException if it is not a number
     */
    public UnixNumericGroupPrincipal(String name, boolean primaryGroup) {
        if (name == null) {
            throw new NullPointerException("the GID cannot be null");
        }
        Long.parseLong(name);
        this.name = name;
        this.primaryGroup = primaryGroup;
    }

    /** From the number. */
    public UnixNumericGroupPrincipal(long name, boolean primaryGroup) {
        this.name = Long.toString(name);
        this.primaryGroup = primaryGroup;
    }

    /** The GID as text. */
    public String getName() {
        return this.name;
    }

    /** The GID as a number. */
    public long longValue() {
        return Long.parseLong(this.name);
    }

    /** Whether it is the primary group; see the class note. */
    public boolean isPrimaryGroup() {
        return this.primaryGroup;
    }

    public String toString() {
        return this.primaryGroup
                ? "UnixNumericGroupPrincipal [Primary Group]: " + this.name
                : "UnixNumericGroupPrincipal [Supplementary Group]: " + this.name;
    }

    /** By exact class, GID <strong>and</strong> the primary flag. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        UnixNumericGroupPrincipal other = (UnixNumericGroupPrincipal) o;
        return this.name.equals(other.getName()) && this.primaryGroup == other.isPrimaryGroup();
    }

    public int hashCode() {
        return this.primaryGroup ? this.name.hashCode() * 31 : this.name.hashCode();
    }
}
