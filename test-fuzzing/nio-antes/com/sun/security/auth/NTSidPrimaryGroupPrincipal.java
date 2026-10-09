package com.sun.security.auth;

/**
 * A {@link NTSid} that identifies the user's primary group.
 *
 * <p>It adds no behaviour: what it contributes is the <strong>type</strong>. Since
 * {@link NTSid#equals} compares by exact class, this one is never going to satisfy a policy
 * written for another of the four subclasses, even though the SID's text should coincide.
 */
public class NTSidPrimaryGroupPrincipal extends NTSid {

    private static final long serialVersionUID = 8011978367305190527L;

    /**
     * @throws IllegalArgumentException if the SID is empty
     */
    public NTSidPrimaryGroupPrincipal(String name) {
        super(name);
    }

    public String toString() {
        return "NTSidPrimaryGroupPrincipal:  " + getName();
    }

    /** Inherited from {@link NTSid}: by exact class and SID. */
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
