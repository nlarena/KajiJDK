package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.ByteChannel — un canal que lee **y** escribe.
 *
 * <p>No declara nada propio: es la union de {@link ReadableByteChannel} y
 * {@link WritableByteChannel}, y su valor es exactamente ese. Permite que una firma pida "un canal
 * bidireccional" con un solo tipo, en vez de un parametro por cada mitad o una interseccion escrita a
 * mano en cada lugar.
 */
public interface ByteChannel extends ReadableByteChannel, WritableByteChannel {
}
