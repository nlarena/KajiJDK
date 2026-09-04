package javax.xml.crypto;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.crypto.NoSuchMechanismException -- no hay implementacion para ese mecanismo.
 *
 * <p>La unica <b>no comprobada</b> del paquete, y esta bien que lo sea: pedir un mecanismo
 * que la plataforma no tiene es un error de despliegue, no una condicion que el programa
 * pueda manejar. Es la misma decision que {@code NoSuchAlgorithmException} no toma --esa si
 * es comprobada-- y la diferencia se nota al escribir un {@code getInstance}: aca no hay que
 * atajar nada.
 *
 * <p>Redefine {@code getCause} y los tres {@code printStackTrace} porque en el JDK guarda su causa
 * en un campo propio, de cuando {@code Throwable} todavia no las tenia. Aca la causa es la de
 * {@code Throwable} y las redefiniciones delegan: mismo comportamiento, sin dos copias del dato.
 */
public class NoSuchMechanismException extends RuntimeException {

    private static final long serialVersionUID = 4189669069570660166L;

    /** Sin detalle. */
    public NoSuchMechanismException() {
        super();
    }

    /** Con un mensaje. */
    public NoSuchMechanismException(String message) {
        super(message);
    }

    /** Con un mensaje y la causa de abajo. */
    public NoSuchMechanismException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Solo con la causa; el mensaje sale de su {@code toString}. */
    public NoSuchMechanismException(Throwable cause) {
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
