package java.net;

// Un NoRouteToHostException: no hay camino hacia el host (tipicamente un firewall que descarta en silencio, o una red caida).
//
// Subclase de `SocketException` por la misma razon que la madre existe aca (ver `SocketException`):
// nombra un modo de falla, no promete poder producirlo. Quien atrapa `SocketException` la atrapa
// tambien, que es de lo que se trata la jerarquia.
public class NoRouteToHostException extends SocketException {

    private static final long serialVersionUID = -1897550894873493790L;

    public NoRouteToHostException(String msg) {
        super(msg);
    }

    public NoRouteToHostException() {
    }
}
