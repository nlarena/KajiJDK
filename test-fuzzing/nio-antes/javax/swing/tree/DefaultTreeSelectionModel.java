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
 * A tree's selection.
 *
 * <h2>Two representations of the same thing</h2>
 *
 * <p>The chosen paths go in {@link #selection}; the corresponding rows, in a
 * {@link DefaultListSelectionModel}. The two say the same thing, and even so both are needed:
 * the paths survive expanding and collapsing, and the rows are what the view draws.
 *
 * <p>Keeping them in agreement is this class's whole job. {@link #resetRowSelection} is where
 * the rows are rebuilt from the paths, and the view calls it every time what is expanded
 * changes.
 *
 * <h2>The lead</h2>
 *
 * <p>{@link #getLeadSelectionPath} is the last path that was touched, and it is where Shift
 * extends from. It is not the last one in the array: removing a path from the middle does not
 * change the lead.
 */
public class DefaultTreeSelectionModel implements Cloneable, Serializable, TreeSelectionModel {

    /** The selection mode property's name. */
    public static final String SELECTION_MODE_PROPERTY = "selectionMode";

    /** Those who listen to property changes. */
    protected SwingPropertyChangeSupport changeSupport;

    /** The chosen paths. */
    protected TreePath[] selection;

    /** Those who listen to selection changes. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Who translates paths to rows. */
    protected transient RowMapper rowMapper;

    /** The chosen rows; see the class note. */
    protected DefaultListSelectionModel listSelectionModel;

    /** One, contiguous or any. */
    protected int selectionMode;

    /** The last path that was touched. */
    protected TreePath leadPath;

    /** Its position in {@link #selection}, or -1. */
    protected int leadIndex;

    /** Its row, or -1 if it is not seen. */
    protected int leadRow;

    private Hashtable<TreePath, Boolean> uniquePaths;
    private Hashtable<TreePath, Boolean> lastPaths;
    private TreePath[] tempPaths;

    /** A model with nothing chosen, which accepts any set. */
    public DefaultTreeSelectionModel() {
        listSelectionModel = new DefaultListSelectionModel();
        selectionMode = DISCONTIGUOUS_TREE_SELECTION;
        leadIndex = -1;
        leadRow = -1;
        uniquePaths = new Hashtable<TreePath, Boolean>();
        lastPaths = new Hashtable<TreePath, Boolean>();
        tempPaths = new TreePath[1];
    }

    /** Changing who translates rows forces rebuilding them. */
    public void setRowMapper(RowMapper newMapper) {
        rowMapper = newMapper;
        resetRowSelection();
    }

    public RowMapper getRowMapper() {
        return rowMapper;
    }

    /**
     * How many nodes can be chosen at a time.
     *
     * <p>A value that is not one of the three is taken as the most permissive one, not as an error.
     * It is what the JDK does.
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
     * It leaves only those paths chosen.
     *
     * <p>The repeated ones are discarded and, if the mode asks for it, it is cut down to one or to
     * the first contiguous stretch. Cutting down silently is what the JDK does: the mode is a
     * promise about what the model is going to contain, not a validation of what it is asked for.
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

        Vector<PathPlaceHolder> changes = new Vector<PathPlaceHolder>();
        Hashtable<TreePath, Boolean> added = new Hashtable<TreePath, Boolean>();
        Vector<TreePath> cleaned = new Vector<TreePath>();
        for (int i = 0; i < newCount; i++) {
            TreePath p = paths[i];
            if (p != null && !added.containsKey(p)) {
                added.put(p, Boolean.TRUE);
                cleaned.addElement(p);
            }
        }
        TreePath[] finalPaths = new TreePath[cleaned.size()];
        cleaned.copyInto(finalPaths);

        if (selectionMode == TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
                && !arePathsContiguous(finalPaths) && finalPaths.length > 0) {
            finalPaths = new TreePath[] {finalPaths[0]};
        }

        // What arrives first and what leaves afterwards. The order shows: the event carries the
                // paths in an array, and whoever walks it receives them that way.
        for (int i = 0; i < finalPaths.length; i++) {
            if (!wasThere(finalPaths[i])) {
                changes.addElement(new PathPlaceHolder(finalPaths[i], true));
            }
        }
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                if (!added.containsKey(selection[i])) {
                    changes.addElement(new PathPlaceHolder(selection[i], false));
                }
            }
        }

        selection = (finalPaths.length == 0) ? null : finalPaths;
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
        if (changes.size() > 0) {
            notifyPathChange(changes, leadPath);
        }
    }

    private boolean wasThere(TreePath p) {
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

    /** Adds those paths to what is chosen. */
    public void addSelectionPaths(TreePath[] paths) {
        if (paths == null || paths.length == 0) {
            return;
        }
        if (selectionMode == TreeSelectionModel.SINGLE_TREE_SELECTION) {
            setSelectionPaths(paths);
            return;
        }
        Vector<TreePath> together = new Vector<TreePath>();
        if (selection != null) {
            for (int i = 0; i < selection.length; i++) {
                together.addElement(selection[i]);
            }
        }
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null && !isPathSelected(paths[i])) {
                together.addElement(paths[i]);
            }
        }
        TreePath[] arr = new TreePath[together.size()];
        together.copyInto(arr);
        setSelectionPaths(arr);
    }

    public void removeSelectionPath(TreePath path) {
        if (path != null) {
            TreePath[] rPath = new TreePath[1];
            rPath[0] = path;
            removeSelectionPaths(rPath);
        }
    }

    /** Removes those paths from what is chosen. */
    public void removeSelectionPaths(TreePath[] paths) {
        if (paths == null || selection == null || paths.length == 0) {
            return;
        }
        Hashtable<TreePath, Boolean> remove = new Hashtable<TreePath, Boolean>();
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                remove.put(paths[i], Boolean.TRUE);
            }
        }
        Vector<TreePath> remaining = new Vector<TreePath>();
        for (int i = 0; i < selection.length; i++) {
            if (!remove.containsKey(selection[i])) {
                remaining.addElement(selection[i]);
            }
        }
        TreePath[] arr = new TreePath[remaining.size()];
        remaining.copyInto(arr);
        setSelectionPaths(arr);
    }

    /** The first of the chosen ones, or null. */
    public TreePath getSelectionPath() {
        if (selection != null && selection.length > 0) {
            return selection[0];
        }
        return null;
    }

    /** The chosen ones; it is a copy. */
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
            Vector<PathPlaceHolder> changes = new Vector<PathPlaceHolder>();
            for (int counter = 0; counter < selSize; counter++) {
                changes.addElement(new PathPlaceHolder(selection[counter], false));
            }
            selection = null;
            uniquePaths.clear();
            resetRowSelection();
            leadPath = null;
            leadIndex = -1;
            leadRow = -1;
            notifyPathChange(changes, null);
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

    /** The rows of what is chosen, in order; empty if there is nobody to translate. */
    public int[] getSelectionRows() {
        if (rowMapper != null && selection != null && selection.length > 0) {
            int[] rows = rowMapper.getRowsForPaths(selection);
            if (rows != null) {
                int n = 0;
                for (int i = 0; i < rows.length; i++) {
                    if (rows[i] != -1) {
                        n++;
                    }
                }
                if (n != rows.length) {
                    // The paths that are not seen give -1 and are not rows.
                    int[] out = new int[n];
                    int k = 0;
                    for (int i = 0; i < rows.length; i++) {
                        if (rows[i] != -1) {
                            out[k] = rows[i];
                            k++;
                        }
                    }
                    return out;
                }
                return rows;
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
     * It rebuilds the rows from the paths.
     *
     * <p>The view calls it when what is expanded changes; see the class note.
     */
    public void resetRowSelection() {
        listSelectionModel.clearSelection();
        if (selection != null && rowMapper != null) {
            int[] rows = rowMapper.getRowsForPaths(selection);
            if (rows != null) {
                for (int i = 0; i < rows.length; i++) {
                    if (rows[i] != -1) {
                        listSelectionModel.addSelectionInterval(rows[i], rows[i]);
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
     * It cuts the selection down if the mode asks for consecutive rows and they stopped being so.
     *
     * <p>It can happen without anybody touching the selection: collapsing a node in the middle
     * changes the rows. That is why it is called from {@link #resetRowSelection} and not only when
     * choosing.
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
                        // The first gap cuts: what came before stays.
                        if (counter == min) {
                            clearSelection();
                        } else {
                            TreePath[] newSel = new TreePath[counter - min];
                            int[] rows = rowMapper.getRowsForPaths(selection);
                            int k = 0;
                            for (int i = 0; i < rows.length && k < newSel.length; i++) {
                                if (rows[i] < counter && rows[i] >= min) {
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

    /** Whether those paths fall on consecutive rows. */
    protected boolean arePathsContiguous(TreePath[] paths) {
        if (rowMapper == null || paths.length < 2) {
            return true;
        }
        int[] rows = rowMapper.getRowsForPaths(paths);
        if (rows == null) {
            return true;
        }
        int min = Integer.MAX_VALUE;
        int max = -1;
        int seen = 0;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == -1) {
                return false;
            }
            min = Math.min(min, rows[i]);
            max = Math.max(max, rows[i]);
            seen++;
        }
        return (max - min + 1) == seen;
    }

    /** Whether adding those paths leaves the selection as the mode allows. */
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

    /** Whether removing those paths leaves the selection as the mode allows. */
    protected boolean canPathsBeRemoved(TreePath[] paths) {
        if (rowMapper == null || selection == null
                || selectionMode == TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION) {
            return true;
        }
        Hashtable<TreePath, Boolean> remove = new Hashtable<TreePath, Boolean>();
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                remove.put(paths[i], Boolean.TRUE);
            }
        }
        Vector<TreePath> remaining = new Vector<TreePath>();
        for (int i = 0; i < selection.length; i++) {
            if (!remove.containsKey(selection[i])) {
                remaining.addElement(selection[i]);
            }
        }
        TreePath[] arr = new TreePath[remaining.size()];
        remaining.copyInto(arr);
        return arePathsContiguous(arr);
    }

    /**
     * It sends a single notice with everything that came in and everything that went out.
     *
     * <p>One notice per path would make replacing a selection of a hundred nodes cost two hundred
     * notices, and whoever listens would see intermediate states that never existed.
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

    /** It recomputes at what position in the array the lead is. */
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
     * Removes the repeated paths.
     *
     * @deprecated The repeated ones are already discarded when choosing; there is nothing left to
     *     do here.
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

    /** A copy with the same selection and without those who listen. */
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

    /** A path and whether it came in or went out; it is used to build the notice. */
    static final class PathPlaceHolder {

        final TreePath path;
        final boolean isNew;

        PathPlaceHolder(TreePath path, boolean isNew) {
            this.path = path;
            this.isNew = isNew;
        }
    }
}
