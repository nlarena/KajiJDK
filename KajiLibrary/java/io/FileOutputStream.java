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
// **Sin `throws IOException`**, y no por descuido: las bases de este paquete
// (`InputStream`/`OutputStream`/`Closeable`) se escribieron sin declararlo, y un override no puede
// ensanchar las excepciones chequeadas del metodo que sobreescribe (JLS 8.4.8.3). Ver la nota de
// `IOException`. Lo que aca fallaria con una `IOException` en el JDK se señala con
// `UncheckedIOException`, que la envuelve -- asi el motivo no se pierde y el codigo sigue
// compilando contra estas bases.
public class FileOutputStream extends OutputStream {

    // Mas alla de esto se vuelca solo, para que escribir un archivo grande no lo tenga entero dos
    // veces en memoria. No cambia lo que se ve: el archivo queda igual.
    private static final int LIMITE = 1 << 16;

    private final String ruta;
    private byte[] buf = new byte[256];
    private int usados = 0;
    private boolean cerrado = false;
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

    public void write(int b) {
        this.comprobarAbierto();
        this.asegurar(1);
        this.buf[this.usados] = (byte) b;
        this.usados = this.usados + 1;
        if (this.usados >= LIMITE) {
            this.flush();
        }
    }

    public void write(byte[] b, int off, int len) {
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

    public void write(byte[] b) {
        this.write(b, 0, b.length);
    }

    /** Vuelca al disco lo que haya en el buffer. */
    public void flush() {
        if (this.usados == 0 || this.ruta == null) {
            return;
        }
        byte[] trozo = new byte[this.usados];
        System.arraycopy(this.buf, 0, trozo, 0, this.usados);
        if (!Fs.writeAllBytes(this.ruta, trozo, this.anexar)) {
            throw new UncheckedIOException(new IOException("Could not write to " + this.ruta));
        }
        this.anexar = true;
        this.usados = 0;
    }

    public void close() {
        if (!this.cerrado) {
            this.flush();
            this.cerrado = true;
        }
    }

    /** El descriptor. Esta biblioteca no los modela; ver la nota de la clase. */
    public final FileDescriptor getFD() {
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

    private void comprobarAbierto() {
        if (this.cerrado) {
            throw new UncheckedIOException(new IOException("Stream Closed"));
        }
    }
}
