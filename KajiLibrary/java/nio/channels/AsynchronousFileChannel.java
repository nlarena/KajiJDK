package java.nio.channels;

import java.io.IOException;
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
 * <p>Los dos {@code open()} <strong>no estan</strong>. No es por el archivo --{@link FileChannel} si
 * se abre, y funciona-- sino por lo asincronico: un canal de estos necesita un pool de hilos donde
 * correr los `CompletionHandler` y un `Future` que alguien complete desde afuera. Se podria armar
 * sobre el `java.util.concurrent` de esta biblioteca, y seria una fachada: los nativos de archivo de
 * esta VM son sincronicos y leen el archivo entero, asi que cada "operacion asincronica" seria una
 * sincronica corrida en otro hilo. Eso no es falso en si mismo --el JDK hace algo parecido en varias
 * plataformas-- pero el precio de la mentira esta en otro lado: sin cancelacion real, sin
 * paralelismo real y con el archivo leido entero por operacion, prometer la API asincronica es
 * prometer las propiedades por las que uno la elige.
 *
 * <p>{@code lock}, {@code tryLock} y sus formas asincronicas tampoco estan, por lo mismo que en
 * {@link FileChannel}: un candado de archivo sirve para excluir a **otros procesos** y esta VM no
 * tiene con que. Ver la cabecera de {@link FileChannel}, donde esta el razonamiento completo.
 *
 * <p>Queda entonces el contrato: {@link #size()}, {@link #truncate}, {@link #force} y las cuatro
 * formas de leer y escribir, que es lo que cualquier implementacion tiene que cumplir.
 */
public abstract class AsynchronousFileChannel implements AsynchronousChannel {

    protected AsynchronousFileChannel() {
    }

    /** El tama&ntilde;o del archivo. Es sincronico, tambien en el JDK: no hay nada que esperar. */
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
