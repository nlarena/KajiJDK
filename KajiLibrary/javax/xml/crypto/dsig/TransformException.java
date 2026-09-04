package javax.xml.crypto.dsig;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.dsig.TransformException -- fallo una transformacion.
 *
 * <p>Sale de {@link Transform#transform}. Lo que fallo es el <b>camino</b> entre el dato y
 * su resumen: una expresion XPath mal escrita, una hoja de estilo que tira, datos de un
 * tipo que la transformacion no acepta.
 *
 * <p>No es un fallo criptografico. Distinguirla de {@link XMLSignatureException} importa
 * al diagnosticar: aca la firma ni siquiera se llego a comparar.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} por lo mismo que las de
 * {@code javax.xml.crypto}: en el JDK la causa vive en un campo propio, de antes de que
 * {@code Throwable} las tuviera, y aca delega.
 */
public class TransformException extends Exception {

    private static final long serialVersionUID = 526117000366604532L;

    /** Sin detalle. */
    public TransformException() {
        super();
    }

    /** Con un mensaje. */
    public TransformException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public TransformException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Solo con la causa. */
    public TransformException(Throwable cause) {
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
