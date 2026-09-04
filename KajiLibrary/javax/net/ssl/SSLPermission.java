package javax.net.ssl;

import java.security.BasicPermission;

/**
 * El permiso sobre operaciones sensibles de SSL.
 *
 * <p>Los dos nombres definidos son {@code "setHostnameVerifier"} y {@code "getSSLSessionContext"}, y
 * el primero explica por que esto existe: quien puede cambiar el verificador de nombres puede
 * hacerlo aceptar cualquier certificado, o sea desactivar la mitad de la proteccion de TLS sin que
 * nada mas cambie.
 */
public final class SSLPermission extends BasicPermission {

    private static final long serialVersionUID = -3456898025505876775L;

    /** Un permiso con ese nombre. */
    public SSLPermission(String name) {
        super(name);
    }

    /** Igual; {@code actions} se ignora, y el JDK hace lo mismo. */
    public SSLPermission(String name, String actions) {
        super(name, actions);
    }
}
