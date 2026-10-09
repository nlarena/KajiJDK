package com.sun.security.jgss;

import javax.security.auth.Subject;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;

/**
 * The bridge between GSS-API and JAAS.
 *
 * <p>A single method, and it does a single thing: to pass an identity of the GSS world --a
 * {@link GSSName} and a {@link GSSCredential}-- to the world of {@link Subject}, which is where
 * everything that authorizes in Java expects it. Without it, a program that authenticates by
 * GSS-API has nothing with which to call {@code Subject.doAs}.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #createSubject} is not implemented. The conversion **is not generic**: the
 * `GSSName` has to be translated into the corresponding `KerberosPrincipal` and the
 * `GSSCredential` into the {@code KerberosTicket} and {@code KerberosKey} it carries inside,
 * and that asks for the whole Kerberos provider --which this library does not have. It throws
 * {@link UnsupportedOperationException} with the reason.
 *
 * <p>The alternative would be to return an empty `Subject`, or one with the name put in as a
 * generic principal. It would be worse: a `Subject` without the Kerberos credentials **seems**
 * a valid identity, passes through `Subject.doAs`, and fails much later --on the first call
 * that needs the ticket-- with no clue that the identity came badly built from here.
 */
public class GSSUtil {

    /** It is not instantiated: it is a utility class. */
    private GSSUtil() {
    }

    /**
     * The {@link Subject} that corresponds to that GSS identity.
     *
     * <p><b>Not implemented in this library.</b> See the class note.
     *
     * @param principals the name, or `null`
     * @param credentials the credentials, or `null`
     * @throws UnsupportedOperationException always, in this library
     */
    public static Subject createSubject(GSSName principals, GSSCredential credentials) {
        throw new UnsupportedOperationException(
                "cannot build a Subject from GSS identities: the conversion is Kerberos-specific "
                + "(KerberosPrincipal, KerberosTicket, KerberosKey) and this library does "
                + "not have a Kerberos mechanism");
    }
}
