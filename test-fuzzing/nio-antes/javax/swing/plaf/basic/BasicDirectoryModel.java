package javax.swing.plaf.basic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.JFileChooser;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileSystemView;

/**
 * The list of files a {@link JFileChooser} shows.
 *
 * <h2>Folders first, files afterwards</h2>
 *
 * <p>The model keeps two separate lists and presents them as one: {@link #getElementAt} serves
 * the folders first and the files afterwards. It is not an aesthetic preference -- it is what
 * makes navigating fast: folders are where one goes, and they are always at the top, in the
 * same place.
 *
 * <p>Within each group the order is decided by {@link #lt}, which compares by name ignoring
 * case. A subclass that wants to sort by date or by size redefines that method and touches
 * nothing else.
 *
 * <h2>The loading is synchronous, and in the JDK it is not</h2>
 *
 * <p>The JDK reads the folder in another thread, because listing a network folder or a sleeping
 * disk may take a while and would freeze the window. Here it is read in the calling thread, and
 * it is on purpose: this VM has a collection problem whereby {@code new File(parent, child)} in
 * a secondary thread kills the thread without throwing anything -- see {@code java/BxDbgF.java},
 * which reproduces it in three lines --, and the loader would be doing exactly that. A model
 * that answers zero for ever is worse than one that takes a while.
 *
 * <p>The difference that shows is in our favour: in the JDK {@link #getSize} may answer zero
 * right after creating the model and the real number a few milliseconds later; here it is
 * already there. When the VM's problem is fixed, this goes back to being a thread.
 *
 * <p>{@link #invalidateFileCache} throws away what was read and {@link #validateFileCache}
 * reads again; the {@link JFileChooser} calls them when it changes folder, filter, or whether
 * it shows the hidden ones.
 *
 * <h2>Out of range blows up</h2>
 *
 * <p>{@link #getElementAt} with an index that does not exist throws
 * {@code ArrayIndexOutOfBoundsException}, on both sides. It checks nothing: it lets the
 * {@link Vector} inside blow up, and it is measured.
 */
public class BasicDirectoryModel extends AbstractListModel<Object>
        implements PropertyChangeListener {

    private final JFileChooser filechooser;
    private Vector<File> fileCache = new Vector<File>();
    private Vector<File> directories;
    private Vector<File> files;
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /** For that chooser; it starts reading its folder right away. */
    public BasicDirectoryModel(JFileChooser filechooser) {
        this.filechooser = filechooser;
        validateFileCache();
    }

    /** It throws away what was read and reads again; see the class note. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)
                || JFileChooser.FILE_VIEW_CHANGED_PROPERTY.equals(prop)
                || JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(prop)
                || JFileChooser.FILE_HIDING_CHANGED_PROPERTY.equals(prop)
                || JFileChooser.FILE_SELECTION_MODE_CHANGED_PROPERTY.equals(prop)) {
            invalidateFileCache();
            validateFileCache();
        } else if ("UI".equals(prop) || "JFileChooserDialogIsClosingProperty".equals(prop)) {
            invalidateFileCache();
        }
    }

    /** It forgets the two derived lists; the next query builds them again. */
    public void invalidateFileCache() {
        directories = null;
        files = null;
    }

    /**
     * The folders, with {@code ".."} in front.
     *
     * <p>That first element is not in the model -- {@link #getSize} does not count it and
     * {@link #getElementAt} does not return it --: it is the shortcut to the directory above,
     * which the chooser draws separately. It is measured, and it is easy to get confused:
     * {@code getDirectories().size()} plus {@code getFiles().size()} gives one more than
     * {@code getSize()}.
     */
    public Vector<File> getDirectories() {
        synchronized (fileCache) {
            if (directories != null) {
                return directories;
            }
            buildLists();
            return directories;
        }
    }

    /**
     * The files that are not folders.
     *
     * <p>Mind the name: it is not "everything there is", it is "what is not a folder". The whole
     * lot is asked for by going through the model with {@link #getElementAt}.
     */
    public Vector<File> getFiles() {
        synchronized (fileCache) {
            if (files != null) {
                return files;
            }
            buildLists();
            return files;
        }
    }

    /** It splits what was read into folders and files; see {@link #getDirectories}. */
    private void buildLists() {
        Vector<File> newFolders = new Vector<File>();
        Vector<File> newFiles = new Vector<File>();
        newFolders.addElement(filechooser.getFileSystemView()
                .createFileObject(filechooser.getCurrentDirectory(), ".."));
        for (int i = 0; i < fileCache.size(); i++) {
            File f = fileCache.get(i);
            if (filechooser.isTraversable(f)) {
                newFolders.addElement(f);
            } else {
                newFiles.addElement(f);
            }
        }
        directories = newFolders;
        files = newFiles;
    }

    /** It reads the folder; see the class note about why it is not in another thread. */
    public void validateFileCache() {
        File currentDirectory = filechooser.getCurrentDirectory();
        if (currentDirectory == null) {
            return;
        }
        FileSystemView fsv = filechooser.getFileSystemView();
        File[] read = fsv.getFiles(currentDirectory, filechooser.isFileHidingEnabled());
        if (read == null) {
            return;
        }
        Vector<File> traversable = new Vector<File>();
        Vector<File> loose = new Vector<File>();
        for (int i = 0; i < read.length; i++) {
            File f = read[i];
            if (!filechooser.accept(f)) {
                continue;
            }
            if (filechooser.isTraversable(f)) {
                traversable.addElement(f);
            } else if (filechooser.isFileSelectionEnabled()) {
                loose.addElement(f);
            }
        }
        sort(traversable);
        sort(loose);
        Vector<File> newCache = new Vector<File>();
        newCache.addAll(traversable);
        newCache.addAll(loose);
        synchronized (this) {
            fileCache = newCache;
            invalidateFileCache();
        }
        fireContentsChanged();
    }

    /**
     * It changes a file's name.
     *
     * <p>If it goes well, it reads the folder again: the file changes place in the order.
     */
    public boolean renameFile(File oldFile, File newFile) {
        synchronized (this) {
            if (oldFile.renameTo(newFile)) {
                validateFileCache();
                return true;
            }
            return false;
        }
    }

    /** It tells that the whole list changed. */
    public void fireContentsChanged() {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public int getSize() {
        return fileCache.size();
    }

    public boolean contains(Object o) {
        return fileCache.contains(o);
    }

    public int indexOf(Object o) {
        return fileCache.indexOf(o);
    }

    /**
     * The element at that position: the folders first, the files afterwards.
     *
     * @throws ArrayIndexOutOfBoundsException if the index does not exist; see the class note
     */
    public Object getElementAt(int index) {
        return fileCache.elementAt(index);
    }

    /** It does nothing: the model learns about the changes on its own, not through events. */
    public void intervalAdded(ListDataEvent e) {
    }

    /** The same. */
    public void intervalRemoved(ListDataEvent e) {
    }

    /**
     * It sorts the list with {@link #lt}.
     *
     * <p>Insertion sort: the list is the size of a folder and the code is read in one sitting.
     * With folders of a hundred thousand files it would have to be changed.
     */
    protected void sort(Vector<? extends File> v) {
        for (int i = 1; i < v.size(); i++) {
            File current = v.get(i);
            int j = i - 1;
            while (j >= 0 && lt(current, v.get(j))) {
                putAt(v, j + 1, v.get(j));
                j--;
            }
            putAt(v, j + 1, current);
        }
    }

    /** The {@code set} with the wildcard in place; see finding #516 about this. */
    @SuppressWarnings("unchecked")
    private static void putAt(Vector<? extends File> v, int i, File f) {
        ((Vector<File>) v).set(i, f);
    }

    /**
     * Whether {@code a} goes before {@code b}.
     *
     * <p>By name and ignoring case: in a list of files, {@code Documents} and {@code documents}
     * have to end up together, not one at each end.
     */
    protected boolean lt(File a, File b) {
        return a.getName().compareToIgnoreCase(b.getName()) < 0;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return changeSupport.getPropertyChangeListeners();
    }

    /** In order to tell about the model's own changes; see {@link #addPropertyChangeListener}. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}
