package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ClosedChannelException — Se intento una operacion sobre un canal cerrado.
 *
 * <p>Es de E/S y no de estado, aunque parezca lo contrario: un canal se puede cerrar por causas
 * ajenas al programa --el otro extremo, una interrupcion-- asi que el que llama tiene que
 * contemplarla, y de ahi que sea chequeada.
 */
public class ClosedChannelException extends java.io.IOException {

    private static final long serialVersionUID = 1000000006L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public ClosedChannelException() {
        super();
    }
}
