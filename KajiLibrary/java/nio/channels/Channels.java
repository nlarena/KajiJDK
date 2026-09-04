package java.nio.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * KajiLibrary's java.nio.channels.Channels — el puente entre `java.io` y `java.nio.channels`.
 *
 * <p>Existe porque las dos jerarquias nunca se unificaron: media biblioteca habla `InputStream` y la
 * otra media habla `ReadableByteChannel`, y sin este puente cada quien copiaria a mano el mismo lazo
 * de `byte[]` a `ByteBuffer`. Todo lo de aca es adaptacion pura --no abre nada, no crea recursos--
 * y por eso **esta implementado entero y de verdad**, a diferencia del resto del paquete: recibe
 * algo que ya funciona y lo devuelve con la otra cara puesta.
 *
 * <h2>Lo unico que hay que saber antes de usarlo</h2>
 *
 * <p>Los adaptadores **no bufferean**: una lectura sobre el `InputStream` que envuelve un canal es
 * una lectura sobre el canal. Es a proposito --meter un buffer cambiaria cuantos bytes pide el de
 * abajo y cuando, y eso sobre un canal se nota-- pero significa que leer de a un byte de uno de
 * estos es tan caro como leer de a un byte del canal. Envolver en `BufferedInputStream` es la
 * respuesta, y es decision de quien llama, no de aca.
 *
 * <p>Cerrar el adaptador **cierra lo adaptado**. Es lo que el JDK hace y lo que hay que hacer: el
 * adaptador no tiene recursos propios, asi que si su `close()` no cerrara el de abajo no cerraria
 * absolutamente nada y seria una trampa silenciosa.
 *
 * <h2>Excepciones: la misma torcedura de siempre</h2>
 *
 * <p>`java.io.InputStream.read()` de esta biblioteca **no declara `IOException`** (ver la nota de
 * `java.io.IOException`), pero {@link ReadableByteChannel#read} si. Al ir de canal a stream la
 * excepcion no tiene por donde salir, asi que sale envuelta en {@link UncheckedIOException}: el
 * motivo no se pierde, cambia de tipo. En el sentido contrario --de stream a canal-- no hay
 * problema, porque estrechar lo que se declara siempre es legal.
 *
 * <h2>Lo que quedo afuera</h2>
 *
 * <p><strong>Nada.</strong> Los doce metodos publicos del JDK 25 estan. Los dos que toman
 * {@link AsynchronousByteChannel} tambien, y no contradicen que este paquete no sepa **fabricar**
 * canales asincronicos: estos no fabrican ninguno, adaptan el que les den. Si alguien consigue uno,
 * envolverlo funciona.
 */
public final class Channels {

    // Sin instancias: es un cajon de estaticos.
    private Channels() {
        throw new AssertionError("no instanciar");
    }

    // ---- de canal a stream -----------------------------------------------------------------------

    /**
     * Un {@link InputStream} que lee de `ch`.
     *
     * <p>Sin buffer; ver la nota de la clase.
     */
    public static InputStream newInputStream(ReadableByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new StreamDeCanal(ch);
    }

    /** Un {@link OutputStream} que escribe en `ch`. Sin buffer; ver la nota de la clase. */
    public static OutputStream newOutputStream(WritableByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new StreamACanal(ch);
    }

    /**
     * Un {@link InputStream} que lee de un canal asincronico, **bloqueando**.
     *
     * <p>Que la fuente sea asincronica no cambia que un `InputStream` sea sincronico: cada `read`
     * lanza la operacion y espera su resultado. Sirve para pasarle un canal asincronico a codigo
     * que solo sabe de streams, no para ganar concurrencia.
     */
    public static InputStream newInputStream(AsynchronousByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new StreamDeCanalAsinc(ch);
    }

    /** Un {@link OutputStream} que escribe en un canal asincronico, bloqueando en cada `write`. */
    public static OutputStream newOutputStream(AsynchronousByteChannel ch) {
        if (ch == null) {
            throw new NullPointerException();
        }
        return new StreamACanalAsinc(ch);
    }

    // ---- de stream a canal -----------------------------------------------------------------------

    /**
     * Un {@link ReadableByteChannel} que lee de `in`.
     *
     * <p>El canal que sale **no es interrumpible ni selectable**, y no por omision: un
     * `InputStream` no ofrece manera de abortar una lectura empezada, asi que prometer
     * {@link InterruptibleChannel} seria prometer algo que el de abajo no puede dar.
     */
    public static ReadableByteChannel newChannel(InputStream in) {
        if (in == null) {
            throw new NullPointerException();
        }
        return new CanalDeStream(in);
    }

    /** Un {@link WritableByteChannel} que escribe en `out`. Mismas salvedades que el de lectura. */
    public static WritableByteChannel newChannel(OutputStream out) {
        if (out == null) {
            throw new NullPointerException();
        }
        return new CanalAStream(out);
    }

    // ---- de canal a texto ------------------------------------------------------------------------

    /**
     * Un {@link Reader} que decodifica lo que sale de `ch` con `dec`.
     *
     * <p>`minBufferCap` es un **piso sugerido** para el buffer interno, no un tamanio exacto ni una
     * garantia; `-1` pide el que la implementacion prefiera. Aca se acepta y se ignora: el buffer lo
     * elige `InputStreamReader`, y fingir que se lo respeta no cambiaria nada salvo la creencia de
     * quien lee el codigo.
     */
    public static Reader newReader(ReadableByteChannel ch, CharsetDecoder dec, int minBufferCap) {
        if (ch == null || dec == null) {
            throw new NullPointerException();
        }
        return new InputStreamReader(newInputStream(ch), dec);
    }

    /** Como el otro, con el juego de caracteres nombrado. */
    public static Reader newReader(ReadableByteChannel ch, String csName) {
        if (csName == null) {
            throw new NullPointerException();
        }
        return newReader(ch, Charset.forName(csName));
    }

    /**
     * Como el otro, con `charset`.
     *
     * <p>Los bytes malformados se **reemplazan**, no hacen fallar la lectura: es lo que hace el
     * decodificador por omision y lo que el JDK especifica para esta forma.
     */
    public static Reader newReader(ReadableByteChannel ch, Charset charset) {
        if (ch == null || charset == null) {
            throw new NullPointerException();
        }
        return new InputStreamReader(newInputStream(ch), charset);
    }

    /** Un {@link Writer} que codifica con `enc` hacia `ch`. `minBufferCap`, como en `newReader`. */
    public static Writer newWriter(WritableByteChannel ch, CharsetEncoder enc, int minBufferCap) {
        if (ch == null || enc == null) {
            throw new NullPointerException();
        }
        return new OutputStreamWriter(newOutputStream(ch), enc);
    }

    /** Como el otro, con el juego de caracteres nombrado. */
    public static Writer newWriter(WritableByteChannel ch, String csName) {
        if (csName == null) {
            throw new NullPointerException();
        }
        return newWriter(ch, Charset.forName(csName));
    }

    /** Como el otro, con `cs`. */
    public static Writer newWriter(WritableByteChannel ch, Charset cs) {
        if (ch == null || cs == null) {
            throw new NullPointerException();
        }
        return new OutputStreamWriter(newOutputStream(ch), cs);
    }

    // ---- adaptadores -----------------------------------------------------------------------------

    // Un stream sobre un canal. `read(byte[],int,int)` es el que hace el trabajo y `read()` se
    // apoya en el: al reves --uno a uno-- cada byte costaria una llamada al canal.
    private static final class StreamDeCanal extends InputStream {

        private final ReadableByteChannel canal;
        private final byte[] uno = new byte[1];

        StreamDeCanal(ReadableByteChannel canal) {
            this.canal = canal;
        }

        public int read() {
            int n = this.read(this.uno, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return this.uno[0] & 0xff;
        }

        public int read(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            try {
                return this.canal.read(ByteBuffer.wrap(b, off, len));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public int available() {
            // Un canal no sabe decir cuanto tiene listo, y `0` es la respuesta honesta: el contrato
            // de `available` es "cuantos se pueden leer sin bloquear", y de eso aca no hay dato.
            return 0;
        }

        public void close() throws java.io.IOException {
            this.canal.close();
        }
    }

    private static final class StreamACanal extends OutputStream {

        private final WritableByteChannel canal;
        private final byte[] uno = new byte[1];

        StreamACanal(WritableByteChannel canal) {
            this.canal = canal;
        }

        public void write(int b) {
            this.uno[0] = (byte) b;
            this.write(this.uno, 0, 1);
        }

        public void write(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            ByteBuffer bb = ByteBuffer.wrap(b, off, len);
            try {
                // El lazo no es defensivo de mas: un canal puede escribir menos de lo que se le da,
                // y `OutputStream.write` promete que escribe todo. Sin el lazo, esa promesa es falsa
                // justo en el caso raro, que es el peor lugar donde puede estarlo.
                while (bb.hasRemaining()) {
                    int n = this.canal.write(bb);
                    if (n <= 0 && bb.hasRemaining()) {
                        throw new IOException("el canal no acepta mas bytes");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.canal.close();
        }
    }

    // Un canal sobre un stream. Se aprovecha el arreglo del buffer cuando lo tiene: sin eso habria
    // que copiar dos veces --stream a temporal, temporal a buffer-- para nada.
    private static final class CanalDeStream implements ReadableByteChannel {

        private final InputStream fuente;
        private boolean abierto = true;

        CanalDeStream(InputStream fuente) {
            this.fuente = fuente;
        }

        public int read(ByteBuffer dst) throws IOException {
            if (!this.abierto) {
                throw new ClosedChannelException();
            }
            int libres = dst.remaining();
            if (libres == 0) {
                return 0;
            }
            if (dst.hasArray() && !dst.isReadOnly()) {
                int base = dst.arrayOffset() + dst.position();
                int n = this.fuente.read(dst.array(), base, libres);
                if (n > 0) {
                    dst.position(dst.position() + n);
                }
                return n;
            }
            byte[] tmp = new byte[libres];
            int n = this.fuente.read(tmp, 0, libres);
            if (n > 0) {
                dst.put(tmp, 0, n);
            }
            return n;
        }

        public boolean isOpen() {
            return this.abierto;
        }

        public void close() throws java.io.IOException {
            if (this.abierto) {
                this.abierto = false;
                this.fuente.close();
            }
        }
    }

    private static final class CanalAStream implements WritableByteChannel {

        private final OutputStream destino;
        private boolean abierto = true;

        CanalAStream(OutputStream destino) {
            this.destino = destino;
        }

        public int write(ByteBuffer src) throws IOException {
            if (!this.abierto) {
                throw new ClosedChannelException();
            }
            int n = src.remaining();
            if (n == 0) {
                return 0;
            }
            if (src.hasArray()) {
                int base = src.arrayOffset() + src.position();
                this.destino.write(src.array(), base, n);
            } else {
                byte[] tmp = new byte[n];
                src.get(src.position(), tmp, 0, n);
                this.destino.write(tmp, 0, n);
            }
            // Un `OutputStream` escribe todo o tira, asi que llegar aca significa que entraron los
            // `n`: la posicion avanza entera y no hay escritura parcial que reportar.
            src.position(src.position() + n);
            return n;
        }

        public boolean isOpen() {
            return this.abierto;
        }

        public void close() throws java.io.IOException {
            if (this.abierto) {
                this.abierto = false;
                this.destino.close();
            }
        }
    }

    // Los dos asincronicos comparten la manera de esperar, que es la parte delicada: una
    // `ExecutionException` esconde la causa real y devolverla tal cual haria perder el motivo.
    private static int esperar(Future<Integer> f) throws IOException {
        try {
            Integer n = f.get();
            if (n == null) {
                return -1;
            }
            return n.intValue();
        } catch (InterruptedException e) {
            // Se repone la marca antes de salir: tragarse una interrupcion deja al hilo creyendo que
            // nunca lo interrumpieron, y el que decide que hacer con eso esta mas arriba.
            Thread.currentThread().interrupt();
            throw new ClosedByInterruptException();
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof IOException) {
                throw (IOException) causa;
            }
            if (causa instanceof RuntimeException) {
                throw (RuntimeException) causa;
            }
            if (causa instanceof Error) {
                throw (Error) causa;
            }
            throw new IOException(causa);
        }
    }

    private static final class StreamDeCanalAsinc extends InputStream {

        private final AsynchronousByteChannel canal;
        private final byte[] uno = new byte[1];

        StreamDeCanalAsinc(AsynchronousByteChannel canal) {
            this.canal = canal;
        }

        public int read() {
            int n = this.read(this.uno, 0, 1);
            if (n <= 0) {
                return -1;
            }
            return this.uno[0] & 0xff;
        }

        public int read(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            if (len == 0) {
                return 0;
            }
            try {
                return esperar(this.canal.read(ByteBuffer.wrap(b, off, len)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.canal.close();
        }
    }

    private static final class StreamACanalAsinc extends OutputStream {

        private final AsynchronousByteChannel canal;
        private final byte[] uno = new byte[1];

        StreamACanalAsinc(AsynchronousByteChannel canal) {
            this.canal = canal;
        }

        public void write(int b) {
            this.uno[0] = (byte) b;
            this.write(this.uno, 0, 1);
        }

        public void write(byte[] b, int off, int len) {
            if (b == null) {
                throw new NullPointerException();
            }
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            ByteBuffer bb = ByteBuffer.wrap(b, off, len);
            try {
                while (bb.hasRemaining()) {
                    int n = esperar(this.canal.write(bb));
                    if (n <= 0 && bb.hasRemaining()) {
                        throw new IOException("el canal no acepta mas bytes");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public void close() throws java.io.IOException {
            this.canal.close();
        }
    }
}
