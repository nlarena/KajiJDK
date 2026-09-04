package java.rmi;

/**
 * KajiLibrary's java.rmi.StubNotFoundException -- Falta la clase talon.
 *
 * <p>No se encontro la clase talon --el intermediario que traduce una llamada local en una remota--
 * al exportar un objeto o al recibir una referencia.
 *
 * <p>Es una excepcion de otra epoca. Desde 1.5 los talones se generan solos con
 * {@code java.lang.reflect.Proxy}, asi que en la practica solo aparece con codigo compilado con
 * {@code rmic}.
 */
public class StubNotFoundException extends RemoteException {

    private static final long serialVersionUID = -7088199405468872373L;

    /** @param s el mensaje */
    public StubNotFoundException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public StubNotFoundException(String s, Exception ex) {
        super(s, ex);
    }
}
