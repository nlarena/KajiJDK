// Los tres metodos de canal de `java.nio.file.spi.FileSystemProvider`.
//
// **Se comprueba contra `java` real corriendo lo mismo**, y eso es lo que decide que se prueba y que
// no. Los dos lados son distintos a proposito:
//
//   - el **proveedor por omision** se prueba solo donde las dos VMs contestan igual: abrir un canal,
//     escribir, leer de vuelta. `newAsynchronousFileChannel` **no** se le pregunta al de por
//     omision, porque el del JDK real si lo implementa y el de KajiJDK no (ver la cabecera de
//     `AsynchronousFileChannel`): ahi la diferencia es honesta pero no comparable;
//   - los **valores por omision de la clase abstracta** se prueban con un proveedor de juguete
//     definido aca abajo, que implementa `newByteChannel` sobre un arreglo en memoria y nada mas.
//     Ese camino es codigo de `FileSystemProvider` puro --el mismo en las dos VMs-- asi que ahi si
//     se puede exigir que coincidan hasta la excepcion.
//
// Lo que se le exige a los valores por omision es la parte que suele escribirse mal: que
// `newInputStream` y `newOutputStream` **salgan de `newByteChannel`** en vez de ser huecos, y que
// rechacen las opciones que se contradicen con la operacion --y con la excepcion que corresponde a
// cada caso, que no es la misma--.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpiCanalProvTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    // ---- el proveedor de juguete -----------------------------------------------------------------

    // Un canal sobre un `byte[]` que crece. Es lo minimo que `newInputStream`/`newOutputStream`
    // necesitan para funcionar, y por eso alcanza para comprobar que efectivamente los usan.
    static final class CanalEnMemoria implements SeekableByteChannel {
        byte[] datos;
        int largo;
        long pos = 0;
        boolean abierto = true;

        CanalEnMemoria(byte[] inicial) {
            this.datos = new byte[inicial.length + 16];
            System.arraycopy(inicial, 0, this.datos, 0, inicial.length);
            this.largo = inicial.length;
        }

        public int read(ByteBuffer dst) {
            if (pos >= largo) {
                return -1;
            }
            int n = largo - (int) pos;
            if (n > dst.remaining()) {
                n = dst.remaining();
            }
            dst.put(datos, (int) pos, n);
            pos = pos + n;
            return n;
        }

        public int write(ByteBuffer src) {
            int n = src.remaining();
            while (pos + n > datos.length) {
                byte[] mas = new byte[datos.length * 2 + 16];
                System.arraycopy(datos, 0, mas, 0, largo);
                datos = mas;
            }
            src.get(datos, (int) pos, n);
            pos = pos + n;
            if (pos > largo) {
                largo = (int) pos;
            }
            return n;
        }

        public long position() {
            return pos;
        }

        public SeekableByteChannel position(long nueva) {
            pos = nueva;
            return this;
        }

        public long size() {
            return largo;
        }

        public SeekableByteChannel truncate(long tam) {
            if (tam < largo) {
                largo = (int) tam;
            }
            if (pos > largo) {
                pos = largo;
            }
            return this;
        }

        public boolean isOpen() {
            return abierto;
        }

        public void close() {
            abierto = false;
        }

        byte[] contenido() {
            byte[] r = new byte[largo];
            System.arraycopy(datos, 0, r, 0, largo);
            return r;
        }
    }

    // Implementa `newByteChannel` y **nada mas**: todo lo demas tira. Es a proposito -- si algun
    // valor por omision que se prueba aca tocara otro metodo, la prueba lo diria en vez de pasar.
    static final class ProveedorDeJuguete extends FileSystemProvider {
        CanalEnMemoria ultimo;
        Set<? extends OpenOption> ultimasOpciones;

        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                FileAttribute<?>... attrs) throws IOException {
            ultimasOpciones = options;
            ultimo = new CanalEnMemoria("hola".getBytes(StandardCharsets.UTF_8));
            return ultimo;
        }

        public String getScheme() {
            return "juguete";
        }

        public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
            throw new UnsupportedOperationException();
        }

        public FileSystem getFileSystem(URI uri) {
            throw new UnsupportedOperationException();
        }

        public Path getPath(URI uri) {
            throw new UnsupportedOperationException();
        }

        public DirectoryStream<Path> newDirectoryStream(Path dir,
                DirectoryStream.Filter<? super Path> filter) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void delete(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void move(Path source, Path target, CopyOption... options) throws IOException {
            throw new UnsupportedOperationException();
        }

        public boolean isSameFile(Path path, Path path2) throws IOException {
            throw new UnsupportedOperationException();
        }

        public boolean isHidden(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        public FileStore getFileStore(Path path) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void checkAccess(Path path, AccessMode... modes) throws IOException {
            throw new UnsupportedOperationException();
        }

        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
                LinkOption... options) {
            throw new UnsupportedOperationException();
        }

        public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
                LinkOption... options) throws IOException {
            throw new UnsupportedOperationException();
        }

        public Map<String, Object> readAttributes(Path path, String attributes,
                LinkOption... options) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
                throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    // ---- los valores por omision de la clase abstracta -------------------------------------------

    static void porOmision() throws IOException {
        ProveedorDeJuguete p = new ProveedorDeJuguete();
        Path ruta = Paths.get("spicanalprov_juguete.bin");
        Files.write(ruta, "hola".getBytes(StandardCharsets.UTF_8));

        // `newInputStream` **no** pasa por el `newByteChannel` de este proveedor: el JDK lo arma con
        // `Files.newByteChannel`, que atiende a la ruta. Con una ruta del sistema de archivos por
        // omision, entonces, lo que se lee es el archivo de verdad y el canal de juguete queda sin
        // tocar. Eso es exactamente lo que se comprueba, porque es la parte sorprendente.
        p.ultimo = null;
        InputStream in = p.newInputStream(ruta);
        byte[] leido = new byte[16];
        int n = in.read(leido, 0, 16);
        in.close();
        ok(n == 4);
        ok(new String(leido, 0, 4, StandardCharsets.UTF_8).equals("hola"));
        ok(p.ultimo == null);

        // `WRITE` y `APPEND` no son opciones de una lectura: `UnsupportedOperationException`. Se
        // rechazan antes de tocar ninguna ruta, asi que aca el proveedor si decide.
        boolean tiro = false;
        try {
            p.newInputStream(ruta, StandardOpenOption.WRITE);
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        tiro = false;
        try {
            p.newInputStream(ruta, StandardOpenOption.APPEND);
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        // Una opcion que si vale para leer se acepta y el stream sale igual.
        InputStream in2 = p.newInputStream(ruta, StandardOpenOption.READ);
        in2.close();

        // `newOutputStream`, en cambio, **si** llama al `newByteChannel` de este proveedor --la
        // asimetria es del JDK, ver la cabecera de `FileSystemProvider`-- y lo que escribe llega al
        // canal de juguete, no al archivo.
        OutputStream out = p.newOutputStream(ruta);
        out.write("chau".getBytes(StandardCharsets.UTF_8));
        out.flush();
        byte[] quedo = p.ultimo.contenido();
        out.close();
        ok(new String(quedo, StandardCharsets.UTF_8).startsWith("chau"));

        // Sin opciones abre con CREATE + TRUNCATE_EXISTING + WRITE, y sin READ.
        ok(p.ultimasOpciones.contains(StandardOpenOption.WRITE));
        ok(p.ultimasOpciones.contains(StandardOpenOption.CREATE));
        ok(p.ultimasOpciones.contains(StandardOpenOption.TRUNCATE_EXISTING));
        ok(!p.ultimasOpciones.contains(StandardOpenOption.READ));

        // `READ` en una escritura es `IllegalArgumentException`, **no** `UnsupportedOperation`: el
        // argumento se contradice con la operacion, no es algo que falte implementar.
        tiro = false;
        try {
            p.newOutputStream(ruta, StandardOpenOption.READ);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        // Con opciones propias, `WRITE` se agrega igual y las pedidas se conservan.
        OutputStream out2 = p.newOutputStream(ruta, StandardOpenOption.APPEND);
        out2.close();
        ok(p.ultimasOpciones.contains(StandardOpenOption.APPEND));
        ok(p.ultimasOpciones.contains(StandardOpenOption.WRITE));

        // Los otros dos metodos de canal son opcionales y este proveedor no los da.
        tiro = false;
        try {
            p.newFileChannel(ruta, new HashSet<OpenOption>());
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        tiro = false;
        try {
            p.newAsynchronousFileChannel(ruta, new HashSet<OpenOption>(), null);
        } catch (UnsupportedOperationException e) {
            tiro = true;
        }
        ok(tiro);

        Files.deleteIfExists(ruta);
    }

    // ---- el proveedor por omision ----------------------------------------------------------------

    static void porOmisionDelSistema() throws IOException {
        FileSystemProvider p = FileSystems.getDefault().provider();
        ok(p.getScheme().equals("file"));

        // `installedProviders` no dice cuantos hay --el JDK real trae mas de uno-- pero si que el
        // primero es el de por omision.
        ok(FileSystemProvider.installedProviders().get(0).getScheme().equals("file"));

        Path f = Paths.get("spicanalprov_tmp.bin");
        Files.deleteIfExists(f);

        Set<OpenOption> escribir = new HashSet<OpenOption>();
        escribir.add(StandardOpenOption.CREATE);
        escribir.add(StandardOpenOption.WRITE);

        SeekableByteChannel c = p.newByteChannel(f, escribir);
        byte[] payload = "spi-canal".getBytes(StandardCharsets.UTF_8);
        int escritos = c.write(ByteBuffer.wrap(payload));
        c.close();
        ok(escritos == payload.length);

        Set<OpenOption> leer = new HashSet<OpenOption>();
        leer.add(StandardOpenOption.READ);

        SeekableByteChannel r = p.newByteChannel(f, leer);
        ok(r.size() == payload.length);
        ByteBuffer buf = ByteBuffer.allocate(64);
        int n = r.read(buf);
        r.close();
        ok(n == payload.length);
        ok(new String(buf.array(), 0, n, StandardCharsets.UTF_8).equals("spi-canal"));

        // `newFileChannel` da lo mismo con el tipo que promete mas, y respeta la posicion.
        FileChannel fc = p.newFileChannel(f, leer);
        fc.position(4);
        ByteBuffer buf2 = ByteBuffer.allocate(64);
        int n2 = fc.read(buf2);
        fc.close();
        ok(n2 == payload.length - 4);
        ok(new String(buf2.array(), 0, n2, StandardCharsets.UTF_8).equals("canal"));

        Files.deleteIfExists(f);
    }

    public static int run() {
        try {
            porOmision();
            porOmisionDelSistema();
        } catch (Throwable t) {
            if (primerFallo < 0) {
                primerFallo = cuantas;
            }
        }
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
