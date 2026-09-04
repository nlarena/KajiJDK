package java.rmi;

/**
 * KajiLibrary's java.rmi.MarshalException -- Fallo al escribir los argumentos.
 *
 * <p>Es del lado que <b>envia</b>: no se pudieron serializar los argumentos de la llamada, o el
 * identificador del metodo. Su espejo es {@link UnmarshalException}, del lado que recibe.
 *
 * <p>Tiene una consecuencia que conviene tener presente: como la llamada nunca salio completa, el
 * metodo remoto <b>no se ejecuto</b>. Es de las pocas excepciones de RMI de las que se puede afirmar
 * eso.
 */
public class MarshalException extends RemoteException {

    private static final long serialVersionUID = 6223554758134037936L;

    /** @param s el mensaje */
    public MarshalException(String s) {
        super(s);
    }

    /**
     * @param s el mensaje
     * @param ex la causa
     */
    public MarshalException(String s, Exception ex) {
        super(s, ex);
    }
}
