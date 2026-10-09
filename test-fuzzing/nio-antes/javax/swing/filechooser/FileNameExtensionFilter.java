package javax.swing.filechooser;

import java.io.File;
import java.util.Locale;

/**
 * A {@link FileFilter} by extension, which is the case that covers almost all of them.
 *
 * <p>Example: {@code new FileNameExtensionFilter("Images", "jpg", "png")}.
 *
 * <h2>Three decisions that are not visible in the signature</h2>
 *
 * <p><strong>Directories always pass</strong>, without looking at their name. Without that the
 * user could not navigate to the folder where their images are -- the filter would have hidden
 * the way there. It is the trap {@link FileFilter#accept} warns about.
 *
 * <p><strong>The comparison ignores case</strong>, and is done by lowercasing both sides with
 * {@link Locale#ENGLISH} and not with the system's. It is not a detail: in Turkish, {@code "I"}
 * in lower case is not {@code "i"}, so a file {@code FOTO.JPG} would stop matching
 * {@code "jpg"} on a Turkish machine. A file name is not language text.
 *
 * <p><strong>The extensions are copied</strong> in the constructor. Without the copy, whoever
 * built the filter could change the array afterwards and the filter would change meaning without
 * anybody touching it -- and {@link #getExtensions} returns a copy for the same reason.
 */
public final class FileNameExtensionFilter extends FileFilter {

    private final String description;
    private final String[] extensions;
    private final String[] lowercase;

    /**
     * @param description the text for the filter list
     * @param extensions the extensions, without the dot
     * @throws IllegalArgumentException if there is no extension, or if one is {@code null} or empty
     */
    public FileNameExtensionFilter(String description, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            throw new IllegalArgumentException("At least one extension is needed");
        }
        this.description = description;
        this.extensions = new String[extensions.length];
        this.lowercase = new String[extensions.length];
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i] == null || extensions[i].isEmpty()) {
                throw new IllegalArgumentException("An extension cannot be null or empty");
            }
            this.extensions[i] = extensions[i];
            this.lowercase[i] = extensions[i].toLowerCase(Locale.ENGLISH);
        }
    }

    /** Whether {@code f} is a directory, or its name ends in one of the extensions. */
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        // A name that starts with a dot and has no other one --`.gitignore`-- has no extension: it
        // is a hidden name. Hence the dot has to be after the first character.
        if (dot > 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.ENGLISH);
            for (int i = 0; i < this.lowercase.length; i++) {
                if (this.lowercase[i].equals(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The text for the filter list. */
    public String getDescription() {
        return this.description;
    }

    /** The extensions, just as they were passed, in a new array. */
    public String[] getExtensions() {
        String[] copy = new String[this.extensions.length];
        for (int i = 0; i < this.extensions.length; i++) {
            copy[i] = this.extensions[i];
        }
        return copy;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[description=");
        sb.append(getDescription());
        sb.append(" extensions=[");
        for (int i = 0; i < this.extensions.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.extensions[i]);
        }
        sb.append("]]");
        return sb.toString();
    }
}
