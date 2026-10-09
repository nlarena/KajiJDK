package javax.naming.ldap;

import java.io.Serializable;

/**
 * The response to an {@link ExtendedRequest}.
 *
 * <p>Deliberately thin: OID and bytes. What those bytes <em>mean</em> is known by the concrete
 * subclass, which is the one that adds the meaningful accessors -- see {@link StartTlsResponse},
 * whose response carries no data and instead offers {@code negotiate()}.
 */
public interface ExtendedResponse extends Serializable {

    /** The OID of the operation that produced it, or {@code null}. */
    String getID();

    /** The BER-encoded response, or {@code null} if it carries no data. */
    byte[] getEncodedValue();
}
