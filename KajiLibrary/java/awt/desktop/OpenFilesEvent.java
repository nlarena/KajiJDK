package java.awt.desktop;

import java.io.File;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.OpenFilesEvent -- el sistema pide abrir archivos.
 *
 * <p>Lo entrega {@link OpenFilesHandler}. Llega cuando alguien arrastra archivos al icono del
 * programa, o hace doble clic en uno cuyo tipo el programa declaro manejar.
 *
 * <h2>El termino de busqueda</h2>
 *
 * <p>{@link #getSearchTerm} es la parte que sorprende. Cuando el archivo se abre desde el buscador del
 * escritorio, trae <b>lo que el usuario habia escrito</b> para encontrarlo. Un editor puede usarlo
 * para saltar directo a esa palabra dentro del documento, que es exactamente lo que el usuario espera
 * y casi nadie implementa.
 *
 * <p>Es la cadena vacia --no null-- cuando no vino de una busqueda.
 */
public final class OpenFilesEvent extends FilesEvent {

    private static final long serialVersionUID = -3982871005867718956L;

    /** Lo que el usuario habia buscado, o vacio. */
    final String searchTerm;

    /**
     * @param files los archivos a abrir
     * @param searchTerm lo que el usuario habia buscado; null se guarda como cadena vacia
     */
    public OpenFilesEvent(final List<File> files, final String searchTerm) {
        super(files);
        if (searchTerm == null) {
            this.searchTerm = "";
        } else {
            this.searchTerm = searchTerm;
        }
    }

    /** Lo que el usuario habia buscado; vacio si no vino de una busqueda. Ver la nota de la clase. */
    public String getSearchTerm() {
        return this.searchTerm;
    }
}
