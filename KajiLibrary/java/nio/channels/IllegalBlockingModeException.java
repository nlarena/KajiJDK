package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.IllegalBlockingModeException — La operacion pedida no vale en el modo de bloqueo en que el canal esta.
 *
 * <p>Un canal en modo no bloqueante no puede hacer una lectura que espere, y uno en modo
 * bloqueante no se puede registrar en un selector. No son limitaciones arbitrarias: registrar un
 * canal bloqueante haria que el selector se trabara en el, que es exactamente lo que un selector
 * existe para evitar.
 */
public class IllegalBlockingModeException extends IllegalStateException {

    private static final long serialVersionUID = 1000000010L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public IllegalBlockingModeException() {
        super();
    }
}
