package java.nio.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

import jdk.internal.io.Fs;

// El unico proveedor de KajiJDK: el del esquema `file`, sobre los seis nativos de
// `jdk.internal.io.Fs`.
//
// **Es una fachada sobre `Files`, no al reves.** En el JDK la logica vive en el proveedor y `Files`
// delega; aca se hizo al reves porque no hay mas que un proveedor y porque `Files` es donde alguien
// va a buscar el codigo. La consecuencia es que esta clase es casi toda una linea por metodo, que es
// exactamente lo que se quiere de una fachada.
//
// **Que no puede hacer, y como lo dice.** Los metodos que la spec declara `abstract` hay que
// implementarlos si o si; los que necesitan algo que esta VM no tiene levantan
// `UnsupportedOperationException` con el motivo escrito. Un `UnsupportedOperationException` no es un
// hueco tapado: dice "esto no existe aca" y no se lo puede confundir con un resultado.
//
// Excepcion notable: `getFileAttributeView` devuelve `null`, que **es** la respuesta que manda la
// spec cuando la vista pedida no esta disponible -- y aca no hay ninguna disponible.
final class KajiFileSystemProvider extends FileSystemProvider {

    static final KajiFileSystemProvider INSTANCIA = new KajiFileSystemProvider();

    private KajiFileSystemProvider() {
    }

    public String getScheme() {
        return "file";
    }

    private static void comprobarUri(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        String esquema = uri.getScheme();
        if (esquema == null || !esquema.equalsIgnoreCase("file")) {
            throw new IllegalArgumentException("URI scheme is not \"file\"");
        }
    }

    /**
     * Siempre falla: el sistema de archivos por omision se crea con la VM y no se puede volver a
     * crear. Es lo mismo que hace el JDK.
     */
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        comprobarUri(uri);
        throw new FileSystemAlreadyExistsException();
    }

    public FileSystem getFileSystem(URI uri) {
        comprobarUri(uri);
        return KajiFileSystem.INSTANCE;
    }

    public Path getPath(URI uri) {
        comprobarUri(uri);
        return Path.of(uri);
    }

    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return Files.newInputStream(path, options);
    }

    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return Files.newOutputStream(path, options);
    }

    /**
     * Un canal sobre el archivo. Lo devuelve `FileChannel.open`, que es lo que esta VM sabe abrir.
     *
     * <p>Devolver directamente el `FileChannel` --y no envolverlo para esconder que lo es-- es lo
     * que hace el JDK: el tipo declarado es lo que se promete, el de mas es lo que se da.
     */
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        return FileChannel.open(path, options, attrs);
    }

    /** El mismo canal, con el tipo que promete mas. Ver `newByteChannel`. */
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        return FileChannel.open(path, options, attrs);
    }

    /**
     * No se puede: no hay nativo que enumere un directorio. Ver la nota de `DirectoryStream`.
     */
    public DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException {
        throw new UnsupportedOperationException("KajiJDK cannot list a directory");
    }

    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        Files.createDirectory(dir, attrs);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        Files.copy(source, target, options);
    }

    public void move(Path source, Path target, CopyOption... options) throws IOException {
        Files.move(source, target, options);
    }

    /**
     * Contesta `true` cuando las dos rutas son la misma escrita de dos formas, y falla en el resto.
     *
     * <p>Dos rutas iguales son el mismo archivo, y eso la spec lo dice sin mirar el disco. Lo mismo
     * vale despues de `normalize()` --`a/./b` y `a/b` son la misma ruta-- **siempre que las dos sean
     * absolutas o las dos relativas**: mezclarlas obligaria a llevar la relativa a absoluta, y con
     * `user.dir` valiendo `null` en esta VM eso no resuelve nada, inventa un prefijo. Comparar
     * contra un prefijo inventado puede dar un `true` falso, que es peor que no contestar.
     *
     * <p>Para dos rutas que **no** normalizan igual hay que comparar la identidad del archivo --el
     * inodo, el file key-- y no hay nativo que la devuelva: contestar `false` seria afirmar que son
     * archivos distintos cuando podrian ser dos nombres del mismo (sobre Windows alcanza con cambiar
     * mayusculas). Por eso falla en vez de adivinar.
     *
     * <p>Este metodo es `abstract` en la spec, asi que hay que darle un cuerpo: no existe la opcion
     * de omitirlo, como si la hay en `Files.isSameFile`, que por eso no esta.
     */
    public boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        if (path.isAbsolute() == path2.isAbsolute()
                && path.normalize().equals(path2.normalize())) {
            return true;
        }
        throw new UnsupportedOperationException(
                "KajiJDK has no file key: two different paths cannot be compared");
    }

    /** El nombre empieza con un punto. La definicion y el porque estan en `Files.isHidden`. */
    public boolean isHidden(Path path) throws IOException {
        return Files.isHidden(path);
    }

    /** No se puede: no hay estadisticas de volumen. Ver `FileStore`. */
    public FileStore getFileStore(Path path) throws IOException {
        throw new UnsupportedOperationException("KajiJDK models no file store");
    }

    /**
     * Comprueba existencia y, si se piden, lectura y escritura.
     *
     * <p>`EXECUTE` falla: `stat` no trae bit de ejecucion, y aceptarlo en silencio diria que se
     * puede ejecutar sin haberlo comprobado.
     */
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        String p = path.toString();
        int st = Fs.stat(p);
        if ((st & Fs.EXISTE) == 0) {
            throw new NoSuchFileException(p);
        }
        int i = 0;
        while (i < modes.length) {
            AccessMode m = modes[i];
            if (m == AccessMode.READ) {
                if ((st & Fs.SE_LEE) == 0) {
                    throw new AccessDeniedException(p);
                }
            } else if (m == AccessMode.WRITE) {
                if ((st & Fs.SE_ESCRIBE) == 0) {
                    throw new AccessDeniedException(p);
                }
            } else {
                throw new UnsupportedOperationException("no execute bit in stat");
            }
            i = i + 1;
        }
    }

    /**
     * Siempre `null`: no hay ninguna vista disponible. Es la respuesta que manda la spec.
     *
     * <p>Que no haya **vista** y si haya `readAttributes` no es una contradiccion: una vista lee y
     * escribe, y escribir metadatos no se puede. El detalle esta en `Files.getFileAttributeView`.
     */
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options) {
        return Files.getFileAttributeView(path, type, options);
    }

    /** La vista `basic`, sacada de `stat` y `size`. Ver `Files.readAttributes`. */
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        return Files.readAttributes(path, type, options);
    }

    /** Idem, por nombre de atributo. */
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
            throws IOException {
        return Files.readAttributes(path, attributes, options);
    }

    /** No se puede: no hay nativo que escriba metadatos. */
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
            throws IOException {
        throw new UnsupportedOperationException("KajiJDK cannot write file attributes");
    }

    /**
     * Un `stat` y listo.
     *
     * <p>Se sobreescribe el de la clase base --que arma un `checkAccess` y atrapa-- porque aca hay
     * una respuesta directa y mas barata.
     */
    public boolean exists(Path path, LinkOption... options) {
        return Files.exists(path, options);
    }
}
