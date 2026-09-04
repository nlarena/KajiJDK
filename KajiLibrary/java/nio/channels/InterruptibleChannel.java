package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.InterruptibleChannel — un canal que se puede interrumpir.
 *
 * <p>Marca el canal como asincronicamente cerrable e interrumpible, y las dos cosas van juntas por
 * una razon concreta: si un hilo esta bloqueado leyendo, la unica forma de sacarlo de ahi es cerrar
 * el canal debajo suyo. Por eso interrumpir a un hilo bloqueado en uno de estos **cierra el canal**
 * y le tira {@link ClosedByInterruptException}.
 *
 * <p>Puede parecer brusco, y es deliberado: una operacion de E/S abandonada a la mitad deja el canal
 * en un estado que nadie puede describir --cuantos bytes se leyeron, donde quedo el otro extremo--
 * asi que se lo cierra en vez de entregarlo asi.
 *
 * <p>{@link #close()} se redeclara para documentar que cualquier hilo bloqueado en este canal se
 * despierta con {@link AsynchronousCloseException}.
 *
 */
public interface InterruptibleChannel extends Channel {

    /** Cierra el canal; los hilos bloqueados en el se despiertan con una excepcion. */
    void close() throws java.io.IOException;
}
