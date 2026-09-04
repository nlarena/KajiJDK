package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ShutdownChannelGroupException — El grupo de canales ya se apago, o el manejador de terminacion no se puede invocar porque el grupo
 * del canal se apago.
 *
 * <p>Los dos casos son el mismo problema visto desde los dos extremos: no queda quien corra el
 * trabajo.
 */
public class ShutdownChannelGroupException extends IllegalStateException {

    private static final long serialVersionUID = 1000000021L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ShutdownChannelGroupException() {
        super();
    }
}
