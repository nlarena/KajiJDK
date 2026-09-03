package java.io;

// KajiLibrary's java.io.PipedReader -- la punta de lectura de una tuberia de **caracteres**.
//
// El mismo buffer circular que `PipedInputStream` con `char` en vez de `byte`, y por la misma razon
// que existen `Reader` y `InputStream` por separado: pasar texto por una tuberia de bytes obliga a
// codificar de un lado y decodificar del otro, y si los dos extremos son Java eso es trabajo puro
// --y una fuente de caracteres partidos al medio cuando un caracter multibyte cae justo en el
// borde del buffer.
//
// Todo lo dicho en `PipedInputStream` vale igual: el centinela `in == -1` para distinguir vacio de
// lleno, las esperas con plazo de un segundo para poder detectar que el otro extremo se murio, y
// la sincronizacion concentrada de este lado.
//
// Las excepciones son las del JDK y chequeadas, como en `PipedInputStream`; ver la nota de alla
// por que durante un tiempo no lo fueron.
public class PipedReader extends Reader {

    boolean closedByWriter = false;

    boolean closedByReader = false;

    boolean connected = false;

    Thread readSide;
    Thread writeSide;

    private static final int DEFAULT_PIPE_SIZE = 1024;

    char[] buffer;

    /** Donde se escribe el proximo caracter; **-1 si la tuberia esta vacia**. */
    int in = -1;

    /** De donde se lee el proximo caracter. */
    int out = 0;

    /** Conecta esta punta a `src` con el buffer del tamano por omision. */
    public PipedReader(PipedWriter src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Conecta esta punta a `src`.
     *
     * @throws IllegalArgumentException si `pipeSize` no es positivo
     */
    public PipedReader(PipedWriter src, int pipeSize) throws IOException {
        this.iniciarBuffer(pipeSize);
        this.connect(src);
    }

    /** Sin conectar: hace falta un `connect` antes de usarla. */
    public PipedReader() {
        this.iniciarBuffer(DEFAULT_PIPE_SIZE);
    }

    /**
     * Sin conectar, con el buffer del tamano dado.
     *
     * @throws IllegalArgumentException si `pipeSize` no es positivo
     */
    public PipedReader(int pipeSize) {
        this.iniciarBuffer(pipeSize);
    }

    private void iniciarBuffer(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe size <= 0");
        }
        this.buffer = new char[pipeSize];
    }

    /**
     * Conecta esta punta al `PipedWriter` dado.
     *
     * @throws IOException si alguna de las dos puntas ya estaba conectada
     */
    public void connect(PipedWriter src) throws IOException {
        src.connect(this);
    }

    /** Recibe un caracter del escritor. **Bloquea si la tuberia esta llena.** */
    synchronized void receive(int c) throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }

        this.writeSide = Thread.currentThread();
        while (this.in == this.out) {
            if (this.readSide != null && !this.readSide.isAlive()) {
                throw new IOException("Pipe broken");
            }
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        this.buffer[this.in] = (char) c;
        this.in = this.in + 1;
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
    }

    synchronized void receive(char[] c, int off, int len) throws IOException {
        int desde = off;
        int quedan = len;
        while (quedan > 0) {
            this.receive(c[desde]);
            desde = desde + 1;
            quedan = quedan - 1;
        }
    }

    synchronized void receivedLast() {
        this.closedByWriter = true;
        this.notifyAll();
    }

    /**
     * Lee un caracter. **Bloquea hasta que haya uno**, o hasta que el escritor cierre.
     *
     * @return el caracter, o -1 si se acabo
     */
    public synchronized int read() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.writeSide != null && !this.writeSide.isAlive()
                && !this.closedByWriter && this.in < 0) {
            throw new IOException("Write end dead");
        }

        this.readSide = Thread.currentThread();
        int trials = 2;
        while (this.in < 0) {
            if (this.closedByWriter) {
                return -1;
            }
            if (this.writeSide != null && !this.writeSide.isAlive()) {
                trials = trials - 1;
                if (trials < 0) {
                    throw new IOException("Pipe broken");
                }
            }
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
        int ret = this.buffer[this.out];
        this.out = this.out + 1;
        if (this.out >= this.buffer.length) {
            this.out = 0;
        }
        if (this.in == this.out) {
            this.in = -1;
        }
        return ret;
    }

    /**
     * Lee hasta `len` caracteres. Bloquea hasta que haya **al menos uno**.
     *
     * @return cuantos se leyeron, o -1 si se acabo
     */
    public synchronized int read(char[] cbuf, int off, int len) throws IOException {
        if (cbuf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > cbuf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        int c = this.read();
        if (c < 0) {
            return -1;
        }
        cbuf[off] = (char) c;
        int rlen = 1;
        while (this.in >= 0 && rlen < len) {
            int available;
            if (this.in > this.out) {
                available = this.in - this.out;
            } else {
                available = this.buffer.length - this.out;
            }
            if (available > len - rlen) {
                available = len - rlen;
            }
            System.arraycopy(this.buffer, this.out, cbuf, off + rlen, available);
            this.out = this.out + available;
            rlen = rlen + available;
            if (this.out >= this.buffer.length) {
                this.out = 0;
            }
            if (this.in == this.out) {
                this.in = -1;
            }
        }
        return rlen;
    }

    /** Si hay al menos un caracter listo para leer sin bloquear. */
    public synchronized boolean ready() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        return this.in >= 0;
    }

    /** Cierra la punta de lectura. */
    public void close() throws IOException {
        this.in = -1;
        this.closedByReader = true;
    }
}
