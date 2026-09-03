package java.io;

// KajiLibrary's java.io.PipedWriter -- la punta de escritura de una tuberia de caracteres.
//
// El espejo de `PipedOutputStream`: casi sin estado propio, todo el trabajo del lado del
// `PipedReader`. Las dos puntas tienen que estar en hilos distintos, por la misma razon de siempre
// --escribir hasta llenar el buffer desde el mismo hilo que tendria que vaciarlo se cuelga.
//
// Las excepciones son las del JDK y chequeadas, como en `PipedInputStream`; ver la nota de alla
// por que durante un tiempo no lo fueron.
public class PipedWriter extends Writer {

    private PipedReader sink;

    private boolean closed = false;

    /** Conecta esta punta al lector dado. */
    public PipedWriter(PipedReader snk) throws IOException {
        this.connect(snk);
    }

    /** Sin conectar: hace falta un `connect` antes de escribir. */
    public PipedWriter() {
    }

    /**
     * Conecta esta punta al lector dado y deja la tuberia vacia.
     *
     * @throws IOException si alguna de las dos puntas ya estaba conectada
     */
    public synchronized void connect(PipedReader snk) throws IOException {
        if (snk == null) {
            throw new NullPointerException();
        }
        if (this.sink != null || snk.connected) {
            throw new IOException("Already connected");
        }
        this.sink = snk;
        snk.in = -1;
        snk.out = 0;
        snk.connected = true;
    }

    /** Escribe un caracter. **Bloquea si la tuberia esta llena.** */
    public void write(int c) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(c);
    }

    /** Escribe `len` caracteres. **Bloquea hasta que entren todos.** */
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        if (cbuf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > cbuf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        this.sink.receive(cbuf, off, len);
    }

    // Despierta al lector; no hay nada guardado de este lado que vaciar.
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            if (this.sink.closedByReader || this.closed) {
                throw new IOException("Pipe closed");
            }
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    /**
     * Cierra la punta de escritura.
     *
     * <p>El lector termina de leer lo que quedo en el buffer antes de ver el fin de stream.
     */
    public void close() throws IOException {
        this.closed = true;
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
