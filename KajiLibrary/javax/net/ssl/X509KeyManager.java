package javax.net.ssl;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Elige que certificado propio presentar, entre los que haya.
 *
 * <h2>Los alias, y por que hay dos niveles</h2>
 *
 * <p>{@link #getClientAliases} y {@link #getServerAliases} <em>enumeran</em> los candidatos;
 * {@link #chooseClientAlias} y {@link #chooseServerAlias} <em>eligen</em> uno. Estan separados
 * porque enumerar es una consulta sin contexto y elegir depende de con quien se esta hablando — de
 * ahi que los segundos reciban el {@link Socket}.
 *
 * <p>El parametro {@code issuers} es la lista de emisores que el par declaro aceptar. Presentar un
 * certificado firmado por alguien que no esta en esa lista es garantia de rechazo.
 */
public interface X509KeyManager extends KeyManager {

    /** Los alias de cliente utilizables para ese tipo de clave y esos emisores. */
    String[] getClientAliases(String keyType, Principal[] issuers);

    /** Elige el alias de cliente, o {@code null} si ninguno sirve. */
    String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket);

    /** Los alias de servidor utilizables. */
    String[] getServerAliases(String keyType, Principal[] issuers);

    /** Elige el alias de servidor, o {@code null}. */
    String chooseServerAlias(String keyType, Principal[] issuers, Socket socket);

    /** La cadena de certificados de ese alias. */
    X509Certificate[] getCertificateChain(String alias);

    /** La clave privada de ese alias. */
    PrivateKey getPrivateKey(String alias);
}
