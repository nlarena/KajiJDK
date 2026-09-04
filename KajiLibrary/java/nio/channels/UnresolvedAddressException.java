package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.UnresolvedAddressException — Se uso una direccion de socket que nunca se resolvio.
 *
 * <p>Una direccion sin resolver es un nombre sin numero: se puede construir y pasar, pero no se
 * puede usar para conectar. Fallar aca, y no adentro de la pila de red, hace que el error senale
 * el lugar donde se puede arreglar.
 */
public class UnresolvedAddressException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000022L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public UnresolvedAddressException() {
        super();
    }
}
