package java.awt.desktop;

import java.io.File;
import java.util.List;

/**
 * KajiLibrary's java.awt.desktop.OpenFilesEvent -- the system asks for files to be opened.
 *
 * <p>It is handed over by {@link OpenFilesHandler}. It arrives when someone drags files onto the
 * program's icon, or double-clicks one whose type the program declared it handles.
 *
 * <h2>The search term</h2>
 *
 * <p>{@link #getSearchTerm} is the part that surprises. When the file is opened from the desktop's
 * search, it brings <b>what the user had typed</b> to find it. An editor can use it to jump straight
 * to that word inside the document, which is exactly what the user expects and almost nobody
 * implements.
 *
 * <p>It is the empty string --not null-- when it did not come from a search.
 */
public final class OpenFilesEvent extends FilesEvent {

    private static final long serialVersionUID = -3982871005867718956L;

    /** What the user had searched for, or empty. */
    final String searchTerm;

    /**
     * @param files the files to open
     * @param searchTerm what the user had searched for; null is stored as the empty string
     */
    public OpenFilesEvent(final List<File> files, final String searchTerm) {
        super(files);
        if (searchTerm == null) {
            this.searchTerm = "";
        } else {
            this.searchTerm = searchTerm;
        }
    }

    /** What the user had searched for; empty if it did not come from a search. See the class note. */
    public String getSearchTerm() {
        return this.searchTerm;
    }
}
