package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AcceptPendingException — Se pidio aceptar una conexion sobre un canal que ya tiene una aceptacion en curso.
 *
 * <p>Un canal asincronico admite **una** operacion de aceptacion a la vez: la segunda no se
 * encola, se rechaza. Encolarlas silenciosamente haria que el orden en que se completan
 * dependiera de detalles que el que llama no controla.
 */
public class AcceptPendingException extends IllegalStateException {

    private static final long serialVersionUID = 1000000000L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public AcceptPendingException() {
        super();
    }
}
