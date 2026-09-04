package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.WritableByteChannel — un canal donde se pueden escribir bytes.
 *
 * <p>La contraparte de {@link ReadableByteChannel}. Un solo metodo, y su contrato tiene una parte que
 * sorprende a quien viene de los flujos: **puede escribir menos bytes de los que hay en el buffer**, y
 * por eso devuelve cuantos escribio. Un canal no bloqueante escribe lo que entra ahora y vuelve.
 *
 * <p>De ahi que el patron correcto sea un bucle mientras `buf.hasRemaining()`, y no una sola llamada.
 */
public interface WritableByteChannel extends Channel {

    /**
     * Escribe bytes del buffer, desde su posicion actual.
     *
     * @return cuantos escribio, que puede ser cero
     * @throws IOException si falla la escritura
     */
    int write(ByteBuffer src) throws IOException;
}
