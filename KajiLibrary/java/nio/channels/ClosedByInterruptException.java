package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedByInterruptException — El hilo que estaba bloqueado en una operacion de E/S fue **interrumpido**, y por eso el canal se
 * cerro.
 *
 * <p>El cierre no es un efecto colateral desprolijo sino la semantica de un canal interrumpible:
 * una operacion de E/S a medias no se puede dejar a medias, asi que interrumpir cierra. Por eso
 * extiende `AsynchronousCloseException` --el canal quedo cerrado-- y no una excepcion de
 * interrupcion a secas.
 */
public class ClosedByInterruptException extends AsynchronousCloseException {

    private static final long serialVersionUID = 1000000005L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ClosedByInterruptException() {
        super();
    }
}
