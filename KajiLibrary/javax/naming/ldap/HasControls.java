package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * Implemented by a search result that also carries controls from the server.
 *
 * <h2>Why it is a separate interface and not a method of {@code SearchResult}</h2>
 *
 * <p>Because {@code javax.naming} is protocol-neutral: it serves LDAP, DNS, a file directory.
 * Controls are LDAP's, so putting them in the common type would tie the general API to a
 * particular protocol.
 *
 * <p>The practical consequence is that you have to ask with {@code instanceof} before reading
 * them.
 */
public interface HasControls {

    /** The controls that came with this result, or {@code null} if none came. */
    Control[] getControls() throws NamingException;
}
