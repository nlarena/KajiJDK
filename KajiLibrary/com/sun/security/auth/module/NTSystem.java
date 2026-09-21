package com.sun.security.auth.module;

/**
 * Who the process's user is, according to Windows: name, domain and the SIDs.
 *
 * <h2>What a SID is and why it is not a number</h2>
 *
 * <p>A SID identifies an account uniquely <strong>and for ever</strong>: deleting a user and
 * creating another with the same name gives different SIDs, which is what keeps the new one
 * from inheriting the old one's permissions. That is why Windows keeps SIDs in the access
 * control lists and not names.
 *
 * <p>It is written {@code S-1-5-21-...-1001}: the authority, the domain and the relative
 * identifier inside that domain. The name is only a label on top of that.
 *
 * <h2>Why the constructor fails instead of answering something</h2>
 *
 * <p>Because there is no way of obtaining a SID in pure Java. They come from
 * {@code OpenProcessToken} and {@code GetTokenInformation}, which are calls to the Windows
 * API.
 *
 * <p>And an invented SID is worse than none, for the same reason as an invented uid: it is used
 * in order to compare against access control lists, and a comparison against a made-up value
 * may come out true. That is why it fails from the start.
 *
 * <p>{@link #getImpersonationToken} is even clearer: it returns a descriptor of the operating
 * system, a pointer. There is no honest value to return without the operating system on the
 * other side.
 *
 * @since 1.4
 */
public class NTSystem {

    /**
     * It asks Windows who the process's user is.
     *
     * @throws UnsupportedOperationException always, in this library: the SIDs come from the
     *     Windows API and this VM has no way of calling it
     */
    public NTSystem() {
        throw new UnsupportedOperationException(
                "NTSystem's data come from OpenProcessToken/GetTokenInformation, which this VM "
                + "cannot call; an invented SID is compared against access control lists and may "
                + "come out true, so failing is the only defensible thing");
    }

    /**
     * The user's name.
     *
     * @return the name, or {@code null} if it could not be obtained
     */
    public String getName() {
        return null;
    }

    /**
     * The domain it belongs to.
     *
     * @return the domain, or {@code null} if it could not be obtained
     */
    public String getDomain() {
        return null;
    }

    /**
     * The domain's SID.
     *
     * @return the SID, or {@code null} if it could not be obtained
     */
    public String getDomainSID() {
        return null;
    }

    /**
     * The user's SID.
     *
     * @return the SID, or {@code null} if it could not be obtained
     */
    public String getUserSID() {
        return null;
    }

    /**
     * The primary group's SID.
     *
     * @return the SID, or {@code null} if it could not be obtained
     */
    public String getPrimaryGroupID() {
        return null;
    }

    /**
     * The SIDs of the other groups.
     *
     * @return the SIDs, or {@code null} if they could not be obtained
     */
    public String[] getGroupIDs() {
        return null;
    }

    /**
     * The descriptor of the process's impersonation token.
     *
     * <p>It is a pointer of the operating system, not a datum: whoever receives it passes it back
     * to Windows. Without Windows on the other side there is nothing that means anything.
     *
     * @return the descriptor
     * @throws UnsupportedOperationException always, in this library
     */
    public synchronized long getImpersonationToken() {
        throw new UnsupportedOperationException(
                "the impersonation token is a descriptor of the operating system; there is "
                + "nothing to return without Windows on the other side");
    }
}
