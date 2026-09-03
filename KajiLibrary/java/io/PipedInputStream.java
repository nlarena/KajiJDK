package java.io;

// KajiLibrary's java.io.PipedInputStream -- la punta de lectura de una tuberia entre dos hilos.
//
// ===============================================================================================
// QUE ES Y POR QUE SE PUEDE ESCRIBIR ENTERA
// ===============================================================================================
//
// Un buffer circular con espera. No toca el disco ni la red ni el sistema operativo: lo unico que
// necesita es un monitor, y `wait`/`notifyAll` ya funcionan en esta VM. Por eso esta clase se
// implementa completa y sin concesiones, a diferencia de los streams de archivo.
//
// El buffer es circular con la convencion clasica: `out` es el proximo byte a leer, `in` el proximo
// lugar a escribir, y **`in == -1` significa vacio**. Hace falta ese valor centinela porque con
// solo dos indices `in == out` seria ambiguo --lleno y vacio se verian igual-- y la alternativa
// (dejar un lugar sin usar) desperdiciaria espacio. Con el centinela, `in == out` significa
// **lleno** sin ambiguedad.
//
// ===============================================================================================
// LAS ESPERAS Y LOS AVISOS
// ===============================================================================================
//
// Igual que el JDK: se espera con `wait(1000)` y se avisa con `notifyAll()`. El plazo no es para
// no perderse un aviso --los avisos estan todos-- sino para poder **revisar si el hilo del otro
// lado sigue vivo**: un extremo cuyo hilo se muere sin cerrar no manda ningun aviso, y sin
// despertarse cada tanto el otro se quedaria esperando para siempre en vez de romper con
// "Pipe broken".
//
// El `notifyAll()` esta en cada punto donde **cambia el estado**: despues de dejar datos (los dos
// `receive`), despues de consumir (los dos `read`), al esperar lugar y al cerrar. El de `receive`
// es el que importa y el que faltaba: sin el, el lector solo se enteraba de que habia datos cuando
// vencia su propio plazo, y una transferencia grande se clavaba con el escritor sin poder entrar al
// monitor. Se arreglo junto con el bug de la VM que lo tapaba --ver el finding #471, donde un
// `wait` con plazo vencido dejaba al hilo dentro del conjunto de espera del monitor--.
//
// **Las excepciones son las del JDK y son chequeadas.** Esto no siempre fue asi: mientras
// `InputStream.read()` no declaraba `throws IOException`, un override tampoco podia --JLS 8.4.8.3
// prohibe ensanchar las chequeadas-- y "Pipe not connected", "Pipe broken" y "Write end dead"
// salian envueltos en `UncheckedIOException`. La base ya lo declara, asi que el envoltorio se fue:
// dejarlo era lo peor de los dos mundos, una firma que promete `IOException` y un cuerpo que tira
// algo que ningun `catch (IOException)` agarra.
public class PipedInputStream extends InputStream {

    boolean closedByWriter = false;

    volatile boolean closedByReader = false;

    boolean connected = false;

    // Quien lee y quien escribe, para poder preguntar si el del otro lado sigue vivo. Es la unica
    // forma de distinguir "todavia no escribio nada" de "se murio y no va a escribir nunca".
    Thread readSide;
    Thread writeSide;

    private static final int DEFAULT_PIPE_SIZE = 1024;

    /** El tamano del buffer cuando no se pide otro. */
    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

    /** El buffer circular. */
    protected byte[] buffer;

    /** Donde se escribe el proximo byte; **-1 si la tuberia esta vacia**. */
    protected int in = -1;

    /** De donde se lee el proximo byte. */
    protected int out = 0;

    /** Conecta esta punta a `src` con el buffer del tamano por omision. */
    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Conecta esta punta a `src`.
     *
     * @param pipeSize el tamano del buffer
     * @throws IllegalArgumentException si `pipeSize` no es positivo
     */
    public PipedInputStream(PipedOutputStream src, int pipeSize) throws IOException {
        this.iniciarBuffer(pipeSize);
        this.connect(src);
    }

    /** Sin conectar: hace falta un `connect` antes de usarla. */
    public PipedInputStream() {
        this.iniciarBuffer(DEFAULT_PIPE_SIZE);
    }

    /**
     * Sin conectar, con el buffer del tamano dado.
     *
     * @throws IllegalArgumentException si `pipeSize` no es positivo
     */
    public PipedInputStream(int pipeSize) {
        this.iniciarBuffer(pipeSize);
    }

    private void iniciarBuffer(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        this.buffer = new byte[pipeSize];
    }

    /**
     * Conecta esta punta al `PipedOutputStream` dado.
     *
     * @throws IOException si alguna de las dos puntas ya estaba conectada
     */
    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    /**
     * Recibe un byte del escritor. **Bloquea si la tuberia esta llena.**
     *
     * @throws IOException si la tuberia esta rota, cerrada o sin conectar
     */
    protected synchronized void receive(int b) throws IOException {
        this.revisarParaRecibir();
        this.writeSide = Thread.currentThread();
        if (this.in == this.out) {
            this.esperarLugar();
        }
        if (this.in < 0) {
            this.in = 0;
            this.out = 0;
        }
        this.buffer[this.in] = (byte) (b & 0xFF);
        this.in = this.in + 1;
        if (this.in >= this.buffer.length) {
            this.in = 0;
        }
        // El buffer paso de vacio a con algo: hay que despertar al lector. Sin este aviso el
        // lector solo se entera cuando vence su propio plazo, y con esperas sin plazo no se
        // entera nunca.
        this.notifyAll();
    }

    // Recibe un bloque. Es de paquete porque solo `PipedOutputStream` la llama.
    synchronized void receive(byte[] b, int off, int len) throws IOException {
        this.revisarParaRecibir();
        this.writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        int desde = off;
        while (bytesToTransfer > 0) {
            if (this.in == this.out) {
                this.esperarLugar();
            }
            int nextTransferAmount = 0;
            if (this.out < this.in) {
                // Los datos estan en un tramo: el lugar libre va desde `in` hasta el final.
                nextTransferAmount = this.buffer.length - this.in;
            } else if (this.in < this.out) {
                if (this.in == -1) {
                    // Vacia: se puede escribir todo el buffer desde el principio.
                    this.in = 0;
                    this.out = 0;
                    nextTransferAmount = this.buffer.length - this.in;
                } else {
                    // El lugar libre va desde `in` hasta `out`.
                    nextTransferAmount = this.out - this.in;
                }
            }
            if (nextTransferAmount > bytesToTransfer) {
                nextTransferAmount = bytesToTransfer;
            }
            System.arraycopy(b, desde, this.buffer, this.in, nextTransferAmount);
            bytesToTransfer = bytesToTransfer - nextTransferAmount;
            desde = desde + nextTransferAmount;
            this.in = this.in + nextTransferAmount;
            if (this.in >= this.buffer.length) {
                this.in = 0;
            }
            // Uno por tramo y no uno al final: si el bloque no entra de una, el lector tiene que
            // poder consumir lo que ya hay para hacer lugar al resto.
            this.notifyAll();
        }
    }

    private void revisarParaRecibir() throws IOException {
        if (!this.connected) {
            throw new IOException("Pipe not connected");
        }
        if (this.closedByWriter || this.closedByReader) {
            throw new IOException("Pipe closed");
        }
        if (this.readSide != null && !this.readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }

    // Espera a que el lector haga lugar. El `notifyAll` de adentro no es de cortesia: si el lector
    // esta dormido esperando datos y el escritor esta dormido esperando lugar, alguien tiene que
    // despertar al otro.
    private void esperarLugar() throws IOException {
        while (this.in == this.out) {
            this.revisarParaRecibir();
            this.notifyAll();
            try {
                this.wait(1000);
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
        }
    }

    // Avisa que el escritor cerro. Lo que quede en el buffer todavia se puede leer: recien cuando
    // se vacie el lector va a ver el fin de stream.
    synchronized void receivedLast() {
        this.closedByWriter = true;
        this.notifyAll();
    }

    /**
     * Lee un byte. **Bloquea hasta que haya uno**, o hasta que el escritor cierre.
     *
     * @return el byte, o -1 si se acabo
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
        // Dos vueltas de gracia antes de declarar rota la tuberia: el escritor puede haber
        // terminado justo despues de dejar datos, y en ese caso hay que entregarlos.
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
        int ret = this.buffer[this.out] & 0xFF;
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
     * Lee hasta `len` bytes. Bloquea hasta que haya **al menos uno**, y despues devuelve lo que
     * haya sin esperar a llenar el arreglo.
     *
     * @return cuantos se leyeron, o -1 si se acabo
     */
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        // El primero va por `read()`, que es el que espera y el que reporta el fin de stream.
        int c = this.read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        int rlen = 1;
        // A partir de aca no se espera mas: `in >= 0` mientras quede algo en el buffer.
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
            System.arraycopy(this.buffer, this.out, b, off + rlen, available);
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

    /** Cuantos bytes se pueden leer sin bloquear. */
    public synchronized int available() throws IOException {
        if (this.in < 0) {
            return 0;
        }
        if (this.in == this.out) {
            return this.buffer.length;
        }
        if (this.in > this.out) {
            return this.in - this.out;
        }
        return this.in + this.buffer.length - this.out;
    }

    /** Cierra la punta de lectura. El escritor que siga escribiendo va a fallar. */
    public void close() throws IOException {
        this.closedByReader = true;
        synchronized (this) {
            this.in = -1;
        }
    }
}
