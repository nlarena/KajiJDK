package javax.rmi.ssl;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.StringTokenizer;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * La fabrica que hace que el lado cliente de una llamada RMI viaje por SSL.
 *
 * <p>Un talon RMI lleva adentro la fabrica de sockets con la que se lo tiene que contactar, y la
 * fabrica viaja **serializada** desde el servidor hasta el cliente. De ahi las dos rarezas de esta
 * clase, que de otro modo no se entienden:
 *
 * <ul>
 *   <li>No tiene estado. Todo lo que configura --las suites y los protocolos habilitados-- sale de
 *       propiedades de sistema que se leen **en el cliente**, no de campos que viajarian con el
 *       objeto. Es deliberado: la configuracion SSL del cliente la elige el cliente.
 *   <li>{@link #equals} compara la **clase** y no el contenido. Dos instancias sin estado son
 *       intercambiables, y RMI usa esa igualdad para reutilizar una sola conexion con varios
 *       talones del mismo servidor; si comparara por identidad, cada talon abriria la suya.
 * </ul>
 *
 * <p>Las dos propiedades que lee, separadas por comas:
 * {@code javax.rmi.ssl.client.enabledCipherSuites} y
 * {@code javax.rmi.ssl.client.enabledProtocols}. Si no estan, el socket queda como lo dejo su
 * fabrica.
 */
public class SslRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private static SocketFactory defaultSocketFactory = null;

    private static final long serialVersionUID = -8310631444933958385L;

    /** Una fabrica nueva. */
    public SslRMIClientSocketFactory() {
    }

    /**
     * Un socket SSL conectado a esa maquina y puerto.
     *
     * @throws IOException si no se puede conectar, o si alguna de las dos propiedades nombra una
     *     suite o un protocolo que el socket no soporta
     */
    public Socket createSocket(String host, int port) throws IOException {
        SocketFactory fabrica = getDefaultClientSocketFactory();
        SSLSocket socket = (SSLSocket) fabrica.createSocket(host, port);

        String[] suites = leerLista("javax.rmi.ssl.client.enabledCipherSuites");
        if (suites != null) {
            try {
                socket.setEnabledCipherSuites(suites);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        String[] protocolos = leerLista("javax.rmi.ssl.client.enabledProtocols");
        if (protocolos != null) {
            try {
                socket.setEnabledProtocols(protocolos);
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return socket;
    }

    /** La propiedad, partida por comas, o `null` si no esta. */
    private static String[] leerLista(String propiedad) {
        String valor = System.getProperty(propiedad);
        if (valor == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(valor, ",");
        int n = st.countTokens();
        String[] out = new String[n];
        for (int i = 0; i < n; i++) {
            out[i] = st.nextToken();
        }
        return out;
    }

    /**
     * Dos fabricas de esta clase son iguales.
     *
     * <p>Compara la clase exacta y no `instanceof`, para que una subclase que si tenga estado no
     * salga igual a su base.
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return obj.getClass().equals(this.getClass());
    }

    /** Coherente con {@link #equals}: depende solo de la clase. */
    public int hashCode() {
        return this.getClass().hashCode();
    }

    /**
     * La fabrica SSL de siempre, memorizada.
     *
     * <p>Se memoriza porque `SSLSocketFactory.getDefault()` puede tener que armar un contexto
     * entero, y esto se llama una vez por conexion.
     */
    private static synchronized SocketFactory getDefaultClientSocketFactory() {
        if (defaultSocketFactory == null) {
            defaultSocketFactory = SSLSocketFactory.getDefault();
        }
        return defaultSocketFactory;
    }
}
