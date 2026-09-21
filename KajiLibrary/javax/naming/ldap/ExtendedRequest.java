package javax.naming.ldap;

import java.io.Serializable;
import javax.naming.NamingException;

/**
 * An operation LDAP does not define, identified by an OID.
 *
 * <h2>The other half of extensibility</h2>
 *
 * <p>A {@link Control} modifies an operation that already exists; this <strong>adds a new
 * one</strong>. It is how password change, cancelling an operation in progress, or this package's
 * own {@link StartTlsRequest} are done.
 *
 * <h2>Why the request makes its own response</h2>
 *
 * <p>{@link #createExtendedResponse} is surprising until you see the reason: the LDAP provider
 * receives an OID and some bytes from the server, and <strong>does not know how to interpret
 * them</strong> -- the operation belongs to whoever defined it. The only one that knows which type
 * to build is the request that started it, so the provider hands it the raw bytes and asks it to
 * build the object.
 *
 * <p>It is inversion of control, and it is what allows adding an operation without touching the
 * provider.
 */
public interface ExtendedRequest extends Serializable {

    /** The operation's OID. */
    String getID();

    /** The BER-encoded arguments, or {@code null} if there are none. */
    byte[] getEncodedValue();

    /**
     * Builds the response from what arrived.
     *
     * @param id the OID the server returned, which may be {@code null}
     * @param berValue the buffer with the response, or {@code null}
     * @param offset where it starts within the buffer
     * @param length how many bytes it takes
     * @throws NamingException if the response could not be built
     */
    ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset, int length)
            throws NamingException;
}
