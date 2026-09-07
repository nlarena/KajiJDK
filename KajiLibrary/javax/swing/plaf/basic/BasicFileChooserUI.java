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
 * El aspecto basico de un selector de archivos.
 *
 * <h2>Casi todo lo que hace es texto y acciones</h2>
 *
 * <p>Un selector de archivos no dibuja nada propio: lo que se ve son la lista, los botones y los
 * campos, y esos los pone el aspecto de verdad. Lo que si define el basico son los <em>textos</em>
 * de los botones -- "Open", "Save", "Cancel" --, sus mnemonicos, y las seis acciones que hacen algo:
 * aprobar, cancelar, subir un nivel, ir a la carpeta del usuario, actualizar y crear una carpeta.
 *
 * <p>Esa separacion es lo que deja que un aspecto reemplace el dibujo entero y siga teniendo los
 * mismos botones haciendo lo mismo.
 *
 * <h2>El nombre del archivo no lo guarda el UI</h2>
 *
 * <p>{@link #getFileName} y {@link #getDirectoryName} devuelven {@code null} y ponerlos no hace
 * nada. No es un olvido: el nombre vive en el campo de texto que dibuja el aspecto, y el basico no
 * dibuja ninguno. Un aspecto que ponga campos redefine los cuatro metodos. Esta medido.
 *
 * <h2>La vista de archivos</h2>
 *
 * <p>{@link BasicFileView} contesta como se ve cada archivo -- su nombre, su icono, si esta
 * escondido -- y guarda los iconos en una tabla, porque preguntarle al sistema por el icono de un
 * archivo es caro y una carpeta tiene cientos.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Los once iconos vienen de la tabla del aspecto y aca no hay ninguna: todos quedan en
 * {@code null}. {@link #installComponents} no arma la lista ni los botones -- eso es del aspecto de
 * verdad --, asi que {@link #getApproveButton} y {@link #getDefaultButton} devuelven {@code null},
 * igual que en el JDK.
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

    private final Action approveSelectionAction = new AccionDelSelector(this, "approveSelection");
    private final Action cancelSelectionAction = new AccionDelSelector(this, "cancelSelection");
    private final Action updateAction = new AccionDelSelector(this, "refresh");
    private final Action newFolderAction = new AccionDelSelector(this, "New Folder");
    private final Action goHomeAction = new AccionDelSelector(this, "Go Home");
    private final Action changeToParentDirectoryAction =
            new AccionDelSelector(this, "Go Up");

    /** Para ese selector. */
    public BasicFileChooserUI(JFileChooser b) {
        this.filechooser = b;
    }

    /** Uno nuevo por selector: guarda su modelo de directorio y su vista. */
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
        acceptAllFileFilter = new FiltroDeTodos();
    }

    protected void uninstallDefaults(JFileChooser fc) {
        uninstallIcons(fc);
        uninstallStrings(fc);
        model = null;
        fileView = null;
        acceptAllFileFilter = null;
    }

    /** Ninguno; ver la nota de la clase. */
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

    /** Los textos, los tips y los mnemonicos; los valores son los medidos en el JDK 25. */
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

        // Solo dos tienen letra subrayada; los otros cuatro quedan en cero. Medido.
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

    /** Nada: la lista y los botones son del aspecto de verdad; ver la nota de la clase. */
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

    /** Ninguno: el basico no reacciona a nada por su cuenta. */
    public PropertyChangeListener createPropertyChangeListener(JFileChooser fc) {
        return null;
    }

    /** El que sigue lo elegido en la lista. */
    public ListSelectionListener createListSelectionListener(JFileChooser fc) {
        return new EscuchaDeSeleccion(this);
    }

    /** El que abre una carpeta con dos clicks. */
    protected MouseListener createDoubleClickListener(JFileChooser fc, JList list) {
        return new EscuchaDeDobleClick(this, list);
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

    /** Vuelve a leer la carpeta actual. */
    public void rescanCurrentDirectory(JFileChooser fc) {
        if (model != null) {
            model.validateFileCache();
        }
    }

    /** No hace nada: hacer visible un archivo es cosa de la lista, que aca no esta. */
    public void ensureFileIsVisible(JFileChooser fc, File f) {
    }

    /** {@code null}; ver la nota de la clase. */
    public String getFileName() {
        return null;
    }

    /** No hace nada; ver la nota de la clase. */
    public void setFileName(String filename) {
    }

    /** {@code null}; ver la nota de la clase. */
    public String getDirectoryName() {
        return null;
    }

    /** No hace nada; ver la nota de la clase. */
    public void setDirectoryName(String dirname) {
    }

    /** Si lo elegido es una carpeta; lo usa el boton de aprobar para decidir si abre o elige. */
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

    /** El titulo del dialogo: el que puso el programa, o el del boton de aprobar. */
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

    /** {@code null}; ver la nota de la clase. */
    protected JButton getApproveButton(JFileChooser fc) {
        return null;
    }

    /** {@code null}; ver la nota de la clase. */
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

    /** Tira los iconos guardados; ver la nota de {@link BasicFileView}. */
    public void clearIconCache() {
        if (fileView instanceof BasicFileView) {
            ((BasicFileView) fileView).clearIconCache();
        }
    }

    /**
     * Como se ve cada archivo; ver la nota de la clase.
     *
     * <p>Estatica y con el UI como primer parametro, que es la firma que el JDK genera para una
     * clase interna; ver el hallazgo #518.
     */
    public static class BasicFileView extends FileView {

        /** Los iconos ya preguntados; ver la nota de la clase. */
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

        /** Ninguna: una descripcion larga la da el sistema, y aca no hay. */
        public String getDescription(File f) {
            return (f == null) ? null : f.getName();
        }

        /** El tipo, si el sistema lo sabe. */
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

        /** El de carpeta o el de archivo, guardado para no volver a preguntar. */
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

    /** El filtro que deja pasar todo; su descripcion es la que se ve en el combo de filtros. */
    private static class FiltroDeTodos extends FileFilter {

        public boolean accept(File f) {
            return true;
        }

        public String getDescription() {
            return "All Files";
        }
    }

    /**
     * Cada una de las seis acciones.
     *
     * <p>El nombre es el comando; se compara por nombre, igual que en la barra de titulo de una
     * ventana interna.
     */
    private static class AccionDelSelector extends AbstractAction {

        private final BasicFileChooserUI ui;
        private final String comando;

        AccionDelSelector(BasicFileChooserUI ui, String comando) {
            super(comando);
            this.ui = ui;
            this.comando = comando;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fc = ui.getFileChooser();
            if ("approveSelection".equals(comando)) {
                fc.approveSelection();
            } else if ("cancelSelection".equals(comando)) {
                fc.cancelSelection();
            } else if ("refresh".equals(comando)) {
                ui.rescanCurrentDirectory(fc);
            } else if ("Go Home".equals(comando)) {
                fc.setCurrentDirectory(fc.getFileSystemView().getHomeDirectory());
            } else if ("Go Up".equals(comando)) {
                fc.changeToParentDirectory();
            } else if ("New Folder".equals(comando)) {
                crearCarpeta(fc);
            }
        }

        private void crearCarpeta(JFileChooser fc) {
            File actual = fc.getCurrentDirectory();
            if (actual == null) {
                return;
            }
            try {
                File nueva = fc.getFileSystemView().createNewFolder(actual);
                fc.setSelectedFile(nueva);
                fc.ensureFileIsVisible(nueva);
                ui.rescanCurrentDirectory(fc);
            } catch (java.io.IOException ex) {
                // No se pudo crear -- permisos, disco lleno --. El selector sigue andando.
            }
        }
    }

    /** Marca si lo elegido es una carpeta; ver {@link #isDirectorySelected}. */
    private static class EscuchaDeSeleccion implements ListSelectionListener {

        private final BasicFileChooserUI ui;

        EscuchaDeSeleccion(BasicFileChooserUI ui) {
            this.ui = ui;
        }

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) {
                return;
            }
            JFileChooser fc = ui.getFileChooser();
            File elegido = fc.getSelectedFile();
            ui.setDirectorySelected(elegido != null && elegido.isDirectory());
            ui.setDirectory(ui.isDirectorySelected() ? elegido : null);
        }
    }

    /** Dos clicks sobre una carpeta entran en ella; sobre un archivo, lo aprueban. */
    private static class EscuchaDeDobleClick extends MouseAdapter implements MouseListener {

        private final BasicFileChooserUI ui;
        private final JList list;

        EscuchaDeDobleClick(BasicFileChooserUI ui, JList list) {
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
