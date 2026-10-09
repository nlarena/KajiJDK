package com.sun.security.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * A Windows security identifier: the {@code S-1-5-21-...} that really identifies an account or
 * a group.
 *
 * <h2>Why the SID and not the name</h2>
 *
 * <p>Because the name <strong>may be reused</strong>. Deleting the account {@code john} and
 * creating another with the same name gives a different account, and a policy written against
 * the name would apply to the wrong person. The SID is never reused.
 *
 * <p>It is also why the subclasses exist: {@link NTSidUserPrincipal},
 * {@link NTSidGroupPrincipal}, {@link NTSidDomainPrincipal} and
 * {@link NTSidPrimaryGroupPrincipal} are all SIDs, and what tells them apart is <em>what</em>
 * they identify. Since the comparison is by exact class, a group SID never satisfies a policy
 * written for a user even though the text should coincide.
 */
public class NTSid implements Principal, Serializable {

    private static final long serialVersionUID = 4412290580770249885L;

    private final String sid;

    /**
     * @throws NullPointerException if it is {@code null}
     * @throws IllegalArgumentException if it is empty -- an empty SID identifies nothing, and
     *     accepting it would produce a principal that equals any other empty one
     */
    public NTSid(String stringSid) {
        if (stringSid == null) {
            throw new NullPointerException("the SID cannot be null");
        }
        if (stringSid.isEmpty()) {
            throw new IllegalArgumentException("the SID cannot be empty");
        }
        this.sid = stringSid;
    }

    /** The SID in its text form. */
    public String getName() {
        return this.sid;
    }

    public String toString() {
        return "NTSid:  " + this.sid;
    }

    /** By exact class and SID. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.sid.equals(((NTSid) o).getName());
    }

    public int hashCode() {
        return this.sid.hashCode();
    }
}
