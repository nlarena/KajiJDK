package javax.naming.ldap.spi;

import javax.naming.NamingException;
import java.util.Map;
import java.util.Optional;

/**
 * KajiLibrary's javax.naming.ldap.spi.LdapDnsProvider -- decides which LDAP server to connect to.
 *
 * <p>It is registered as a service and, in the JDK, JNDI consults it before connecting. It exists
 * to replace the default resolution --which queries DNS SRV records-- with something else: a
 * configuration table, service discovery, a balancer of your own. This library ships no LDAP
 * provider, so nothing here consults it.
 *
 * <h2>An empty {@link Optional} is not an error</h2>
 *
 * <p>It is the part to understand. Returning {@code Optional.empty()} means "I cannot resolve this
 * URL", and JNDI carries on with the next provider. Throwing {@link NamingException} means "I know
 * what this is about and something went wrong", and it cuts the lookup short.
 *
 * <p>Mixing them up makes a provider specialized in one domain block all the others.
 *
 * <p>The environment map is JNDI's --the same keys as {@code InitialContext}-- and arrives as is. A
 * provider can look there at the user or the security level to decide where to send the client.
 */
public abstract class LdapDnsProvider {

    /** For the subclasses. */
    protected LdapDnsProvider() {
    }

    /**
     * Which servers to go to for that URL.
     *
     * @param url the LDAP URL to resolve
     * @param env the JNDI environment
     * @return the servers, or empty if this provider cannot resolve it
     * @throws NamingException if it knows what this is about and failed; see the class note
     */
    public abstract Optional<LdapDnsProviderResult> lookupEndpoints(String url, Map<?, ?> env)
        throws NamingException;
}
