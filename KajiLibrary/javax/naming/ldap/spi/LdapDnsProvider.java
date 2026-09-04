package javax.naming.ldap.spi;

import javax.naming.NamingException;
import java.util.Map;
import java.util.Optional;

/**
 * KajiLibrary's javax.naming.ldap.spi.LdapDnsProvider -- decide a que servidor LDAP conectarse.
 *
 * <p>Se registra como servicio y JNDI lo consulta antes de conectar. Existe para poder reemplazar la
 * resolucion por omision --que consulta registros SRV del DNS-- por otra cosa: una tabla de
 * configuracion, un descubrimiento de servicios, un balanceador propio.
 *
 * <h2>{@link Optional} vacio no es un error</h2>
 *
 * <p>Es la parte que hay que entender. Devolver {@code Optional.empty()} significa "yo no se resolver
 * esta URL", y JNDI sigue con el proximo proveedor. Lanzar {@link NamingException} significa "se de
 * que se trata y algo salio mal", y corta la busqueda.
 *
 * <p>Confundirlos hace que un proveedor especializado en un dominio bloquee a todos los demas.
 *
 * <p>El mapa de entorno es el de JNDI --las mismas claves que {@code InitialContext}-- y llega tal
 * cual. Un proveedor puede mirar ahi el usuario o el nivel de seguridad para decidir a donde mandar.
 */
public abstract class LdapDnsProvider {

    /** Para las subclases. */
    protected LdapDnsProvider() {
    }

    /**
     * A que servidores ir para esa URL.
     *
     * @param url la URL LDAP que se quiere resolver
     * @param env el entorno de JNDI
     * @return los servidores, o vacio si este proveedor no sabe resolverla
     * @throws NamingException si sabe de que se trata y fallo; ver la nota de la clase
     */
    public abstract Optional<LdapDnsProviderResult> lookupEndpoints(String url, Map<?, ?> env)
        throws NamingException;
}
