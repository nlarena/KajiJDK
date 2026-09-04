package java.awt.desktop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.FilesEvent -- un evento que trae archivos.
 *
 * <p>La base de {@link OpenFilesEvent} y {@link PrintFilesEvent}. Su constructor es de acceso de
 * paquete: la clase no se instancia sola.
 *
 * <h2>{@link #getFiles} devuelve una copia</h2>
 *
 * <p>Una copia <b>nueva y modificable</b> en cada llamada. Eso tiene dos consecuencias que conviene
 * tener presentes:
 *
 * <ul>
 *   <li>comparar con {@code ==} el resultado de dos llamadas da false;
 *   <li>modificar lo que devuelve no cambia el evento, pero modificar la lista que se le paso al
 *       constructor <b>si</b> -- el evento guarda esa lista, no una copia.
 * </ul>
 *
 * <p>Es lo que hace el JDK y se comprobo contra el JDK 25.
 */
public class FilesEvent extends AppEvent {

    private static final long serialVersionUID = 5271763715462312871L;

    /** La lista que se paso, sin copiar. Ver la nota de la clase. */
    final List<File> files;

    /** De acceso de paquete; solo las dos subclases lo usan. */
    FilesEvent(final List<File> files) {
        this.files = files;
    }

    /** Una copia modificable de los archivos, o null si no habia lista. Ver la nota de la clase. */
    public List<File> getFiles() {
        if (this.files == null) {
            return null;
        }
        return new ArrayList<File>(this.files);
    }
}
