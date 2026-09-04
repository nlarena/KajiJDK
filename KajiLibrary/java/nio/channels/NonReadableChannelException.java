package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.NonReadableChannelException — Se leyo de un canal que no se abrio para lectura.
 *
 * <p>El permiso se fija al abrir y no cambia. Que sea `IllegalStateException` --y no de
 * argumento-- es correcto: el argumento de `read` esta bien, lo que no corresponde es pedirselo a
 * **este** canal.
 */
public class NonReadableChannelException extends IllegalStateException {

    private static final long serialVersionUID = 1000000015L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public NonReadableChannelException() {
        super();
    }
}
