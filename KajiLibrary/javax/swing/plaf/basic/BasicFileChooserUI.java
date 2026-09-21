package javax.swing.plaf.basic;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FileChooserUI;

/**
 * The basic look and feel of a file chooser.
 *
 * <h2>Almost everything it does is text and actions</h2>
 *
 * <p>A file chooser draws nothing of its own: what is seen are the list, the buttons and the
 * fields, and those are put in by the real look and feel. What the basic one does define are
 * the buttons' <em>texts</em> -- "Open", "Save", "Cancel" --, their mnemonics, and the six
 * actions that do something: approve, cancel, go up one level, go to the user's folder, refresh
 * and create a folder.
 *
 * <p>That separation is what lets a look and feel replace the whole drawing and go on having
 * the same buttons doing the same thing.
 *
 * <h2>The file's name is not kept by the look and feel</h2>
 *
 * <p>{@link #getFileName} and {@link #getDirectoryName} return {@code null} and setting them
 * does nothing. It is not an oversight: the name lives in the text field the look and feel
 * draws, and the basic one draws none. A look and feel that puts fields in redefines the four
 * methods. It is measured.
 *
 * <h2>The file view</h2>
 *
 * <p>{@link BasicFileView} answers how each file is seen -- its name, its icon, whether it is
 * hidden -- and keeps the icons in a table, because asking the system for a file's icon is
 * expensive and a folder has hundreds.
 *
 * <h2>What is left said</h2>
 *
 * <p>The eleven icons come from the look and feel's table and here there is none: all of them
 * are left {@code null}. {@link #installComponents} does not build the list nor the buttons
 * -- that belongs to the real look and feel --, so {@link #getApproveButton} and
 * {@link #getDefaultButton} return {@code null}, just as in the JDK.
 */
public class BasicFileChooserUI extends FileChooserUI {

    protected JPanel accessoryPanel = null;

    protected Icon directoryIcon = null;
    protected Icon fileIcon = null;
    protected Icon computerIcon = null;
    protected Icon hardDriveIcon = null;
    protected Icon floppyDriveIcon = null;
    protected Icon newFolderIcon = null;
    protected Icon upFolderIcon = null;
    protected Icon homeFolderIcon = null;
    protected Icon listViewIcon = null;
    protected Icon detailsViewIcon = null;
    protected Icon viewMenuIcon = null;

    protected int saveButtonMnemonic = 0;
    protected int openButtonMnemonic = 0;
    protected int cancelButtonMnemonic = 0;
    protected int updateButtonMnemonic = 0;
    protected int helpButtonMnemonic = 0;
    protected int directoryOpenButtonMnemonic = 0;

    protected String saveButtonText = null;
    protected String openButtonText = null;
    protected String cancelButtonText = null;
    protected String updateButtonText = null;
    protected String helpButtonText = null;
    protected String directoryOpenButtonText = null;

    protected String saveButtonToolTipText = null;
    protected String openButtonToolTipText = null;
    protected String cancelButtonToolTipText = null;
    protected String updateButtonToolTipText = null;
    protected String helpButtonToolTipText = null;
    protected String directoryOpenButtonToolTipText = null;

    private final JFileChooser filechooser;
    private BasicDirectoryModel model;
    private FileView fileView;
    private FileFilter acceptAllFileFilter;
    private PropertyChangeListener propertyChangeListener;
    private boolean directorySelected;
    private File directory;

    private final Action approveSelectionAction = new ChooserAction(this, "approveSelection");
    private final Action cancelSelectionAction = new ChooserAction(this, "cancelSelection");
    private final Action updateAction = new ChooserAction(this, "refresh");
    private final Action newFolderAction = new ChooserAction(this, "New Folder");
    private final Action goHomeAction = new ChooserAction(this, "Go Home");
    private final Action changeToParentDirectoryAction =
            new ChooserAction(this, "Go Up");

    /** For that chooser. */
    public BasicFileChooserUI(JFileChooser b) {
        this.filechooser = b;
    }

    /** A new one per chooser: it keeps its directory model and its view. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicFileChooserUI((JFileChooser) c);
    }

    public void installUI(JComponent c) {
        accessoryPanel = new JPanel(new BorderLayout());
        JFileChooser fc = (JFileChooser) c;
        fc.setLayout(new BorderLayout());
        fc.add(accessoryPanel, BorderLayout.AFTER_LINE_ENDS);
        installDefaults(fc);
        installComponents(fc);
        installListeners(fc);
    }

    public void uninstallUI(JComponent c) {
        JFileChooser fc = (JFileChooser) c;
        uninstallListeners(fc);
        uninstallComponents(fc);
        uninstallDefaults(fc);
        if (accessoryPanel != null) {
            accessoryPanel.removeAll();
        }
        accessoryPanel = null;
        fc.setLayout(null);
    }

    protected void installDefaults(JFileChooser fc) {
        installIcons(fc);
        installStrings(fc);
        createModel();
        fileView = new BasicFileView(this);
        acceptAllFileFilter = new AcceptAllFilter();
    }

    protected void uninstallDefaults(JFileChooser fc) {
        uninstallIcons(fc);
        uninstallStrings(fc);
        model = null;
        fileView = null;
        acceptAllFileFilter = null;
    }

    /** None; see the class note. */
    protected void installIcons(JFileChooser fc) {
    }

    protected void uninstallIcons(JFileChooser fc) {
        directoryIcon = null;
        fileIcon = null;
        computerIcon = null;
        hardDriveIcon = null;
        floppyDriveIcon = null;
        newFolderIcon = null;
        upFolderIcon = null;
        homeFolderIcon = null;
        detailsViewIcon = null;
        listViewIcon = null;
        viewMenuIcon = null;
    }

    /** The texts, the tips and the mnemonics; the values are those measured in JDK 25. */
    protected void installStrings(JFileChooser fc) {
        saveButtonText = "Save";
        openButtonText = "Open";
        cancelButtonText = "Cancel";
        updateButtonText = "Update";
        helpButtonText = "Help";
        directoryOpenButtonText = "Open";

        saveButtonToolTipText = "Save selected file";
        openButtonToolTipText = "Open selected file";
        cancelButtonToolTipText = "Abort file chooser dialog";
        updateButtonToolTipText = "Update directory listing";
        helpButtonToolTipText = "FileChooser help";
        directoryOpenButtonToolTipText = "Open selected directory";

        // Only two have an underlined letter; the other four are left at zero. Measured.
        saveButtonMnemonic = 0;
        openButtonMnemonic = 0;
        cancelButtonMnemonic = 0;
        updateButtonMnemonic = 'U';
        helpButtonMnemonic = 'H';
        directoryOpenButtonMnemonic = 0;
    }

    protected void uninstallStrings(JFileChooser fc) {
        saveButtonText = null;
        openButtonText = null;
        cancelButtonText = null;
        updateButtonText = null;
        helpButtonText = null;
        directoryOpenButtonText = null;
        saveButtonToolTipText = null;
        openButtonToolTipText = null;
        cancelButtonToolTipText = null;
        updateButtonToolTipText = null;
        helpButtonToolTipText = null;
        directoryOpenButtonToolTipText = null;
    }

    /** Nothing: the list and the buttons belong to the real look and feel; see the class note. */
    public void installComponents(JFileChooser fc) {
    }

    public void uninstallComponents(JFileChooser fc) {
    }

    protected void installListeners(JFileChooser fc) {
        propertyChangeListener = createPropertyChangeListener(fc);
        if (propertyChangeListener != null) {
            fc.addPropertyChangeListener(propertyChangeListener);
        }
        if (model != null) {
            fc.addPropertyChangeListener(model);
        }
    }

    protected void uninstallListeners(JFileChooser fc) {
        if (propertyChangeListener != null) {
            fc.removePropertyChangeListener(propertyChangeListener);
        }
        if (model != null) {
            fc.removePropertyChangeListener(model);
        }
        propertyChangeListener = null;
    }

    /** None: the basic one does not react to anything on its own. */
    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return null;
    }

    /** The one that follows what is chosen in the list. */
    public ListSelectionListener createListSelectionListener(JFileChooser fc) {
        return new SelectionListenerImpl(this);
    }

    /** The one that opens a folder with two clicks. */
    protected MouseListener createDoubleClickListener(JFileChooser fc, JList list) {
        return new DoubleClickListener(this, list);
    }

    protected void createModel() {
        if (model != null) {
            model.invalidateFileCache();
        }
        model = new BasicDirectoryModel(getFileChooser());
    }

    public BasicDirectoryModel getModel() {
        return model;
    }

    public JFileChooser getFileChooser() {
        return filechooser;
    }

    public JPanel getAccessoryPanel() {
        return accessoryPanel;
    }

    /** It reads the current folder again. */
    public void rescanCurrentDirectory(JFileChooser fc) {
        if (model != null) {
            model.validateFileCache();
        }
    }

    /** It does nothing: making a file visible is the list's business, and it is not here. */
    public void ensureFileIsVisible(JFileChooser fc, File f) {
    }

    /** {@code null}; see the class note. */
    public String getFileName() {
        return null;
    }

    /** It does nothing; see the class note. */
    public void setFileName(String filename) {
    }

    /** {@code null}; see the class note. */
    public String getDirectoryName() {
        return null;
    }

    /** It does nothing; see the class note. */
    public void setDirectoryName(String dirname) {
    }

    /**
     * Whether what is chosen is a folder; the approve button uses it in order to decide whether it
     * opens or chooses.
     */
    protected boolean isDirectorySelected() {
        return directorySelected;
    }

    protected void setDirectorySelected(boolean b) {
        directorySelected = b;
    }

    protected File getDirectory() {
        return directory;
    }

    protected void setDirectory(File f) {
        directory = f;
    }

    public FileFilter getAcceptAllFileFilter(JFileChooser fc) {
        return acceptAllFileFilter;
    }

    public FileView getFileView(JFileChooser fc) {
        return fileView;
    }

    /** The dialog's title: the one the program set, or the approve button's. */
    public String getDialogTitle(JFileChooser fc) {
        String dialogTitle = fc.getDialogTitle();
        if (dialogTitle != null) {
            return dialogTitle;
        }
        if (fc.getDialogType() == JFileChooser.OPEN_DIALOG) {
            return openButtonText;
        }
        if (fc.getDialogType() == JFileChooser.SAVE_DIALOG) {
            return saveButtonText;
        }
        return getApproveButtonText(fc);
    }

    public int getApproveButtonMnemonic(JFileChooser fc) {
        int m = fc.getApproveButtonMnemonic();
        if (m > 0) {
            return m;
        }
        return (fc.getDialogType() == JFileChooser.SAVE_DIALOG)
                ? saveButtonMnemonic : openButtonMnemonic;
    }

    public String getApproveButtonToolTipText(JFileChooser fc) {
        String t = fc.getApproveButtonToolTipText();
        if (t != null) {
            return t;
        }
        return (fc.getDialogType() == JFileChooser.SAVE_DIALOG)
                ? saveButtonToolTipText : openButtonToolTipText;
    }

    public String getApproveButtonText(JFileChooser fc) {
        String t = fc.getApproveButtonText();
        if (t != null) {
            return t;
        }
        return (fc.getDialogType() == JFileChooser.SAVE_DIALOG)
                ? saveButtonText : openButtonText;
    }

    /** {@code null}; see the class note. */
    protected JButton getApproveButton(JFileChooser fc) {
        return null;
    }

    /** {@code null}; see the class note. */
    public JButton getDefaultButton(JFileChooser fc) {
        return null;
    }

    public Action getNewFolderAction() {
        return newFolderAction;
    }

    public Action getGoHomeAction() {
        return goHomeAction;
    }

    public Action getChangeToParentDirectoryAction() {
        return changeToParentDirectoryAction;
    }

    public Action getApproveSelectionAction() {
        return approveSelectionAction;
    }

    public Action getCancelSelectionAction() {
        return cancelSelectionAction;
    }

    public Action getUpdateAction() {
        return updateAction;
    }

    /** It throws away the kept icons; see {@link BasicFileView}'s note. */
    public void clearIconCache() {
        if (fileView instanceof BasicFileView) {
            ((BasicFileView) fileView).clearIconCache();
        }
    }

    /**
     * How each file is seen; see the class note.
     *
     * <p>Static and with the look and feel as the first parameter, which is the signature the JDK
     * generates for an inner class; see finding #518.
     */
    public static class BasicFileView extends FileView {

        /** The icons already asked for; see the class note. */
        protected Hashtable<File, Icon> iconCache = new Hashtable<File, Icon>();

        private final BasicFileChooserUI ui;

        public BasicFileView(BasicFileChooserUI ui) {
            this.ui = ui;
        }

        public void clearIconCache() {
            iconCache = new Hashtable<File, Icon>();
        }

        public String getName(File f) {
            if (f == null) {
                return null;
            }
            String fileName = ui.getFileChooser().getFileSystemView().getSystemDisplayName(f);
            return fileName;
        }

        /** None: a long description is given by the system, and here there is none. */
        public String getDescription(File f) {
            return (f == null) ? null : f.getName();
        }

        /** The type, if the system knows it. */
        public String getTypeDescription(File f) {
            if (f == null) {
                return null;
            }
            return ui.getFileChooser().getFileSystemView().getSystemTypeDescription(f);
        }

        public Icon getCachedIcon(File f) {
            return iconCache.get(f);
        }

        public void cacheIcon(File f, Icon i) {
            if (f == null || i == null) {
                return;
            }
            iconCache.put(f, i);
        }

        /** The folder one or the file one, kept so as not to ask again. */
        public Icon getIcon(File f) {
            Icon icon = getCachedIcon(f);
            if (icon != null) {
                return icon;
            }
            if (f != null) {
                icon = f.isDirectory() ? ui.directoryIcon : ui.fileIcon;
            }
            cacheIcon(f, icon);
            return icon;
        }

        public Boolean isHidden(File f) {
            if (f == null) {
                return Boolean.FALSE;
            }
            String name = f.getName();
            if (name != null && !name.equals("") && name.charAt(0) == '.') {
                return Boolean.TRUE;
            }
            return Boolean.valueOf(f.isHidden());
        }
    }

    /**
     * The filter that lets everything through; its description is the one seen in the filter combo
     * box.
     */
    private static class AcceptAllFilter extends FileFilter {

        public boolean accept(File f) {
            return true;
        }

        public String getDescription() {
            return "All Files";
        }
    }

    /**
     * Each of the six actions.
     *
     * <p>The name is the command; it is compared by name, just as in an internal frame's title
     * bar.
     */
    private static class ChooserAction extends AbstractAction {

        private final BasicFileChooserUI ui;
        private final String command;

        ChooserAction(BasicFileChooserUI ui, String command) {
            super(command);
            this.ui = ui;
            this.command = command;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = ui.getFileChooser();
            if ("approveSelection".equals(command)) {
                fc.approveSelection();
            } else if ("cancelSelection".equals(command)) {
                fc.cancelSelection();
            } else if ("refresh".equals(command)) {
                ui.rescanCurrentDirectory(fc);
            } else if ("Go Home".equals(command)) {
                fc.setCurrentDirectory(fc.getFileSystemView().getHomeDirectory());
            } else if ("Go Up".equals(command)) {
                fc.changeToParentDirectory();
            } else if ("New Folder".equals(command)) {
                createFolder(fc);
            }
        }

        private void createFolder(JFileChooser fc) {
            File current = fc.getCurrentDirectory();
            if (current == null) {
                return;
            }
            try {
                File created = fc.getFileSystemView().createNewFolder(current);
                fc.setSelectedFile(created);
                fc.ensureFileIsVisible(created);
                ui.rescanCurrentDirectory(fc);
            } catch (java.io.IOException ex) {
                // It could not be created -- permissions, a full disk --. The chooser goes on
                // working.
            }
        }
    }

    /** It marks whether what is chosen is a folder; see {@link #isDirectorySelected}. */
    private static class SelectionListenerImpl implements ListSelectionListener {

        private final BasicFileChooserUI ui;

        SelectionListenerImpl(BasicFileChooserUI ui) {
            this.ui = ui;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            JFileChooser fc = ui.getFileChooser();
            File chosen = fc.getSelectedFile();
            ui.setDirectorySelected(chosen != null && chosen.isDirectory());
            ui.setDirectory(ui.isDirectorySelected() ? chosen : null);
        }
    }

    /** Two clicks on a folder go into it; on a file, they approve it. */
    private static class DoubleClickListener extends MouseAdapter implements MouseListener {

        private final BasicFileChooserUI ui;
        private final JList list;

        DoubleClickListener(BasicFileChooserUI ui, JList list) {
            this.ui = ui;
            this.list = list;
        }

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2 || list == null) {
                return;
            }
            int index = list.locationToIndex(e.getPoint());
            if (index < 0) {
                return;
            }
            Object o = list.getModel().getElementAt(index);
            if (!(o instanceof File)) {
                return;
            }
            File f = (File) o;
            JFileChooser fc = ui.getFileChooser();
            if (f.isDirectory()) {
                fc.setCurrentDirectory(f);
                ui.rescanCurrentDirectory(fc);
            } else {
                fc.setSelectedFile(f);
                fc.approveSelection();
            }
        }
    }
}
