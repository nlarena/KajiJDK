package javax.net.ssl;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Un {@link X509TrustManager} que ademas ve <strong>con quien</strong> se esta hablando.
 *
 * <h2>Por que eso cambia todo</h2>
 *
 * <p>Los metodos de {@link X509TrustManager} reciben la cadena y el tipo de autenticacion, y nada
 * mas. Con eso se puede verificar que el certificado sea valido y este firmado por alguien de
 * confianza — pero <strong>no</strong> que sea de quien nos conectamos, porque ese dato no llega.
 *
 * <p>Estos cuatro reciben el {@link Socket} o el {@link SSLEngine}, o sea el nombre que se pidio.
 * Es lo que permite la verificacion de identidad del extremo, la que frena a un atacante con un
 * certificado legitimo de otro dominio. Un manejador que implemente solo la interfaz vieja deja ese
 * agujero abierto, y por eso el JDK usa esta clase siempre que puede.
 */
public abstract class X509ExtendedTrustManager implements X509TrustManager {

    /** Para las subclases. */
    public X509ExtendedTrustManager() {
    }

    /**
     * @throws CertificateException si el cliente no es de fiar
     */
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException;

    /**
     * @throws CertificateException si el servidor no es de fiar, incluida la falla de identidad
     */
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType,
            Socket socket) throws CertificateException;

    /**
     * @throws CertificateException si el cliente no es de fiar
     */
    public abstract void checkClientTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException;

    /**
     * @throws CertificateException si el servidor no es de fiar
     */
    public abstract void checkServerTrusted(X509Certificate[] chain, String authType,
            SSLEngine engine) throws CertificateException;
}
