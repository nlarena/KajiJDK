package javax.naming.ldap.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.naming.ldap.spi.LdapDnsProviderResult -- a que servidores LDAP ir.
 *
 * <p>Lo que devuelve un {@link LdapDnsProvider}: el dominio que resolvio y la lista de puntos finales,
 * en la forma {@code ldap://maquina:puerto}.
 *
 * <p>La <b>lista</b> es el punto de la clase. Un dominio LDAP no es un servidor sino varios, y el
 * orden importa: JNDI los prueba en ese orden y se queda con el primero que responda. Un proveedor que
 * los ordene por cercania o por carga esta haciendo balanceo, y esta clase es como lo comunica.
 *
 * <p>Es inmutable: la lista se copia al construir y {@link #getEndpoints} la devuelve de solo lectura.
 */
public final class LdapDnsProviderResult {

    /** El dominio que se resolvio. */
    private final String domainName;

    /** Los servidores, en orden de preferencia. */
    private final List<String> endpoints;

    /**
     * @param domainName el dominio resuelto
     * @param endpoints los servidores, en orden de preferencia; se copia
     * @throws NullPointerException si la lista es null
     */
    public LdapDnsProviderResult(String domainName, List<String> endpoints) {
        this.domainName = domainName;
        this.endpoints = Collections.unmodifiableList(new ArrayList<String>(endpoints));
    }

    /** El dominio resuelto. */
    public String getDomainName() {
        return this.domainName;
    }

    /** Los servidores, en orden y de solo lectura. Ver la nota de la clase. */
    public List<String> getEndpoints() {
        return this.endpoints;
    }
}
