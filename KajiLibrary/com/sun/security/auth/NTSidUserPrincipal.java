package com.sun.security.auth;

/**
 * A {@link NTSid} that identifies a user.
 *
 * <p>It adds no behaviour: what it contributes is the <strong>type</strong>. Since
 * {@link NTSid#equals} compares by exact class, this one is never going to satisfy a policy
 * written for another of the four subclasses, even though the SID's text should coincide.
 */
public class NTSidUserPrincipal extends NTSid {

    private static final long serialVersionUID = -5573239889517749525L;

    /**
     * @throws IllegalArgumentException if the SID is empty
     */
    public NTSidUserPrincipal(String name) {
        super(name);
    }

    public String toString() {
        return "NTSidUserPrincipal:  " + getName();
    }

    /** Inherited from {@link NTSid}: by exact class and SID. */
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
