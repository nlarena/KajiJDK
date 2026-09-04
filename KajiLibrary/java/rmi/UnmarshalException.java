package java.rmi;

/**
 * KajiLibrary's java.rmi.UnmarshalException -- Fallo al leer lo que llego.
 *
 * <p>El espejo de {@link MarshalException}: es del lado que <b>recibe</b>. No se pudo deserializar
 * --tipicamente porque falta una clase, o porque la version no coincide--.
 *
 * <p>A diferencia de aquella, aca <b>no se sabe</b> si el metodo remoto se ejecuto: si el fallo fue al
 * leer el resultado, ya corrio.
 */
public class UnmarshalException extends RemoteException {

    private static final long serialVersionUID = 594380845140740218L;

    /** @param s el mensaje */
    public UnmarshalException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public UnmarshalException(String s, Exception ex) {
        super(s, ex);
    }
}
