package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.SeekableByteChannel — un canal con **posicion**.
 *
 * <p>Es la diferencia entre un archivo y un socket: en un archivo se puede ir y volver, en un socket
 * los bytes pasan una sola vez. Todo lo que este tipo agrega --{@link #position()},
 * {@link #truncate}, {@link #size()}-- solo tiene sentido sobre algo que se puede recorrer.
 *
 * <p>Redeclara `read` y `write` aunque ya los hereda, tal como el JDK, y no es redundante: es donde
 * se documenta que **las dos avanzan la posicion**, que es lo que las distingue de las del canal
 * comun.
 *
 * <p>La implementacion que trae esta biblioteca es {@link FileChannel}, que se obtiene con
 * {@link FileChannel#open} o con `java.nio.file.Files.newByteChannel`. La posicion ahi es
 * **simulada**: los nativos de esta VM leen y escriben el archivo entero, asi que el canal lleva la
 * cuenta por su lado y cada operacion recorre todo. Sale caro y no miente, que es el trato que
 * explica la cabecera de {@link FileChannel}.
 */
public interface SeekableByteChannel extends ByteChannel {

    /** Lee desde la posicion actual y la avanza. */
    int read(ByteBuffer dst) throws IOException;

    /** Escribe desde la posicion actual y la avanza. */
    int write(ByteBuffer src) throws IOException;

    /** La posicion actual, en bytes desde el principio. */
    long position() throws IOException;

    /**
     * Mueve la posicion.
     *
     * <p>Se admite **mas alla del final**: no es un error, y leer ahi devuelve -1. Escribir ahi deja
     * un hueco, que es como se hacen los archivos ralos.
     */
    SeekableByteChannel position(long newPosition) throws IOException;

    /** El tamanio actual, en bytes. */
    long size() throws IOException;

    /**
     * Corta el contenido a ese tamanio.
     *
     * <p>Si la posicion quedaba mas alla del nuevo final, pasa a ser el nuevo final: no puede quedar
     * apuntando afuera de lo que existe.
     */
    SeekableByteChannel truncate(long size) throws IOException;
}
