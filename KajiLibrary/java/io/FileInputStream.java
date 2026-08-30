package java.io;

import jdk.internal.io.Fs;

// KajiLibrary's java.io.FileInputStream -- un stream de bytes leidos de un archivo.
//
// **El archivo se lee entero al construirse.** Es la diferencia visible con el del JDK, que abre un
// descriptor y va leyendo a medida que se le pide, y conviene tenerla presente:
//
//   - un archivo de un giga entra en memoria de una, no de a pedazos;
//   - los cambios que otro proceso le haga **despues** de construir el stream no se ven -- lo que
//     se lee es la foto del momento de abrir;
//   - a cambio, `close()` no puede fallar ni faltar, y no hay descriptor que se quede colgado.
//
// La razon esta un nivel mas abajo, en `jdk.internal.io.Fs`: el nativo lee el archivo entero porque
// no hay handles abiertos en esta VM. Cuando los haya, esta clase se reescribe sin que nada de lo
// que la usa se entere -- `Scanner` y `Formatter` hablan con `InputStream`, no con esto.
//
// **Sin `throws IOException`**, y no por descuido: las bases de este paquete
// (`InputStream`/`OutputStream`/`Closeable`) se escribieron sin declararlo, y un override no puede
// ensanchar las excepciones chequeadas del metodo que sobreescribe (JLS 8.4.8.3). Ver la nota de
// `IOException`. Lo que aca fallaria con una `IOException` en el JDK se señala con
// `UncheckedIOException`, que la envuelve -- asi el motivo no se pierde y el codigo sigue
// compilando contra estas bases.
public class FileInputStream extends InputStream {

    private final byte[] datos;
    private int pos;
    private int marca = -1;
    private boolean cerrado = false;

    /**
     * Abre `name` para lectura.
     *
     * @throws FileNotFoundException si no existe, es un directorio, o no se puede leer
     */
    public FileInputStream(String name) throws FileNotFoundException {
        this(name == null ? null : new File(name));
    }

    /**
     * Abre `file` para lectura.
     *
     * @throws FileNotFoundException si no existe, es un directorio, o no se puede leer
     */
    public FileInputStream(File file) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        // Un directorio se rechaza aparte y con su propio mensaje: leer uno "falla" de una forma
        // distinta a que el archivo no este, y confundirlas manda a buscar al lugar equivocado.
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " (Is a directory)");
        }
        byte[] b = Fs.readAllBytes(file.getPath());
        if (b == null) {
            throw new FileNotFoundException(file.getPath() + " (No such file or directory)");
        }
        this.datos = b;
        this.pos = 0;
    }

    /** Abre por descriptor. Esta biblioteca no modela descriptores; ver la nota de la clase. */
    public FileInputStream(FileDescriptor fdObj) {
        if (fdObj == null) {
            throw new NullPointerException();
        }
        this.datos = new byte[0];
        this.pos = 0;
    }

    public int read() {
        this.comprobarAbierto();
        if (this.pos >= this.datos.length) {
            return -1;
        }
        int b = this.datos[this.pos] & 0xff;
        this.pos = this.pos + 1;
        return b;
    }

    public int read(byte[] b, int off, int len) {
        this.comprobarAbierto();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (this.pos >= this.datos.length) {
            return -1;                       // fin de flujo, y **no** cero: son cosas distintas
        }
        int n = this.datos.length - this.pos;
        if (n > len) {
            n = len;
        }
        System.arraycopy(this.datos, this.pos, b, off, n);
        this.pos = this.pos + n;
        return n;
    }

    public int read(byte[] b) {
        return this.read(b, 0, b.length);
    }

    public long skip(long n) {
        this.comprobarAbierto();
        if (n <= 0L) {
            return 0L;
        }
        long disponible = this.datos.length - this.pos;
        long saltados = n < disponible ? n : disponible;
        this.pos = this.pos + (int) saltados;
        return saltados;
    }

    /**
     * Cuantos bytes quedan.
     *
     * <p>Aca es exacto --el archivo esta entero en memoria-- mientras que en el JDK es una
     * estimacion. Es una de las pocas cosas en que leer todo de una sale ganando.
     */
    public int available() {
        this.comprobarAbierto();
        return this.datos.length - this.pos;
    }

    public boolean markSupported() {
        return true;
    }

    public synchronized void mark(int readlimit) {
        this.marca = this.pos;
    }

    public synchronized void reset() {
        this.comprobarAbierto();
        if (this.marca < 0) {
            throw new UncheckedIOException(new IOException("Resetting to invalid mark"));
        }
        this.pos = this.marca;
    }

    public void close() {
        this.cerrado = true;
    }

    /** El descriptor. Esta biblioteca no los modela; ver la nota de la clase. */
    public final FileDescriptor getFD() {
        return new FileDescriptor();
    }

    private void comprobarAbierto() {
        if (this.cerrado) {
            throw new UncheckedIOException(new IOException("Stream Closed"));
        }
    }
}
