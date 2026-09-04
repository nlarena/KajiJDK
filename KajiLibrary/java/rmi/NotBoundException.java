package java.rmi;

/**
 * KajiLibrary's java.rmi.NotBoundException -- ese nombre no esta en el registro.
 *
 * <p>El espejo de {@link AlreadyBoundException}: sale de {@code lookup} y de {@code unbind}.
 *
 * <p>Tampoco hereda de {@link RemoteException}, por lo mismo: el registro contesto bien, lo que pasa
 * es que no tiene nada con ese nombre. Distinguirlas importa -- un cliente que ataja esta puede
 * esperar y reintentar; uno que ataja una {@code RemoteException} tiene un problema de red.
 */
public class NotBoundException extends Exception {

    private static final long serialVersionUID = -1857741824849069317L;

    /** Sin detalle. */
    public NotBoundException() {
        super();
    }

    /** @param s el nombre que no estaba */
    public NotBoundException(String s) {
        super(s);
    }
}
