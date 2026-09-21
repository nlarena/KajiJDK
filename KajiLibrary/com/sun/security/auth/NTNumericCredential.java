package com.sun.security.auth;

/**
 * The access token handle Windows gave the process.
 *
 * <h2>Why it is a credential and not a principal</h2>
 *
 * <p>A principal says <em>who you are</em>; a credential is <em>what you prove it with</em>.
 * This number does not identify anybody by itself: it is an opaque reference to a structure of
 * the operating system, which is what the process presents in order to act in the user's name.
 *
 * <p>Hence it implements neither {@link java.security.Principal} nor is it serializable: a
 * handle only means something <strong>on the machine and in the process where it was
 * created</strong>. Sending it somewhere else would give a number that over there points at
 * something else, or at nothing.
 */
public class NTNumericCredential {

    private final long impersonationToken;

    /** With the handle the system gave. */
    public NTNumericCredential(long token) {
        this.impersonationToken = token;
    }

    /** The handle. */
    public long getToken() {
        return this.impersonationToken;
    }

    public String toString() {
        return "NTNumericCredential: " + String.valueOf(this.impersonationToken);
    }

    /** By exact class and value. */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }
        return this.impersonationToken == ((NTNumericCredential) o).getToken();
    }

    public int hashCode() {
        return (int) this.impersonationToken;
    }
}
