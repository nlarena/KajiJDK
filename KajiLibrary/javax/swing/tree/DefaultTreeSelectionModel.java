package javax.swing.tree;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.EventListener;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.SwingPropertyChangeSupport;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * La seleccion de un arbol.
 *
 * <h2>Dos representaciones de lo mismo</h2>
 *
 * <p>Los caminos elegidos van en {@link #selection}; las filas correspondientes, en un
 * {@link DefaultListSelectionModel}. Las dos dicen lo mismo, y sin embargo hacen falta las dos: los
 * caminos sobreviven a desplegar y plegar, y las filas son lo que la vista dibuja.
 *
 * <p>Mantenerlas en acuerdo es todo el trabajo de esta clase. {@link #resetRowSelection} es donde
 * se rehacen las filas a partir de los caminos, y la llama la vista cada vez que cambia lo
 * desplegado.
 *
 * <h2>El guia</h2>
 *
 * <p>{@link #getLeadSelectionPath} es el ultimo camino que se toco, y es desde donde se extiende con
 * Shift. No es el ultimo del arreglo: sacar un camino del medio no cambia el guia.
 */
public class DefaultTreeSelectionModel implements Cloneable, Serializable, TreeSelectionModel {

    /** El nombre de la propiedad del modo de seleccion. */
    public static final String SELECTION_MODE_PROPERTY = "selectionMode";

    /** Quienes escuchan los cambios de propiedad. */
    protected SwingPropertyChangeSupport changeSupport;

    /** Los caminos elegidos. */
    protected TreePath[] selection;

    /** Quienes escuchan los cambios de seleccion. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Quien traduce caminos a filas. */
    protected transient RowMapper rowMapper;

    /** Las filas elegidas; ver la nota de la clase. */
    protected DefaultListSelectionModel listSelectionModel;

    /** Uno, contiguos o cualquiera. */
    protected int selectionMode;

    /** El ultimo camino que se toco. */
    protected TreePath leadPath;

    /** Su posicion en {@link #selection}, o -1. */
    protected int leadIndex;

    /** Su fila, o -1 si no se ve. */
    protected int leadRow;

    private Hashtable<TreePath, Boolean> uniquePaths;
    private Hashtable<TreePath, Boolean> lastPaths;
    private TreePath[] tempPaths;

    /** Un modelo sin nada elegido, que acepta cualquier conjunto. */
    public DefaultTreeSelectionModel() {
        listSelectionModel = new DefaultListSelectionModel();
        selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        leadIndex = -1;
        leadRow = -1;
        uniquePaths = new Hashtable<TreePath, Boolean>();
        lastPaths = new Hashtable<TreePath, Boolean>();
        tempPaths = new TreePath[1];
    }

    /** Cambiar quien traduce filas obliga a rehacerlas. */
    public void setRowMapper(RowMapper newMapper) {
        rowMapper = newMapper;
        resetRowSelection();
    }

    public RowMapper getRowMapper() {
        return rowMapper;
    }

    /**
     * Cuantos nodos se pueden elegir a la vez.
     *
     * <p>Un valor que no sea uno de los tres se toma como el mas permisivo, no como un error. Es lo
     * que hace el JDK.
     */
    public void setSelectionMode(int mode) {
        int oldMode = selectionMode;
        selectionMode = mode;
        if (selectionMode != TreeSelectionModel.SINGLE_TREE_SELECTION
                && selectionMode != TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
                && selectionMode != TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
            selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;
        }
        if (oldMode != selectionMode && changeSupport != null) {
            changeSupport.firePropertyChange(SELECTION_MODE_PROPERTY,
                    Integer.valueOf(oldMode), Integer.valueOf(selectionMode));
        }
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    public void setSelectionPath(TreePath path) {
        if (path == null) {
            setSelectionPaths(null);
        } else {
            TreePath[] newPaths = new TreePath[1];
            newPaths[0] = path;
            setSelectionPaths(newPaths);
        }
    }

    /**
     * Deja elegidos solo esos caminos.
     *
     * <p>Los repetidos se descartan y, si el modo lo pide, se recorta a uno o al primer tramo
     * contiguo. Recortar en silencio es lo que hace el JDK: el modo es una promesa sobre lo que el
     * modelo va a contener, no una validacion de lo que se le pide.
     */
    public void setSelectionPaths(TreePath[] pPaths) {
        int newCount = (pPaths == null) ? 0 : pPaths.length;
        if (newCount == 0 && (selection == null || selection.length == 0)) {
            return;
        }
        TreePath[] paths = pPaths;
        if (selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION && newCount > 1) {
            paths = new TreePath[] {pPaths[0]};
            newCount = 1;
        }

        Vector<PathPlaceHolder> cambios = new Vector<PathPlaceHolder>();
        Hashtable<TreePath, Boolean> nuevos = new Hashtable<TreePath, Boolean>();
        Vector<TreePath> limpios = new Vector<TreePath>();
        for (int i = 0; i < newCount; i++) {
            TreePath p = paths[i];
            if (p != null && !nuevos.containsKey(p)) {
                nuevos.put(p, Boolean.TRUE);
                limpios.addElement(p);
            }
        }
        TreePath[] finales = new TreePath[limpios.size()];
        limpios.copyInto(finales);

        if (selectionMode == TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
                && !arePathsContiguous(finales) && finales.length > 0) {
            finales = new TreePath[] {finales[0]};
        }

        // Lo que llega primero y lo que se va despues. El orden se ve: el evento lleva los
        // caminos en un arreglo, y quien lo recorra los recibe asi.
        for (int i = 0; i < finales.length; i++) {
            if (!estaba(finales[i])) {
                cambios.addElement(new PathPlaceHolder(finales[i], true));
            }
        }
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                if (!nuevos.containsKey(selection[i])) {
                    cambios.addElement(new PathPlaceHolder(selection[i], false));
                }
            }
        }

        selection = (finales.length == 0) ? null : finales;
        uniquePaths.clear();
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                uniquePaths.put(selection[i], Boolean.TRUE);
            }
        }
        leadPath = (selection == null || selection.length == 0)
                ? null : selection[selection.length - 1];
        updateLeadIndex();
        resetRowSelection();
        if (cambios.size() > 0) {
            notifyPathChange(cambios, leadPath);
        }
    }

    private boolean estaba(TreePath p) {
        if (selection == null) {
            return false;
        }
        for (int i = 0; i < selection.length; i++) {
            if (selection[i].equals(p)) {
                return true;
            }
        }
        return false;
    }

    public void addSelectionPath(TreePath path) {
        if (path != null) {
            TreePath[] toAdd = new TreePath[1];
            toAdd[0] = path;
            addSelectionPaths(toAdd);
        }
    }

    /** Agrega esos caminos a lo elegido. */
    public void addSelectionPaths(TreePath[] paths) {
        if (paths == null || paths.length == 0) {
            return;
        }
        if (selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION) {
            setSelectionPaths(paths);
            return;
        }
        Vector<TreePath> juntos = new Vector<TreePath>();
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                juntos.addElement(selection[i]);
            }
        }
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null && !isPathSelected(paths[i])) {
                juntos.addElement(paths[i]);
            }
        }
        TreePath[] arr = new TreePath[juntos.size()];
        juntos.copyInto(arr);
        setSelectionPaths(arr);
    }

    public void removeSelectionPath(TreePath path) {
        if (path != null) {
            TreePath[] rPath = new TreePath[1];
            rPath[0] = path;
            removeSelectionPaths(rPath);
        }
    }

    /** Saca esos caminos de lo elegido. */
    public void removeSelectionPaths(TreePath[] paths) {
        if (paths == null || selection == null || paths.length == 0) {
            return;
        }
        Hashtable<TreePath, Boolean> sacar = new Hashtable<TreePath, Boolean>();
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                sacar.put(paths[i], Boolean.TRUE);
            }
        }
        Vector<TreePath> quedan = new Vector<TreePath>();
        for (int i = 0; i < selection.length; i++) {
            if (!sacar.containsKey(selection[i])) {
                quedan.addElement(selection[i]);
            }
        }
        TreePath[] arr = new TreePath[quedan.size()];
        quedan.copyInto(arr);
        setSelectionPaths(arr);
    }

    /** El primero de los elegidos, o nulo. */
    public TreePath getSelectionPath() {
        if (selection != null && selection.length > 0) {
            return selection[0];
        }
        return null;
    }

    /** Los elegidos; es una copia. */
    public TreePath[] getSelectionPaths() {
        if (selection != null) {
            TreePath[] out = new TreePath[selection.length];
            System.arraycopy(selection, 0, out, 0, selection.length);
            return out;
        }
        return new TreePath[0];
    }

    public int getSelectionCount() {
        return (selection == null) ? 0 : selection.length;
    }

    public boolean isPathSelected(TreePath path) {
        return (path == null) ? false : (uniquePaths.get(path) != null);
    }

    public boolean isSelectionEmpty() {
        return (selection == null || selection.length == 0);
    }

    public void clearSelection() {
        if (selection != null && selection.length > 0) {
            int selSize = selection.length;
            boolean[] newness = new boolean[selSize];
            Vector<PathPlaceHolder> cambios = new Vector<PathPlaceHolder>();
            for (int counter = 0; counter < selSize; counter++) {
                cambios.addElement(new PathPlaceHolder(selection[counter], false));
            }
            selection = null;
            uniquePaths.clear();
            resetRowSelection();
            leadPath = null;
            leadIndex = -1;
            leadRow = -1;
            notifyPathChange(cambios, null);
        }
    }

    public void addTreeSelectionListener(TreeSelectionListener x) {
        listenerList.add(TreeSelectionListener.class, x);
    }

    public void removeTreeSelectionListener(TreeSelectionListener x) {
        listenerList.remove(TreeSelectionListener.class, x);
    }

    public TreeSelectionListener[] getTreeSelectionListeners() {
        return listenerList.getListeners(TreeSelectionListener.class);
    }

    protected void fireValueChanged(TreeSelectionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeSelectionListener.class) {
                ((TreeSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Las filas de lo elegido, ordenadas; vacio si no hay quien traduzca. */
    public int[] getSelectionRows() {
        if (rowMapper != null && selection != null && selection.length > 0) {
            int[] filas = rowMapper.getRowsForPaths(selection);
            if (filas != null) {
                int n = 0;
                for (int i = 0; i < filas.length; i++) {
                    if (filas[i] != -1) {
                        n++;
                    }
                }
                if (n != filas.length) {
                    // Los caminos que no se ven dan -1 y no son filas.
                    int[] out = new int[n];
                    int k = 0;
                    for (int i = 0; i < filas.length; i++) {
                        if (filas[i] != -1) {
                            out[k] = filas[i];
                            k++;
                        }
                    }
                    return out;
                }
                return filas;
            }
        }
        return new int[0];
    }

    public int getMinSelectionRow() {
        return listSelectionModel.getMinSelectionIndex();
    }

    public int getMaxSelectionRow() {
        return listSelectionModel.getMaxSelectionIndex();
    }

    public boolean isRowSelected(int row) {
        return listSelectionModel.isSelectedIndex(row);
    }

    /**
     * Rehace las filas a partir de los caminos.
     *
     * <p>La llama la vista cuando cambia lo desplegado; ver la nota de la clase.
     */
    public void resetRowSelection() {
        listSelectionModel.clearSelection();
        if (selection != null && rowMapper != null) {
            int[] filas = rowMapper.getRowsForPaths(selection);
            if (filas != null) {
                for (int i = 0; i < filas.length; i++) {
                    if (filas[i] != -1) {
                        listSelectionModel.addSelectionInterval(filas[i], filas[i]);
                    }
                }
            }
        }
        insureRowContinuity();
        leadRow = -1;
        if (leadPath != null && rowMapper != null) {
            tempPaths[0] = leadPath;
            int[] f = rowMapper.getRowsForPaths(tempPaths);
            if (f != null && f.length > 0) {
                leadRow = f[0];
            }
        }
    }

    public int getLeadSelectionRow() {
        return leadRow;
    }

    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new SwingPropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Recorta la seleccion si el modo pide filas seguidas y dejaron de serlo.
     *
     * <p>Puede pasar sin que nadie toque la seleccion: plegar un nodo del medio cambia las filas.
     * Por eso se llama desde {@link #resetRowSelection} y no solo al elegir.
     */
    protected void insureRowContinuity() {
        if (selectionMode == TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
                && selection != null && rowMapper != null) {
            DefaultListSelectionModel lModel = listSelectionModel;
            int min = lModel.getMinSelectionIndex();
            if (min != -1) {
                int max = lModel.getMaxSelectionIndex();
                for (int counter = min; counter <= max; counter++) {
                    if (!lModel.isSelectedIndex(counter)) {
                        // El primer hueco corta: se queda lo de antes.
                        if (counter == min) {
                            clearSelection();
                        } else {
                            TreePath[] newSel = new TreePath[counter - min];
                            int[] filas = rowMapper.getRowsForPaths(selection);
                            int k = 0;
                            for (int i = 0; i < filas.length && k < newSel.length; i++) {
                                if (filas[i] < counter && filas[i] >= min) {
                                    newSel[k] = selection[i];
                                    k++;
                                }
                            }
                            setSelectionPaths(newSel);
                        }
                        return;
                    }
                }
            }
        } else if (selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION
                && selection != null && selection.length > 1) {
            setSelectionPath(selection[0]);
        }
    }

    /** Si esos caminos caen en filas seguidas. */
    protected boolean arePathsContiguous(TreePath[] paths) {
        if (rowMapper == null || paths.length < 2) {
            return true;
        }
        int[] filas = rowMapper.getRowsForPaths(paths);
        if (filas == null) {
            return true;
        }
        int min = Integer.MAX_VALUE;
        int max = -1;
        int vistas = 0;
        for (int i = 0; i < filas.length; i++) {
            if (filas[i] == -1) {
                return false;
            }
            min = Math.min(min, filas[i]);
            max = Math.max(max, filas[i]);
            vistas++;
        }
        return (max - min + 1) == vistas;
    }

    /** Si agregar esos caminos deja la seleccion como el modo permite. */
    protected boolean canPathsBeAdded(TreePath[] paths) {
        if (paths == null || paths.length == 0 || rowMapper == null || selection == null
                || selectionMode == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
            return true;
        }
        Vector<TreePath> v = new Vector<TreePath>();
        for (int i = 0; i < selection.length; i++) {
            v.addElement(selection[i]);
        }
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null && !isPathSelected(paths[i])) {
                v.addElement(paths[i]);
            }
        }
        TreePath[] arr = new TreePath[v.size()];
        v.copyInto(arr);
        return arePathsContiguous(arr);
    }

    /** Si sacar esos caminos deja la seleccion como el modo permite. */
    protected boolean canPathsBeRemoved(TreePath[] paths) {
        if (rowMapper == null || selection == null
                || selectionMode == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
            return true;
        }
        Hashtable<TreePath, Boolean> sacar = new Hashtable<TreePath, Boolean>();
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                sacar.put(paths[i], Boolean.TRUE);
            }
        }
        Vector<TreePath> quedan = new Vector<TreePath>();
        for (int i = 0; i < selection.length; i++) {
            if (!sacar.containsKey(selection[i])) {
                quedan.addElement(selection[i]);
            }
        }
        TreePath[] arr = new TreePath[quedan.size()];
        quedan.copyInto(arr);
        return arePathsContiguous(arr);
    }

    /**
     * Manda un solo aviso con todo lo que entro y lo que salio.
     *
     * <p>Un aviso por camino haria que reemplazar una seleccion de cien nodos costara doscientos
     * avisos, y quien escucha veria estados intermedios que nunca existieron.
     */
    protected void notifyPathChange(Vector<?> changedPaths, TreePath oldLeadSelection) {
        int cPathCount = changedPaths.size();
        boolean[] newness = new boolean[cPathCount];
        TreePath[] paths = new TreePath[cPathCount];
        for (int counter = 0; counter < cPathCount; counter++) {
            PathPlaceHolder placeholder = (PathPlaceHolder) changedPaths.elementAt(counter);
            newness[counter] = placeholder.isNew;
            paths[counter] = placeholder.path;
        }
        TreeSelectionEvent event = new TreeSelectionEvent(this, paths, newness,
                oldLeadSelection, leadPath);
        fireValueChanged(event);
    }

    /** Recalcula en que posicion del arreglo esta el guia. */
    protected void updateLeadIndex() {
        leadIndex = -1;
        if (leadPath != null && selection != null) {
            for (int counter = selection.length - 1; counter >= 0; counter--) {
                if (selection[counter] == leadPath) {
                    leadIndex = counter;
                    break;
                }
            }
        }
    }

    /**
     * Saca los caminos repetidos.
     *
     * @deprecated Los repetidos ya se descartan al elegir; no queda nada que hacer aca.
     */
    @Deprecated
    protected void insureUniqueness() {
    }

    public String toString() {
        int selCount = getSelectionCount();
        StringBuilder retBuffer = new StringBuilder();
        int[] rows;
        if (rowMapper != null) {
            rows = rowMapper.getRowsForPaths(selection);
        } else {
            rows = null;
        }
        retBuffer.append(getClass().getName()).append(" ").append(hashCode()).append(" [");
        for (int counter = 0; counter < selCount; counter++) {
            if (rows != null) {
                retBuffer.append(selection[counter].getLastPathComponent()).append("@")
                        .append(rows[counter]).append(" ");
            } else {
                retBuffer.append(selection[counter].getLastPathComponent()).append(" ");
            }
        }
        retBuffer.append("]");
        return retBuffer.toString();
    }

    /** Una copia con la misma seleccion y sin los que escuchan. */
    public Object clone() throws CloneNotSupportedException {
        DefaultTreeSelectionModel clone = (DefaultTreeSelectionModel) super.clone();
        clone.changeSupport = null;
        if (selection != null) {
            int selLength = selection.length;
            clone.selection = new TreePath[selLength];
            System.arraycopy(selection, 0, clone.selection, 0, selLength);
        }
        clone.listenerList = new EventListenerList();
        clone.listSelectionModel = (DefaultListSelectionModel) listSelectionModel.clone();
        clone.uniquePaths = new Hashtable<TreePath, Boolean>();
        clone.lastPaths = new Hashtable<TreePath, Boolean>();
        clone.tempPaths = new TreePath[1];
        return clone;
    }

    /** Un camino y si entro o salio; se usa para armar el aviso. */
    static final class PathPlaceHolder {

        final TreePath path;
        final boolean isNew;

        PathPlaceHolder(TreePath path, boolean isNew) {
            this.path = path;
            this.isNew = isNew;
        }
    }
}
