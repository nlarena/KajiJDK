package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * La operacion extendida que convierte una conexion LDAP en claro en una cifrada.
 *
 * <h2>StartTLS contra LDAPS</h2>
 *
 * <p>Hay dos formas de cifrar LDAP y no son la misma. <strong>LDAPS</strong> abre TLS desde el
 * primer byte, en un puerto propio. <strong>StartTLS</strong> —esto— empieza en claro en el puerto
 * de siempre y negocia el cambio a mitad de camino.
 *
 * <p>La ventaja de StartTLS es que usa un solo puerto y deja que el cliente decida; la desventaja es
 * exactamente eso, y hay que tenerla presente: un atacante en el medio puede <em>quitar</em> el
 * anuncio de que StartTLS esta disponible, y un cliente que solo cifra "si el servidor lo ofrece"
 * termina hablando en claro sin enterarse. Por eso la decision de exigirlo tiene que ser del
 * cliente y no depender de lo que el servidor diga.
 *
 * <p>Que no lleve valor —{@link #getEncodedValue} devuelve {@code null}— es correcto: el pedido es
 * solo el OID.
 */
public class StartTlsRequest implements ExtendedRequest {

    private static final long serialVersionUID = 4441679576360753397L;

    /** El OID de la operacion, del RFC 2830. */
    public static final String OID = "1.3.6.1.4.1.1466.20037";

    public StartTlsRequest() {
    }

    public String getID() {
        return OID;
    }

    /** {@code null}: este pedido no lleva datos. */
    public byte[] getEncodedValue() {
        return null;
    }

    /**
     * Busca una implementacion de {@link StartTlsResponse} por {@link java.util.ServiceLoader}.
     *
     * <p>No la construye directamente porque negociar TLS depende del proveedor: cada uno sabe
     * envolver <em>su</em> socket. En esta VM no hay ninguno registrado, asi que declina — el
     * mecanismo esta y lo que falta es quien se registre.
     *
     * @throws NamingException si no hay implementacion
     */
    public ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset,
            int length) throws NamingException {
        if (id != null && !id.equals(OID)) {
            throw new NamingException("la respuesta no es de StartTLS: " + id);
        }
        java.util.Iterator<StartTlsResponse> it =
                java.util.ServiceLoader.load(StartTlsResponse.class).iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw new NamingException(
                "no hay ninguna implementacion de StartTlsResponse registrada");
    }
}
