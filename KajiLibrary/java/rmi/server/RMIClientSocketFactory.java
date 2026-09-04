package java.rmi.server;

import java.io.IOException;
import java.net.Socket;

/**
 * Como el cliente abre la conexion hacia un objeto remoto.
 *
 * <h2>Por que esto viaja con el objeto</h2>
 *
 * <p>Es la parte que sorprende de RMI: cuando un objeto remoto se exporta con una fabrica propia,
 * <strong>la fabrica se serializa junto con el stub</strong> y llega al cliente. Ahi corre, y es lo
 * que decide como se abre el socket.
 *
 * <p>Eso es lo que permite que un objeto exija TLS sin que el cliente configure nada — ver
 * {@code javax.rmi.ssl.SslRMIClientSocketFactory}. Y es tambien por que tiene que implementar
 * {@code equals} y {@code hashCode}: RMI las usa para reusar conexiones, y dos fabricas
 * equivalentes que no se declaren iguales abren un socket cada una.
 */
public interface RMIClientSocketFactory {

    /** Abre una conexion al servidor. */
    Socket createSocket(String host, int port) throws IOException;
}
