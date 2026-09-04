package javax.xml.crypto.dsig;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.dsig.XMLSignatureException -- no se pudo firmar o validar.
 *
 * <p>La generica de la operacion. Cubre desde una clave que no sirve para el algoritmo hasta
 * un error del proveedor.
 *
 * <p>Lo que <b>no</b> significa es que la firma sea invalida: eso lo dice
 * {@link XMLSignature#validate} devolviendo false. Confundir las dos cosas --tratar la
 * excepcion como un rechazo de la firma-- esconde errores de configuracion, y al reves es peor.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} por lo mismo que las de
 * {@code javax.xml.crypto}: en el JDK la causa vive en un campo propio, de antes de que
 * {@code Throwable} las tuviera, y aca delega.
 */
public class XMLSignatureException extends Exception {

    private static final long serialVersionUID = -9077726597114319058L;

    /** Sin detalle. */
    public XMLSignatureException() {
        super();
    }

    /** Con un mensaje. */
    public XMLSignatureException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public XMLSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Solo con la causa. */
    public XMLSignatureException(Throwable cause) {
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
