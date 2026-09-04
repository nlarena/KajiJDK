package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.GatheringByteChannel — escribe juntando varios buffers.
 *
 * <p>El simetrico de {@link ScatteringByteChannel}: una sola escritura vacia el primer buffer, sigue
 * con el segundo, y asi. Es lo que permite mandar encabezado y cuerpo en una sola operacion sin
 * armar antes un buffer unico con todo copiado adentro.
 */
public interface GatheringByteChannel extends WritableByteChannel {

    /**
     * Escribe juntando los buffers indicados.
     *
     * @param srcs los buffers
     * @param offset el primero a usar
     * @param length cuantos usar
     * @return cuantos bytes escribio en total
     * @throws IOException si falla la escritura
     */
    long write(ByteBuffer[] srcs, int offset, int length) throws IOException;

    /** Escribe juntando todos los buffers. */
    long write(ByteBuffer[] srcs) throws IOException;
}
