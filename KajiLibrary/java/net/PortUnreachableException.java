package java.net;

// Un PortUnreachableException: llego un ICMP port-unreachable en un socket UDP conectado: del otro lado no habia nadie escuchando.
//
// Subclase de `SocketException` por la misma razon que la madre existe aca (ver `SocketException`):
// nombra un modo de falla, no promete poder producirlo. Quien atrapa `SocketException` la atrapa
// tambien, que es de lo que se trata la jerarquia.
public class PortUnreachableException extends SocketException {

    private static final long serialVersionUID = 8462541992376507323L;

    public PortUnreachableException(String msg) {
        super(msg);
    }

    public PortUnreachableException() {
    }
}
