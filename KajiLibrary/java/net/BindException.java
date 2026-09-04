package java.net;

// Un BindException: no se pudo asignar la direccion local pedida (el puerto ya esta tomado, o la direccion no es de esta maquina).
//
// Subclase de `SocketException` por la misma razon que la madre existe aca (ver `SocketException`):
// nombra un modo de falla, no promete poder producirlo. Quien atrapa `SocketException` la atrapa
// tambien, que es de lo que se trata la jerarquia.
public class BindException extends SocketException {

    private static final long serialVersionUID = -5945005768251722951L;

    public BindException(String msg) {
        super(msg);
    }

    public BindException() {
    }
}
