package com.sun.security.auth;

/**
 * A {@link NTSid} that identifies a domain.
 *
 * <p>It adds no behaviour: what it contributes is the <strong>type</strong>. Since
 * {@link NTSid#equals} compares by exact class, this one is never going to satisfy a policy
 * written for another of the four subclasses, even though the SID's text should coincide.
 */
public class NTSidDomainPrincipal extends NTSid {

    private static final long serialVersionUID = 5247810094488968179L;

    /**
     * @throws IllegalArgumentException if the SID is empty
     */
    public NTSidDomainPrincipal(String name) {
        super(name);
    }

    public String toString() {
        return "NTSidDomainPrincipal:  " + getName();
    }

    /** Inherited from {@link NTSid}: by exact class and SID. */
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
