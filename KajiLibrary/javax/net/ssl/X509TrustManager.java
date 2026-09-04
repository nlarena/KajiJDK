package javax.net.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Decide si una cadena de certificados X.509 es de fiar.
 *
 * <h2>Por que los metodos no devuelven un booleano</h2>
 *
 * <p>Porque una excepcion puede decir <em>por que</em> no, y un {@code false} no. Rechazar por
 * certificado vencido, por firma invalida o por emisor desconocido son tres situaciones distintas,
 * y quien llama —o quien lee un log— necesita distinguirlas. Devolver normalmente es aceptar.
 *
 * <p>La asimetria entre los dos metodos es real y no decorativa: al servidor se lo valida contra su
 * nombre, y al cliente contra la lista de emisores aceptados. No es la misma pregunta con los roles
 * cambiados.
 */
public interface X509TrustManager extends TrustManager {

    /**
     * @throws CertificateException si el cliente no es de fiar, con el motivo adentro
     */
    void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException;

    /**
     * @throws CertificateException si el servidor no es de fiar
     */
    void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException;

    /**
     * Los emisores que este manejador acepta.
     *
     * <p>No es solo informativo: es lo que el servidor le manda al cliente para decirle que
     * certificados le sirven. Sin eso el cliente tendria que adivinar cual de los suyos presentar.
     */
    X509Certificate[] getAcceptedIssuers();
}
