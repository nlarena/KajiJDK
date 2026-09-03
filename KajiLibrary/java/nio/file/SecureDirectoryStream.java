package java.nio.file;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Set;

// Un `DirectoryStream` sobre el que se puede operar **relativo al directorio abierto**, sin volver a
// resolver la ruta completa.
//
// **De que protege.** Si entre que se listo `/tmp/x` y que se borra `/tmp/x/y` alguien reemplaza
// `/tmp/x` por un enlace a otro lado, borrar por ruta absoluta borra el archivo equivocado. Este
// tipo opera contra el directorio ya abierto, asi que el cambio de abajo no lo redirige.
//
// **La interfaz esta entera.** `newByteChannel` estuvo omitido mientras
// `java.nio.channels.SeekableByteChannel` no existia en esta biblioteca --no se puede declarar un
// metodo que devuelve un tipo que no esta escrito--; ahora existe, con `FileChannel` detras, asi que
// el metodo se declara.
//
// KajiJDK no produce ninguno: no hay `DirectoryStream` que funcione, ver `Files.newDirectoryStream`.
// Es una interfaz sin implementaciones, y eso esta bien: es el tipo que las firmas nombran, y el
// dia que haya un nativo que enumere directorios lo unico que falta es la clase.
//
// @param <T> el tipo de las entradas
public interface SecureDirectoryStream<T> extends DirectoryStream<T> {

    /** Abre un subdirectorio relativo a este. */
    SecureDirectoryStream<T> newDirectoryStream(T path, LinkOption... options) throws IOException;

    /**
     * Abre un canal sobre una entrada relativa a este directorio.
     *
     * <p>Sin `CREATE` ni `CREATE_NEW` en `options` el archivo tiene que existir; con `CREATE_NEW`
     * la creacion es atomica respecto de este directorio abierto, que es de lo que este tipo
     * protege.
     */
    SeekableByteChannel newByteChannel(T path, Set<? extends OpenOption> options,
            FileAttribute<?>... attrs) throws IOException;

    /** Borra un archivo relativo a este directorio. */
    void deleteFile(T path) throws IOException;

    /** Borra un subdirectorio (vacio) relativo a este. */
    void deleteDirectory(T path) throws IOException;

    /** Mueve una entrada de este directorio a otro, tambien abierto. */
    void move(T srcpath, SecureDirectoryStream<T> targetdir, T targetpath) throws IOException;

    /** Una vista de atributos del propio directorio abierto. */
    <V extends FileAttributeView> V getFileAttributeView(Class<V> type);

    /** Una vista de atributos de una entrada relativa a este directorio. */
    <V extends FileAttributeView> V getFileAttributeView(T path, Class<V> type,
            LinkOption... options);
}
