package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.AlreadyBoundException — Se quiso atar un canal a una direccion cuando ya estaba atado.
 *
 * <p>Atar es una operacion de una sola vez: un canal atado dos veces tendria dos direcciones y
 * ninguna forma de decidir cual usar.
 */
public class AlreadyBoundException extends IllegalStateException {

    private static final long serialVersionUID = 1000000001L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public AlreadyBoundException() {
        super();
    }
}
