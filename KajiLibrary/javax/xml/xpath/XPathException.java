package javax.xml.xpath;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.xpath.XPathException -- la raiz de los errores de XPath.
 *
 * <p>Dos constructores y ninguno acepta null: uno pide mensaje y el otro pide causa, y el que reciba
 * null tira {@link NullPointerException} en el momento. Es mas estricto que lo habitual y esta bien
 * que lo sea -- una excepcion sin mensaje ni causa no le sirve a nadie, y el costo de descubrirlo es
 * un rastro de pila que no dice nada.
 *
 * <p>El constructor con causa deja el mensaje en el {@code toString} de la causa. Por eso no hay un
 * constructor con los dos: si se tiene la causa, el mensaje sale de ahi, y si se quiere uno propio,
 * la via es el de mensaje mas {@code initCause}.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} porque en el JDK guarda su causa en
 * un campo propio, de cuando {@code Throwable} todavia no las tenia. Aca la causa es la de
 * {@code Throwable} y las redefiniciones delegan: el comportamiento observable es el mismo y no hay
 * dos copias del mismo dato que puedan discrepar.
 */
public class XPathException extends Exception {

    private static final long serialVersionUID = -1837080260374986980L;

    /**
     * Con un mensaje.
     *
     * @throws NullPointerException si es null; ver la nota de la clase
     */
    public XPathException(String message) {
        super(message);
        if (message == null) {
            throw new NullPointerException("message can't be null");
        }
    }

    /**
     * Con una causa; el mensaje sale del {@code toString} de ella.
     *
     * @throws NullPointerException si es null
     */
    public XPathException(Throwable cause) {
        super(cause);
        if (cause == null) {
            throw new NullPointerException("cause can't be null");
        }
    }

    /** La causa, o null si se construyo con mensaje y nadie la fijo. */
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
