package java.net;

// Un ConnectException: el otro extremo rechazo la conexion o no contesto a tiempo.
//
// Subclase de `SocketException` por la misma razon que la madre existe aca (ver `SocketException`):
// nombra un modo de falla, no promete poder producirlo. Quien atrapa `SocketException` la atrapa
// tambien, que es de lo que se trata la jerarquia.
public class ConnectException extends SocketException {

    private static final long serialVersionUID = 3767514772251481192L;

    public ConnectException(String msg) {
        super(msg);
    }

    public ConnectException() {
    }
}
