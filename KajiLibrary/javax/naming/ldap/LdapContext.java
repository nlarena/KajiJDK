package javax.naming.ldap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * A directory context that also speaks the LDAP v3 extensions.
 *
 * <h2>What it adds on top of {@link DirContext}</h2>
 *
 * <p>LDAP's two forms of extensibility, which {@code javax.naming} cannot have because it is
 * protocol-neutral: {@link Control}s and extended operations.
 *
 * <h2>The three kinds of control, which are easy to mix up</h2>
 *
 * <ul>
 * <li><strong>connection</strong> -- they travel with every operation of this context and of those
 *     derived from it. They are set when creating the context or with {@link #reconnect};</li>
 * <li><strong>request</strong> -- only for the next operations of <em>this</em> context, and they
 *     are <strong>not</strong> inherited. It is the difference that surprises most;</li>
 * <li><strong>response</strong> -- the ones the server sent with the last operation.</li>
 * </ul>
 *
 * <p>Hence {@link #newInstance}: to use other controls without overwriting those of the context
 * you already have, you take a copy. The original context stays as it was.
 */
public interface LdapContext extends DirContext {

    /**
     * The property with the list of control factories.
     *
     * <p>Separated by colons, and consulted in order until one recognizes the control; see
     * {@link ControlFactory}.
     */
    String CONTROL_FACTORIES = "java.naming.factory.control";

    /**
     * Runs an extended operation.
     *
     * <p>The request itself builds the response -- see {@link ExtendedRequest} for why.
     */
    ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException;

    /**
     * A copy of this context with other request controls.
     *
     * <p>It shares the connection: it does not open a new one. That is what makes it cheap to have
     * several views with different configuration over the same server.
     */
    LdapContext newInstance(Control[] requestControls) throws NamingException;

    /**
     * Reconnects with other connection controls.
     *
     * <p>{@code null} removes whatever there was; so does an empty array. The reconnection may not
     * be immediate: the provider may defer it until the next operation.
     */
    void reconnect(Control[] connCtls) throws NamingException;

    /** The connection controls, or {@code null}. */
    Control[] getConnectControls() throws NamingException;

    /** Sets this context's request controls; they are not inherited. */
    void setRequestControls(Control[] requestControls) throws NamingException;

    /** The request controls, or {@code null}. */
    Control[] getRequestControls() throws NamingException;

    /**
     * The controls the server sent with the last operation.
     *
     * <p>Read them right away: the next operation on this context replaces them.
     */
    Control[] getResponseControls() throws NamingException;
}
