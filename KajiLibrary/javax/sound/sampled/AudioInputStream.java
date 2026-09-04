package javax.sound.sampled;

import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.sampled.AudioInputStream -- un flujo de bytes que sabe que formato tiene.
 *
 * <p>Un {@link InputStream} con un {@link AudioFormat} pegado y un largo en cuadros. Eso es todo, y
 * alcanza: cualquier cosa que produzca o consuma audio en este paquete habla este tipo.
 *
 * <h2>Lee de a cuadros enteros</h2>
 *
 * <p>Es la parte que hay que tener presente. {@link #read(byte[], int, int)} <b>redondea hacia abajo</b>
 * al cuadro: pedir 5 bytes de un formato de 4 bytes por cuadro devuelve 4, no 5. Y
 * {@link #read()} lanza {@link IOException} si el cuadro ocupa mas de un byte.
 *
 * <p>No es un capricho: medio cuadro no significa nada, y devolverlo dejaria el flujo desalineado y
 * todo lo que siga sonaria a ruido.
 *
 * <h2>{@link #skip} tambien</h2>
 *
 * <p>Redondea igual. Saltar una cantidad que no sea multiplo del cuadro salta menos, nunca mas.
 */
public class AudioInputStream extends InputStream {

    /** De donde salen los bytes. */
    private final InputStream stream;

    /** Que formato tienen. */
    protected AudioFormat format;

    /** Cuantos cuadros hay, o {@link AudioSystem#NOT_SPECIFIED}. */
    protected long frameLength;

    /** Cuantos bytes ocupa un cuadro. */
    protected int frameSize;

    /** En que cuadro va la lectura. */
    protected long framePos;

    /** Donde estaba al marcar. */
    private long markpos;

    /** Lo que sobro de un cuadro incompleto entre dos lecturas. */
    private byte[] pushBackBuffer = null;

    /** Cuantos bytes hay guardados ahi. */
    private int pushBackLen = 0;

    /**
     * @param stream de donde leer
     * @param format que formato tienen los bytes
     * @param length cuantos cuadros, o {@link AudioSystem#NOT_SPECIFIED}
     */
    public AudioInputStream(InputStream stream, AudioFormat format, long length) {
        this.stream = stream;
        this.format = format;
        this.frameLength = length;
        this.frameSize = format.getFrameSize();
        if (this.frameSize == AudioSystem.NOT_SPECIFIED || this.frameSize <= 0) {
            this.frameSize = 1;
        }
        this.framePos = 0;
        this.markpos = 0;
    }

    /**
     * Un flujo sobre lo que capture esa linea de entrada.
     *
     * <p>El largo es {@link AudioSystem#NOT_SPECIFIED}: una linea de captura no tiene final.
     */
    public AudioInputStream(TargetDataLine line) {
        this(new TargetDataLineInputStream(line), line.getFormat(),
             AudioSystem.NOT_SPECIFIED);
    }

    /** Que formato tienen los bytes. */
    public AudioFormat getFormat() {
        return this.format;
    }

    /** Cuantos cuadros, o {@link AudioSystem#NOT_SPECIFIED}. */
    public long getFrameLength() {
        return this.frameLength;
    }

    /**
     * Un byte.
     *
     * @throws IOException si un cuadro ocupa mas de un byte; ver la nota de la clase
     */
    @Override
    public int read() throws IOException {
        if (this.frameSize != 1) {
            throw new IOException("cannot read a single byte if frame size > 1");
        }
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        if (n <= 0) {
            return -1;
        }
        return one[0] & 0xFF;
    }

    /** Todo lo que entre en el arreglo, redondeado al cuadro. */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Hasta {@code len} bytes, redondeado hacia abajo al cuadro.
     *
     * <p>Ver la nota de la clase sobre por que.
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.frameSize != 1) {
            len = len - (len % this.frameSize);
        }
        if (len == 0) {
            return 0;
        }
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (left <= 0) {
                return -1;
            }
            if (len > left) {
                len = (int) left;
            }
        }
        int read = this.stream.read(b, off, len);
        if (read < 0) {
            return -1;
        }
        // Si el flujo de abajo corto a mitad de cuadro, se completa antes de devolver: el contrato de
        // esta clase es que nunca sale medio cuadro.
        if (this.frameSize != 1 && read % this.frameSize != 0) {
            read = completeFrame(b, off, read);
        }
        this.framePos = this.framePos + read / this.frameSize;
        return read;
    }

    /** Lee lo que falte para cerrar el ultimo cuadro; si no llega, descarta el resto. */
    private int completeFrame(byte[] b, int off, int read) throws IOException {
        int missing = this.frameSize - (read % this.frameSize);
        int got = 0;
        while (got < missing) {
            int n = this.stream.read(b, off + read + got, missing - got);
            if (n < 0) {
                return read - (read % this.frameSize);
            }
            got = got + n;
        }
        return read + missing;
    }

    /** Saltea, redondeado hacia abajo al cuadro. */
    @Override
    public long skip(long n) throws IOException {
        if (this.frameSize != 1) {
            n = n - (n % this.frameSize);
        }
        if (n <= 0) {
            return 0;
        }
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (n > left) {
                n = left;
            }
        }
        long skipped = this.stream.skip(n);
        if (skipped % this.frameSize != 0) {
            skipped = skipped - (skipped % this.frameSize);
        }
        this.framePos = this.framePos + skipped / this.frameSize;
        return skipped;
    }

    /** Cuantos bytes se pueden leer sin bloquear, redondeado al cuadro. */
    @Override
    public int available() throws IOException {
        int n = this.stream.available();
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            long left = (this.frameLength - this.framePos) * this.frameSize;
            if (n > left) {
                n = (int) left;
            }
        }
        if (this.frameSize != 1) {
            n = n - (n % this.frameSize);
        }
        return n;
    }

    /** Cierra el flujo de abajo. */
    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    /** Marca, si el flujo de abajo sabe. */
    @Override
    public void mark(int readlimit) {
        this.stream.mark(readlimit);
        if (markSupported()) {
            this.markpos = this.framePos;
        }
    }

    /**
     * Vuelve a la marca.
     *
     * @throws IOException si el flujo de abajo no soporta marcas
     */
    @Override
    public void reset() throws IOException {
        this.stream.reset();
        this.framePos = this.markpos;
    }

    /** Si el flujo de abajo soporta marcas. */
    @Override
    public boolean markSupported() {
        return this.stream.markSupported();
    }

    /**
     * El puente entre una linea de captura y un {@link InputStream}.
     *
     * <p>De acceso de paquete: existe solo para el constructor que toma una {@link TargetDataLine}.
     * Arranca la linea en la primera lectura, no al construirse, porque una linea arrancada esta
     * capturando y llenando su bufer aunque nadie lea.
     */
    private static final class TargetDataLineInputStream extends InputStream {

        /** De donde se captura. */
        private final TargetDataLine line;

        TargetDataLineInputStream(TargetDataLine line) {
            this.line = line;
        }

        @Override
        public int available() throws IOException {
            return this.line.available();
        }

        @Override
        public void close() throws IOException {
            this.line.close();
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return one[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!this.line.isActive()) {
                this.line.start();
            }
            return this.line.read(b, off, len);
        }
    }
}
