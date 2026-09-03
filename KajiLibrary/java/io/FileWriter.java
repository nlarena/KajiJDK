package java.io;

import java.nio.charset.Charset;

// KajiLibrary's java.io.FileWriter -- caracteres escritos a un archivo.
//
// El espejo de `FileReader`: es `new OutputStreamWriter(new FileOutputStream(f, append), cs)` con
// un nombre corto.
//
// **Hay que cerrar.** Lo hereda de `FileOutputStream`, y aca duele mas que alla porque el que
// escribe texto suele confiar en que "ya esta escrito": un `FileWriter` abandonado sin `close()`
// deja el archivo vacio o a medias. Ver la nota de `FileOutputStream`.
//
// El parametro `append` decide entre agregar al final y **truncar**, y truncar es lo que pasa por
// omision: abrir un archivo con `new FileWriter(f)` borra lo que hubiera aunque despues no se
// escriba nada. Es el comportamiento del JDK, y es la fuente de perdida de datos mas comun de todo
// el paquete.
public class FileWriter extends OutputStreamWriter {

    /**
     * Abre `fileName` con el charset por omision, **truncando** lo que hubiera.
     *
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(String fileName) throws IOException {
        super(new FileOutputStream(fileName));
    }

    /**
     * Abre `fileName` con el charset por omision.
     *
     * @param append si lo escrito se agrega al final en vez de reemplazar el contenido
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(String fileName, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append));
    }

    /**
     * Abre `file` con el charset por omision, **truncando** lo que hubiera.
     *
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(File file) throws IOException {
        super(new FileOutputStream(file));
    }

    /**
     * Abre `file` con el charset por omision.
     *
     * @param append si lo escrito se agrega al final en vez de reemplazar el contenido
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
    }

    /** Escribe por descriptor. Esta biblioteca no modela descriptores; ver `FileOutputStream`. */
    public FileWriter(FileDescriptor fd) {
        super(new FileOutputStream(fd));
    }

    /**
     * Abre `fileName` con el charset dado, **truncando** lo que hubiera.
     *
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(String fileName, Charset charset) throws IOException {
        super(new FileOutputStream(fileName), charset);
    }

    /**
     * Abre `fileName` con el charset dado.
     *
     * @param append si lo escrito se agrega al final en vez de reemplazar el contenido
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(String fileName, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(fileName, append), charset);
    }

    /**
     * Abre `file` con el charset dado, **truncando** lo que hubiera.
     *
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(File file, Charset charset) throws IOException {
        super(new FileOutputStream(file), charset);
    }

    /**
     * Abre `file` con el charset dado.
     *
     * @param append si lo escrito se agrega al final en vez de reemplazar el contenido
     * @throws IOException si es un directorio, o no se puede escribir ahi
     */
    public FileWriter(File file, Charset charset, boolean append) throws IOException {
        super(new FileOutputStream(file, append), charset);
    }
}
