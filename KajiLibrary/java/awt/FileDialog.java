package java.awt;

import java.io.File;
import java.io.FilenameFilter;

/**
 * El cuadro de "abrir" o "guardar" del sistema.
 *
 * <p>Es **modal**: {@link #setVisible setVisible(true)} no vuelve hasta que el usuario elige o
 * cancela, y recién ahí {@link #getFile} tiene la respuesta. Un `null` ahí quiere decir que canceló.
 *
 * <p>Sin pantalla no hay cuadro del sistema que mostrar, así que nunca se elige nada y el archivo
 * queda como lo dejó {@link #setFile}. El resto de la clase --el modo, el directorio, el filtro, la
 * selección múltiple-- funciona entero: es estado, no interfaz.
 *
 * <p>El {@link FilenameFilter} tiene una advertencia que viene del JDK y no de acá: en Windows
 * **no se usa**, porque el cuadro nativo filtra por extensión y no admite un predicado. Lo que se
 * fije se guarda y {@link #getFilenameFilter} lo devuelve, pero no cambia lo que el usuario ve.
 */
public class FileDialog extends Dialog {

    private static final long serialVersionUID = 5035145889651310422L;

    private static int fileDialogCounter = 0;

    /** El cuadro es para abrir. */
    public static final int LOAD = 0;

    /** Es para guardar. */
    public static final int SAVE = 1;

    /** Cuál de los dos. */
    int mode;

    /** En qué directorio arranca. */
    String dir;

    /** Qué archivo quedó elegido, o `null` si ninguno. */
    String file;

    /** El filtro, o `null`. */
    FilenameFilter filter;

    /** Si deja elegir varios. */
    private boolean multipleMode = false;

    /** Los archivos elegidos. */
    private File[] files = new File[0];

    /** Un cuadro de apertura sin título, colgado de ese marco. */
    public FileDialog(Frame parent) {
        this(parent, "", LOAD);
    }

    /** Un cuadro de apertura con ese título. */
    public FileDialog(Frame parent, String title) {
        this(parent, title, LOAD);
    }

    /**
     * Un cuadro con ese título y ese modo.
     *
     * @throws IllegalArgumentException si el modo no es {@link #LOAD} ni {@link #SAVE}
     */
    public FileDialog(Frame parent, String title, int mode) {
        super(parent, title, true);
        this.setMode(mode);
        this.setLayout(null);
    }

    /** Un cuadro de apertura sin título, colgado de ese diálogo. */
    public FileDialog(Dialog parent) {
        this(parent, "", LOAD);
    }

    /** Un cuadro de apertura con ese título, colgado de ese diálogo. */
    public FileDialog(Dialog parent, String title) {
        this(parent, title, LOAD);
    }

    /**
     * Un cuadro con ese título y ese modo, colgado de ese diálogo.
     *
     * @throws IllegalArgumentException si el modo no es {@link #LOAD} ni {@link #SAVE}
     */
    public FileDialog(Dialog parent, String title, int mode) {
        super(parent, title, true);
        this.setMode(mode);
        this.setLayout(null);
    }

    /**
     * Cambia el título.
     *
     * <p>Un `null` se toma como cadena vacía: el cuadro del sistema no admite quedarse sin título.
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

    /** Lo declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Si es de apertura o de guardado. */
    public int getMode() {
        return this.mode;
    }

    /**
     * Cambia el modo.
     *
     * @throws IllegalArgumentException si no es {@link #LOAD} ni {@link #SAVE}
     */
    public void setMode(int mode) {
        if (mode != LOAD && mode != SAVE) {
            throw new IllegalArgumentException("illegal file dialog mode");
        }
        this.mode = mode;
    }

    /**
     * En qué directorio arranca.
     *
     * @return el directorio, o `null` si no se fijó ninguno
     */
    public String getDirectory() {
        return this.dir;
    }

    /**
     * Cambia el directorio de arranque.
     *
     * <p>Una cadena vacía cuenta como `null`, que es como el JDK distingue "sin preferencia" de un
     * directorio de verdad.
     */
    public void setDirectory(String dir) {
        this.dir = dir != null && dir.isEmpty() ? null : dir;
    }

    /**
     * Qué archivo quedó elegido.
     *
     * @return el nombre, o `null` si el usuario canceló o el cuadro no llegó a mostrarse
     */
    public String getFile() {
        return this.file;
    }

    /**
     * Los archivos elegidos.
     *
     * @return los archivos; un arreglo vacío si no se eligió ninguno. Nunca `null`.
     */
    public File[] getFiles() {
        synchronized (this.getObjectLock()) {
            File[] r = new File[this.files.length];
            System.arraycopy(this.files, 0, r, 0, this.files.length);
            return r;
        }
    }

    /**
     * Fija el archivo que el cuadro muestra de entrada.
     *
     * <p>También es lo que {@link #getFile} devuelve mientras nadie elija otra cosa. Una cadena vacía
     * cuenta como `null`, igual que en {@link #setDirectory}.
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

    /** Deja elegir varios archivos o uno solo. */
    public void setMultipleMode(boolean enable) {
        synchronized (this.getObjectLock()) {
            this.multipleMode = enable;
        }
    }

    /** Si deja elegir varios. */
    public boolean isMultipleMode() {
        synchronized (this.getObjectLock()) {
            return this.multipleMode;
        }
    }

    /**
     * El filtro.
     *
     * @return el filtro, o `null` si no se fijó ninguno
     */
    public FilenameFilter getFilenameFilter() {
        return this.filter;
    }

    /** Cambia el filtro; ver la advertencia de la clase sobre Windows. */
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
