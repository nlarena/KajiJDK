package javax.swing.plaf;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;

/**
 * A {@link JFileChooser}'s look and feel.
 *
 * <h2>Why the "accept all" filter comes from the look and feel</h2>
 *
 * <p>{@link #getAcceptAllFileFilter} returns the filter that accepts any file. It could be a
 * constant of the chooser's, but its description is text shown to the user and depends on the
 * language and the system; that is why the look and feel builds it, being the one that has the
 * table of texts.
 *
 * <p>The same goes for {@link #getApproveButtonText} and {@link #getDialogTitle}: the chooser
 * only asks for them when the program did not set its own.
 */
public abstract class FileChooserUI extends ComponentUI {

    /** For the subclasses. */
    protected FileChooserUI() {
    }

    /** The filter that accepts everything; see the class note. */
    public abstract FileFilter getAcceptAllFileFilter(JFileChooser fc);

    /** The view with which files are named and drawn. */
    public abstract FileView getFileView(JFileChooser fc);

    /** The accept button's text. */
    public abstract String getApproveButtonText(JFileChooser fc);

    /** The dialog's title. */
    public abstract String getDialogTitle(JFileChooser fc);

    /** Reads the current folder from disk again. */
    public abstract void rescanCurrentDirectory(JFileChooser fc);

    /** Scrolls the list so that that file is seen. */
    public abstract void ensureFileIsVisible(JFileChooser fc, File f);

    /**
     * The button that is activated with Enter.
     *
     * @return null if the look and feel designates none.
     */
    public JButton getDefaultButton(JFileChooser fc) {
        return null;
    }
}
