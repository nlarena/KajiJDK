package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.KeySelectorException -- el selector no pudo elegir una clave.
 *
 * <p>Es comprobada porque no poder elegir una clave es un resultado <b>esperable</b> de
 * validar una firma ajena: el {@code KeyInfo} nombra una clave que no se conoce, o no
 * nombra ninguna. Quien valida tiene que decidir que hacer, y el compilador lo obliga a
 * mirarlo.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} porque en el JDK guarda su causa
 * en un campo propio, de cuando {@code Throwable} todavia no las tenia. Aca la causa es la de
 * {@code Throwable} y las redefiniciones delegan: mismo comportamiento, sin dos copias del dato.
 */
public class KeySelectorException extends Exception {

    private static final long serialVersionUID = -7155660112864185370L;

    /** Sin detalle. */
    public KeySelectorException() {
        super();
    }

    /** Con un mensaje. */
    public KeySelectorException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public KeySelectorException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Solo con la causa; el mensaje sale de su {@code toString}. */
    public KeySelectorException(Throwable cause) {
        super(cause);
    }

    /** La causa, o null. */
    public Throwable getCause() {
        return super.getCause();
    }

    /** A la salida de error. */
    public void printStackTrace() {
        super.printStackTrace();
    }

    /** A ese flujo. */
    public void printStackTrace(PrintStream s) {
        super.printStackTrace(s);
    }

    /** A ese escritor. */
    public void printStackTrace(PrintWriter s) {
        super.printStackTrace(s);
    }
}
