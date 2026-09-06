package jdk.jshell.execution;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Un flujo que le pone una etiqueta a lo que escribe, para compartir un canal.
 *
 * <h2>Para que</h2>
 *
 * <p>Entre JShell y el proceso que ejecuta hay una sola conexion, y por ahi tienen que pasar varias
 * corrientes distintas: la salida del programa del usuario, su salida de error, y --en el otro
 * sentido-- su entrada. Cada una se envuelve en uno de estos con su nombre, y del otro lado
 * {@link DemultiplexInput} las reparte.
 *
 * <h2>El formato</h2>
 *
 * <p>Cada bloque es: un byte con el largo del nombre, el nombre, un byte con el largo de los datos,
 * y los datos. Los largos entran en un byte, asi que un bloque nunca pasa de 127 bytes de datos y
 * una escritura larga se parte en varios. El nombre se repite en cada bloque: cuesta unos bytes y a
 * cambio el canal no tiene estado, que es lo que permite intercalar dos corrientes sin coordinarlas.
 *
 * <p>La escritura del bloque va sincronizada sobre el flujo de abajo. Sin eso, dos corrientes que
 * escriben a la vez entrelazarian sus bloques a medio armar y el que lee no podria separarlos.
 */
final class MultiplexingOutputStream extends OutputStream {

    /** Lo mas grande que puede medir un bloque de datos: el largo va en un solo byte. */
    private static final int MAXIMO = 127;

    private final byte[] nombre;
    private final OutputStream destino;

    MultiplexingOutputStream(String nombre, OutputStream destino) {
        this.nombre = nombre.getBytes(StandardCharsets.UTF_8);
        this.destino = destino;
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[] {(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int i = 0;
        while (i < len) {
            final int cuanto = Math.min(len - i, MAXIMO);
            final byte[] bloque = new byte[nombre.length + cuanto + 2];
            bloque[0] = (byte) nombre.length;
            System.arraycopy(nombre, 0, bloque, 1, nombre.length);
            bloque[nombre.length + 1] = (byte) cuanto;
            System.arraycopy(b, off + i, bloque, nombre.length + 2, cuanto);
            synchronized (destino) {
                destino.write(bloque);
            }
            i += cuanto;
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (destino) {
            destino.flush();
        }
    }
}
