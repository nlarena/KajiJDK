package java.awt.desktop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.FilesEvent -- an event that carries files.
 *
 * <p>The base of {@link OpenFilesEvent} and {@link PrintFilesEvent}. Its constructor is
 * package-private: the class is not instantiated on its own.
 *
 * <h2>{@link #getFiles} returns a copy</h2>
 *
 * <p>A <b>new and modifiable</b> copy on each call. That has two consequences worth keeping in mind:
 *
 * <ul>
 *   <li>comparing the result of two calls with {@code ==} gives false;
 *   <li>modifying what it returns does not change the event, but modifying the list handed to the
 *       constructor <b>does</b> -- the event keeps that list, not a copy.
 * </ul>
 *
 * <p>It is what the JDK does and it was checked against JDK 25.
 */
public class FilesEvent extends AppEvent {

    private static final long serialVersionUID = 5271763715462312871L;

    /** The list that was handed in, uncopied. See the class note. */
    final List<File> files;

    /** Package-private; only the two subclasses use it. */
    FilesEvent(final List<File> files) {
        this.files = files;
    }

    /** A modifiable copy of the files, or null if there was no list. See the class note. */
    public List<File> getFiles() {
        if (this.files == null) {
            return null;
        }
        return new ArrayList<File>(this.files);
    }
}
