package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

// Un `AsynchronousFileChannel` sobre el `FileChannel` de esta biblioteca y un pool de hilos.
//
// ===============================================================================================
// QUE COMPRA LO ASINCRONICO ACA, Y QUE NO
// ===============================================================================================
//
// **Compra** lo que la API promete de verdad: pedir una lectura no bloquea al que la pide, se pueden
// tener muchas en vuelo, y el resultado llega por un `Future` o por un manejador. Eso alcanza para
// lo que uno elige esta API: no atar un hilo por operacion.
//
// **No compra** velocidad. Abajo no hay lectura asincronica del sistema: hay un `FileChannel`
// bloqueante corriendo en otro hilo, y ese `FileChannel` --como explica su cabecera-- lee el archivo
// entero en cada operacion. Ocho lecturas en vuelo son ocho hilos leyendo ocho veces el archivo.
// El JDK hace lo mismo --un pool sobre lecturas bloqueantes-- en las plataformas sin `aio`, asi que
// la forma es la suya; lo que cambia es el costo de la operacion de abajo, y ese esta documentado
// donde corresponde.
//
// **La cancelacion es la del JDK en esas mismas plataformas**: `Future.cancel(true)` interrumpe al
// hilo que esta en la operacion. Si la operacion ya empezo a escribir, cancelarla no la deshace.
//
// De paquete a proposito: se llega por `AsynchronousFileChannel.open`.
final class AsyncFileChannelImpl extends AsynchronousFileChannel {

    private final KajiFileChannel channel;
    private final AsyncChannelGroup group;

    AsyncFileChannelImpl(KajiFileChannel channel, AsyncChannelGroup group) {
        this.channel = channel;
        this.group = group;
        group.register(this);
    }

    @Override
    public long size() throws IOException {
        return this.channel.size();
    }

    @Override
    public AsynchronousFileChannel truncate(long size) throws IOException {
        this.channel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        this.channel.force(metaData);
    }

    @Override
    public <A> void read(ByteBuffer dst, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        comprobar(dst, position);
        AsyncTask.notifying(this.group.pool(), new Leer(this.channel, dst, position), attachment,
                handler);
    }

    @Override
    public Future<Integer> read(ByteBuffer dst, long position) {
        comprobar(dst, position);
        return AsyncTask.future(this.group.pool(), new Leer(this.channel, dst, position));
    }

    @Override
    public <A> void write(ByteBuffer src, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler) {
        comprobar(src, position);
        AsyncTask.notifying(this.group.pool(), new Escribir(this.channel, src, position), attachment,
                handler);
    }

    @Override
    public Future<Integer> write(ByteBuffer src, long position) {
        comprobar(src, position);
        return AsyncTask.future(this.group.pool(), new Escribir(this.channel, src, position));
    }

    // ---- locks -----------------------------------------------------------------------------------
    //
    // Taking a lock can block --that is what `wait` means-- so it goes to the pool like every other
    // blocking operation here. `tryLock` does not block by definition, so it runs on the calling
    // thread: sending it to the pool would add a hop and buy nothing.

    @Override
    public <A> void lock(long position, long size, boolean shared, A attachment,
            CompletionHandler<FileLock, ? super A> handler) {
        final KajiFileLock reserved = reserve(position, size, shared);
        AsyncTask.notifying(this.group.pool(), new Lock(this.channel, reserved), attachment,
                handler);
    }

    @Override
    public Future<FileLock> lock(long position, long size, boolean shared) {
        final KajiFileLock reserved = reserve(position, size, shared);
        return AsyncTask.future(this.group.pool(), new Lock(this.channel, reserved));
    }

    /**
     * Claims the region before going to the pool.
     *
     * <p>An overlap has to be refused on the caller's thread: the API throws
     * {@link java.nio.channels.OverlappingFileLockException} instead of delivering it through the
     * future, so the check cannot wait. `IOException` cannot be thrown from here either -- these two
     * methods do not declare it -- so a failure to even check becomes a reservation that the pool
     * will fail on.
     */
    private KajiFileLock reserve(long position, long size, boolean shared) {
        try {
            return this.channel.reserveFor(this, position, size, shared);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        return this.channel.acquireFor(this, position, size, shared, false);
    }

    /** Turning a reservation into a real lock, to run on the pool. */
    private static final class Lock implements Callable<FileLock> {

        private final KajiFileChannel channel;
        private final KajiFileLock reserved;

        Lock(KajiFileChannel channel, KajiFileLock reserved) {
            this.channel = channel;
            this.reserved = reserved;
        }

        public FileLock call() throws IOException {
            // `wait` is true: the whole point of the asynchronous form is that waiting costs the
            // caller nothing.
            this.channel.complete(this.reserved, true);
            return this.reserved;
        }
    }

    @Override
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    @Override
    public void close() throws IOException {
        this.group.unregister(this);
        this.channel.close();
    }

    /**
     * Las comprobaciones que van en el hilo que llama y no en el pool.
     *
     * <p>Una posicion negativa es un error del programa y tiene que aparecer donde se lo cometio, no
     * en un `Future` que alguien mire tres pasos despues.
     */
    private void comprobar(ByteBuffer b, long position) {
        if (b == null) {
            throw new NullPointerException("buffer");
        }
        if (position < 0) {
            throw new IllegalArgumentException("Negative position");
        }
        // El channel cerrado NO se comprueba aca: la API dice que esa falla llega por el `Future` o
        // por `failed`, no por la llamada. Lo tira `FileChannel` adentro del pool, que es donde va.
    }

    /** Una lectura, para correr en el pool. */
    private static final class Leer implements Callable<Integer> {

        private final FileChannel channel;
        private final ByteBuffer destino;
        private final long posicion;

        Leer(FileChannel channel, ByteBuffer destino, long posicion) {
            this.channel = channel;
            this.destino = destino;
            this.posicion = posicion;
        }

        public Integer call() throws IOException {
            return Integer.valueOf(this.channel.read(this.destino, this.posicion));
        }
    }

    /** Una escritura, para correr en el pool. */
    private static final class Escribir implements Callable<Integer> {

        private final FileChannel channel;
        private final ByteBuffer origen;
        private final long posicion;

        Escribir(FileChannel channel, ByteBuffer origen, long posicion) {
            this.channel = channel;
            this.origen = origen;
            this.posicion = posicion;
        }

        public Integer call() throws IOException {
            return Integer.valueOf(this.channel.write(this.origen, this.posicion));
        }
    }
}
