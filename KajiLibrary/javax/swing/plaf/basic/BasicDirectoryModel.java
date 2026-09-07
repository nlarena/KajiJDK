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
 * La lista de archivos que muestra un {@link JFileChooser}.
 *
 * <h2>Primero las carpetas, despues los archivos</h2>
 *
 * <p>El modelo guarda dos listas separadas y las presenta como una sola: {@link #getElementAt} sirve
 * primero las carpetas y despues los archivos. No es una preferencia estetica -- es lo que hace que
 * navegar sea rapido: las carpetas son a donde se va, y estan siempre arriba, en el mismo lugar.
 *
 * <p>Dentro de cada grupo el orden lo decide {@link #lt}, que compara por nombre sin distinguir
 * mayusculas. Una subclase que quiera ordenar por fecha o por tamano redefine ese metodo y no toca
 * nada mas.
 *
 * <h2>La carga es sincronica, y en el JDK no</h2>
 *
 * <p>El JDK lee la carpeta en otro hilo, porque listar una carpeta de red o un disco dormido puede
 * tardar y congelaria la ventana. Aca se lee en el hilo que llama, y es a proposito: esta VM tiene
 * un problema de recoleccion por el cual {@code new File(padre, hijo)} en un hilo secundario mata
 * el hilo sin tirar nada -- ver {@code java/BxDbgF.java}, que lo reproduce en tres lineas --, y el
 * cargador quedaria haciendo exactamente eso. Un modelo que contesta cero para siempre es peor que
 * uno que tarda.
 *
 * <p>La diferencia que se ve es a favor: en el JDK {@link #getSize} puede contestar cero justo
 * despues de crear el modelo y el numero de verdad unos milisegundos mas tarde; aca ya esta.
 * Cuando el problema de la VM se arregle, esto vuelve a ser un hilo.
 *
 * <p>{@link #invalidateFileCache} tira lo leido y {@link #validateFileCache} vuelve a leer; el
 * {@link JFileChooser} las llama cuando cambia de carpeta, de filtro, o de si muestra los ocultos.
 *
 * <h2>Fuera de rango revienta</h2>
 *
 * <p>{@link #getElementAt} con un indice que no existe tira
 * {@code ArrayIndexOutOfBoundsException}, para los dos lados. No comprueba nada: deja que reviente
 * el {@link Vector} de adentro, y esta medido.
 */
public class BasicDirectoryModel extends AbstractListModel<Object>
        implements PropertyChangeListener {

    private final JFileChooser filechooser;
    private Vector<File> fileCache = new Vector<File>();
    private Vector<File> directories;
    private Vector<File> files;
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    /** Para ese selector; empieza a leer su carpeta enseguida. */
    public BasicDirectoryModel(JFileChooser filechooser) {
        this.filechooser = filechooser;
        validateFileCache();
    }

    /** Tira lo leido y vuelve a leer; ver la nota de la clase. */
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

    /** Olvida las dos listas derivadas; la proxima consulta las vuelve a armar. */
    public void invalidateFileCache() {
        directories = null;
        files = null;
    }

    /**
     * Las carpetas, con {@code ".."} adelante.
     *
     * <p>Ese primer elemento no esta en el modelo -- {@link #getSize} no lo cuenta y
     * {@link #getElementAt} no lo devuelve --: es el atajo al directorio de arriba, que el selector
     * dibuja aparte. Esta medido, y es facil de confundir: {@code getDirectories().size()} mas
     * {@code getFiles().size()} da uno mas que {@code getSize()}.
     */
    public Vector<File> getDirectories() {
        synchronized (fileCache) {
            if (directories != null) {
                return directories;
            }
            armarListas();
            return directories;
        }
    }

    /**
     * Los archivos que no son carpetas.
     *
     * <p>Ojo con el nombre: no es "todo lo que hay", es "lo que no es carpeta". Todo junto se pide
     * recorriendo el modelo con {@link #getElementAt}.
     */
    public Vector<File> getFiles() {
        synchronized (fileCache) {
            if (files != null) {
                return files;
            }
            armarListas();
            return files;
        }
    }

    /** Parte lo leido en carpetas y archivos; ver {@link #getDirectories}. */
    private void armarListas() {
        Vector<File> nuevasCarpetas = new Vector<File>();
        Vector<File> nuevosArchivos = new Vector<File>();
        nuevasCarpetas.addElement(filechooser.getFileSystemView()
                .createFileObject(filechooser.getCurrentDirectory(), ".."));
        for (int i = 0; i < fileCache.size(); i++) {
            File f = fileCache.get(i);
            if (filechooser.isTraversable(f)) {
                nuevasCarpetas.addElement(f);
            } else {
                nuevosArchivos.addElement(f);
            }
        }
        directories = nuevasCarpetas;
        files = nuevosArchivos;
    }

    /** Lee la carpeta; ver la nota de la clase sobre por que no es en otro hilo. */
    public void validateFileCache() {
        File currentDirectory = filechooser.getCurrentDirectory();
        if (currentDirectory == null) {
            return;
        }
        FileSystemView fsv = filechooser.getFileSystemView();
        File[] leidos = fsv.getFiles(currentDirectory, filechooser.isFileHidingEnabled());
        if (leidos == null) {
            return;
        }
        Vector<File> traversables = new Vector<File>();
        Vector<File> sueltos = new Vector<File>();
        for (int i = 0; i < leidos.length; i++) {
            File f = leidos[i];
            if (!filechooser.accept(f)) {
                continue;
            }
            if (filechooser.isTraversable(f)) {
                traversables.addElement(f);
            } else if (filechooser.isFileSelectionEnabled()) {
                sueltos.addElement(f);
            }
        }
        sort(traversables);
        sort(sueltos);
        Vector<File> nuevoCache = new Vector<File>();
        nuevoCache.addAll(traversables);
        nuevoCache.addAll(sueltos);
        synchronized (this) {
            fileCache = nuevoCache;
            invalidateFileCache();
        }
        fireContentsChanged();
    }

    /**
     * Cambia el nombre de un archivo.
     *
     * <p>Si sale bien, vuelve a leer la carpeta: el archivo cambia de lugar en el orden.
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

    /** Avisa que la lista entera cambio. */
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
     * El elemento de esa posicion: primero las carpetas, despues los archivos.
     *
     * @throws ArrayIndexOutOfBoundsException si el indice no existe; ver la nota de la clase
     */
    public Object getElementAt(int index) {
        return fileCache.elementAt(index);
    }

    /** No hace nada: el modelo se entera de los cambios por su cuenta, no por eventos. */
    public void intervalAdded(ListDataEvent e) {
    }

    /** Idem. */
    public void intervalRemoved(ListDataEvent e) {
    }

    /**
     * Ordena la lista con {@link #lt}.
     *
     * <p>Ordenamiento por insercion: la lista tiene el tamano de una carpeta y el codigo se lee de
     * una sentada. Con carpetas de cien mil archivos habria que cambiarlo.
     */
    protected void sort(Vector<? extends File> v) {
        for (int i = 1; i < v.size(); i++) {
            File actual = v.get(i);
            int j = i - 1;
            while (j >= 0 && lt(actual, v.get(j))) {
                ponerEn(v, j + 1, v.get(j));
                j--;
            }
            ponerEn(v, j + 1, actual);
        }
    }

    /** El {@code set} con el comodin puesto; ver el hallazgo #516 sobre esto. */
    @SuppressWarnings("unchecked")
    private static void ponerEn(Vector<? extends File> v, int i, File f) {
        ((Vector<File>) v).set(i, f);
    }

    /**
     * Si {@code a} va antes que {@code b}.
     *
     * <p>Por nombre y sin distinguir mayusculas: en una lista de archivos, {@code Documentos} y
     * {@code documentos} tienen que quedar juntos, no uno en cada punta.
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

    /** Para avisar de los cambios propios del modelo; ver {@link #addPropertyChangeListener}. */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
}
