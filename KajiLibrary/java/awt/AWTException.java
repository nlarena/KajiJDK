package java.awt;

/**
 * Algo del AWT salio mal de una forma que quien llama tiene que atender: es una excepcion
 * verificada, no un error de programacion.
 */
public class AWTException extends Exception {

    private static final long serialVersionUID = -1900414231151323879L;

    public AWTException(String msg) {
        super(msg);
    }
}
