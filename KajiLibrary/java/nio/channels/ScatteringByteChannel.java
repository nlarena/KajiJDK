package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * KajiLibrary's java.nio.channels.ScatteringByteChannel — lee repartiendo en varios buffers.
 *
 * <p>"Scattering" es literal: una sola lectura llena el primer buffer, y lo que sobra pasa al
 * siguiente. Sirve para leer de una vez un mensaje con encabezado de largo fijo y cuerpo variable sin
 * copiar despues: un buffer para cada parte, una sola llamada al sistema.
 *
 * <p>Devuelve `long` y no `int` porque la suma de varios buffers puede pasarse de lo que entra en un
 * `int`.
 */
public interface ScatteringByteChannel extends ReadableByteChannel {

    /**
     * Lee repartiendo en los buffers indicados.
     *
     * @param dsts los buffers
     * @param offset el primero a usar
     * @param length cuantos usar
     * @return cuantos bytes leyo en total, o -1 si el canal llego a su fin
     * @throws IOException si falla la lectura
     */
    long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /** Lee repartiendo en todos los buffers. */
    long read(ByteBuffer[] dsts) throws IOException;
}
