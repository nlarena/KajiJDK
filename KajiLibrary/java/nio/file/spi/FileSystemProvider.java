package java.nio.file.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

// La costura por la que se enchufa un sistema de archivos: todo lo que `Files` hace, lo hace
// llamando a un proveedor.
//
// **Como esta partida la clase, que es lo que explica que metodos son `abstract`.** Lo `abstract` es
// lo que ningun proveedor puede heredar de otro --borrar, copiar, crear un directorio, leer
// atributos--; lo concreto son las combinaciones que se arman con eso: `deleteIfExists` es
// `delete()` atrapando `NoSuchFileException`, y `readAttributesIfExists` es lo mismo con
// `readAttributes`. Escribirlas una vez aca en vez de en cada proveedor es el punto de la clase.
//
// **Los tres metodos de canales estan, y `newByteChannel` es la razon por la que importan.** Es
// `abstract` --como en el JDK-- y es de donde salen `newInputStream` y `newOutputStream` por
// omision: un proveedor que sepa abrir un canal ya sabe abrir los dos streams, y no tiene que
// escribirlos. `newFileChannel` y `newAsynchronousFileChannel` son concretos y por omision fallan,
// tambien como en el JDK: son la promesa opcional de que el canal devuelto ademas se puede mapear a
// memoria o candar, y eso no lo puede cumplir cualquier proveedor.
//
// La API entera de la clase esta. Lo que un proveedor concreto pueda hacer con ella es asunto suyo:
// el de esta VM abre canales sobre los seis nativos de `Fs` --que leen y escriben el archivo entero
// de una-- y por eso no ofrece el asincronico, donde la firma prometeria un paralelismo que no hay.
// Ver la cabecera de `java.nio.channels.AsynchronousFileChannel`.
public abstract class FileSystemProvider {

    /** Para las subclases. */
    protected FileSystemProvider() {
    }

    /**
     * Los proveedores instalados, con el de por omision primero.
     *
     * <p>KajiJDK devuelve **exactamente uno**: el del esquema `file`. No hay carga por servicios
     * --nada que descubra proveedores en el classpath-- asi que la lista no puede crecer, y por eso
     * es inmutable en vez de una copia defensiva.
     */
    public static List<FileSystemProvider> installedProviders() {
        return Collections.singletonList(FileSystems.getDefault().provider());
    }

    /** El esquema de URI que atiende este proveedor: `"file"`, `"jar"`, ... */
    public abstract String getScheme();

    /** Crea un sistema de archivos nuevo para `uri`. */
    public abstract FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException;

    /** El sistema de archivos que ya existe para `uri`. */
    public abstract FileSystem getFileSystem(URI uri);

    /** La ruta que nombra `uri`. */
    public abstract Path getPath(URI uri);

    /**
     * Crea un sistema de archivos a partir de un archivo --tipicamente un ZIP--.
     *
     * <p>Por omision falla: es la sobrecarga que solo tiene sentido para proveedores de contenedor.
     */
    public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Abre `path` para leer.
     *
     * <p>Concreto porque se arma sobre un canal de lectura envuelto en un stream. Ningun proveedor
     * necesita escribirlo, y por eso esta aca una vez.
     *
     * <p>`APPEND` y `WRITE` se rechazan en vez de ignorarse: pedir que se escriba algo que se va a
     * devolver como stream de lectura no es una opcion redundante, es una confusion sobre lo que se
     * esta abriendo, y en silencio se descubre tarde.
     *
     * <p><strong>El canal sale de `Files.newByteChannel`, no de `this.newByteChannel`</strong>, y no
     * es un descuido: es lo que hace el JDK --se comprobo desasemblando `FileSystemProvider`-- y se
     * copia para que las dos VMs contesten igual. Notar que `newOutputStream`, cuatro lineas mas
     * abajo, si llama a `this`: la asimetria es del JDK, no de aca. Da lo mismo para cualquier
     * proveedor real, porque la ruta pertenece al proveedor que la abre; solo se nota si a un
     * proveedor se le pasa una ruta ajena, y ahi el JDK atiende a la ruta. **No "arreglar" esto sin
     * volver a medir contra el JDK**: cambiarlo por `this` es una divergencia observable.
     *
     * @throws UnsupportedOperationException si se pide `APPEND` o `WRITE`
     */
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        for (OpenOption opt : options) {
            if (opt == StandardOpenOption.APPEND || opt == StandardOpenOption.WRITE) {
                throw new UnsupportedOperationException("'" + opt + "' not allowed");
            }
        }
        return Channels.newInputStream(Files.newByteChannel(path, options));
    }

    /**
     * Abre `path` para escribir, tambien sobre `newByteChannel`.
     *
     * <p>Sin opciones vale lo que vale en `Files`: crear si no esta y truncar si estaba. `READ` es
     * `IllegalArgumentException` --y no `UnsupportedOperationException` como el caso simetrico de
     * `newInputStream`-- porque asi lo distingue el JDK: alla la opcion es imposible de honrar, aca
     * el argumento se contradice con la operacion.
     *
     * @throws IllegalArgumentException si se pide `READ`
     */
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        Set<OpenOption> opts = new HashSet<OpenOption>();
        if (options.length == 0) {
            opts.add(StandardOpenOption.CREATE);
            opts.add(StandardOpenOption.TRUNCATE_EXISTING);
        } else {
            for (OpenOption opt : options) {
                if (opt == StandardOpenOption.READ) {
                    throw new IllegalArgumentException("READ not allowed");
                }
                opts.add(opt);
            }
        }
        opts.add(StandardOpenOption.WRITE);
        return Channels.newOutputStream(this.newByteChannel(path, opts));
    }

    /**
     * Abre un canal sobre `path`: la operacion de la que salen las demas formas de leer y escribir.
     *
     * <p>Es `abstract` y es el unico de los tres metodos de canal que lo es, porque es el minimo:
     * un proveedor que sepa contestarlo ya le da a sus usuarios los dos streams y todo lo que
     * `Files` arma encima. Los otros dos prometen mas y por eso son opcionales.
     */
    public abstract SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException;

    /**
     * Como `newByteChannel`, pero prometiendo un `FileChannel`.
     *
     * <p>Por omision falla, y la diferencia con `newByteChannel` es lo que explica que sean dos
     * metodos: un `FileChannel` no es solo un canal con posicion, es uno que ademas se puede mapear
     * a memoria y candar contra otros procesos. Un proveedor de ZIP puede dar lo primero y no lo
     * segundo, asi que la promesa se pide aparte.
     */
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Un canal asincronico sobre `path`, corriendo las operaciones en `executor`.
     *
     * <p>Por omision falla. El proveedor de esta VM **no lo sobreescribe**: los seis nativos de
     * archivo son sincronicos y leen el archivo entero de una, asi que lo unico que se podria
     * devolver es una fachada que corre operaciones sincronicas en otro hilo --sin cancelacion y sin
     * paralelismo real--. Devolver eso seria prometer justo las propiedades por las que uno elige
     * esta API. El razonamiento completo esta en la cabecera de `AsynchronousFileChannel`.
     */
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
            Set<? extends OpenOption> options, ExecutorService executor,
            FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Abre un directorio para recorrerlo, quedandose con las entradas que acepte `filter`. */
    public abstract DirectoryStream<Path> newDirectoryStream(Path dir,
            DirectoryStream.Filter<? super Path> filter) throws IOException;

    /** Crea un directorio. */
    public abstract void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException;

    /** Crea un enlace simbolico. Por omision falla: no todo sistema los tiene. */
    public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Crea un enlace duro. Por omision falla. */
    public void createLink(Path link, Path existing) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Borra un archivo, o un directorio vacio. */
    public abstract void delete(Path path) throws IOException;

    /**
     * Borra si esta; devuelve si borro algo.
     *
     * <p>Concreto porque es `delete()` atrapando la de "no estaba" -- ningun proveedor necesita
     * escribirlo. Notar que **no es atomico**: entre el intento y el fallo alguien pudo crear el
     * archivo.
     */
    public boolean deleteIfExists(Path path) throws IOException {
        try {
            this.delete(path);
            return true;
        } catch (NoSuchFileException e) {
            return false;
        }
    }

    /** El destino de un enlace simbolico. Por omision falla. */
    public Path readSymbolicLink(Path link) throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Copia `source` a `target`. */
    public abstract void copy(Path source, Path target, CopyOption... options) throws IOException;

    /** Mueve `source` a `target`. */
    public abstract void move(Path source, Path target, CopyOption... options) throws IOException;

    /** Si las dos rutas nombran el mismo archivo. */
    public abstract boolean isSameFile(Path path, Path path2) throws IOException;

    /** Si el archivo esta marcado como oculto. */
    public abstract boolean isHidden(Path path) throws IOException;

    /** El volumen donde vive el archivo. */
    public abstract FileStore getFileStore(Path path) throws IOException;

    /**
     * Comprueba que el archivo existe y que se puede acceder de los modos pedidos.
     *
     * <p>Sin modos comprueba solo que existe. Devolver `void` y tirar es a proposito: el motivo del
     * rechazo --no existe, o existe y no hay permiso-- es informacion que un booleano perderia.
     */
    public abstract void checkAccess(Path path, AccessMode... modes) throws IOException;

    /** Una vista de atributos del archivo, o `null` si el proveedor no la tiene. */
    public abstract <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
            LinkOption... options);

    /** Lee los atributos de una sola vez, con el tipo pedido. */
    public abstract <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
            LinkOption... options) throws IOException;

    /** Lee atributos sueltos por nombre: `"basic:size,lastModifiedTime"`, `"posix:*"`. */
    public abstract Map<String, Object> readAttributes(Path path, String attributes,
            LinkOption... options) throws IOException;

    /** Fija un atributo por nombre. */
    public abstract void setAttribute(Path path, String attribute, Object value,
            LinkOption... options) throws IOException;

    /**
     * Si el archivo existe.
     *
     * <p>Concreto: es `checkAccess` sin modos, atrapando. Un proveedor que tenga una forma mas
     * barata de contestarlo lo sobreescribe.
     */
    public boolean exists(Path path, LinkOption... options) {
        try {
            this.checkAccess(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Los atributos, o `null` si el archivo no esta.
     *
     * <p>Existe para ahorrar el par comprobar-y-leer, que ademas tiene una carrera en el medio: aca
     * la lectura es una sola y el `null` sale de la misma operacion que hubiera fallado.
     */
    public <A extends BasicFileAttributes> A readAttributesIfExists(Path path, Class<A> type,
            LinkOption... options) throws IOException {
        try {
            return this.readAttributes(path, type, options);
        } catch (NoSuchFileException e) {
            return null;
        }
    }
}
