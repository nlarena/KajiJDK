package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NotYetBoundException — Se uso un canal de servidor que todavia no se ato a ninguna direccion.
 *
 * <p>Aceptar conexiones sin haber dicho en que puerto escuchar no tiene respuesta posible.
 */
public class NotYetBoundException extends IllegalStateException {

    private static final long serialVersionUID = 1000000017L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public NotYetBoundException() {
        super();
    }
}
