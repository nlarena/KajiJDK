package java.security.cert;

// The parameters of a `CertStore` that queries an LDAP directory.
//
// It survives out of compatibility: the idea of publishing certificates in a corporate LDAP is
// behind us and today the real mechanism is the AIA extension of the certificate itself, which says
// over HTTP where the issuer is. It is implemented all the same because it is a data class and its
// absence would break old code that names it without getting as far as using it.
//
// **It opens no connection**: it is only the pair (server, port). The one that connects is the
// provider of `CertStore`, and this library brings none that does.
public class LDAPCertStoreParameters implements CertStoreParameters {

    private static final int PUERTO_LDAP = 389;

    private final String serverName;
    private final int port;

    public LDAPCertStoreParameters(String serverName, int port) {
        if (serverName == null) {
            throw new NullPointerException();
        }
        this.serverName = serverName;
        this.port = port;
    }

    // With the standard port of LDAP.
    public LDAPCertStoreParameters(String serverName) {
        this(serverName, PUERTO_LDAP);
    }

    // Localhost on the standard port.
    public LDAPCertStoreParameters() {
        this("localhost", PUERTO_LDAP);
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LDAPCertStoreParameters: [\n");
        sb.append("  serverName: " + this.serverName + "\n");
        sb.append("  port: " + this.port + "\n");
        sb.append("]");
        return sb.toString();
    }
}
