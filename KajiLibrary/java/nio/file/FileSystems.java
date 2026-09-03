package java.nio.file;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

// La fabrica de sistemas de archivos.
//
// **KajiJDK tiene exactamente uno: el del esquema `file`.** No hay carga por servicios --nada que
// descubra proveedores en el classpath-- asi que no hay forma de instalar un proveedor de ZIP ni de
// nada. Los metodos que crearian otro sistema **existen y fallan** con la excepcion que la spec ya
// preve para el caso --`ProviderNotFoundException` cuando no hay quien atienda el esquema,
// `FileSystemAlreadyExistsException` cuando se pide crear el que ya esta-- en vez de devolver algo.
//
// Que las cinco sobrecargas de `newFileSystem` esten y fallen no es relleno: el codigo que hoy abre
// un ZIP con `newFileSystem(path)` compila, y falla en el unico lugar donde se puede ver que falta,
// con un mensaje que lo dice.
public final class FileSystems {

    // Solo fabricas: no hay nada que instanciar.
    private FileSystems() {
    }

    /**
     * El sistema de archivos por omision, el que ve los archivos del sistema operativo.
     *
     * <p>Siempre el mismo objeto, y no se puede cerrar: `close()` no hace nada y `isOpen()` es
     * siempre `true`, igual que en el JDK. Un sistema por omision cerrable seria un pie para dejar
     * a la VM sin acceso a disco desde cualquier parte del programa.
     */
    public static FileSystem getDefault() {
        return KajiFileSystem.INSTANCE;
    }

    private static void soloFile(URI uri) {
        if (uri == null) {
            throw new NullPointerException();
        }
        String esquema = uri.getScheme();
        if (esquema == null || !esquema.equalsIgnoreCase("file")) {
            throw new ProviderNotFoundException("Provider \"" + esquema + "\" not installed");
        }
    }

    /**
     * El sistema de archivos ya creado para `uri`.
     *
     * @throws ProviderNotFoundException si el esquema no es `file`
     */
    public static FileSystem getFileSystem(URI uri) {
        soloFile(uri);
        return KajiFileSystem.INSTANCE;
    }

    /**
     * @throws ProviderNotFoundException si el esquema no es `file`
     * @throws FileSystemAlreadyExistsException si lo es -- el sistema por omision ya existe
     */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        soloFile(uri);
        throw new FileSystemAlreadyExistsException();
    }

    /** Como el otro; el `ClassLoader` no cambia nada porque no hay proveedores que cargar. */
    public static FileSystem newFileSystem(URI uri, Map<String, ?> env, ClassLoader loader)
            throws IOException {
        return newFileSystem(uri, env);
    }

    /**
     * Abriria un archivo --tipicamente un ZIP-- como sistema de archivos.
     *
     * @throws ProviderNotFoundException siempre: KajiJDK no tiene ningun proveedor de contenedor
     */
    public static FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
        if (path == null) {
            throw new NullPointerException();
        }
        throw new ProviderNotFoundException("no container provider installed for " + path);
    }

    /** Como el otro, sin entorno. */
    public static FileSystem newFileSystem(Path path) throws IOException {
        return newFileSystem(path, (Map<String, ?>) null);
    }

    /** Como el otro; el `ClassLoader` no cambia nada. */
    public static FileSystem newFileSystem(Path path, ClassLoader loader) throws IOException {
        return newFileSystem(path, (Map<String, ?>) null);
    }

    /** Como el otro; el `ClassLoader` no cambia nada. */
    public static FileSystem newFileSystem(Path path, Map<String, ?> env, ClassLoader loader)
            throws IOException {
        return newFileSystem(path, env);
    }
}
