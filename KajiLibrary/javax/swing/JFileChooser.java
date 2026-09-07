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
 * El selector de archivos.
 *
 * <h2>Es un componente, no un dialogo</h2>
 *
 * <p>{@link #showOpenDialog} arma un {@link JDialog} y lo muestra, pero el selector en si es un
 * {@link JComponent}: se lo puede poner adentro de una ventana propia. Es lo que permite un
 * programa con el selector siempre visible en un panel.
 *
 * <h2>Tres cosas que se confunden</h2>
 *
 * <p>El <em>modo de seleccion</em> ({@link #setFileSelectionMode}) dice si se eligen archivos,
 * carpetas o los dos. El <em>filtro</em> dice cuales se muestran. La <em>vista del sistema</em>
 * ({@link FileSystemView}) dice como se llama y que icono tiene cada uno. Son independientes: se
 * puede estar eligiendo carpetas y filtrar por extension.
 *
 * <h2>Lo que devuelve mostrar el dialogo</h2>
 *
 * <p>Un entero, no el archivo. {@link #APPROVE_OPTION} significa que el usuario acepto, y recien
 * entonces {@link #getSelectedFile} tiene sentido. Ignorar el resultado y leer el archivo
 * directamente es el error mas comun con esta clase: al cancelar se lee el que estaba antes.
 *
 * <p>Sin pantalla el dialogo no se puede mostrar; el selector se construye y se configura igual.
 */
public class JFileChooser extends JComponent implements Accessible {

    private static final String uiClassID = "FileChooserUI";

    /** Un dialogo de abrir. */
    public static final int OPEN_DIALOG = 0;

    /** Un dialogo de guardar. */
    public static final int SAVE_DIALOG = 1;

    /** Un dialogo con un boton de texto propio. */
    public static final int CUSTOM_DIALOG = 2;

    /** El usuario cancelo. */
    public static final int CANCEL_OPTION = 1;

    /** El usuario acepto; recien ahi vale el archivo elegido. */
    public static final int APPROVE_OPTION = 0;

    /** Algo fallo. */
    public static final int ERROR_OPTION = -1;

    /** Solo archivos. */
    public static final int FILES_ONLY = 0;

    /** Solo carpetas. */
    public static final int DIRECTORIES_ONLY = 1;

    /** Archivos y carpetas. */
    public static final int FILES_AND_DIRECTORIES = 2;

    /** El comando del boton de cancelar. */
    public static final String CANCEL_SELECTION = "CancelSelection";

    /** El comando del boton de aceptar. */
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

    /** Un selector en la carpeta de siempre del usuario. */
    public JFileChooser() {
        this((File) null, (FileSystemView) null);
    }

    /** Un selector en esa carpeta. */
    public JFileChooser(String currentDirectoryPath) {
        this(currentDirectoryPath, (FileSystemView) null);
    }

    /** Un selector en esa carpeta. */
    public JFileChooser(File currentDirectory) {
        this(currentDirectory, (FileSystemView) null);
    }

    /** Un selector con esa vista del sistema de archivos. */
    public JFileChooser(FileSystemView fsv) {
        this((File) null, fsv);
    }

    /** Un selector en esa carpeta y con esa vista. */
    public JFileChooser(File currentDirectory, FileSystemView fsv) {
        setup(fsv);
        setCurrentDirectory(currentDirectory);
    }

    /** Un selector en esa carpeta y con esa vista. */
    public JFileChooser(String currentDirectoryPath, FileSystemView fsv) {
        setup(fsv);
        if (currentDirectoryPath == null) {
            setCurrentDirectory(null);
        } else {
            setCurrentDirectory(fileSystemView.createFileObject(currentDirectoryPath));
        }
    }

    /** Arma la vista del sistema y el aspecto. */
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

    /** El archivo elegido; vale despues de {@link #APPROVE_OPTION}. */
    public File getSelectedFile() {
        return selectedFile;
    }

    /**
     * Elige ese archivo.
     *
     * <p>Ademas mueve la carpeta actual a la que lo contiene: elegir un archivo de otro lado tiene
     * que dejar el selector mostrando ese lado.
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
     * Los elegidos cuando se puede elegir varios; un arreglo vacio si no hay ninguno.
     *
     * <p>Devuelve una copia: el arreglo interno no sale de la clase, asi que tocarlo no cambia la
     * eleccion por la espalda.
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
     * Elige esos archivos.
     *
     * <p>El primero pasa a ser tambien el archivo elegido, asi que el aviso de
     * {@code SelectedFile} llega <em>antes</em> que el de {@code SelectedFiles}. Nulo o vacio
     * dejan la eleccion en nada.
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
     * La carpeta que se esta mostrando.
     *
     * <p>Nulo significa la carpeta de siempre del usuario. Un archivo que no es carpeta se toma
     * como su carpeta padre, que es lo que espera quien pasa una ruta cualquiera.
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

    /** Vuelve a leer la carpeta actual del disco. */
    public void rescanCurrentDirectory() {
        FileChooserUI ui = getUI();
        if (ui != null) {
            ui.rescanCurrentDirectory(this);
        }
    }

    /** Desplaza para que ese archivo se vea. */
    public void ensureFileIsVisible(File f) {
        FileChooserUI ui = getUI();
        if (ui != null) {
            ui.ensureFileIsVisible(this, f);
        }
    }

    /**
     * Muestra el dialogo de abrir.
     *
     * @return {@link #APPROVE_OPTION}, {@link #CANCEL_OPTION} o {@link #ERROR_OPTION}.
     * @throws HeadlessException si no hay pantalla.
     */
    public int showOpenDialog(Component parent) throws HeadlessException {
        setDialogType(OPEN_DIALOG);
        return showDialog(parent, null);
    }

    /**
     * Muestra el dialogo de guardar.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public int showSaveDialog(Component parent) throws HeadlessException {
        setDialogType(SAVE_DIALOG);
        return showDialog(parent, null);
    }

    /**
     * Muestra el dialogo con ese texto en el boton de aceptar.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
        if (dialog != null) {
            // Ya esta abierto: mostrarlo dos veces dejaria dos dialogos sobre el mismo selector.
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
     * Arma el dialogo modal que contiene al selector.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    protected JDialog createDialog(Component parent) throws HeadlessException {
        String title = getDialogTitle();
        JDialog d = new JDialog((java.awt.Frame) null, title, true);
        d.getContentPane().add(this, java.awt.BorderLayout.CENTER);
        d.pack();
        return d;
    }

    /** Si se ven los botones de aceptar y cancelar; se esconden al usarlo dentro de un panel. */
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
     * Abrir, guardar o propio.
     *
     * @throws IllegalArgumentException si no es uno de los tres.
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

    /** El titulo; si no se puso, el que proponga el aspecto. */
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

    /** El texto del boton de aceptar; si no se puso, el que proponga el aspecto. */
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

    /** Los filtros que el usuario puede elegir. */
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
     * Saca un filtro; devuelve si estaba.
     *
     * <p>Si era el que estaba en uso, se pasa al primero que quede: quedarse sin filtro dejaria de
     * mostrar todo.
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

    /** Deja solo el filtro de "todos los archivos". */
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

    /** El filtro que acepta todo; lo da el aspecto porque su texto depende del idioma. */
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

    /** Un componente propio que se muestra al costado, para una vista previa. */
    public JComponent getAccessory() {
        return accessory;
    }

    public void setAccessory(JComponent newAccessory) {
        JComponent oldValue = accessory;
        accessory = newAccessory;
        firePropertyChange(ACCESSORY_CHANGED_PROPERTY, oldValue, accessory);
    }

    /**
     * Si se eligen archivos, carpetas o los dos.
     *
     * @throws IllegalArgumentException si no es uno de los tres.
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
     * Si se puede elegir mas de un archivo.
     *
     * <p>Cambiar de modo limpia lo que habia elegido, en las dos direcciones: al prender se
     * descarta el archivo unico y al apagar el arreglo. Arrastrar una eleccion de un modo al otro
     * dejaria al selector mostrando una eleccion que su modo nuevo no sabe representar.
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

    /** Si los archivos ocultos se esconden. */
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
     * El filtro en uso.
     *
     * <p>Lo que ya estaba elegido y el filtro nuevo no acepta deja de estar elegido. Sin eso, el
     * selector devolveria un archivo que el mismo esta escondiendo.
     */
    public void setFileFilter(FileFilter filter) {
        FileFilter oldValue = fileFilter;
        fileFilter = filter;
        if (filter != null) {
            if (isMultiSelectionEnabled() && selectedFiles != null && selectedFiles.length > 0) {
                Vector<File> quedan = new Vector<File>();
                boolean cambio = false;
                for (int i = 0; i < selectedFiles.length; i++) {
                    if (filter.accept(selectedFiles[i])) {
                        quedan.addElement(selectedFiles[i]);
                    } else {
                        cambio = true;
                    }
                }
                if (cambio) {
                    File[] arr = new File[quedan.size()];
                    quedan.copyInto(arr);
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

    /** Quien decide el nombre, el icono y la descripcion de cada archivo. */
    public void setFileView(FileView fileView) {
        FileView oldValue = this.fileView;
        this.fileView = fileView;
        firePropertyChange(FILE_VIEW_CHANGED_PROPERTY, oldValue, fileView);
    }

    public FileView getFileView() {
        return fileView;
    }

    /**
     * El nombre que se muestra de ese archivo.
     *
     * <p>Primero la vista propia, si la hay, y si no la del aspecto. Ese orden es lo que permite
     * cambiar solo algunos nombres sin escribir una vista entera.
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

    /** Si se puede entrar a ese archivo; una carpeta si, un archivo no. */
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
     * Si el filtro acepta ese archivo.
     *
     * <p>Una carpeta se acepta siempre: filtrar carpetas impediria llegar a los archivos que estan
     * adentro.
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

    /** El usuario acepto; cierra el dialogo y avisa. */
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
