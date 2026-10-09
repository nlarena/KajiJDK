package java.awt;

import java.io.File;
import java.io.FilenameFilter;

/**
 * The system's "open" or "save" box.
 *
 * <p>It is **modal**: {@link #setVisible setVisible(true)} does not come back until the user
 * chooses or cancels, and only then does {@link #getFile} have the answer. A `null` there means
 * they cancelled.
 *
 * <p>Without a screen there is no system box to show, so nothing ever gets chosen and the file
 * stays as {@link #setFile} left it. The rest of the class --the mode, the directory, the filter,
 * the multiple selection-- works in full: it is state, not interface.
 *
 * <p>The {@link FilenameFilter} carries a warning that comes from the JDK and not from here: on
 * Windows it **is not used**, because the native box filters by extension and takes no predicate.
 * Whatever is set is kept and {@link #getFilenameFilter} returns it, but it does not change what
 * the user sees.
 */
public class FileDialog extends Dialog {

    private static final long serialVersionUID = 5035145889651310422L;

    private static int fileDialogCounter = 0;

    /** The box is for opening. */
    public static final int LOAD = 0;

    /** It is for saving. */
    public static final int SAVE = 1;

    /** Which of the two. */
    int mode;

    /** Which directory it starts in. */
    String dir;

    /** Which file ended up chosen, or `null` if none. */
    String file;

    /** The filter, or `null`. */
    FilenameFilter filter;

    /** Whether it lets several be chosen. */
    private boolean multipleMode = false;

    /** The chosen files. */
    private File[] files = new File[0];

    /** An open box with no title, hanging from that frame. */
    public FileDialog(Frame parent) {
        this(parent, "", LOAD);
    }

    /** An open box with that title. */
    public FileDialog(Frame parent, String title) {
        this(parent, title, LOAD);
    }

    /**
     * A box with that title and that mode.
     *
     * @throws IllegalArgumentException if the mode is neither {@link #LOAD} nor {@link #SAVE}
     */
    public FileDialog(Frame parent, String title, int mode) {
        super(parent, title, true);
        this.setMode(mode);
        this.setLayout(null);
    }

    /** An open box with no title, hanging from that dialog. */
    public FileDialog(Dialog parent) {
        this(parent, "", LOAD);
    }

    /** An open box with that title, hanging from that dialog. */
    public FileDialog(Dialog parent, String title) {
        this(parent, title, LOAD);
    }

    /**
     * A box with that title and that mode, hanging from that dialog.
     *
     * @throws IllegalArgumentException if the mode is neither {@link #LOAD} nor {@link #SAVE}
     */
    public FileDialog(Dialog parent, String title, int mode) {
        super(parent, title, true);
        this.setMode(mode);
        this.setLayout(null);
    }

    /**
     * Changes the title.
     *
     * <p>A `null` is taken as the empty string: the system box cannot be left with no title.
     */
    public void setTitle(String title) {
        super.setTitle(title == null ? "" : title);
    }

    String constructComponentName() {
        synchronized (FileDialog.class) {
            String n = "filedlg" + fileDialogCounter;
            fileDialogCounter = fileDialogCounter + 1;
            return n;
        }
    }

    /** Declares it showable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Whether it is for opening or for saving. */
    public int getMode() {
        return this.mode;
    }

    /**
     * Changes the mode.
     *
     * @throws IllegalArgumentException if it is neither {@link #LOAD} nor {@link #SAVE}
     */
    public void setMode(int mode) {
        if (mode != LOAD && mode != SAVE) {
            throw new IllegalArgumentException("illegal file dialog mode");
        }
        this.mode = mode;
    }

    /**
     * Which directory it starts in.
     *
     * @return the directory, or `null` if none was set
     */
    public String getDirectory() {
        return this.dir;
    }

    /**
     * Changes the starting directory.
     *
     * <p>An empty string counts as `null`, which is how the JDK tells "no preference" from a real
     * directory.
     */
    public void setDirectory(String dir) {
        this.dir = dir != null && dir.isEmpty() ? null : dir;
    }

    /**
     * Which file ended up chosen.
     *
     * @return the name, or `null` if the user cancelled or the box never got to be shown
     */
    public String getFile() {
        return this.file;
    }

    /**
     * The chosen files.
     *
     * @return the files; an empty array if none was chosen. Never `null`.
     */
    public File[] getFiles() {
        synchronized (this.getObjectLock()) {
            File[] r = new File[this.files.length];
            System.arraycopy(this.files, 0, r, 0, this.files.length);
            return r;
        }
    }

    /**
     * Sets the file the box shows to begin with.
     *
     * <p>It is also what {@link #getFile} returns while nobody chooses anything else. An empty
     * string counts as `null`, just as in {@link #setDirectory}.
     */
    public void setFile(String file) {
        this.file = file != null && file.isEmpty() ? null : file;
        synchronized (this.getObjectLock()) {
            if (this.file == null) {
                this.files = new File[0];
            } else {
                this.files = new File[1];
                this.files[0] = new File(this.file);
            }
        }
    }

    /** Lets several files be chosen, or only one. */
    public void setMultipleMode(boolean enable) {
        synchronized (this.getObjectLock()) {
            this.multipleMode = enable;
        }
    }

    /** Whether it lets several be chosen. */
    public boolean isMultipleMode() {
        synchronized (this.getObjectLock()) {
            return this.multipleMode;
        }
    }

    /**
     * The filter.
     *
     * @return the filter, or `null` if none was set
     */
    public FilenameFilter getFilenameFilter() {
        return this.filter;
    }

    /** Changes the filter; see the warning of the class about Windows. */
    public synchronized void setFilenameFilter(FilenameFilter filter) {
        this.filter = filter;
    }

    protected String paramString() {
        return super.paramString() + ",dir= " + this.dir
                + ",file= " + this.file + (this.mode == LOAD ? ",load" : ",save");
    }

    boolean postsOldMouseEvents() {
        return false;
    }
}
