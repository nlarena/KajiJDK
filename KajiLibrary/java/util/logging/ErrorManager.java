package java.util.logging;

/**
 * KajiLibrary's java.util.logging.ErrorManager -- que hacer cuando **el registro mismo** falla.
 *
 * <p>Existe por un problema circular: si escribir un mensaje falla, no se puede reportar el fallo
 * escribiendo un mensaje. Y lanzar tampoco sirve -- una aplicacion no deberia caerse porque el disco
 * de la traza se lleno. La salida del JDK es reportar **una sola vez** al error estandar y despues
 * callarse, que es lo que hace esta implementacion.
 */
public class ErrorManager {

    /** Un fallo sin clasificar. */
    public static final int GENERIC_FAILURE = 0;

    /** Fallo al escribir. */
    public static final int WRITE_FAILURE = 1;

    /** Fallo al vaciar el buffer. */
    public static final int FLUSH_FAILURE = 2;

    /** Fallo al cerrar. */
    public static final int CLOSE_FAILURE = 3;

    /** Fallo al abrir. */
    public static final int OPEN_FAILURE = 4;

    /** Fallo al aplicar la configuracion. */
    public static final int FORMAT_FAILURE = 5;

    private boolean yaReporto = false;

    /** Reporta el fallo; solo el primero sale. */
    public synchronized void error(String msg, Exception ex, int code) {
        if (this.yaReporto) {
            return;
        }
        this.yaReporto = true;
        System.err.println("java.util.logging.ErrorManager: " + code
                + (msg != null ? ": " + msg : ""));
        if (ex != null) {
            ex.printStackTrace();
        }
    }
}
