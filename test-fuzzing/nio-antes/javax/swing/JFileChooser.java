package javax.swing;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FileChooserUI;

/**
 * The file chooser.
 *
 * <h2>It is a component, not a dialog</h2>
 *
 * <p>{@link #showOpenDialog} builds a {@link JDialog} and shows it, but the chooser itself is a
 * {@link JComponent}: it can be put inside a window of one's own. It is what allows a program
 * with the chooser always visible in a pane.
 *
 * <h2>Three things that get confused</h2>
 *
 * <p>The <em>selection mode</em> ({@link #setFileSelectionMode}) says whether files, folders or
 * both are chosen. The <em>filter</em> says which are shown. The <em>system view</em>
 * ({@link FileSystemView}) says what each one is called and what icon it has. They are
 * independent: one may be choosing folders and filtering by extension.
 *
 * <h2>What showing the dialog returns</h2>
 *
 * <p>An integer, not the file. {@link #APPROVE_OPTION} means that the user accepted, and only
 * then does {@link #getSelectedFile} make sense. Ignoring the result and reading the file
 * directly is the commonest mistake with this class: on cancelling the one that was there
 * before is read.
 *
 * <p>With no screen the dialog cannot be shown; the chooser is built and configured all the
 * same.
 */
public class JFileChooser extends JComponent implements Accessible {

    private static final String uiClassID = "FileChooserUI";

    /** An open dialog. */
    public static final int OPEN_DIALOG = 0;

    /** A save dialog. */
    public static final int SAVE_DIALOG = 1;

    /** A dialog with a button text of its own. */
    public static final int CUSTOM_DIALOG = 2;

    /** The user cancelled. */
    public static final int CANCEL_OPTION = 1;

    /** The user accepted; only then does the chosen file hold. */
    public static final int APPROVE_OPTION = 0;

    /** Something failed. */
    public static final int ERROR_OPTION = -1;

    /** Files only. */
    public static final int FILES_ONLY = 0;

    /** Folders only. */
    public static final int DIRECTORIES_ONLY = 1;

    /** Files and folders. */
    public static final int FILES_AND_DIRECTORIES = 2;

    /** The cancel button's command. */
    public static final String CANCEL_SELECTION = "CancelSelection";

    /** The accept button's command. */
    public static final String APPROVE_SELECTION = "ApproveSelection";

    public static final String APPROVE_BUTTON_TEXT_CHANGED_PROPERTY = "ApproveButtonTextChangedProperty";
    public static final String APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY =
            "ApproveButtonToolTipTextChangedProperty";
    public static final String APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY =
            "ApproveButtonMnemonicChangedProperty";
    public static final String CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY =
            "ControlButtonsAreShownChangedProperty";
    public static final String DIRECTORY_CHANGED_PROPERTY = "directoryChanged";
    public static final String SELECTED_FILE_CHANGED_PROPERTY = "SelectedFileChangedProperty";
    public static final String SELECTED_FILES_CHANGED_PROPERTY = "SelectedFilesChangedProperty";
    public static final String MULTI_SELECTION_ENABLED_CHANGED_PROPERTY =
            "MultiSelectionEnabledChangedProperty";
    public static final String FILE_SYSTEM_VIEW_CHANGED_PROPERTY = "FileSystemViewChanged";
    public static final String FILE_VIEW_CHANGED_PROPERTY = "fileViewChanged";
    public static final String FILE_HIDING_CHANGED_PROPERTY = "FileHidingChanged";
    public static final String FILE_FILTER_CHANGED_PROPERTY = "fileFilterChanged";
    public static final String FILE_SELECTION_MODE_CHANGED_PROPERTY = "fileSelectionChanged";
    public static final String ACCESSORY_CHANGED_PROPERTY = "AccessoryChangedProperty";
    public static final String ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY =
            "acceptAllFileFilterUsedChanged";
    public static final String DIALOG_TITLE_CHANGED_PROPERTY = "DialogTitleChangedProperty";
    public static final String DIALOG_TYPE_CHANGED_PROPERTY = "DialogTypeChangedProperty";
    public static final String CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY =
            "ChoosableFileFilterChangedProperty";

    protected AccessibleContext accessibleContext;

    private String dialogTitle = null;
    private String approveButtonText = null;
    private String approveButtonToolTipText = null;
    private int approveButtonMnemonic = 0;
    private Vector<FileFilter> filters = new Vector<FileFilter>(5);
    private JDialog dialog = null;
    private int dialogType = OPEN_DIALOG;
    private int returnValue = ERROR_OPTION;
    private JComponent accessory = null;
    private FileView fileView = null;
    private boolean controlsShown = true;
    private boolean useFileHiding = true;
    private int fileSelectionMode = FILES_ONLY;
    private boolean multiSelectionEnabled = false;
    private boolean useAcceptAllFileFilter = true;
    private boolean dragEnabled = false;
    private FileFilter fileFilter = null;
    private FileSystemView fileSystemView = null;
    private File currentDirectory = null;
    private File selectedFile = null;
    private File[] selectedFiles = null;

    /** A chooser in the user's usual folder. */
    public JFileChooser() {
        this((File) null, (FileSystemView) null);
    }

    /** A chooser in that folder. */
    public JFileChooser(String currentDirectoryPath) {
        this(currentDirectoryPath, (FileSystemView) null);
    }

    /** A chooser in that folder. */
    public JFileChooser(File currentDirectory) {
        this(currentDirectory, (FileSystemView) null);
    }

    /** A chooser with that file system view. */
    public JFileChooser(FileSystemView fsv) {
        this((File) null, fsv);
    }

    /** A chooser in that folder and with that view. */
    public JFileChooser(File currentDirectory, FileSystemView fsv) {
        setup(fsv);
        setCurrentDirectory(currentDirectory);
    }

    /** A chooser in that folder and with that view. */
    public JFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        setup(fsv);
        if (currentDirectoryPath == null) {
            setCurrentDirectory(null);
        } else {
            setCurrentDirectory(fileSystemView.createFileObject(currentDirectoryPath));
        }
    }

    /** It builds the system view and the look and feel. */
    protected void setup(FileSystemView view) {
        if (view == null) {
            view = FileSystemView.getFileSystemView();
        }
        setFileSystemView(view);
        updateUI();
        if (isAcceptAllFileFilterUsed()) {
            setFileFilter(getAcceptAllFileFilter());
        }
        setLayout(new java.awt.BorderLayout());
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /** The chosen file; it holds after {@link #APPROVE_OPTION}. */
    public File getSelectedFile() {
        return selectedFile;
    }

    /**
     * It chooses that file.
     *
     * <p>It also moves the current folder to the one that contains it: choosing a file from
     * somewhere else has to leave the chooser showing that place.
     */
    public void setSelectedFile(File file) {
        File oldValue = selectedFile;
        selectedFile = file;
        if (selectedFile != null) {
            if (file.isAbsolute() && !getFileSystemView().isParent(getCurrentDirectory(),
                    selectedFile)) {
                setCurrentDirectory(selectedFile.getParentFile());
            }
            if (!isMultiSelectionEnabled() || selectedFiles == null
                    || selectedFiles.length == 1) {
                ensureFileIsVisible(selectedFile);
            }
        }
        firePropertyChange(SELECTED_FILE_CHANGED_PROPERTY, oldValue, selectedFile);
    }

    /**
     * Those chosen when several may be chosen; an empty array if there are none.
     *
     * <p>It returns a copy: the internal array does not leave the class, so touching it does not
     * change the choice behind its back.
     */
    public File[] getSelectedFiles() {
        if (selectedFiles == null) {
            return new File[0];
        }
        File[] out = new File[selectedFiles.length];
        System.arraycopy(selectedFiles, 0, out, 0, selectedFiles.length);
        return out;
    }

    /**
     * It chooses those files.
     *
     * <p>The first also becomes the chosen file, so the {@code SelectedFile} notice arrives
     * <em>before</em> the {@code SelectedFiles} one. Null or empty leave the choice at nothing.
     */
    public void setSelectedFiles(File[] selectedFiles) {
        File[] oldValue = this.selectedFiles;
        if (selectedFiles == null || selectedFiles.length == 0) {
            selectedFiles = null;
            this.selectedFiles = null;
            setSelectedFile(null);
        } else {
            this.selectedFiles = new File[selectedFiles.length];
            System.arraycopy(selectedFiles, 0, this.selectedFiles, 0, selectedFiles.length);
            setSelectedFile(this.selectedFiles[0]);
        }
        firePropertyChange(SELECTED_FILES_CHANGED_PROPERTY, oldValue, selectedFiles);
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    /**
     * The folder that is being shown.
     *
     * <p>Null means the user's usual folder. A file that is not a folder is taken as its parent
     * folder, which is what whoever passes any path expects.
     */
    public void setCurrentDirectory(File dir) {
        File oldValue = currentDirectory;
        if (dir != null && !dir.exists()) {
            dir = currentDirectory;
        }
        if (dir == null) {
            dir = getFileSystemView().getDefaultDirectory();
        }
        if (currentDirectory != null && currentDirectory.equals(dir)) {
            return;
        }
        File prev = null;
        while (!isTraversable(dir) && prev != dir) {
            prev = dir;
            dir = getFileSystemView().getParentDirectory(dir);
        }
        currentDirectory = dir;
        firePropertyChange(DIRECTORY_CHANGED_PROPERTY, oldValue, currentDirectory);
    }

    public void changeToParentDirectory() {
        selectedFile = null;
        File oldValue = getCurrentDirectory();
        setCurrentDirectory(getFileSystemView().getParentDirectory(oldValue));
    }

    /** It reads the current folder from the disk again. */
    public void rescanCurrentDirectory() {
        FileChooserUI ui = getUI();
        if (ui != null) {
            ui.rescanCurrentDirectory(this);
        }
    }

    /** It scrolls so that that file is seen. */
    public void ensureFileIsVisible(File f) {
        FileChooserUI ui = getUI();
        if (ui != null) {
            ui.ensureFileIsVisible(this, f);
        }
    }

    /**
     * It shows the open dialog.
     *
     * @return {@link #APPROVE_OPTION}, {@link #CANCEL_OPTION} or {@link #ERROR_OPTION}.
     * @throws HeadlessException if there is no screen.
     */
    public int showOpenDialog(Component parent) throws HeadlessException {
        setDialogType(OPEN_DIALOG);
        return showDialog(parent, null);
    }

    /**
     * It shows the save dialog.
     *
     * @throws HeadlessException if there is no screen.
     */
    public int showSaveDialog(Component parent) throws HeadlessException {
        setDialogType(SAVE_DIALOG);
        return showDialog(parent, null);
    }

    /**
     * It shows the dialog with that text on the accept button.
     *
     * @throws HeadlessException if there is no screen.
     */
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        if (dialog != null) {
            // It is already open: showing it twice would leave two dialogs over the same chooser.
            return ERROR_OPTION;
        }
        if (approveButtonText != null) {
            setApproveButtonText(approveButtonText);
            setDialogType(CUSTOM_DIALOG);
        }
        dialog = createDialog(parent);
        returnValue = ERROR_OPTION;
        dialog.setVisible(true);
        firePropertyChange("JFileChooserDialogIsClosingProperty", dialog, null);
        dialog.dispose();
        dialog = null;
        return returnValue;
    }

    /**
     * It builds the modal dialog that contains the chooser.
     *
     * @throws HeadlessException if there is no screen.
     */
    protected JDialog createDialog(Component parent) throws HeadlessException {
        String title = getDialogTitle();
        JDialog d = new JDialog((java.awt.Frame) null, title, true);
        d.getContentPane().add(this, java.awt.BorderLayout.CENTER);
        d.pack();
        return d;
    }

    /**
     * Whether the accept and cancel buttons are seen; they are hidden when using it inside a pane.
     */
    public boolean getControlButtonsAreShown() {
        return controlsShown;
    }

    public void setControlButtonsAreShown(boolean b) {
        if (controlsShown == b) {
            return;
        }
        boolean oldValue = controlsShown;
        controlsShown = b;
        firePropertyChange(CONTROL_BUTTONS_ARE_SHOWN_CHANGED_PROPERTY, oldValue, controlsShown);
    }

    public int getDialogType() {
        return dialogType;
    }

    /**
     * Open, save or custom.
     *
     * @throws IllegalArgumentException if it is not one of the three.
     */
    public void setDialogType(int dialogType) {
        if (this.dialogType == dialogType) {
            return;
        }
        if (!(dialogType == OPEN_DIALOG || dialogType == SAVE_DIALOG
                || dialogType == CUSTOM_DIALOG)) {
            throw new IllegalArgumentException("Incorrect Dialog Type: " + dialogType);
        }
        int oldValue = this.dialogType;
        this.dialogType = dialogType;
        if (dialogType == OPEN_DIALOG || dialogType == SAVE_DIALOG) {
            setApproveButtonText(null);
        }
        firePropertyChange(DIALOG_TYPE_CHANGED_PROPERTY, oldValue, dialogType);
    }

    public void setDialogTitle(String dialogTitle) {
        String oldValue = this.dialogTitle;
        this.dialogTitle = dialogTitle;
        if (dialog != null) {
            dialog.setTitle(dialogTitle);
        }
        firePropertyChange(DIALOG_TITLE_CHANGED_PROPERTY, oldValue, dialogTitle);
    }

    /** The title; if it was not set, the one the look and feel proposes. */
    public String getDialogTitle() {
        String title = dialogTitle;
        if (title == null) {
            FileChooserUI ui = getUI();
            if (ui != null) {
                title = ui.getDialogTitle(this);
            }
        }
        return title;
    }

    public void setApproveButtonToolTipText(String toolTipText) {
        if (approveButtonToolTipText == toolTipText) {
            return;
        }
        String oldValue = approveButtonToolTipText;
        approveButtonToolTipText = toolTipText;
        firePropertyChange(APPROVE_BUTTON_TOOL_TIP_TEXT_CHANGED_PROPERTY, oldValue,
                approveButtonToolTipText);
    }

    public String getApproveButtonToolTipText() {
        return approveButtonToolTipText;
    }

    public int getApproveButtonMnemonic() {
        return approveButtonMnemonic;
    }

    public void setApproveButtonMnemonic(int mnemonic) {
        if (approveButtonMnemonic == mnemonic) {
            return;
        }
        int oldValue = approveButtonMnemonic;
        approveButtonMnemonic = mnemonic;
        firePropertyChange(APPROVE_BUTTON_MNEMONIC_CHANGED_PROPERTY, oldValue,
                approveButtonMnemonic);
    }

    public void setApproveButtonMnemonic(char mnemonic) {
        int vk = (int) mnemonic;
        if (vk >= 'a' && vk <= 'z') {
            vk = vk - ('a' - 'A');
        }
        setApproveButtonMnemonic(vk);
    }

    public void setApproveButtonText(String approveButtonText) {
        if (this.approveButtonText == approveButtonText) {
            return;
        }
        String oldValue = this.approveButtonText;
        this.approveButtonText = approveButtonText;
        firePropertyChange(APPROVE_BUTTON_TEXT_CHANGED_PROPERTY, oldValue, approveButtonText);
    }

    /** The accept button's text; if it was not set, the one the look and feel proposes. */
    public String getApproveButtonText() {
        String text = approveButtonText;
        if (text == null) {
            FileChooserUI ui = getUI();
            if (ui != null) {
                text = ui.getApproveButtonText(this);
            }
        }
        return text;
    }

    /** The filters the user may choose. */
    public FileFilter[] getChoosableFileFilters() {
        FileFilter[] filterArray = new FileFilter[filters.size()];
        filters.copyInto(filterArray);
        return filterArray;
    }

    public void addChoosableFileFilter(FileFilter filter) {
        if (filter != null && !filters.contains(filter)) {
            FileFilter[] oldValue = getChoosableFileFilters();
            filters.addElement(filter);
            firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                    getChoosableFileFilters());
            if (fileFilter == null && filters.size() == 1) {
                setFileFilter(filter);
            }
        }
    }

    /**
     * It removes a filter; it returns whether it was there.
     *
     * <p>If it was the one in use, it goes to the first that is left: being left with no filter
     * would stop showing everything.
     */
    public boolean removeChoosableFileFilter(FileFilter f) {
        int index = filters.indexOf(f);
        if (index >= 0) {
            if (getFileFilter() == f) {
                FileFilter aaff = getAcceptAllFileFilter();
                if (isAcceptAllFileFilterUsed() && (aaff != f)) {
                    setFileFilter(aaff);
                } else if (filters.size() > 1) {
                    setFileFilter(filters.elementAt(index == 0 ? 1 : 0));
                } else {
                    setFileFilter(null);
                }
            }
            FileFilter[] oldValue = getChoosableFileFilters();
            filters.removeElement(f);
            firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                    getChoosableFileFilters());
            return true;
        }
        return false;
    }

    /** It leaves only the "all files" filter. */
    public void resetChoosableFileFilters() {
        FileFilter[] oldValue = getChoosableFileFilters();
        setFileFilter(null);
        filters.removeAllElements();
        if (isAcceptAllFileFilterUsed()) {
            addChoosableFileFilter(getAcceptAllFileFilter());
        }
        firePropertyChange(CHOOSABLE_FILE_FILTER_CHANGED_PROPERTY, oldValue,
                getChoosableFileFilters());
    }

    /**
     * The filter that accepts everything; the look and feel gives it because its text depends on
     * the language.
     */
    public FileFilter getAcceptAllFileFilter() {
        FileChooserUI ui = getUI();
        if (ui != null) {
            return ui.getAcceptAllFileFilter(this);
        }
        return null;
    }

    public boolean isAcceptAllFileFilterUsed() {
        return useAcceptAllFileFilter;
    }

    public void setAcceptAllFileFilterUsed(boolean b) {
        boolean oldValue = useAcceptAllFileFilter;
        useAcceptAllFileFilter = b;
        if (!b) {
            removeChoosableFileFilter(getAcceptAllFileFilter());
        } else {
            removeChoosableFileFilter(getAcceptAllFileFilter());
            addChoosableFileFilter(getAcceptAllFileFilter());
        }
        firePropertyChange(ACCEPT_ALL_FILE_FILTER_USED_CHANGED_PROPERTY, oldValue,
                useAcceptAllFileFilter);
    }

    /** A component of one's own that is shown at the side, for a preview. */
    public JComponent getAccessory() {
        return accessory;
    }

    public void setAccessory(JComponent newAccessory) {
        JComponent oldValue = accessory;
        accessory = newAccessory;
        firePropertyChange(ACCESSORY_CHANGED_PROPERTY, oldValue, accessory);
    }

    /**
     * Whether files, folders or both are chosen.
     *
     * @throws IllegalArgumentException if it is not one of the three.
     */
    public void setFileSelectionMode(int mode) {
        if (fileSelectionMode == mode) {
            return;
        }
        if (mode == FILES_ONLY || mode == DIRECTORIES_ONLY || mode == FILES_AND_DIRECTORIES) {
            int oldValue = fileSelectionMode;
            fileSelectionMode = mode;
            firePropertyChange(FILE_SELECTION_MODE_CHANGED_PROPERTY, oldValue, fileSelectionMode);
        } else {
            throw new IllegalArgumentException("Incorrect Mode for file selection: " + mode);
        }
    }

    public int getFileSelectionMode() {
        return fileSelectionMode;
    }

    public boolean isFileSelectionEnabled() {
        return (fileSelectionMode == FILES_ONLY
                || fileSelectionMode == FILES_AND_DIRECTORIES);
    }

    public boolean isDirectorySelectionEnabled() {
        return (fileSelectionMode == DIRECTORIES_ONLY
                || fileSelectionMode == FILES_AND_DIRECTORIES);
    }

    /**
     * Whether more than one file may be chosen.
     *
     * <p>Changing mode clears what had been chosen, in both directions: on switching it on the
     * single file is discarded and on switching it off the array. Dragging a choice from one mode
     * to the other would leave the chooser showing a choice its new mode cannot represent.
     */
    public void setMultiSelectionEnabled(boolean b) {
        if (multiSelectionEnabled == b) {
            return;
        }
        boolean oldValue = multiSelectionEnabled;
        multiSelectionEnabled = b;
        if (multiSelectionEnabled) {
            setSelectedFile(null);
        } else {
            setSelectedFiles(null);
        }
        firePropertyChange(MULTI_SELECTION_ENABLED_CHANGED_PROPERTY, oldValue,
                multiSelectionEnabled);
    }

    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }

    /** Whether the hidden files are hidden. */
    public boolean isFileHidingEnabled() {
        return useFileHiding;
    }

    public void setFileHidingEnabled(boolean b) {
        boolean oldValue = useFileHiding;
        useFileHiding = b;
        firePropertyChange(FILE_HIDING_CHANGED_PROPERTY, oldValue, useFileHiding);
        rescanCurrentDirectory();
    }

    /**
     * The filter in use.
     *
     * <p>What was already chosen and the new filter does not accept stops being chosen. Without
     * that, the chooser would return a file it is itself hiding.
     */
    public void setFileFilter(FileFilter filter) {
        FileFilter oldValue = fileFilter;
        fileFilter = filter;
        if (filter != null) {
            if (isMultiSelectionEnabled() && selectedFiles != null && selectedFiles.length > 0) {
                Vector<File> left = new Vector<File>();
                boolean change = false;
                for (int i = 0; i < selectedFiles.length; i++) {
                    if (filter.accept(selectedFiles[i])) {
                        left.addElement(selectedFiles[i]);
                    } else {
                        change = true;
                    }
                }
                if (change) {
                    File[] arr = new File[left.size()];
                    left.copyInto(arr);
                    setSelectedFiles(arr);
                }
            } else if (selectedFile != null && !filter.accept(selectedFile)) {
                setSelectedFile(null);
            }
        }
        firePropertyChange(FILE_FILTER_CHANGED_PROPERTY, oldValue, fileFilter);
    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    /** Who decides each file's name, icon and description. */
    public void setFileView(FileView fileView) {
        FileView oldValue = this.fileView;
        this.fileView = fileView;
        firePropertyChange(FILE_VIEW_CHANGED_PROPERTY, oldValue, fileView);
    }

    public FileView getFileView() {
        return fileView;
    }

    /**
     * The name that is shown of that file.
     *
     * <p>First the view of one's own, if there is one, and otherwise the look and feel's. That
     * order is what allows only some names to be changed without writing a whole view.
     */
    public String getName(File f) {
        String filename = null;
        if (f != null) {
            if (getFileView() != null) {
                filename = getFileView().getName(f);
            }
            if (filename == null && fileSystemView != null) {
                filename = fileSystemView.getSystemDisplayName(f);
            }
        }
        return filename;
    }

    public String getDescription(File f) {
        if (f != null && getFileView() != null) {
            return getFileView().getDescription(f);
        }
        return null;
    }

    public String getTypeDescription(File f) {
        String typeDescription = null;
        if (f != null) {
            if (getFileView() != null) {
                typeDescription = getFileView().getTypeDescription(f);
            }
            if (typeDescription == null && fileSystemView != null) {
                typeDescription = fileSystemView.getSystemTypeDescription(f);
            }
        }
        return typeDescription;
    }

    public Icon getIcon(File f) {
        Icon icon = null;
        if (f != null) {
            if (getFileView() != null) {
                icon = getFileView().getIcon(f);
            }
            if (icon == null && fileSystemView != null) {
                icon = fileSystemView.getSystemIcon(f);
            }
        }
        return icon;
    }

    /** Whether that file can be entered; a folder yes, a file no. */
    public boolean isTraversable(File f) {
        Boolean traversable = null;
        if (f != null) {
            if (getFileView() != null) {
                traversable = getFileView().isTraversable(f);
            }
            if (traversable == null && fileSystemView != null) {
                traversable = fileSystemView.isTraversable(f);
            }
        }
        return (traversable != null && traversable.booleanValue());
    }

    /**
     * Whether the filter accepts that file.
     *
     * <p>A folder is always accepted: filtering folders would make it impossible to reach the
     * files inside them.
     */
    public boolean accept(File f) {
        boolean shown = true;
        if (f != null && fileFilter != null) {
            shown = fileFilter.accept(f);
        }
        return shown;
    }

    public void setFileSystemView(FileSystemView fsv) {
        FileSystemView oldValue = fileSystemView;
        fileSystemView = fsv;
        firePropertyChange(FILE_SYSTEM_VIEW_CHANGED_PROPERTY, oldValue, fileSystemView);
    }

    public FileSystemView getFileSystemView() {
        return fileSystemView;
    }

    /** The user accepted; it closes the dialog and gives notice. */
    public void approveSelection() {
        returnValue = APPROVE_OPTION;
        if (dialog != null) {
            dialog.setVisible(false);
        }
        fireActionPerformed(APPROVE_SELECTION);
    }

    public void cancelSelection() {
        returnValue = CANCEL_OPTION;
        if (dialog != null) {
            dialog.setVisible(false);
        }
        fireActionPerformed(CANCEL_SELECTION);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected void fireActionPerformed(String command) {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command,
                            System.currentTimeMillis(), 0);
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public FileChooserUI getUI() {
        return (FileChooserUI) ui;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }
}
