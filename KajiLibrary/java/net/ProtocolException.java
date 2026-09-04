package java.net;

import java.io.IOException;

// El otro extremo hablo el protocolo mal: la conexion esta viva, lo que llego no tiene sentido.
//
// No hereda de `SocketException` a proposito, y no es un descuido del JDK: `SocketException` es
// "el socket fallo" y esto es "el socket anduvo y el mensaje estaba roto". Son dos capas distintas,
// y quien atrapa una casi nunca quiere la otra.
public class ProtocolException extends IOException {

    private static final long serialVersionUID = 8207694371842273524L;

    public ProtocolException(String host) {
        super(host);
    }

    public ProtocolException() {
    }
}
