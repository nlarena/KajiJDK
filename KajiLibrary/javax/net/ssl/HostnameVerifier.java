package javax.net.ssl;

/**
 * La ultima palabra sobre si el nombre del servidor corresponde a su certificado.
 *
 * <h2>Por que esto existe y es un punto de extension</h2>
 *
 * <p>Que un certificado sea valido y este firmado por alguien de confianza <strong>no dice que sea
 * de quien nos conectamos</strong>: un certificado legitimo de otro sitio pasa todas las
 * verificaciones criptograficas. Comparar el nombre pedido contra el del certificado es un paso
 * aparte, y es el que frena a un atacante que consiguio un certificado valido de cualquier otro
 * dominio.
 *
 * <p>Se consulta <em>solo cuando la verificacion estandar ya fallo</em>. Devolver {@code true}
 * desde aca anula esa proteccion, que es la razon de que sea tan facil desactivar la seguridad de
 * TLS sin darse cuenta.
 */
public interface HostnameVerifier {

    /**
     * @return {@code true} para aceptar la conexion pese a que el nombre no coincidio
     */
    boolean verify(String hostname, SSLSession session);
}
