package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.TimeUnit;

/**
 * KajiLibrary's java.nio.channels.AsynchronousChannelGroup — el pool de hilos donde corren los
 * `CompletionHandler`.
 *
 * <p>Existe porque un canal asincronico no atiende sus propias respuestas: cuando una lectura
 * termina, alguien tiene que correr el {@link CompletionHandler}, y ese alguien es un hilo del
 * grupo. Compartir un grupo entre muchos canales es todo el punto --mil conexiones, ocho hilos-- y
 * es lo que separa a esta API de "un hilo por operacion".
 *
 * <p>Las dos formas de apagarlo no son grados de lo mismo:
 *
 * <ul>
 *   <li>{@link #shutdown()} cierra la puerta: no se aceptan canales nuevos, pero lo que hay sigue
 *       hasta que se cierren todos. **Vuelve en el acto** y no espera nada;
 *   <li>{@link #shutdownNow()} cierra los canales abiertos, lo que hace fallar a las operaciones en
 *       curso con {@link AsynchronousCloseException}.
 * </ul>
 *
 * <p>Un grupo con un canal abierto que nadie cierra **no termina nunca**, y ese es el modo de fallar
 * mas comun con esta clase: `shutdown()` seguido de un `awaitTermination` que no vuelve.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los tres estaticos --{@code withFixedThreadPool}, {@code withCachedThreadPool},
 * {@code withThreadPool}-- <strong>no estan</strong>. Los tres piden el grupo al proveedor del
 * sistema, y no hay proveedor del sistema: esta VM no tiene nativos de red. Ver
 * {@link AsynchronousChannelProvider}.
 *
 * <p>Se podria haber armado un grupo sobre un `ExecutorService` de verdad --eso si existe en esta
 * biblioteca-- pero seria un grupo sin un solo canal que meterle, y su `awaitTermination` seria una
 * ceremonia sobre nada. El resto de la clase queda como contrato.
 */
public abstract class AsynchronousChannelGroup {

    private final AsynchronousChannelProvider proveedor;

    protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
        this.proveedor = provider;
    }

    /** El proveedor que lo fabrico. */
    public final AsynchronousChannelProvider provider() {
        return this.proveedor;
    }

    /** Si ya no acepta canales nuevos. */
    public abstract boolean isShutdown();

    /** Si ademas ya no queda nada corriendo y los hilos se fueron. */
    public abstract boolean isTerminated();

    /** Cierra la puerta a canales nuevos y vuelve en el acto. Ver la nota de la clase. */
    public abstract void shutdown();

    /** Cierra los canales abiertos; las operaciones en curso fallan. */
    public abstract void shutdownNow() throws IOException;

    /**
     * Espera a que el grupo termine.
     *
     * @return `true` si termino, `false` si se agoto la espera. Distinguirlos importa: un `false`
     *         casi siempre significa que quedo un canal sin cerrar
     */
    public abstract boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException;
}
