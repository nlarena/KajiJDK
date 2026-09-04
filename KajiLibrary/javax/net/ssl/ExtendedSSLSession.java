package javax.net.ssl;

import java.util.Collections;
import java.util.List;

/**
 * Una {@link SSLSession} que ademas informa lo que TLS moderno negocia y la interfaz original no
 * preveia.
 *
 * <p>Es una clase abstracta y no metodos nuevos en la interfaz por la razon de siempre: agregarlos
 * a {@code SSLSession} habria roto a quien la implementara. Los que tienen cuerpo aca devuelven lo
 * vacio o tiran {@link UnsupportedOperationException} segun si "nada" es una respuesta sensata.
 */
public abstract class ExtendedSSLSession implements SSLSession {

    public ExtendedSSLSession() {
    }

    /** Los algoritmos de firma que esta punta acepta, en orden de preferencia. */
    public abstract String[] getLocalSupportedSignatureAlgorithms();

    /** Los que declaro el par, o {@code null} si no los declaro. */
    public abstract String[] getPeerSupportedSignatureAlgorithms();

    /**
     * Los nombres SNI que pidio el cliente.
     *
     * <p>Vacia por omision, que es lo correcto: no haber pedido ninguno es normal y no un error.
     *
     * @throws UnsupportedOperationException si la implementacion no lo soporta
     */
    public List<SNIServerName> getRequestedServerNames() {
        throw new UnsupportedOperationException("esta sesion no informa los nombres SNI pedidos");
    }

    /**
     * Las respuestas OCSP grapadas al handshake.
     *
     * <p>Vacia por omision. Grapar la respuesta de revocacion al handshake evita que el cliente
     * tenga que consultarla por su cuenta — otra conexion, otro punto de falla, y una filtracion de
     * a quien se conecta.
     */
    public List<byte[]> getStatusResponses() {
        return Collections.<byte[]>emptyList();
    }

    /**
     * Deriva una clave a partir del secreto de la sesion.
     *
     * @throws UnsupportedOperationException si la implementacion no lo soporta
     */
    public javax.crypto.SecretKey exportKeyingMaterialKey(String keyAlg, String label,
            byte[] context, int length) throws SSLKeyException {
        throw new UnsupportedOperationException("esta sesion no exporta material de claves");
    }

    /**
     * Lo mismo, como bytes crudos.
     *
     * @throws UnsupportedOperationException si la implementacion no lo soporta
     */
    public byte[] exportKeyingMaterialData(String label, byte[] context, int length)
            throws SSLKeyException {
        throw new UnsupportedOperationException("esta sesion no exporta material de claves");
    }
}
