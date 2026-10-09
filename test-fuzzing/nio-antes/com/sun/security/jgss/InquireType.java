package com.sun.security.jgss;

/**
 * What may be asked of an already established context.
 *
 * <p>It is the argument of {@link ExtendedGSSContext#inquireSecContext}, and each constant
 * brings a different return type --the method returns {@code Object} precisely for that. The six
 * are Kerberos 5's: GSS-API defines no portable query, so everything that may be asked is
 * specific to the mechanism.
 */
public enum InquireType {

    /**
     * The session key. It returns a {@link javax.crypto.SecretKey}.
     *
     * @deprecated It gives the key without saying which kind of Kerberos encryption it goes with;
     *     use {@link #KRB5_GET_SESSION_KEY_EX}, which returns both things.
     */
    @Deprecated
    KRB5_GET_SESSION_KEY,

    /** The session key together with its encryption type. It returns a Kerberos `EncryptionKey`. */
    KRB5_GET_SESSION_KEY_EX,

    /** The service ticket's flags. It returns a {@code boolean[]} of 32 positions. */
    KRB5_GET_TKT_FLAGS,

    /** The ticket's authorization data. It returns an {@link AuthorizationDataEntry}`[]`. */
    KRB5_GET_AUTHZ_DATA,

    /** The ticket's authentication instant. It returns a {@link java.util.Date}. */
    KRB5_GET_AUTHTIME,

    /** The delegated credential, in KRB-CRED format. It returns a `KerberosCredMessage`. */
    KRB5_GET_KRB_CRED
}
