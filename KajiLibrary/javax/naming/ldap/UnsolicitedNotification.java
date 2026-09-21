package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * A message the server sends without anybody having asked for it.
 *
 * <h2>Why it exists</h2>
 *
 * <p>Because the server sometimes has something to say that is not a response to anything: that it
 * is going to close the connection, that the session changed state, that there was a warning.
 * Without this mechanism it would have to wait for the next operation to tell, or simply cut off.
 *
 * <p>It extends {@link ExtendedResponse} --it is a response with an OID and data-- and
 * {@link HasControls}, because it may carry controls.
 *
 * <p>The best-known case is the <em>Notice of Disconnection</em>: the server says why it is going
 * to close, and without this the connection would simply drop without explanation.
 */
public interface UnsolicitedNotification extends ExtendedResponse, HasControls {

    /** The URLs it redirects to, or {@code null} if it does not redirect. */
    String[] getReferrals();

    /** The error it reports, or {@code null} if it is not an error. */
    NamingException getException();
}
