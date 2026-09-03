package java.nio.file.attribute;

import java.io.IOException;

// La vista con la que se leen los `BasicFileAttributes` y se fijan las tres marcas de tiempo.
//
// Su nombre es `"basic"` -- fijo, y el que usa `Files.getAttribute("basic:size", ...)`.
//
// **KajiJDK no tiene una implementacion.** Ni la lectura ni la escritura son posibles: `stat` de
// `jdk.internal.io.Fs` no devuelve tiempos y no hay ningun nativo que los escriba. La interfaz esta
// para que el tipo exista y para que quien traiga los nativos manaña solo tenga que implementarla.
public interface BasicFileAttributeView extends FileAttributeView {

    /** Siempre `"basic"`. */
    String name();

    /** Lee los atributos de una sola vez, para que salgan todos del mismo instante. */
    BasicFileAttributes readAttributes() throws IOException;

    /**
     * Fija las marcas de tiempo. Un argumento en `null` deja esa marca como estaba.
     *
     * <p>Van los tres juntos, y no un metodo por marca, porque cambiar uno solo suele tocar los
     * otros de rebote en el sistema de archivos: hacerlo en una llamada deja explicito que la
     * operacion es una sola.
     */
    void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
            throws IOException;
}
