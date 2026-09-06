package java.nio.channels.spi;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

// El unico provider de canales asincronicos que esta biblioteca registra de fabrica.
//
// ===============================================================================================
// QUE PONE ABAJO
// ===============================================================================================
//
// Un pool de hilos y los canales bloqueantes que la biblioteca ya tenia: `SocketChannel` y
// `ServerSocketChannel`, que hablan con la red de verdad por `jdk.internal.net.Net`. Cada operacion
// asincronica es una bloqueante corriendo en un hilo del pool.
//
// **Esa es una implementacion legitima y no un atajo**: es la que el JDK usa en las plataformas que
// no tienen entrada y salida asincronica del sistema. Lo que compra es lo que uno viene a buscar a
// esta API --no atar un hilo del programa por operacion-- y lo que no compra es una operacion por
// hilo del sistema, que es otra cosa.
//
// De paquete a proposito: no es API del JDK. Se llega a el por `AsynchronousChannelProvider.provider()`,
// que lo devuelve cuando ni la propiedad de sistema ni el `ServiceLoader` nombraron otro.
final class KajiAsyncChannelProvider extends AsynchronousChannelProvider {

    KajiAsyncChannelProvider() {
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads,
            ThreadFactory threadFactory) throws IOException {
        if (nThreads <= 0) {
            throw new IllegalArgumentException("nThreads <= 0");
        }
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        return group(Executors.newFixedThreadPool(nThreads, threadFactory), true);
    }

    @Override
    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor,
            int initialSize) throws IOException {
        if (executor == null) {
            throw new NullPointerException("executor");
        }
        // `initialSize` es una sugerencia sobre cuantos hilos arrancar leyendo del pool. Aca no hay
        // hilos leyendo de ningun lado --cada operacion se manda al pool cuando se la pide-- asi que
        // no hay nada que dimensionar. Se acepta cualquier valor, como el JDK.
        return group(executor, false);
    }

    @Override
    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            AsynchronousChannelGroup group) throws IOException {
        return java.nio.channels.AsyncChannelFactory.serverSocket(this, groupOf(group));
    }

    @Override
    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group)
            throws IOException {
        return java.nio.channels.AsyncChannelFactory.socket(this, groupOf(group));
    }

    /**
     * El group de fallback, que se arma la primera vez que alguien abre un channel sin group.
     *
     * <p>Es un pool que crece --`newCachedThreadPool`-- porque es lo que el JDK usa para el suyo: un
     * group compartido por todo el programa no puede tener un tope elegido de antemano.
     */
    synchronized AsynchronousChannelGroup defaultGroup() {
        if (this.fallback == null) {
            this.fallback = group(Executors.newCachedThreadPool(), true);
        }
        return this.fallback;
    }

    private AsynchronousChannelGroup fallback;

    /** El group que se le pidio, o el de fallback si no se le pidio ninguno. */
    private AsynchronousChannelGroup groupOf(AsynchronousChannelGroup group) {
        return group == null ? defaultGroup() : group;
    }

    private AsynchronousChannelGroup group(ExecutorService pool, boolean owned) {
        return java.nio.channels.AsyncChannelFactory.group(this, pool, owned);
    }
}
