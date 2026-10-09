package javax.swing.filechooser;

import java.io.File;

import javax.swing.Icon;

/**
 * How a file is shown to the user: its name, its icon, its description.
 *
 * <h2>The convention of returning {@code null}</h2>
 *
 * <p>The five methods return {@code null} by default, and that does not mean "I do not know":
 * it means <strong>"use what you were going to use"</strong>. Whoever asks falls back on the
 * system's {@link FileSystemView}.
 *
 * <p>That convention is what makes the class practical. A {@code FileView} that only wants to
 * change the icon of {@code .java} files writes one method, answers {@code null} to everything
 * else, and the rest goes on looking as the system shows it. Without it the five would have to be
 * reimplemented.
 *
 * <p>{@link #isTraversable} returns {@link Boolean} and not {@code boolean} precisely because of
 * this: it needs a third value. It is the only way to tell "it is not traversable" from "you
 * decide".
 */
public abstract class FileView {

    /** For the subclasses. */
    protected FileView() {
    }

    /** The name to show, or {@code null} to leave the system's. */
    public String getName(File f) {
        return null;
    }

    /** A description of this file in particular, or {@code null}. */
    public String getDescription(File f) {
        return null;
    }

    /**
     * A description of the file type, or {@code null}.
     *
     * <p>Different from {@link #getDescription}: that one talks about <em>this</em> file, this one
     * about its kind -- "Text document" against "The meeting notes".
     */
    public String getTypeDescription(File f) {
        return null;
    }

    /** The icon, or {@code null} to leave the system's. */
    public Icon getIcon(File f) {
        return null;
    }

    /**
     * Whether {@code f} can be entered, or {@code null} to let the system decide.
     *
     * <p>It is not the same as being a directory: a compressed folder may be traversable without
     * being one, and a directory with no permissions may not be, being one.
     */
    public Boolean isTraversable(File f) {
        return null;
    }
}
