package javax.swing.filechooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

/**
 * Lo que el selector de archivos necesita saber del sistema y {@link File} no cuenta.
 *
 * <h2>Que le falta a {@link File}</h2>
 *
 * <p>{@code File} es una ruta: sabe si existe, si es directorio, que contiene. Lo que no sabe es
 * nada de lo que el <em>escritorio</em> le agrega encima — que {@code C:\} se llama "Disco local",
 * que hay una carpeta "Mis documentos" que no es una ruta fija, que un acceso directo apunta a otro
 * lado, que tal icono corresponde a tal tipo. Esta clase es esa capa.
 *
 * <p>La division importa porque son dos modelos distintos: el del sistema de archivos y el que el
 * usuario ve. Un selector que mostrara solo el primero seria correcto y ajeno.
 *
 * <h2>Lo que esta VM contesta</h2>
 *
 * <p>{@link #getFileSystemView} devuelve una vista <strong>generica</strong>, construida sobre lo
 * que {@link File} si sabe. Es honesta y limitada, y conviene tener claro el limite: los nombres
 * para mostrar son los del sistema de archivos, no los del escritorio; {@link #getSystemIcon}
 * devuelve {@code null} porque los iconos del sistema no son cosa de Java; y los accesos directos no
 * se resuelven, porque eso pide hablar con la shell. Cada uno de esos metodos dice que devuelve, en
 * vez de fingir un dato de escritorio que nadie le dio.
 */
public abstract class FileSystemView {

    private static FileSystemView laGenerica;

    /**
     * La vista del sistema.
     *
     * <p>En el JDK real elige entre una implementacion de Windows, una de Unix y una generica. Aca
     * hay una sola: distinguirlas serviria para dar nombres de escritorio, y esos no estan
     * disponibles en ninguna de las dos plataformas desde Java puro.
     */
    public static FileSystemView getFileSystemView() {
        if (laGenerica == null) {
            laGenerica = new VistaGenerica();
        }
        return laGenerica;
    }

    /** Para las subclases. */
    public FileSystemView() {
    }

    /**
     * Si {@code f} es una raiz del arbol que el usuario ve.
     *
     * <p>No es lo mismo que no tener padre: en Windows el escritorio es raiz para el usuario y esta
     * adentro del perfil.
     */
    public boolean isRoot(File f) {
        if (f == null || !f.isAbsolute()) {
            return false;
        }
        File[] raices = getRoots();
        for (int i = 0; i < raices.length; i++) {
            if (raices[i].equals(f)) {
                return true;
            }
        }
        return false;
    }

    /** Si se puede entrar en {@code f}; {@code null} solo si no se puede decidir. */
    public Boolean isTraversable(File f) {
        return Boolean.valueOf(f.isDirectory());
    }

    /**
     * El nombre para mostrar.
     *
     * <p>El del sistema de archivos, no el del escritorio: una carpeta que Windows muestra
     * traducida aparece aca con su nombre real.
     */
    public String getSystemDisplayName(File f) {
        if (f == null) {
            return null;
        }
        String nombre = f.getName();
        // Una raiz como `C:\` tiene nombre vacio, y mostrar la nada seria peor que mostrar la ruta.
        if (nombre.isEmpty()) {
            return f.getPath();
        }
        return nombre;
    }

    /** La descripcion del tipo; {@code null} en esta VM, que no habla con el escritorio. */
    public String getSystemTypeDescription(File f) {
        return null;
    }

    /** El icono del sistema; {@code null} en esta VM. Ver la nota de la clase. */
    public Icon getSystemIcon(File f) {
        return null;
    }

    /** El icono del sistema en el tamano pedido; {@code null} en esta VM. */
    public Icon getSystemIcon(File f, int width, int height) {
        return null;
    }

    /** Si {@code folder} es el padre de {@code file}. */
    public boolean isParent(File folder, File file) {
        if (folder == null || file == null) {
            return false;
        }
        File padre = file.getParentFile();
        return folder.equals(padre);
    }

    /** El hijo de {@code parent} llamado {@code fileName}. */
    public File getChild(File parent, String fileName) {
        return createFileObject(parent, fileName);
    }

    /**
     * Si {@code f} es un archivo de verdad y no un nodo inventado por el escritorio.
     *
     * <p>Lo segundo existe: "Mi PC" aparece en el arbol y no es una ruta. Aca todo lo que llega es
     * una ruta, asi que la respuesta es siempre {@code true}.
     */
    public boolean isFileSystem(File f) {
        return true;
    }

    /** Crea una carpeta nueva; lo unico que las implementaciones tienen que escribir. */
    public abstract File createNewFolder(File containingDir) throws IOException;

    /** Si esta oculto. */
    public boolean isHiddenFile(File f) {
        return f.isHidden();
    }

    /** Si es una raiz del sistema de archivos. */
    public boolean isFileSystemRoot(File dir) {
        return dir != null && dir.getParentFile() == null;
    }

    /** Si es una unidad. Sin hablar con el sistema no se puede saber: {@code false}. */
    public boolean isDrive(File dir) {
        return false;
    }

    /** Si es una disquetera. {@code false} por lo mismo. */
    public boolean isFloppyDrive(File dir) {
        return false;
    }

    /** Si es un nodo de red. {@code false} por lo mismo. */
    public boolean isComputerNode(File dir) {
        return false;
    }

    /** Las raices del arbol. */
    public File[] getRoots() {
        return File.listRoots();
    }

    /** La carpeta del usuario. */
    public File getHomeDirectory() {
        return createFileObject(System.getProperty("user.home"));
    }

    /** Por donde empieza a mostrar un selector recien abierto. */
    public File getDefaultDirectory() {
        return getHomeDirectory();
    }

    /** Un {@link File} hijo, del tipo que esta vista use. */
    public File createFileObject(File dir, String filename) {
        if (dir == null) {
            return new File(filename);
        }
        return new File(dir, filename);
    }

    /** Un {@link File} a partir de una ruta. */
    public File createFileObject(String path) {
        File f = new File(path);
        if (isFileSystemRoot(f)) {
            return createFileSystemRoot(f);
        }
        return f;
    }

    /**
     * Lo que hay adentro de {@code dir}.
     *
     * @param useFileHiding si se esconden los archivos ocultos
     */
    public File[] getFiles(File dir, boolean useFileHiding) {
        List<File> visibles = new ArrayList<File>();
        File[] contenido = dir.listFiles();
        // `listFiles` devuelve `null` —no un arreglo vacio— cuando no se puede leer el directorio.
        // Confundir los dos casos es un `NullPointerException` en el peor momento: navegando.
        if (contenido == null) {
            return new File[0];
        }
        for (int i = 0; i < contenido.length; i++) {
            File f = contenido[i];
            if (!useFileHiding || !isHiddenFile(f)) {
                visibles.add(f);
            }
        }
        return visibles.toArray(new File[visibles.size()]);
    }

    /** El padre, o {@code null} si es una raiz. */
    public File getParentDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return null;
        }
        return dir.getParentFile();
    }

    /** Los lugares que el selector ofrece en su lista desplegable. */
    public File[] getChooserComboBoxFiles() {
        return getRoots();
    }

    /** Los lugares que el selector ofrece en su panel de atajos. */
    public final File[] getChooserShortcutPanelFiles() {
        return new File[0];
    }

    /** Si es un acceso directo. Sin hablar con la shell no se puede saber: {@code false}. */
    public boolean isLink(File file) {
        return false;
    }

    /**
     * Adonde apunta un acceso directo.
     *
     * @return {@code null} siempre en esta VM, que es lo que corresponde a {@link #isLink} diciendo
     *     que nada es un enlace
     */
    public File getLinkLocation(File file) throws FileNotFoundException {
        return null;
    }

    /** El {@link File} que representa una raiz del sistema de archivos. */
    protected File createFileSystemRoot(File f) {
        return new RaizDelSistema(f);
    }

    /**
     * Una raiz, que se comporta distinto en dos cosas.
     *
     * <p>{@code C:\} <strong>siempre</strong> es un directorio aunque la unidad este vacia o no
     * responda, y su nombre no es la cadena vacia que devolveria {@link File#getName} sino su ruta.
     * Sin esta clase, una unidad sin disco desapareceria del arbol.
     */
    static class RaizDelSistema extends File {

        private static final long serialVersionUID = 1L;

        public RaizDelSistema(File f) {
            super(f, "");
        }

        public RaizDelSistema(String s) {
            super(s);
        }

        public boolean isDirectory() {
            return true;
        }

        public String getName() {
            return getPath();
        }
    }

    /** La unica implementacion de esta VM; ver la nota de {@link FileSystemView}. */
    static class VistaGenerica extends FileSystemView {

        VistaGenerica() {
        }

        public File createNewFolder(File containingDir) throws IOException {
            if (containingDir == null) {
                throw new IOException("Hace falta el directorio que la contiene");
            }
            File nueva = createFileObject(containingDir, "NewFolder");
            // El nombre se numera hasta encontrar uno libre. Crear a ciegas pisaria una carpeta del
            // usuario, y fallar a la primera obligaria a renombrar antes de poder crear la segunda.
            int i = 2;
            while (nueva.exists() && i < 100) {
                nueva = createFileObject(containingDir, "NewFolder." + String.valueOf(i));
                i = i + 1;
            }
            if (nueva.exists()) {
                throw new IOException("El directorio ya existe: " + nueva.getAbsolutePath());
            }
            if (!nueva.mkdir()) {
                throw new IOException("No se pudo crear " + nueva.getAbsolutePath());
            }
            return nueva;
        }
    }
}
