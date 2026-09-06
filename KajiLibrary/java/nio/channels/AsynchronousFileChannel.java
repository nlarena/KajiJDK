package java.nio.channels;

import java.io.IOException;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousFileChannel — un canal de archivo cuyas operaciones se
 * piden y se contestan despues.
 *
 * <p>No hereda de {@link FileChannel} y no es un descuido: son dos jerarquias distintas sobre el
 * mismo archivo. La diferencia visible es que **no tiene posicion corriente**. Todas sus lecturas y
 * escrituras llevan la posicion como argumento, y tiene que ser asi: con varias operaciones en vuelo
 * a la vez, una posicion compartida no significaria nada --cual de las tres la avanzo primero?--.
 *
 * <p>Cada operacion se puede pedir de dos maneras, y las dos estan por una razon distinta: con
 * {@link CompletionHandler} para el codigo que reacciona a eventos, y devolviendo un
 * {@link Future} para el que en algun momento quiere sentarse a esperar.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p><strong>Los dos {@code open()} ya estan.</strong> Esta nota argumentaba que un pool de hilos
 * sobre lecturas bloqueantes seria una fachada, porque prometeria las propiedades por las que uno
 * elige esta API. Vale la pena separar cuales:
 *
 * <ul>
 *   <li><strong>No atar un hilo del programa por operacion</strong> --que es por lo que se elige--
 *       se cumple: pedir una lectura vuelve en el acto y el resultado llega por un `Future` o por un
 *       manejador;
 *   <li><strong>una operacion por hilo del sistema</strong> no se cumple, y no se puede: los nativos
 *       de archivo de esta VM son sincronicos. Es exactamente lo que le pasa al JDK en las
 *       plataformas sin `aio`, donde usa un pool igual que este.
 * </ul>
 *
 * <p>Lo que queda por decir es el costo, y esta dicho donde corresponde: el {@link FileChannel} de
 * abajo lee el archivo entero en cada operacion. Ver {@code AsyncFileChannelImpl}.
 *
 * <p><strong>The six locking members are in.</strong> This note used to say they were out because a
 * file lock excludes **other processes** and the VM had nothing to do it with. It does now -- see
 * the header of {@link FileChannel}. Waiting for a lock is exactly the kind of thing this API is
 * for: {@link #lock(long, long, boolean)} hands back a {@link java.util.concurrent.Future} instead
 * of parking the caller.
 *
 * <p>Queda entonces el contrato: {@link #size()}, {@link #truncate}, {@link #force} y las cuatro
 * formas de leer y escribir, que es lo que cualquier implementacion tiene que cumplir.
 */
public abstract class AsynchronousFileChannel implements AsynchronousChannel {

    protected AsynchronousFileChannel() {
    }

    /** El tama&ntilde;o del archivo. Es sincronico, tambien en el JDK: no hay nada que esperar. */
    /**
     * Abre un canal asincronico sobre ese archivo, en el grupo de omision.
     *
     * <p>Sin ninguna opcion se abre para lectura, igual que {@link FileChannel#open}.
     *
     * @param file el archivo
     * @param options como abrirlo
     * @return el canal
     * @throws IOException si no se puede abrir
     */
    public static AsynchronousFileChannel open(java.nio.file.Path file,
            java.nio.file.OpenOption... options) throws IOException {
        final java.util.Set<java.nio.file.OpenOption> conjunto =
                new java.util.HashSet<java.nio.file.OpenOption>();
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                conjunto.add(options[i]);
            }
        }
        if (conjunto.isEmpty()) {
            conjunto.add(java.nio.file.StandardOpenOption.READ);
        }
        return open(file, conjunto, null);
    }

    /**
     * Lo mismo, diciendo en que pool corren los manejadores y con que atributos crear el archivo.
     *
     * <p>El pool va suelto y no como grupo porque es lo que la firma del JDK pide. Se lo envuelve en
     * un grupo aca; si es {@code null}, se usa el de omision.
     *
     * @param file el archivo
     * @param options como abrirlo
     * @param executor donde corren los manejadores, o {@code null} para el pool de omision
     * @param attrs los atributos con que crearlo
     * @return el canal
     * @throws IOException si no se puede abrir
     */
    public static AsynchronousFileChannel open(java.nio.file.Path file,
            java.util.Set<? extends java.nio.file.OpenOption> options,
            java.util.concurrent.ExecutorService executor,
            java.nio.file.attribute.FileAttribute<?>... attrs) throws IOException {
        if (options == null) {
            throw new NullPointerException("options");
        }
        final java.nio.file.OpenOption[] arreglo =
                options.toArray(new java.nio.file.OpenOption[options.size()]);
        final FileChannel bruto = FileChannel.open(file, arreglo);
        final AsynchronousChannelGroup grupo = executor == null
                ? AsynchronousChannelProvider.provider().openAsynchronousChannelGroup(
                        java.util.concurrent.Executors.newCachedThreadPool(), 0)
                : AsynchronousChannelGroup.withThreadPool(executor);
        return AsyncChannelFactory.file(bruto, grupo);
    }

    // ---- locks -----------------------------------------------------------------------------------

    /**
     * Takes an exclusive lock over the whole file, and reports through the handler.
     *
     * @param <A> the type of the attachment
     * @param attachment handed back to the handler untouched
     * @param handler told when the lock is taken, or why it was not
     */
    public final <A> void lock(A attachment, CompletionHandler<FileLock, ? super A> handler) {
        lock(0L, Long.MAX_VALUE, false, attachment, handler);
    }

    /**
     * Takes a lock over that region, and reports through the handler.
     *
     * @param <A> the type of the attachment
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @param attachment handed back to the handler untouched
     * @param handler told when the lock is taken, or why it was not
     */
    public abstract <A> void lock(long position, long size, boolean shared, A attachment,
            CompletionHandler<FileLock, ? super A> handler);

    /**
     * Takes an exclusive lock over the whole file.
     *
     * @return a future for the lock
     */
    public final Future<FileLock> lock() {
        return lock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return a future for the lock
     */
    public abstract Future<FileLock> lock(long position, long size, boolean shared);

    /**
     * Takes an exclusive lock over the whole file without waiting.
     *
     * <p>This one is not asynchronous and does not need to be: not waiting is the whole point, so
     * there is nothing for a future to carry.
     *
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     */
    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Takes a lock over that region without waiting.
     *
     * @param position the first byte
     * @param size how many bytes
     * @param shared true for a lock other readers may share
     * @return the lock, or {@code null} when another process holds it
     * @throws IOException if it cannot be taken
     */
    public abstract FileLock tryLock(long position, long size, boolean shared) throws IOException;

    public abstract long size() throws IOException;

    /** Corta el archivo a `size`. Si ya era mas chico, no hace nada. */
    public abstract AsynchronousFileChannel truncate(long size) throws IOException;

    /** Fuerza al disco lo que este pendiente. */
    public abstract void force(boolean metaData) throws IOException;

    /**
     * Lee desde `position` y avisa a `handler` cuando termina.
     *
     * <p>`attachment` viaja hasta el handler sin que nada lo toque: es como se lleva el contexto de
     * la operacion sin un mapa aparte.
     */
    public abstract <A> void read(ByteBuffer dst, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Como el otro, devolviendo el resultado como {@link Future}. */
    public abstract Future<Integer> read(ByteBuffer dst, long position);

    /** Escribe en `position` y avisa a `handler` cuando termina. */
    public abstract <A> void write(ByteBuffer src, long position, A attachment,
            CompletionHandler<Integer, ? super A> handler);

    /** Como el otro, devolviendo el resultado como {@link Future}. */
    public abstract Future<Integer> write(ByteBuffer src, long position);
}
