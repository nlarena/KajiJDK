package javax.naming.ldap.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.naming.ldap.spi.LdapDnsProviderResult -- which LDAP servers to go to.
 *
 * <p>What an {@link LdapDnsProvider} returns: the domain it resolved and the list of endpoints, in
 * the form {@code ldap://host:port}.
 *
 * <p>The <b>list</b> is the point of the class. An LDAP domain is not one server but several, and
 * the order matters: JNDI tries them in that order and keeps the first that answers. A provider
 * that orders them by proximity or load is doing balancing, and this class is how it says so.
 *
 * <p>It is immutable: the list is copied on construction and {@link #getEndpoints} returns it
 * read-only.
 */
public final class LdapDnsProviderResult {

    /** The domain that was resolved. */
    private final String domainName;

    /** The servers, in order of preference. */
    private final List<String> endpoints;

    /**
     * @param domainName the resolved domain; kept as given, even null (the JDK turns null into
     *     {@code ""})
     * @param endpoints the servers, in order of preference; copied, null elements included (the
     *     JDK's {@code List.copyOf} rejects them with {@code NullPointerException})
     * @throws NullPointerException if the list is null
     */
    public LdapDnsProviderResult(String domainName, List<String> endpoints) {
        this.domainName = domainName;
        this.endpoints = Collections.unmodifiableList(new ArrayList<String>(endpoints));
    }

    /** The resolved domain. */
    public String getDomainName() {
        return this.domainName;
    }

    /** The servers, in order and read-only. See the class note. */
    public List<String> getEndpoints() {
        return this.endpoints;
    }
}
