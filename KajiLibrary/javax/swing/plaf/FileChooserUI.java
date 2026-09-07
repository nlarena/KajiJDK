package javax.swing.plaf;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

/**
 * El aspecto de un {@link JFileChooser}.
 *
 * <h2>Por que el filtro de "todos" lo da el aspecto</h2>
 *
 * <p>{@link #getAcceptAllFileFilter} devuelve el filtro que acepta cualquier archivo. Podria ser
 * una constante del selector, pero su descripcion es texto que se le muestra al usuario y depende
 * del idioma y del sistema; por eso lo arma el aspecto, que es quien tiene la tabla de textos.
 *
 * <p>Lo mismo vale para {@link #getApproveButtonText} y {@link #getDialogTitle}: el selector solo
 * los pide cuando el programa no puso los suyos.
 */
public abstract class FileChooserUI extends ComponentUI {

    /** Para las subclases. */
    protected FileChooserUI() {
    }

    /** El filtro que acepta todo; ver la nota de la clase. */
    public abstract FileFilter getAcceptAllFileFilter(JFileChooser fc);

    /** La vista con la que se nombran y dibujan los archivos. */
    public abstract FileView getFileView(JFileChooser fc);

    /** El texto del boton de aceptar. */
    public abstract String getApproveButtonText(JFileChooser fc);

    /** El titulo del dialogo. */
    public abstract String getDialogTitle(JFileChooser fc);

    /** Vuelve a leer la carpeta actual del disco. */
    public abstract void rescanCurrentDirectory(JFileChooser fc);

    /** Desplaza la lista para que ese archivo se vea. */
    public abstract void ensureFileIsVisible(JFileChooser fc, File f);

    /**
     * El boton que se activa con Enter.
     *
     * @return nulo si el aspecto no designa ninguno.
     */
    public JButton getDefaultButton(JFileChooser fc) {
        return null;
    }
}
