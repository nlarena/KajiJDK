package java.nio.channels;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.AsynchronousByteChannel — lee y escribe bytes sin esperar.
 *
 * <p>Cada operacion viene en **dos formas**, y la eleccion no es de gusto:
 *
 * <ul>
 * <li>La que devuelve {@link Future} sirve cuando el que pide quiere esperar el resultado en algun
 *     momento, en su propio hilo.</li>
 * <li>La que toma un {@link CompletionHandler} sirve cuando no hay a quien hacer esperar: el aviso
 *     llega solo, en el hilo que el canal elija.</li>
 * </ul>
 *
 * <p>Un canal admite **una lectura y una escritura** en curso a la vez; pedir una segunda tira
 * {@link ReadPendingException} o {@link WritePendingException}. No es una limitacion de la
 * implementacion sino parte del contrato: dos lecturas simultaneas sobre el mismo canal no tendrian
 * un orden definido para los bytes.
 */
public interface AsynchronousByteChannel extends AsynchronousChannel {

    /** Lee, avisando por el manejador. */
    <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler);

    /** Lee, entregando un `Future` con la cantidad leida. */
    Future<Integer> read(ByteBuffer dst);

    /** Escribe, avisando por el manejador. */
    <A> void write(ByteBuffer src, A attachment, CompletionHandler<Integer, ? super A> handler);

    /** Escribe, entregando un `Future` con la cantidad escrita. */
    Future<Integer> write(ByteBuffer src);
}
