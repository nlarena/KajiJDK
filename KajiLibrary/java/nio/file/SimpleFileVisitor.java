package java.nio.file;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

// Un `FileVisitor` que no hace nada y sigue de largo, para heredar y sobreescribir solo lo que
// interesa.
//
// **Las fallas se relanzan, no se tragan.** `visitFileFailed` y `postVisitDirectory` levantan la
// excepcion que reciben. Es la eleccion prudente para un valor por omision: una subclase que quiera
// ignorar errores tiene que decirlo, y no al reves -- un recorrido que se saltea archivos en
// silencio da un resultado incompleto sin avisar.
//
// @param <T> el tipo de las rutas
public class SimpleFileVisitor<T> implements FileVisitor<T> {

    /** Para las subclases. */
    protected SimpleFileVisitor() {
    }

    /** Entra al directorio. */
    public FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(dir);
        Objects.requireNonNull(attrs);
        return FileVisitResult.CONTINUE;
    }

    /** No hace nada con el archivo. */
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(attrs);
        return FileVisitResult.CONTINUE;
    }

    /** Relanza la falla. */
    public FileVisitResult visitFileFailed(T file, IOException exc) throws IOException {
        Objects.requireNonNull(file);
        throw exc;
    }

    /** Relanza la falla si la hubo; si no, sigue. */
    public FileVisitResult postVisitDirectory(T dir, IOException exc) throws IOException {
        Objects.requireNonNull(dir);
        if (exc != null) {
            throw exc;
        }
        return FileVisitResult.CONTINUE;
    }
}
