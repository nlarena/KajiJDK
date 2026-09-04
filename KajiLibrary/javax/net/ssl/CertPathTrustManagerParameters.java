package javax.net.ssl;

import java.security.cert.CertPathParameters;

/**
 * Envuelve unos {@link CertPathParameters} para pasarselos a una {@link TrustManagerFactory}.
 *
 * <p>Es un adaptador y nada mas, y esa modestia es el punto: la validacion de una cadena de
 * certificados ya esta especificada en {@code java.security.cert}, con sus revocaciones, sus anclas
 * y sus politicas. Esta clase no repite nada de eso — solo hace que esa configuracion entre por
 * donde {@link TrustManagerFactory#init(ManagerFactoryParameters)} la espera.
 */
public class CertPathTrustManagerParameters implements ManagerFactoryParameters {

    private final CertPathParameters parameters;

    /**
     * Se guarda una <strong>copia</strong>, no la referencia: {@link CertPathParameters} es mutable,
     * y una politica de confianza que alguien puede cambiar despues de haberla entregado no es una
     * politica.
     *
     * @throws NullPointerException si {@code parameters} es {@code null}
     */
    public CertPathTrustManagerParameters(CertPathParameters parameters) {
        this.parameters = (CertPathParameters) parameters.clone();
    }

    /** Una copia de los parametros, por la misma razon. */
    public CertPathParameters getParameters() {
        return (CertPathParameters) this.parameters.clone();
    }
}
