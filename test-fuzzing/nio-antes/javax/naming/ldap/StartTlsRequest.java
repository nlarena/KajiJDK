package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * The extended operation that turns a cleartext LDAP connection into an encrypted one.
 *
 * <h2>StartTLS versus LDAPS</h2>
 *
 * <p>There are two ways to encrypt LDAP and they are not the same. <strong>LDAPS</strong> opens TLS
 * from the first byte, on a port of its own. <strong>StartTLS</strong> --this-- starts in cleartext
 * on the usual port and negotiates the switch halfway.
 *
 * <p>StartTLS's advantage is that it uses a single port and lets the client decide; the
 * disadvantage is exactly that, and it has to be kept in mind: an attacker in the middle can
 * <em>strip</em> the announcement that StartTLS is available, and a client that only encrypts "if
 * the server offers it" ends up talking in cleartext without noticing. That is why the decision
 * to require it has to be the client's and not depend on what the server says.
 *
 * <p>That it carries no value --{@link #getEncodedValue} returns {@code null}-- is correct: the
 * request is just the OID.
 */
public class StartTlsRequest implements ExtendedRequest {

    private static final long serialVersionUID = 4441679576360753397L;

    /** The operation's OID, from RFC 2830. */
    public static final String OID = "1.3.6.1.4.1.1466.20037";

    public StartTlsRequest() {
    }

    public String getID() {
        return OID;
    }

    /** {@code null}: this request carries no data. */
    public byte[] getEncodedValue() {
        return null;
    }

    /**
     * Looks up a {@link StartTlsResponse} implementation through {@link java.util.ServiceLoader}.
     *
     * <p>It does not build one directly because negotiating TLS depends on the provider: each one
     * knows how to wrap <em>its</em> socket. In this VM none is registered, so it declines -- the
     * mechanism is there and what is missing is someone who registers.
     *
     * @throws NamingException if there is no implementation
     */
    public ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset,
            int length) throws NamingException {
        if (id != null && !id.equals(OID)) {
            throw new NamingException("the response is not StartTLS: " + id);
        }
        java.util.Iterator<StartTlsResponse> it =
                java.util.ServiceLoader.load(StartTlsResponse.class).iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw new NamingException(
                "no StartTlsResponse implementation is registered");
    }
}
