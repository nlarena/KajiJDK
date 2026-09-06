package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
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
 * <p><strong>Los tres estaticos ya estan.</strong> Esta nota decia que no, con dos argumentos: que
 * no habia proveedor del sistema porque la VM no tenia nativos de red, y que un grupo sin canales
 * que meterle seria una ceremonia sobre nada. Los dos dejaron de valer al mismo tiempo: la VM tiene
 * nativos de red --{@code jdk.internal.net.Net}-- asi que {@link AsynchronousSocketChannel} y
 * {@link AsynchronousServerSocketChannel} se abren de verdad, y con canales adentro el grupo hace
 * exactamente lo que promete. Ver {@code KajiAsyncChannelProvider}, que es el proveedor de fabrica.
 */
public abstract class AsynchronousChannelGroup {

    private final AsynchronousChannelProvider proveedor;

    protected AsynchronousChannelGroup(AsynchronousChannelProvider provider) {
        this.proveedor = provider;
    }

    /**
     * Un grupo con un pool de tamano fijo.
     *
     * @param nThreads cuantos hilos
     * @param threadFactory con que fabrica de hilos armarlos
     * @return el grupo
     * @throws IOException si no se puede armar
     * @throws IllegalArgumentException si `nThreads` no es positivo
     * @throws NullPointerException si la fabrica es nula
     */
    public static AsynchronousChannelGroup withFixedThreadPool(int nThreads,
            ThreadFactory threadFactory) throws IOException {
        return AsynchronousChannelProvider.provider()
                .openAsynchronousChannelGroup(nThreads, threadFactory);
    }

    /**
     * Un grupo sobre un pool que crece segun haga falta.
     *
     * <p>`initialSize` es una sugerencia sobre cuantos hilos arrancar; esta implementacion no
     * necesita ninguno esperando, asi que la acepta y no la usa. Ver `KajiAsyncChannelProvider`.
     *
     * @param executor el pool
     * @param initialSize cuantos hilos arrancar, como sugerencia
     * @return el grupo
     * @throws IOException si no se puede armar
     * @throws NullPointerException si el pool es nulo
     */
    public static AsynchronousChannelGroup withCachedThreadPool(ExecutorService executor,
            int initialSize) throws IOException {
        return AsynchronousChannelProvider.provider()
                .openAsynchronousChannelGroup(executor, initialSize);
    }

    /**
     * Un grupo sobre ese pool.
     *
     * <p>El pool viene de afuera y **no se apaga solo** cuando el grupo termina: quien lo presto
     * puede estar usandolo para otra cosa. Apagarlo es de quien lo armo.
     *
     * @param executor el pool
     * @return el grupo
     * @throws IOException si no se puede armar
     * @throws NullPointerException si el pool es nulo
     */
    public static AsynchronousChannelGroup withThreadPool(ExecutorService executor)
            throws IOException {
        return AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(executor, 0);
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
