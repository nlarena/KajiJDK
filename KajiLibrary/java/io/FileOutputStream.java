package java.io;

import jdk.internal.io.Fs;

// KajiLibrary's java.io.FileOutputStream -- un stream de bytes que se escriben a un archivo.
//
// **Lo escrito se acumula en memoria y va al disco en cada `flush`, `close` o cuando el buffer se
// llena.** Es la contracara de `FileInputStream`, por la misma razon: el nativo escribe el archivo
// entero de una porque esta VM no tiene descriptores abiertos.
//
// Dos consecuencias que conviene saber:
//
//   - **hay que cerrar o vaciar.** Un `FileOutputStream` que se abandona sin `close()` pierde lo que
//     quedaba en el buffer. En el JDK tambien conviene cerrar, pero ahi el sistema operativo termina
//     escribiendo lo que ya se le entrego; aca no hay nada entregado hasta el `flush`.
//   - el archivo se **trunca al construir** (salvo en modo `append`), como el del JDK: abrir para
//     escribir borra lo que habia, aunque despues no se escriba nada.
//
// **`getChannel()` comparte la posicion con el flujo**, como manda el contrato: escribir por uno
// mueve al otro, y mover el canal cambia donde escribe el flujo. Pedirlo tiene un precio, y por eso
// no se paga hasta que alguien lo pide: a partir de ahi el volcado deja de ser un agregado al final
// y pasa a escribir en la posicion del canal, que en esta VM cuesta reescribir el archivo entero.
// Quien nunca llame a `getChannel()` escribe exactamente como antes. El como esta en `Canales`.
//
// **Los errores salen como `IOException` chequeada**, igual que en el JDK. Hubo una epoca en que
// no: las bases del paquete (`InputStream`/`OutputStream`/`Closeable`) no declaraban `throws
// IOException`, un override no puede ensanchar las chequeadas de lo que sobreescribe (JLS 8.4.8.3),
// y lo que aca fallaba salia envuelto en `UncheckedIOException`. Las bases ya lo declaran, asi que
// el envoltorio se saco: un `catch (IOException e)` de quien llama tiene que agarrar esto, y con la
// no chequeada le pasaba por al lado y le mataba el hilo.
public class FileOutputStream extends OutputStream {

    // Mas alla de esto se vuelca solo, para que escribir un archivo grande no lo tenga entero dos
    // veces en memoria. No cambia lo que se ve: el archivo queda igual.
    private static final int LIMITE = 1 << 16;

    // Package-private y no privados: `Canales.DeSalida` **comparte** estos con este flujo, que es
    // de lo que se trata `getChannel()`. Ver la cabecera de `Canales`.
    final String ruta;
    byte[] buf = new byte[256];
    int usados = 0;
    boolean cerrado = false;

    /** El canal de este flujo, creado a pedido. Uno solo: el contrato dice "the unique object". */
    private Canales.DeSalida canal;
    // Si lo que viene se agrega a lo ya volcado. Arranca en el modo pedido y pasa a `true` despues
    // del primer volcado: el segundo trozo tiene que agregarse aunque el stream no sea de append.
    private boolean anexar;

    /**
     * Abre `name` para escritura, **truncando** lo que hubiera.
     *
     * @throws FileNotFoundException si no se puede escribir ahi
     */
    public FileOutputStream(String name) throws FileNotFoundException {
        this(name == null ? null : new File(name), false);
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        this(name == null ? null : new File(name), append);
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    /**
     * Abre `file` para escritura.
     *
     * @param append si lo escrito se agrega al final en vez de reemplazar el contenido
     * @throws FileNotFoundException si es un directorio, o no se puede escribir ahi
     */
    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        }
        this.ruta = file.getPath();
        this.anexar = append;
        if (!append) {
            // Truncar **ahora**, no en el primer write: abrir para escribir borra lo que habia
            // aunque despues no se escriba nada, y es lo que hace el del JDK.
            if (!Fs.writeAllBytes(this.ruta, new byte[0], false)) {
                throw new FileNotFoundException(this.ruta + " (Permission denied)");
            }
            this.anexar = true;
        }
    }

    /** Abre por descriptor. Esta biblioteca no modela descriptores; ver la nota de la clase. */
    public FileOutputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.ruta = null;
        this.anexar = true;
    }

    public void write(int b) throws IOException {
        this.comprobarAbierto();
        this.asegurar(1);
        this.buf[this.usados] = (byte) b;
        this.usados = this.usados + 1;
        if (this.usados >= LIMITE) {
            this.flush();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.comprobarAbierto();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        this.asegurar(len);
        System.arraycopy(b, off, this.buf, this.usados, len);
        this.usados = this.usados + len;
        if (this.usados >= LIMITE) {
            this.flush();
        }
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    /** Vuelca al disco lo que haya en el buffer. */
    public void flush() throws IOException {
        if (this.usados == 0 || this.ruta == null) {
            return;
        }
        if (this.canal != null) {
            // Ya hay canal: el que manda es **su** posicion y no el final del archivo, asi que el
            // volcado tiene que pasar por el. Si no, un `position()` hacia atras seguido de un
            // `write` del flujo agregaria al final en vez de escribir donde se pidio.
            this.canal.vaciarPendiente();
            return;
        }
        byte[] trozo = new byte[this.usados];
        System.arraycopy(this.buf, 0, trozo, 0, this.usados);
        if (!Fs.writeAllBytes(this.ruta, trozo, this.anexar)) {
            throw new IOException("Could not write to " + this.ruta);
        }
        this.anexar = true;
        this.usados = 0;
    }

    public void close() throws IOException {
        if (!this.cerrado) {
            this.flush();
            this.cerrado = true;
        }
        // Cerrar el flujo cierra su canal: son la misma cosa vista de dos maneras.
        if (this.canal != null && this.canal.isOpen()) {
            this.canal.close();
        }
    }

    /**
     * El canal de este flujo, **con la misma posicion**: escribir por el flujo mueve el canal y
     * mover el canal cambia donde escribe el flujo. El porque de que sea un solo numero, y el precio
     * de pedirlo, estan en `Canales`.
     *
     * <p>Pedirlo cambia como vuelca este flujo: de agregar al final pasa a escribir en la posicion
     * del canal, que en esta VM cuesta reescribir el archivo entero. Quien no lo pida escribe como
     * antes.
     */
    public java.nio.channels.FileChannel getChannel() {
        if (this.canal == null) {
            this.canal = new Canales.DeSalida(this);
        }
        return this.canal;
    }

    /** El descriptor. Esta biblioteca no los modela; ver la nota de la clase. */
    public final FileDescriptor getFD() throws IOException {
        return new FileDescriptor();
    }

    private void asegurar(int extra) {
        if (this.usados + extra <= this.buf.length) {
            return;
        }
        int nuevo = this.buf.length * 2;
        while (nuevo < this.usados + extra) {
            nuevo = nuevo * 2;
        }
        byte[] mas = new byte[nuevo];
        System.arraycopy(this.buf, 0, mas, 0, this.usados);
        this.buf = mas;
    }

    private void comprobarAbierto() throws IOException {
        if (this.cerrado) {
            throw new IOException("Stream Closed");
        }
    }
}
