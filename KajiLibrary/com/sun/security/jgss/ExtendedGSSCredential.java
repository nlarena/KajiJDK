package com.sun.security.jgss;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

/**
 * A {@link GSSCredential} that knows how to pass itself off as another.
 *
 * <p>It is the client half of Kerberos's constrained delegation (S4U2self + S4U2proxy): a front
 * service --a web server-- authenticates a user by another means and afterwards needs to talk
 * to a back service --a database-- **in that user's name**, without the user having delegated
 * anything to it.
 *
 * <p>That this should be safe depends entirely on the KDC: it is the one that decides, by
 * policy, for which services the front one may ask for another's tickets. The credential that
 * comes out of {@link #impersonate} is no more powerful than what the KDC is willing to emit.
 */
public interface ExtendedGSSCredential extends GSSCredential {

    /**
     * A credential in order to act in `name`'s name.
     *
     * @param name who has to be passed off as
     * @return the credential for that name
     * @throws GSSException if the mechanism does not support the impersonation, or if the KDC
     *     rejects it
     */
    GSSCredential impersonate(org.ietf.jgss.GSSName name) throws GSSException;
}
