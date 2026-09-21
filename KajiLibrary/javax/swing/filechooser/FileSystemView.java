package javax.swing.filechooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

/**
 * What the file chooser needs to know about the system and {@link File} does not tell.
 *
 * <h2>What {@link File} lacks</h2>
 *
 * <p>{@code File} is a path: it knows whether it exists, whether it is a directory, what it
 * contains. What it does not know is anything the <em>desktop</em> adds on top -- that
 * {@code C:\} is called "Local disk", that there is a "My documents" folder which is not a
 * fixed path, that a shortcut points elsewhere, that such an icon corresponds to such a type.
 * This class is that layer.
 *
 * <p>The division matters because they are two different models: the file system's and the one
 * the user sees. A chooser that showed only the first would be correct and alien.
 *
 * <h2>What this VM answers</h2>
 *
 * <p>{@link #getFileSystemView} returns a <strong>generic</strong> view, built on what
 * {@link File} does know. It is honest and limited, and the limit is worth being clear about:
 * the names to show are the file system's, not the desktop's; {@link #getSystemIcon} returns
 * {@code null} because the system's icons are not Java's business; and shortcuts are not
 * resolved, because that asks for talking to the shell. Each of those methods says what it
 * returns, instead of faking a desktop datum nobody gave it.
 */
public abstract class FileSystemView {

    private static FileSystemView generic;

    /**
     * The system's view.
     *
     * <p>In the real JDK it chooses between a Windows implementation, a Unix one and a generic one.
     * Here there is only one: telling them apart would serve to give desktop names, and those are
     * not available on either platform from pure Java.
     */
    public static FileSystemView getFileSystemView() {
        if (generic == null) {
            generic = new GenericView();
        }
        return generic;
    }

    /** For the subclasses. */
    public FileSystemView() {
    }

    /**
     * Whether {@code f} is a root of the tree the user sees.
     *
     * <p>It is not the same as having no parent: on Windows the desktop is a root for the user and
     * sits inside the profile.
     */
    public boolean isRoot(File f) {
        if (f == null || !f.isAbsolute()) {
            return false;
        }
        File[] roots = getRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].equals(f)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code f} can be entered; {@code null} only if it cannot be decided. */
    public Boolean isTraversable(File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    /**
     * The name to show.
     *
     * <p>The file system's, not the desktop's: a folder Windows shows translated appears here with
     * its real name.
     */
    public String getSystemDisplayName(File f) {
        if (f == null) {
            return null;
        }
        String name = f.getName();
        // A root such as `C:\` has an empty name, and showing nothing would be worse than showing
        // the path.
        if (name.isEmpty()) {
            return f.getPath();
        }
        return name;
    }

    /** The type's description; {@code null} in this VM, which does not talk to the desktop. */
    public String getSystemTypeDescription(File f) {
        return null;
    }

    /** The system icon; {@code null} in this VM. See the class note. */
    public Icon getSystemIcon(File f) {
        return null;
    }

    /** The system icon in the requested size; {@code null} in this VM. */
    public Icon getSystemIcon(File f, int width, int height) {
        return null;
    }

    /** Whether {@code folder} is {@code file}'s parent. */
    public boolean isParent(File folder, File file) {
        if (folder == null || file == null) {
            return false;
        }
        File parent = file.getParentFile();
        return folder.equals(parent);
    }

    /** The child of {@code parent} called {@code fileName}. */
    public File getChild(File parent, String fileName) {
        return createFileObject(parent, fileName);
    }

    /**
     * Whether {@code f} is a real file and not a node invented by the desktop.
     *
     * <p>The second thing exists: "My PC" appears in the tree and is not a path. Here everything
     * that arrives is a path, so the answer is always {@code true}.
     */
    public boolean isFileSystem(File f) {
        return true;
    }

    /** Creates a new folder; the only thing implementations have to write. */
    public abstract File createNewFolder(File containingDir) throws IOException;

    /** Whether it is hidden. */
    public boolean isHiddenFile(File f) {
        return f.isHidden();
    }

    /** Whether it is a file system root. */
    public boolean isFileSystemRoot(File dir) {
        return dir != null && dir.getParentFile() == null;
    }

    /** Whether it is a drive. Without talking to the system it cannot be known: {@code false}. */
    public boolean isDrive(File dir) {
        return false;
    }

    /** Whether it is a floppy drive. {@code false} for the same reason. */
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /** Whether it is a network node. {@code false} for the same reason. */
    public boolean isComputerNode(File dir) {
        return false;
    }

    /** The tree's roots. */
    public File[] getRoots() {
        return File.listRoots();
    }

    /** The user's folder. */
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /** Where a freshly opened chooser starts showing. */
    public File getDefaultDirectory() {
        return getHomeDirectory();
    }

    /** A child {@link File}, of the kind this view uses. */
    public File createFileObject(File dir, String filename) {
        if (dir == null) {
            return new File(filename);
        }
        return new File(dir, filename);
    }

    /** A {@link File} from a path. */
    public File createFileObject(String path) {
        File f = new File(path);
        if (isFileSystemRoot(f)) {
            return createFileSystemRoot(f);
        }
        return f;
    }

    /**
     * What is inside {@code dir}.
     *
     * @param useFileHiding whether hidden files are hidden
     */
    public File[] getFiles(File dir, boolean useFileHiding) {
        List<File> visibles = new ArrayList<File>();
        File[] contents = dir.listFiles();
        // `listFiles` returns `null` --not an empty array-- when the directory cannot be read.
        // Confusing the two cases is a `NullPointerException` at the worst moment: while
        // navigating.
        if (contents == null) {
            return new File[0];
        }
        for (int i = 0; i < contents.length; i++) {
            File f = contents[i];
            if (!useFileHiding || !isHiddenFile(f)) {
                visibles.add(f);
            }
        }
        return visibles.toArray(new File[visibles.size()]);
    }

    /** The parent, or {@code null} if it is a root. */
    public File getParentDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return null;
        }
        return dir.getParentFile();
    }

    /** The places the chooser offers in its drop-down list. */
    public File[] getChooserComboBoxFiles() {
        return getRoots();
    }

    /** The places the chooser offers in its shortcut panel. */
    public final File[] getChooserShortcutPanelFiles() {
        return new File[0];
    }

    /** Whether it is a shortcut. Without talking to the shell it cannot be known: {@code false}. */
    public boolean isLink(File file) {
        return false;
    }

    /**
     * Where a shortcut points.
     *
     * @return {@code null} always in this VM, which is what matches {@link #isLink} saying that
     *     nothing is a link
     */
    public File getLinkLocation(File file) throws FileNotFoundException {
        return null;
    }

    /** The {@link File} that represents a file system root. */
    protected File createFileSystemRoot(File f) {
        return new FileSystemRoot(f);
    }

    /**
     * A root, which behaves differently in two things.
     *
     * <p>{@code C:\} is <strong>always</strong> a directory even if the drive is empty or does not
     * answer, and its name is not the empty string {@link File#getName} would return but its path.
     * Without this class, a drive with no disk would disappear from the tree.
     */
    static class FileSystemRoot extends File {

        private static final long serialVersionUID = 1L;

        public FileSystemRoot(File f) {
            super(f, "");
        }

        public FileSystemRoot(String s) {
            super(s);
        }

        public boolean isDirectory() {
            return true;
        }

        public String getName() {
            return getPath();
        }
    }

    /** The only implementation of this VM; see {@link FileSystemView}'s note. */
    static class GenericView extends FileSystemView {

        GenericView() {
        }

        public File createNewFolder(File containingDir) throws IOException {
            if (containingDir == null) {
                throw new IOException("The containing directory is needed");
            }
            File candidate = createFileObject(containingDir, "NewFolder");
            // The name is numbered until a free one is found. Creating blindly would overwrite a
            // folder of the user's, and failing at the first try would force a rename before the
            // second one could be created.
            int i = 2;
            while (candidate.exists() && i < 100) {
                candidate = createFileObject(containingDir, "NewFolder." + String.valueOf(i));
                i = i + 1;
            }
            if (candidate.exists()) {
                throw new IOException(
                    "The directory already exists: " + candidate.getAbsolutePath());
            }
            if (!candidate.mkdir()) {
                throw new IOException("Could not create " + candidate.getAbsolutePath());
            }
            return candidate;
        }
    }
}
