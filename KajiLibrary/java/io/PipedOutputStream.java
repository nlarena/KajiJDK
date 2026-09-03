package java.io;

// KajiLibrary's java.io.PipedOutputStream -- la punta de escritura de una tuberia entre dos hilos.
//
// Casi no tiene estado propio: guarda a quien le escribe y le pasa todo. El buffer, la espera y la
// sincronizacion viven del lado del lector (`PipedInputStream`), que es donde tienen que estar --
// hay un solo buffer y un solo monitor, y ponerlos de un solo lado es lo que evita tener que
// tomar dos candados y ordenarlos.
//
// **Las dos puntas tienen que estar en hilos distintos.** Escribir y leer desde el mismo hilo se
// cuelga en cuanto el buffer se llena: el escritor espera lugar, y el unico que podria hacerlo es
// el mismo hilo que esta esperando. No es un defecto de esta implementacion, es lo que una tuberia
// es.
//
// Las excepciones son las del JDK y chequeadas, como en `PipedInputStream`; ver la nota de alla
// por que durante un tiempo no lo fueron.
public class PipedOutputStream extends OutputStream {

    private PipedInputStream sink;

    /** Conecta esta punta al lector dado. */
    public PipedOutputStream(PipedInputStream snk) throws IOException {
        this.connect(snk);
    }

    /** Sin conectar: hace falta un `connect` antes de escribir. */
    public PipedOutputStream() {
    }

    /**
     * Conecta esta punta al lector dado y deja la tuberia vacia.
     *
     * @throws IOException si alguna de las dos puntas ya estaba conectada
     */
    public synchronized void connect(PipedInputStream snk) throws IOException {
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

    /** Escribe un byte. **Bloquea si la tuberia esta llena.** */
    public void write(int b) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        this.sink.receive(b);
    }

    /** Escribe `len` bytes. **Bloquea hasta que entren todos.** */
    public void write(byte[] b, int off, int len) throws IOException {
        if (this.sink == null) {
            throw new IOException("Pipe not connected");
        }
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        this.sink.receive(b, off, len);
    }

    // No vacia nada --no hay nada guardado de este lado-- sino que **despierta al lector**. Sirve
    // cuando el escritor dejo datos y quiere que el otro los vea ya, sin esperar el plazo de un
    // segundo del `wait`.
    public synchronized void flush() throws IOException {
        if (this.sink != null) {
            synchronized (this.sink) {
                this.sink.notifyAll();
            }
        }
    }

    /**
     * Cierra la punta de escritura.
     *
     * <p>El lector **no** ve el fin de stream enseguida: primero termina de leer lo que quedo en el
     * buffer, y recien cuando se vacia le llega el -1. Cerrar no descarta lo escrito.
     */
    public void close() throws IOException {
        if (this.sink != null) {
            this.sink.receivedLast();
        }
    }
}
