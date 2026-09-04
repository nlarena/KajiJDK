package java.rmi;

/**
 * KajiLibrary's java.rmi.AlreadyBoundException -- ese nombre ya esta ocupado en el registro.
 *
 * <p>Sale de {@code bind}, que se niega a pisar. {@code rebind} es el que si pisa, y por eso no lanza
 * esta.
 *
 * <p>Que sean dos operaciones distintas es a proposito: arrancar dos veces el mismo servidor por error
 * es facil, y con {@code bind} el segundo falla en lugar de robarle los clientes al primero en
 * silencio.
 *
 * <p>No hereda de {@link RemoteException}: no es un problema de la red sino del contenido del
 * registro, y la llamada llego perfectamente.
 */
public class AlreadyBoundException extends Exception {

    private static final long serialVersionUID = 9218657361741657110L;

    /** Sin detalle. */
    public AlreadyBoundException() {
        super();
    }

    /** @param s el nombre que ya estaba */
    public AlreadyBoundException(String s) {
        super(s);
    }
}
