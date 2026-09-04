package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.MarshalException -- no se pudo escribir o leer una estructura como XML.
 *
 * <p>Sale al convertir entre el modelo de objetos y el documento, en cualquiera de las dos
 * direcciones. Es distinta de un fallo de firma: aca el problema es la <b>forma</b> del
 * XML, no la criptografia. Confundirlas manda a buscar un problema de claves donde lo
 * que hay es un elemento mal ubicado.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} porque en el JDK guarda su causa
 * en un campo propio, de cuando {@code Throwable} todavia no las tenia. Aca la causa es la de
 * {@code Throwable} y las redefiniciones delegan: mismo comportamiento, sin dos copias del dato.
 */
public class MarshalException extends Exception {

    private static final long serialVersionUID = -863185580789085695L;

    /** Sin detalle. */
    public MarshalException() {
        super();
    }

    /** Con un mensaje. */
    public MarshalException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public MarshalException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Solo con la causa; el mensaje sale de su {@code toString}. */
    public MarshalException(Throwable cause) {
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
