package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.CancelledKeyException — Se uso una clave de seleccion que ya habia sido cancelada.
 *
 * <p>Una clave cancelada sigue siendo un objeto valido --se la puede tener en la mano-- pero ya
 * no representa un registro vivo. Que tirar sea lo correcto y no devolver un valor neutro: la
 * clave se cancelo porque alguien lo pidio, y seguir usandola es el error.
 */
public class CancelledKeyException extends IllegalStateException {

    private static final long serialVersionUID = 1000000004L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public CancelledKeyException() {
        super();
    }
}
