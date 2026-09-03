package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

// El volumen donde viven los archivos: una particion, un disco, un montaje.
//
// **KajiJDK no construye ninguno, y conviene decir por que.** De los once miembros, ocho piden datos
// que ningun nativo de `jdk.internal.io.Fs` devuelve: el nombre del volumen, su tipo, si esta
// montado de solo lectura, el espacio total, el usable y el sin asignar. Una implementacion tendria
// que devolver `""` y `0`, y un `0` en `getUsableSpace()` no es "no se" -- es "no entra nada", que
// es una respuesta concreta y falsa, del tipo que hace que un programa decida no escribir.
//
// Por eso `Files.getFileStore` y `FileSystem.getFileStores()` **existen pero levantan
// `UnsupportedOperationException`**, y esta clase queda abstracta y sin subclase. La clase vale
// igual: es el tipo que esas firmas nombran, y el dia que haya un nativo de estadisticas de volumen
// lo unico que falta es la subclase.
public abstract class FileStore {

    /** Para las subclases. */
    protected FileStore() {
    }

    /** El nombre del volumen. Su forma depende del sistema; puede no ser unico. */
    public abstract String name();

    /** El tipo del sistema de archivos: `"ntfs"`, `"ext4"`, `"tmpfs"`. */
    public abstract String type();

    /** Si esta montado de solo lectura. */
    public abstract boolean isReadOnly();

    /** El tamaño total, en bytes. */
    public abstract long getTotalSpace() throws IOException;

    /**
     * Los bytes que esta VM puede usar de verdad.
     *
     * <p>Es distinto de `getUnallocatedSpace()` y la diferencia importa: este descuenta las cuotas y
     * el espacio reservado para root, aquel no. Sigue siendo una estimacion -- entre que se pregunta
     * y que se escribe, otro proceso puede haberlo ocupado.
     */
    public abstract long getUsableSpace() throws IOException;

    /** Los bytes libres sin descontar cuotas ni reservas. */
    public abstract long getUnallocatedSpace() throws IOException;

    /**
     * El tamaño del bloque.
     *
     * <p>Concreto y no abstracto: la spec le da un valor por omision --fallar-- para no romper las
     * implementaciones anteriores a que existiera.
     *
     * @throws UnsupportedOperationException si el volumen no lo sabe
     */
    public long getBlockSize() throws IOException {
        throw new UnsupportedOperationException();
    }

    /** Si el volumen soporta una vista de atributos de archivo, por tipo. */
    public abstract boolean supportsFileAttributeView(Class<? extends FileAttributeView> type);

    /** Lo mismo, por nombre de vista (`"basic"`, `"posix"`, ...). */
    public abstract boolean supportsFileAttributeView(String name);

    /** Una vista de atributos **del volumen**, o `null` si no la soporta. */
    public abstract <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type);

    /** Un atributo del volumen por su nombre `"vista:atributo"`. */
    public abstract Object getAttribute(String attribute) throws IOException;
}
