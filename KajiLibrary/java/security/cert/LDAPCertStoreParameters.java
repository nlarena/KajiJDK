package java.security.cert;

// Los parametros de un `CertStore` que consulta un directorio LDAP.
//
// Sobrevive por compatibilidad: la idea de publicar certificados en un LDAP corporativo quedo atras
// y hoy el mecanismo real es la extension AIA del propio certificado, que dice por HTTP donde esta
// el emisor. Se implementa igual porque es una clase de datos y su ausencia romperia codigo viejo
// que la nombra sin llegar a usarla.
//
// **No abre ninguna conexion**: es solo el par (servidor, puerto). Quien conecta es el proveedor de
// `CertStore`, y esta biblioteca no trae ninguno que lo haga.
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

    // Con el puerto estandar de LDAP.
    public LDAPCertStoreParameters(String serverName) {
        this(serverName, PUERTO_LDAP);
    }

    // Localhost en el puerto estandar.
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
