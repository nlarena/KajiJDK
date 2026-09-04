package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.URIReferenceException -- no se pudo resolver una referencia.
 *
 * <p>La lanza un {@link URIDereferencer}. Es la unica excepcion del paquete que lleva un dato propio:
 * la {@link URIReference} que fallo.
 *
 * <p>Ese dato hace falta de verdad. Una firma tiene varias referencias y se resuelven en un bucle; sin
 * saber cual fallo, el mensaje diria "no se pudo resolver" sobre una firma con diez referencias y no
 * habria por donde empezar.
 *
 * <p>Redefine {@code getCause} y los {@code printStackTrace} por lo mismo que las otras tres del
 * paquete: en el JDK la causa vive en un campo propio, y aca delega en {@code Throwable}.
 */
public class URIReferenceException extends Exception {

    private static final long serialVersionUID = 7173469703932561419L;

    /** La que fallo, o null. */
    private URIReference uriReference;

    /** Sin detalle. */
    public URIReferenceException() {
        super();
    }

    /** Con un mensaje. */
    public URIReferenceException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public URIReferenceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Con la referencia que fallo.
     *
     * @param uriReference cual era; ver la nota de la clase
     */
    public URIReferenceException(String message, Throwable cause, URIReference uriReference) {
        super(message, cause);
        this.uriReference = uriReference;
    }

    /** Solo con la causa. */
    public URIReferenceException(Throwable cause) {
        super(cause);
    }

    /** La referencia que fallo, o null si no se dijo. */
    public URIReference getURIReference() {
        return this.uriReference;
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
