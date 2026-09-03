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
// **`getChannel()` devuelve un canal sobre esa misma foto**, y comparte la posicion con el flujo tal
// como manda el contrato: leer de uno mueve al otro. Lee de la foto y no del disco a proposito --
// compartir la posicion pero no el contenido seria peor que no compartir nada -- asi que lo que se
// dijo arriba sobre lo que no se ve vale igual leyendo por el canal. El como esta en `Canales`.
//
// **Los errores salen como `IOException` chequeada**, igual que en el JDK. Hubo una epoca en que
// no: las bases del paquete (`InputStream`/`OutputStream`/`Closeable`) no declaraban `throws
// IOException`, un override no puede ensanchar las chequeadas de lo que sobreescribe (JLS 8.4.8.3),
// y lo que aca fallaba salia envuelto en `UncheckedIOException`. Las bases ya lo declaran, asi que
// el envoltorio se saco: un `catch (IOException e)` de quien llama tiene que agarrar esto, y con la
// no chequeada le pasaba por al lado y le mataba el hilo.
public class FileInputStream extends InputStream {

    // Package-private y no privados: `Canales.DeEntrada` **comparte** estos tres con este flujo,
    // que es de lo que se trata `getChannel()`. Ver la cabecera de `Canales`.
    final byte[] datos;
    // `long` y no `int` aunque el contenido sea un `byte[]`: el canal puede posicionarse mas alla
    // del final --es legal, y lo que sigue es que las lecturas dan -1-- y `position()` tiene que
    // devolver lo que se le puso. Con un `int` habria que recortarlo y dejaria de ser el mismo
    // numero. Los usos como indice van casteados, siempre despues de comprobar el final.
    long pos;
    private long marca = -1;
    boolean cerrado = false;

    /** El canal de este flujo, creado a pedido. Uno solo: el contrato dice "the unique object". */
    private java.nio.channels.FileChannel canal;

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

    public int read() throws IOException {
        this.comprobarAbierto();
        if (this.pos >= this.datos.length) {
            return -1;
        }
        int b = this.datos[(int) this.pos] & 0xff;
        this.pos = this.pos + 1;
        return b;
    }

    public int read(byte[] b, int off, int len) throws IOException {
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
        int n = (int) (this.datos.length - this.pos);
        if (n > len) {
            n = len;
        }
        System.arraycopy(this.datos, (int) this.pos, b, off, n);
        this.pos = this.pos + n;
        return n;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public long skip(long n) throws IOException {
        this.comprobarAbierto();
        if (n <= 0L) {
            return 0L;
        }
        long disponible = this.datos.length - this.pos;
        long saltados = n < disponible ? n : disponible;
        this.pos = this.pos + saltados;
        return saltados;
    }

    /**
     * Cuantos bytes quedan.
     *
     * <p>Aca es exacto --el archivo esta entero en memoria-- mientras que en el JDK es una
     * estimacion. Es una de las pocas cosas en que leer todo de una sale ganando.
     */
    public int available() throws IOException {
        this.comprobarAbierto();
        return (int) (this.datos.length - this.pos);
    }

    public boolean markSupported() {
        return true;
    }

    public synchronized void mark(int readlimit) {
        this.marca = this.pos;
    }

    public synchronized void reset() throws IOException {
        this.comprobarAbierto();
        if (this.marca < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        this.pos = this.marca;
    }

    // (`getChannel` esta mas abajo, junto a `close`.)

    public void close() throws IOException {
        this.cerrado = true;
        // Cerrar el flujo cierra su canal: son la misma cosa vista de dos maneras.
        if (this.canal != null && this.canal.isOpen()) {
            this.canal.close();
        }
    }

    /**
     * El canal de este flujo, **con la misma posicion**: leer del flujo mueve el canal y mover el
     * canal cambia desde donde lee el flujo. No es una copia sincronizada, es un solo numero; el
     * porque esta en `Canales`.
     *
     * <p>Lee de la misma foto que el flujo --la que se saco al construirlo-- y no del disco, por lo
     * mismo: compartir la posicion pero no el contenido seria peor que no compartir nada. Y es de
     * solo lectura, como el del JDK.
     */
    public java.nio.channels.FileChannel getChannel() {
        if (this.canal == null) {
            this.canal = new Canales.DeEntrada(this);
        }
        return this.canal;
    }

    /** El descriptor. Esta biblioteca no los modela; ver la nota de la clase. */
    public final FileDescriptor getFD() throws IOException {
        return new FileDescriptor();
    }

    private void comprobarAbierto() throws IOException {
        if (this.cerrado) {
            throw new IOException("Stream Closed");
        }
    }
}
