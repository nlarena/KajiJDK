package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

/**
 * The entry point to LDAP: the initial context with the LDAP v3 extensions.
 *
 * <h2>What an "initial context" is</h2>
 *
 * <p>It is the {@code javax.naming} pattern: you do not instantiate a concrete provider but this
 * class, which reads the environment --{@code java.naming.factory.initial} and company-- and
 * delegates to the matching factory. It is what allows changing LDAP provider without touching
 * the code.
 *
 * <p>It extends {@link InitialDirContext} and adds what {@link LdapContext} has: extended
 * operations and controls.
 *
 * <h2>Connection controls go in the constructor</h2>
 *
 * <p>And not later, because they are sent <strong>when connecting</strong>. Setting them later
 * would require reconnecting, which is exactly what {@link #reconnect} does.
 *
 * <h2>In this VM</h2>
 *
 * <p>There is no LDAP provider, and this class does not delegate. The constructors pass the
 * environment to {@link InitialDirContext} and drop {@code connCtls}; they fail only when the
 * environment names an initial factory (because {@code InitialContext} never loads one here).
 * The {@link DirContext} methods end in {@code NoInitialContextException}, and the
 * {@link LdapContext} methods of this class throw a plain {@link NamingException} every time, where
 * the JDK forwards them to the provider's {@code LdapContext}. An earlier note said construction
 * always fails and called this the normal {@code javax.naming} behaviour without a factory; neither
 * holds: the JDK would throw {@code NoInitialContextException} and would use a configured
 * factory.
 */
public class InitialLdapContext extends InitialDirContext implements LdapContext {

    private static final String NO_PROVIDER =
            "no LDAP provider is registered in this VM";

    /**
     * With the default environment and no connection controls.
     *
     * @throws NamingException if the environment names an initial factory; see the class note
     */
    public InitialLdapContext() throws NamingException {
        super();
    }

    /**
     * With that environment and those connection controls.
     *
     * @param environment the configuration, or {@code null} for the default one
     * @param connCtls the connection controls, or {@code null}; ignored in this library
     * @throws NamingException if the environment names an initial factory; see the class note
     */
    public InitialLdapContext(Hashtable<?, ?> environment, Control[] connCtls)
            throws NamingException {
        super(environment);
    }

    /**
     * In the JDK, forwarded to the provider's context; here it always throws. See the class note.
     */
    public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /**
     * A copy with other request controls; see {@link LdapContext#newInstance}. Always throws here.
     */
    public LdapContext newInstance(Control[] reqCtls) throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /** Reconnects with other connection controls. Always throws here. */
    public void reconnect(Control[] connCtls) throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /** The connection controls. Always throws here. */
    public Control[] getConnectControls() throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /**
     * Sets the request controls; they are not inherited by derived contexts. Always throws here.
     */
    public void setRequestControls(Control[] requestControls) throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /** The request controls. Always throws here. */
    public Control[] getRequestControls() throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }

    /** The controls the server sent with the last operation. Always throws here. */
    public Control[] getResponseControls() throws NamingException {
        throw new NamingException(NO_PROVIDER);
    }
}
