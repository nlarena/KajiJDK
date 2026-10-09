package javax.swing;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TreeUI;
import javax.swing.text.Position;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * A tree of nodes that can be unfolded and folded.
 *
 * <h2>What the tree knows and what the view knows</h2>
 *
 * <p>The tree keeps the data ({@link TreeModel}), what is chosen
 * ({@link TreeSelectionModel}) and <em>what is unfolded</em>. That last one is its own and not
 * the model's: two trees over the same data may have different things unfolded.
 *
 * <p>What it does not know is the geometry. Which row a path falls in, which path there is at a
 * point, how much a row measures: all that is answered by the {@link TreeUI}, because it
 * depends on how it is drawn.
 *
 * <h2>Paths and rows</h2>
 *
 * <p>Almost everything has two versions, one by path and another by row. They are not
 * interchangeable: a path always exists, a row only if its parents are unfolded. Keeping rows
 * is what makes a selection shift by itself when something further up is folded.
 *
 * <h2>Giving notice before unfolding</h2>
 *
 * <p>{@link TreeWillExpandListener} arrives first and may veto with
 * {@link ExpandVetoException}. It is what allows the children to be loaded on unfolding -- and
 * refused if the loading fails -- instead of having the whole tree in memory.
 */
public class JTree extends JComponent implements Scrollable, Accessible {

    private static final String uiClassID = "TreeUI";

    public static final String CELL_RENDERER_PROPERTY = "cellRenderer";
    public static final String TREE_MODEL_PROPERTY = "model";
    public static final String ROOT_VISIBLE_PROPERTY = "rootVisible";
    public static final String SHOWS_ROOT_HANDLES_PROPERTY = "showsRootHandles";
    public static final String ROW_HEIGHT_PROPERTY = "rowHeight";
    public static final String CELL_EDITOR_PROPERTY = "cellEditor";
    public static final String EDITABLE_PROPERTY = "editable";
    public static final String LARGE_MODEL_PROPERTY = "largeModel";
    public static final String SELECTION_MODEL_PROPERTY = "selectionModel";
    public static final String VISIBLE_ROW_COUNT_PROPERTY = "visibleRowCount";
    public static final String INVOKES_STOP_CELL_EDITING_PROPERTY = "invokesStopCellEditing";
    public static final String SCROLLS_ON_EXPAND_PROPERTY = "scrollsOnExpand";
    public static final String TOGGLE_CLICK_COUNT_PROPERTY = "toggleClickCount";
    public static final String LEAD_SELECTION_PATH_PROPERTY = "leadSelectionPath";
    public static final String ANCHOR_SELECTION_PATH_PROPERTY = "anchorSelectionPath";
    public static final String EXPANDS_SELECTED_PATHS_PROPERTY = "expandsSelectedPaths";

    /** The data. */
    protected transient TreeModel treeModel;

    /** What is chosen. */
    protected transient TreeSelectionModel selectionModel;

    /** Whether the root is seen. */
    protected boolean rootVisible;

    /** Who draws each node. */
    protected transient TreeCellRenderer cellRenderer;

    /** A row's height; zero or less means measuring each one. */
    protected int rowHeight;

    /** Whether the root carries the little unfolding triangle. */
    protected boolean showsRootHandles;

    /** The bridge between the selection model and whoever listens to the tree. */
    protected transient TreeSelectionRedirector selectionRedirector;

    /** Who allows a node to be edited. */
    protected transient TreeCellEditor cellEditor;

    /** Whether the nodes can be edited. */
    protected boolean editable;

    /** Whether the tree is large and it is best not to measure each row. */
    protected boolean largeModel;

    /** How many rows are seen without scrolling. */
    protected int visibleRowCount;

    /** Whether on losing the focus the editing is saved instead of cancelled. */
    protected boolean invokesStopCellEditing;

    /** Whether unfolding scrolls so that what appeared is seen. */
    protected boolean scrollsOnExpand;

    /** How many clicks unfold a node. */
    protected int toggleClickCount;

    /** Who listens to the data model. */
    protected transient TreeModelListener treeModelListener;

    private Hashtable<TreePath, Boolean> expandedState = new Hashtable<TreePath, Boolean>();
    private boolean expandsSelectedPaths = true;
    private boolean dragEnabled;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;
    private TreePath anchorPath;
    private TreePath leadPath;
    private AccessibleContext accessibleContext;

    /**
     * A sample tree.
     *
     * <p>It exists so that {@code new JTree()} shows something. An empty tree would look broken
     * and makes one think that something is left to configure when what is left is the model.
     */
    protected static TreeModel getDefaultTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode("colors");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("blue"));
        parent.add(new DefaultMutableTreeNode("violet"));
        parent.add(new DefaultMutableTreeNode("red"));
        parent.add(new DefaultMutableTreeNode("yellow"));
        parent = new DefaultMutableTreeNode("sports");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("basketball"));
        parent.add(new DefaultMutableTreeNode("soccer"));
        parent.add(new DefaultMutableTreeNode("football"));
        parent.add(new DefaultMutableTreeNode("hockey"));
        parent = new DefaultMutableTreeNode("food");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("hot dogs"));
        parent.add(new DefaultMutableTreeNode("pizza"));
        parent.add(new DefaultMutableTreeNode("ravioli"));
        parent.add(new DefaultMutableTreeNode("bananas"));
        return new DefaultTreeModel(root);
    }

    /** It builds a model from an array, a vector or a table. */
    protected static TreeModel createTreeModel(Object value) {
        DefaultMutableTreeNode root;
        if ((value instanceof Object[]) || (value instanceof Hashtable)
                || (value instanceof Vector)) {
            root = new DynamicUtilTreeNode("root", value);
        } else {
            root = new DynamicUtilTreeNode("root", new Object[] {value});
        }
        return new DefaultTreeModel(root, false);
    }

    /** A sample tree; see {@link #getDefaultTreeModel}. */
    public JTree() {
        this(getDefaultTreeModel());
    }

    /** A tree with those elements as children of a hidden root. */
    public JTree(Object[] value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /** A tree with that vector's elements. */
    public JTree(Vector<?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /**
     * A tree from a table.
     *
     * <p>The keys are the nodes and the values their children, which may in turn be tables. The
     * siblings' order is the table's, that is, there is no guaranteed order.
     */
    public JTree(Hashtable<?, ?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /** A tree over that node as the root. */
    public JTree(TreeNode root) {
        this(root, false);
    }

    /** A tree over that root, choosing how it is decided what is a leaf. */
    public JTree(TreeNode root, boolean asksAllowsChildren) {
        this(new DefaultTreeModel(root, asksAllowsChildren));
    }

    /** A tree over that model. */
    public JTree(TreeModel newModel) {
        super();
        expandedState = new Hashtable<TreePath, Boolean>();
        toggleClickCount = 2;
        visibleRowCount = 20;
        rootVisible = true;
        selectionModel = new DefaultTreeSelectionModel();
        cellRenderer = null;
        scrollsOnExpand = true;
        setOpaque(true);
        rowHeight = 16;
        setModel(newModel);
        updateUI();
    }

    public TreeUI getUI() {
        return (TreeUI) ui;
    }

    public void setUI(TreeUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public TreeCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(TreeCellRenderer x) {
        TreeCellRenderer oldValue = cellRenderer;
        cellRenderer = x;
        firePropertyChange(CELL_RENDERER_PROPERTY, oldValue, cellRenderer);
        invalidate();
    }

    /** Whether the nodes can be edited; switching it off cuts off an editing under way. */
    public void setEditable(boolean flag) {
        boolean oldValue = this.editable;
        this.editable = flag;
        firePropertyChange(EDITABLE_PROPERTY, oldValue, flag);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setCellEditor(TreeCellEditor cellEditor) {
        TreeCellEditor oldEditor = this.cellEditor;
        this.cellEditor = cellEditor;
        firePropertyChange(CELL_EDITOR_PROPERTY, oldEditor, cellEditor);
        invalidate();
    }

    public TreeCellEditor getCellEditor() {
        return cellEditor;
    }

    public TreeModel getModel() {
        return treeModel;
    }

    /**
     * It changes the data.
     *
     * <p>It forgets what is unfolded and what is chosen: the old paths point at nodes that are no
     * longer there, and keeping them would leave the tree showing something that does not exist.
     */
    public void setModel(TreeModel newModel) {
        clearSelection();
        TreeModel oldModel = treeModel;
        if (treeModel != null && treeModelListener != null) {
            treeModel.removeTreeModelListener(treeModelListener);
        }
        if (accessibleContext != null) {
            accessibleContext = null;
        }
        treeModel = newModel;
        clearToggledPaths();
        if (treeModel != null) {
            if (treeModelListener == null) {
                treeModelListener = createTreeModelListener();
            }
            if (treeModelListener != null) {
                treeModel.addTreeModelListener(treeModelListener);
            }
            // The root starts unfolded: otherwise, a new tree would be seen as a single row.
            Object root = treeModel.getRoot();
            if (root != null && !treeModel.isLeaf(root)) {
                expandedState.put(new TreePath(root), Boolean.TRUE);
            }
        }
        firePropertyChange(TREE_MODEL_PROPERTY, oldModel, treeModel);
        invalidate();
    }

    public boolean isRootVisible() {
        return rootVisible;
    }

    /**
     * Whether the root is seen.
     *
     * <p>Hiding it is what makes a tree look like several: the root's children are left as loose
     * roots. That is why the constructors that receive a list hide it.
     */
    public void setRootVisible(boolean rootVisible) {
        boolean oldValue = this.rootVisible;
        this.rootVisible = rootVisible;
        firePropertyChange(ROOT_VISIBLE_PROPERTY, oldValue, this.rootVisible);
    }

    public void setShowsRootHandles(boolean newValue) {
        boolean oldValue = showsRootHandles;
        showsRootHandles = newValue;
        firePropertyChange(SHOWS_ROOT_HANDLES_PROPERTY, oldValue, showsRootHandles);
        invalidate();
    }

    public boolean getShowsRootHandles() {
        return showsRootHandles;
    }

    /**
     * The height of every row.
     *
     * <p>Zero or less means measuring each one. Measuring is right and it costs: with a large
     * tree, a fixed height is the difference between scrolling smoothly and in jumps.
     */
    public void setRowHeight(int rowHeight) {
        int oldValue = this.rowHeight;
        this.rowHeight = rowHeight;
        rowHeightSet = true;
        firePropertyChange(ROW_HEIGHT_PROPERTY, oldValue, this.rowHeight);
        invalidate();
    }

    /**
     * Whether the row height was set by the program; then the look and feel does not overwrite it.
     */
    private boolean rowHeightSet;

    /**
     * The look and feel proposes the row height; see {@link JComponent#customSetUIProperty}.
     *
     * <p>It is the only property the tree adds to the list. A look and feel with fixed-height rows
     * proposes its own, and the basic one proposes zero -- "measure each one" --; in both, if the
     * program has already called {@link #setRowHeight}, the proposal is discarded.
     */
    boolean customSetUIProperty(String propertyName, Object value) {
        if (ROW_HEIGHT_PROPERTY.equals(propertyName)) {
            if (!rowHeightSet) {
                setRowHeight(((Number) value).intValue());
                rowHeightSet = false;
            }
            return true;
        }
        return false;
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public boolean isFixedRowHeight() {
        return (rowHeight > 0);
    }

    /**
     * Whether the tree is large; the look and feel may use it in order not to measure everything.
     */
    public void setLargeModel(boolean newValue) {
        boolean oldValue = largeModel;
        largeModel = newValue;
        firePropertyChange(LARGE_MODEL_PROPERTY, oldValue, newValue);
    }

    public boolean isLargeModel() {
        return largeModel;
    }

    /**
     * What to do with an editing under way when something happens.
     *
     * <p>Save or discard. There is no good answer: saving may put in a half-typed value,
     * discarding may lose what was typed.
     */
    public void setInvokesStopCellEditing(boolean newValue) {
        boolean oldValue = invokesStopCellEditing;
        invokesStopCellEditing = newValue;
        firePropertyChange(INVOKES_STOP_CELL_EDITING_PROPERTY, oldValue, newValue);
    }

    public boolean getInvokesStopCellEditing() {
        return invokesStopCellEditing;
    }

    public void setScrollsOnExpand(boolean newValue) {
        boolean oldValue = scrollsOnExpand;
        scrollsOnExpand = newValue;
        firePropertyChange(SCROLLS_ON_EXPAND_PROPERTY, oldValue, newValue);
    }

    public boolean getScrollsOnExpand() {
        return scrollsOnExpand;
    }

    /** How many clicks unfold a node; zero means that the click does not unfold. */
    public void setToggleClickCount(int clickCount) {
        int oldCount = toggleClickCount;
        toggleClickCount = clickCount;
        firePropertyChange(TOGGLE_CLICK_COUNT_PROPERTY, oldCount, clickCount);
    }

    public int getToggleClickCount() {
        return toggleClickCount;
    }

    /**
     * Whether choosing a hidden node unfolds whatever is needed in order to see it.
     *
     * <p>Switching it off allows something that is not seen to be chosen, which is what a program
     * that chooses on its own and does not want to move what the user left folded wants.
     */
    public void setExpandsSelectedPaths(boolean newValue) {
        boolean oldValue = expandsSelectedPaths;
        expandsSelectedPaths = newValue;
        firePropertyChange(EXPANDS_SELECTED_PATHS_PROPERTY, oldValue, newValue);
    }

    public boolean getExpandsSelectedPaths() {
        return expandsSelectedPaths;
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * How where what is dragged will fall is shown.
     *
     * @throws IllegalArgumentException if the mode does not serve for a tree.
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.ON
                    || dropMode == DropMode.INSERT || dropMode == DropMode.ON_OR_INSERT) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for tree");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /** Whether that node can be edited; by default, if the tree can. */
    public boolean isPathEditable(TreePath path) {
        return isEditable();
    }

    public String getToolTipText(MouseEvent event) {
        return super.getToolTipText(event);
    }

    /**
     * A node's text.
     *
     * <p>By default its {@code toString}. Overriding it is the cheap way of changing how the nodes
     * are seen without writing a whole renderer.
     */
    public String convertValueToText(Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        if (value != null) {
            String sValue = value.toString();
            if (sValue != null) {
                return sValue;
            }
        }
        return "";
    }

    /** How many rows are seen; the look and feel answers it. */
    public int getRowCount() {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getRowCount(this);
        }
        return 0;
    }

    public void setSelectionPath(TreePath path) {
        getSelectionModel().setSelectionPath(path);
    }

    public void setSelectionPaths(TreePath[] paths) {
        getSelectionModel().setSelectionPaths(paths);
    }

    /** The path it extends from with Shift. */
    public void setLeadSelectionPath(TreePath newPath) {
        TreePath oldValue = leadPath;
        leadPath = newPath;
        firePropertyChange(LEAD_SELECTION_PATH_PROPERTY, oldValue, newPath);
    }

    /** The path where the selection started. */
    public void setAnchorSelectionPath(TreePath newPath) {
        TreePath oldValue = anchorPath;
        anchorPath = newPath;
        firePropertyChange(ANCHOR_SELECTION_PATH_PROPERTY, oldValue, newPath);
    }

    public void setSelectionRow(int row) {
        int[] rows = {row};
        setSelectionRows(rows);
    }

    public void setSelectionRows(int[] rows) {
        TreeUI ui = getUI();
        if (ui != null && rows != null) {
            int numRows = rows.length;
            TreePath[] paths = new TreePath[numRows];
            for (int counter = 0; counter < numRows; counter++) {
                paths[counter] = ui.getPathForRow(this, rows[counter]);
            }
            setSelectionPaths(paths);
        }
    }

    public void addSelectionPath(TreePath path) {
        getSelectionModel().addSelectionPath(path);
    }

    public void addSelectionPaths(TreePath[] paths) {
        getSelectionModel().addSelectionPaths(paths);
    }

    public void addSelectionRow(int row) {
        int[] rows = {row};
        addSelectionRows(rows);
    }

    public void addSelectionRows(int[] rows) {
        TreeUI ui = getUI();
        if (ui != null && rows != null) {
            int numRows = rows.length;
            TreePath[] paths = new TreePath[numRows];
            for (int counter = 0; counter < numRows; counter++) {
                paths[counter] = ui.getPathForRow(this, rows[counter]);
            }
            addSelectionPaths(paths);
        }
    }

    /** The node of the first chosen path, not the path. */
    public Object getLastSelectedPathComponent() {
        TreePath selPath = getSelectionModel().getSelectionPath();
        if (selPath != null) {
            return selPath.getLastPathComponent();
        }
        return null;
    }

    public TreePath getLeadSelectionPath() {
        return leadPath;
    }

    public TreePath getAnchorSelectionPath() {
        return anchorPath;
    }

    public TreePath getSelectionPath() {
        return getSelectionModel().getSelectionPath();
    }

    public TreePath[] getSelectionPaths() {
        TreePath[] selectionPaths = getSelectionModel().getSelectionPaths();
        return (selectionPaths != null && selectionPaths.length > 0) ? selectionPaths : null;
    }

    public int[] getSelectionRows() {
        return getSelectionModel().getSelectionRows();
    }

    public int getSelectionCount() {
        return selectionModel.getSelectionCount();
    }

    public int getMinSelectionRow() {
        return getSelectionModel().getMinSelectionRow();
    }

    public int getMaxSelectionRow() {
        return getSelectionModel().getMaxSelectionRow();
    }

    public int getLeadSelectionRow() {
        TreePath leadPath = getLeadSelectionPath();
        if (leadPath != null) {
            return getRowForPath(leadPath);
        }
        return -1;
    }

    public boolean isPathSelected(TreePath path) {
        return getSelectionModel().isPathSelected(path);
    }

    public boolean isRowSelected(int row) {
        return getSelectionModel().isRowSelected(row);
    }

    /**
     * That path's unfolded descendants.
     *
     * <p>It serves in order to keep and restore the tree's state: on rebuilding it, unfolding
     * these paths again leaves it as it was.
     */
    public Enumeration<TreePath> getExpandedDescendants(TreePath parent) {
        if (!isExpanded(parent)) {
            return null;
        }
        Vector<TreePath> out = new Vector<TreePath>();
        Enumeration<TreePath> e = expandedState.keys();
        while (e.hasMoreElements()) {
            TreePath p = e.nextElement();
            Boolean v = expandedState.get(p);
            if (v != null && v.booleanValue() && parent.isDescendant(p) && !p.equals(parent)) {
                out.addElement(p);
            }
        }
        return out.elements();
    }

    /**
     * Whether that path was ever unfolded.
     *
     * <p>Different from being unfolded now: it serves in order to know whether its children have
     * already been loaded.
     */
    public boolean hasBeenExpanded(TreePath path) {
        return (path != null && expandedState.get(path) != null);
    }

    public boolean isExpanded(TreePath path) {
        if (path == null) {
            return false;
        }
        Object value = expandedState.get(path);
        if (value == null || !((Boolean) value).booleanValue()) {
            return false;
        }
        // An unfolded node with a folded parent is not seen, and then it does not count.
        TreePath parentPath = path.getParentPath();
        if (parentPath != null) {
            return isExpanded(parentPath);
        }
        return true;
    }

    public boolean isExpanded(int row) {
        TreeUI tree = getUI();
        if (tree != null) {
            TreePath path = tree.getPathForRow(this, row);
            if (path != null) {
                Boolean value = expandedState.get(path);
                return (value != null && value.booleanValue());
            }
        }
        return false;
    }

    public boolean isCollapsed(TreePath path) {
        return !isExpanded(path);
    }

    public boolean isCollapsed(int row) {
        return !isExpanded(row);
    }

    /** It unfolds whatever is needed for that path to be seen. */
    public void makeVisible(TreePath path) {
        if (path != null) {
            TreePath parentPath = path.getParentPath();
            if (parentPath != null) {
                expandPath(parentPath);
            }
        }
    }

    /** Whether that path is seen, that is, whether all its parents are unfolded. */
    public boolean isVisible(TreePath path) {
        if (path != null) {
            TreePath parentPath = path.getParentPath();
            if (parentPath != null) {
                return isExpanded(parentPath);
            }
            // The root is seen if it is set as visible.
            return true;
        }
        return false;
    }

    /** That path's rectangle; the look and feel answers it. */
    public Rectangle getPathBounds(TreePath path) {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getPathBounds(this, path);
        }
        return null;
    }

    public Rectangle getRowBounds(int row) {
        return getPathBounds(getPathForRow(row));
    }

    /** It unfolds and scrolls so that that path is seen. */
    public void scrollPathToVisible(TreePath path) {
        if (path != null) {
            makeVisible(path);
            Rectangle bounds = getPathBounds(path);
            if (bounds != null) {
                scrollRectToVisible(bounds);
            }
        }
    }

    public void scrollRowToVisible(int row) {
        scrollPathToVisible(getPathForRow(row));
    }

    public TreePath getPathForRow(int row) {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getPathForRow(this, row);
        }
        return null;
    }

    public int getRowForPath(TreePath path) {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getRowForPath(this, path);
        }
        return -1;
    }

    /** It unfolds that path and all its parents. */
    public void expandPath(TreePath path) {
        TreeModel model = getModel();
        if (path != null && model != null && !model.isLeaf(path.getLastPathComponent())) {
            setExpandedState(path, true);
        }
    }

    public void expandRow(int row) {
        expandPath(getPathForRow(row));
    }

    public void collapsePath(TreePath path) {
        setExpandedState(path, false);
    }

    public void collapseRow(int row) {
        collapsePath(getPathForRow(row));
    }

    /** The path there is at that point, or null if there is none. */
    public TreePath getPathForLocation(int x, int y) {
        TreePath closestPath = getClosestPathForLocation(x, y);
        if (closestPath != null) {
            Rectangle pathBounds = getPathBounds(closestPath);
            if (pathBounds != null && x >= pathBounds.x
                    && x < (pathBounds.x + pathBounds.width)
                    && y >= pathBounds.y && y < (pathBounds.y + pathBounds.height)) {
                return closestPath;
            }
        }
        return null;
    }

    public int getRowForLocation(int x, int y) {
        return getRowForPath(getPathForLocation(x, y));
    }

    /** The path nearest to that point, even though the point does not fall on any. */
    public TreePath getClosestPathForLocation(int x, int y) {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getClosestPathForLocation(this, x, y);
        }
        return null;
    }

    public int getClosestRowForLocation(int x, int y) {
        return getRowForPath(getClosestPathForLocation(x, y));
    }

    public boolean isEditing() {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.isEditing(this);
        }
        return false;
    }

    /** It ends the editing saving; it returns whether there was one. */
    public boolean stopEditing() {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.stopEditing(this);
        }
        return false;
    }

    public void cancelEditing() {
        TreeUI tree = getUI();
        if (tree != null) {
            tree.cancelEditing(this);
        }
    }

    public void startEditingAtPath(TreePath path) {
        TreeUI tree = getUI();
        if (tree != null) {
            tree.startEditingAtPath(this, path);
        }
    }

    public TreePath getEditingPath() {
        TreeUI tree = getUI();
        if (tree != null) {
            return tree.getEditingPath(this);
        }
        return null;
    }

    /**
     * It changes the selection model.
     *
     * <p>Null sets one that does not allow anything to be chosen, it does not leave the tree with
     * no model: that way the rest of the code never has to ask whether there is one.
     */
    public void setSelectionModel(TreeSelectionModel selectionModel) {
        TreeSelectionModel newValue = (selectionModel == null)
                ? EmptySelectionModel.sharedInstance() : selectionModel;
        TreeSelectionModel oldValue = this.selectionModel;
        if (this.selectionModel != null && selectionRedirector != null) {
            this.selectionModel.removeTreeSelectionListener(selectionRedirector);
        }
        this.selectionModel = newValue;
        if (selectionRedirector == null) {
            selectionRedirector = new TreeSelectionRedirector(this);
        }
        this.selectionModel.addTreeSelectionListener(selectionRedirector);
        firePropertyChange(SELECTION_MODEL_PROPERTY, oldValue, this.selectionModel);
    }

    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** The paths of the rows between those two, in order. */
    protected TreePath[] getPathBetweenRows(int index0, int index1) {
        TreeUI tree = getUI();
        if (tree != null) {
            int minIndex = Math.min(index0, index1);
            int maxIndex = Math.max(index0, index1);
            TreePath[] selection = new TreePath[maxIndex - minIndex + 1];
            for (int counter = minIndex; counter <= maxIndex; counter++) {
                selection[counter - minIndex] = tree.getPathForRow(this, counter);
            }
            return selection;
        }
        return new TreePath[0];
    }

    public void setSelectionInterval(int index0, int index1) {
        TreePath[] paths = getPathBetweenRows(index0, index1);
        this.getSelectionModel().setSelectionPaths(paths);
    }

    public void addSelectionInterval(int index0, int index1) {
        TreePath[] paths = getPathBetweenRows(index0, index1);
        if (paths != null && paths.length > 0) {
            this.getSelectionModel().addSelectionPaths(paths);
        }
    }

    public void removeSelectionInterval(int index0, int index1) {
        TreePath[] paths = getPathBetweenRows(index0, index1);
        if (paths != null && paths.length > 0) {
            this.getSelectionModel().removeSelectionPaths(paths);
        }
    }

    public void removeSelectionPath(TreePath path) {
        this.getSelectionModel().removeSelectionPath(path);
    }

    public void removeSelectionPaths(TreePath[] paths) {
        this.getSelectionModel().removeSelectionPaths(paths);
    }

    public void removeSelectionRow(int row) {
        int[] rows = {row};
        removeSelectionRows(rows);
    }

    public void removeSelectionRows(int[] rows) {
        TreeUI ui = getUI();
        if (ui != null && rows != null) {
            TreePath[] paths = new TreePath[rows.length];
            for (int counter = rows.length - 1; counter >= 0; counter--) {
                paths[counter] = ui.getPathForRow(this, rows[counter]);
            }
            removeSelectionPaths(paths);
        }
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    public boolean isSelectionEmpty() {
        return getSelectionModel().isSelectionEmpty();
    }

    public void addTreeExpansionListener(TreeExpansionListener tel) {
        listenerList.add(TreeExpansionListener.class, tel);
    }

    public void removeTreeExpansionListener(TreeExpansionListener tel) {
        listenerList.remove(TreeExpansionListener.class, tel);
    }

    public TreeExpansionListener[] getTreeExpansionListeners() {
        return listenerList.getListeners(TreeExpansionListener.class);
    }

    public void addTreeWillExpandListener(TreeWillExpandListener tel) {
        listenerList.add(TreeWillExpandListener.class, tel);
    }

    public void removeTreeWillExpandListener(TreeWillExpandListener tel) {
        listenerList.remove(TreeWillExpandListener.class, tel);
    }

    public TreeWillExpandListener[] getTreeWillExpandListeners() {
        return listenerList.getListeners(TreeWillExpandListener.class);
    }

    public void fireTreeExpanded(TreePath path) {
        Object[] listeners = listenerList.getListenerList();
        TreeExpansionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeExpansionListener.class) {
                if (e == null) {
                    e = new TreeExpansionEvent(this, path);
                }
                ((TreeExpansionListener) listeners[i + 1]).treeExpanded(e);
            }
        }
    }

    public void fireTreeCollapsed(TreePath path) {
        Object[] listeners = listenerList.getListenerList();
        TreeExpansionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeExpansionListener.class) {
                if (e == null) {
                    e = new TreeExpansionEvent(this, path);
                }
                ((TreeExpansionListener) listeners[i + 1]).treeCollapsed(e);
            }
        }
    }

    /**
     * It gives notice before unfolding.
     *
     * @throws ExpandVetoException if somebody refuses; see the class note.
     */
    public void fireTreeWillExpand(TreePath path) throws ExpandVetoException {
        Object[] listeners = listenerList.getListenerList();
        TreeExpansionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeWillExpandListener.class) {
                if (e == null) {
                    e = new TreeExpansionEvent(this, path);
                }
                ((TreeWillExpandListener) listeners[i + 1]).treeWillExpand(e);
            }
        }
    }

    /**
     * It gives notice before folding.
     *
     * @throws ExpandVetoException if somebody refuses.
     */
    public void fireTreeWillCollapse(TreePath path) throws ExpandVetoException {
        Object[] listeners = listenerList.getListenerList();
        TreeExpansionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TreeWillExpandListener.class) {
                if (e == null) {
                    e = new TreeExpansionEvent(this, path);
                }
                ((TreeWillExpandListener) listeners[i + 1]).treeWillCollapse(e);
            }
        }
    }

    public void addTreeSelectionListener(TreeSelectionListener tsl) {
        listenerList.add(TreeSelectionListener.class, tsl);
    }

    public void removeTreeSelectionListener(TreeSelectionListener tsl) {
        listenerList.remove(TreeSelectionListener.class, tsl);
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

    /** It gives notice that something changed and the drawing has to be redone. */
    public void treeDidChange() {
        revalidate();
        repaint();
    }

    /** How many rows are seen; it is what the tree asks the scroller for. */
    public void setVisibleRowCount(int newCount) {
        int oldCount = visibleRowCount;
        visibleRowCount = Math.max(0, newCount);
        firePropertyChange(VISIBLE_ROW_COUNT_PROPERTY, oldCount, visibleRowCount);
        invalidate();
    }

    public int getVisibleRowCount() {
        return visibleRowCount;
    }

    /**
     * The next node whose text begins with that.
     *
     * <p>The same as in {@link JList}: it is what makes typing jump to the node. It uses the text
     * that is shown, not the object.
     *
     * @throws IllegalArgumentException if the prefix is null or the row does not exist.
     */
    public TreePath getNextMatch(String prefix, int startingRow, Position.Bias bias) {
        int max = getRowCount();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startingRow < 0 || startingRow >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase(java.util.Locale.ROOT);
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int row = startingRow;
        do {
            TreePath path = getPathForRow(row);
            String text = convertValueToText(path.getLastPathComponent(), isRowSelected(row),
                    isExpanded(row), true, row, false);
            if (text.toUpperCase(java.util.Locale.ROOT).startsWith(prefix)) {
                return path;
            }
            row = (row + increment + max) % max;
        } while (row != startingRow);
        return null;
    }

    /** The height comes from {@link #getVisibleRowCount} rows, not from all of them. */
    public Dimension getPreferredScrollableViewportSize() {
        int width = getPreferredSize().width;
        int visRows = getVisibleRowCount();
        int height = -1;
        if (isFixedRowHeight()) {
            height = visRows * getRowHeight();
        } else {
            TreeUI ui = getUI();
            if (ui != null && visRows > 0) {
                int rc = ui.getRowCount(this);
                if (rc >= visRows) {
                    Rectangle bounds = getRowBounds(visRows - 1);
                    if (bounds != null) {
                        height = bounds.y + bounds.height;
                    }
                } else if (rc > 0) {
                    Rectangle bounds = getRowBounds(0);
                    if (bounds != null) {
                        height = bounds.height * visRows;
                    }
                }
            }
            if (height == -1) {
                height = 16 * visRows;
            }
        }
        return new Dimension(width, height);
    }

    /** How much the wheel advances by: one row. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            Rectangle rowBounds;
            int firstIndex = getClosestRowForLocation(0, visibleRect.y);
            if (firstIndex != -1) {
                rowBounds = getRowBounds(firstIndex);
                if (rowBounds == null) {
                    return 0;
                }
                if (direction < 0) {
                    int delta = visibleRect.y - rowBounds.y;
                    return (delta > 0) ? delta : rowBounds.height;
                }
                return rowBounds.y + rowBounds.height - visibleRect.y;
            }
            return 0;
        }
        return 4;
    }

    /** How much it advances by on clicking on the bar: a screenful. */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height
                : visibleRect.width;
    }

    public boolean getScrollableTracksViewportWidth() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getWidth() > getPreferredSize().width;
        }
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            return parent.getHeight() > getPreferredSize().height;
        }
        return false;
    }

    /**
     * It unfolds or folds a path, giving notice before and afterwards.
     *
     * <p>Unfolding also unfolds every parent: there is no point in leaving something unfolded
     * that is not seen. Folding does not touch the children, which are left as they were for when
     * it is unfolded again.
     */
    protected void setExpandedState(TreePath path, boolean state) {
        if (path == null) {
            return;
        }
        if (state) {
            // Top down: a folded parent would hide what has just been unfolded.
            Vector<TreePath> chain = new Vector<TreePath>();
            for (TreePath p = path; p != null; p = p.getParentPath()) {
                chain.insertElementAt(p, 0);
            }
            for (int i = 0; i < chain.size(); i++) {
                TreePath p = chain.elementAt(i);
                Boolean v = expandedState.get(p);
                if (v == null || !v.booleanValue()) {
                    try {
                        fireTreeWillExpand(p);
                    } catch (ExpandVetoException eve) {
                        return;
                    }
                    expandedState.put(p, Boolean.TRUE);
                    fireTreeExpanded(p);
                }
            }
        } else {
            Boolean v = expandedState.get(path);
            if (v != null && v.booleanValue()) {
                try {
                    fireTreeWillCollapse(path);
                } catch (ExpandVetoException eve) {
                    return;
                }
                expandedState.put(path, Boolean.FALSE);
                fireTreeCollapsed(path);
            }
        }
    }

    /** That path's descendants that were unfolded at some point. */
    protected Enumeration<TreePath> getDescendantToggledPaths(TreePath parent) {
        if (parent == null) {
            return null;
        }
        Vector<TreePath> descendants = new Vector<TreePath>();
        Enumeration<TreePath> nodes = expandedState.keys();
        while (nodes.hasMoreElements()) {
            TreePath path = nodes.nextElement();
            if (parent.isDescendant(path)) {
                descendants.addElement(path);
            }
        }
        return descendants.elements();
    }

    /** It forgets what was unfolded of those paths. */
    protected void removeDescendantToggledPaths(Enumeration<TreePath> toRemove) {
        if (toRemove != null) {
            while (toRemove.hasMoreElements()) {
                Enumeration<TreePath> descendants = getDescendantToggledPaths(
                        toRemove.nextElement());
                if (descendants != null) {
                    while (descendants.hasMoreElements()) {
                        expandedState.remove(descendants.nextElement());
                    }
                }
            }
        }
    }

    protected void clearToggledPaths() {
        expandedState.clear();
    }

    protected TreeModelListener createTreeModelListener() {
        return new TreeModelHandler(this);
    }

    /**
     * It removes from the selection whatever hangs from that path.
     *
     * <p>It is called when a subtree disappears: leaving a node that is no longer there chosen
     * would make the tree report an impossible selection.
     */
    protected boolean removeDescendantSelectedPaths(TreePath path, boolean includePath) {
        TreePath[] toRemove = getDescendantSelectedPaths(path, includePath);
        if (toRemove != null) {
            getSelectionModel().removeSelectionPaths(toRemove);
            return true;
        }
        return false;
    }

    private TreePath[] getDescendantSelectedPaths(TreePath path, boolean includePath) {
        TreeSelectionModel sm = getSelectionModel();
        TreePath[] selPaths = (sm != null) ? sm.getSelectionPaths() : null;
        if (selPaths != null) {
            Vector<TreePath> v = new Vector<TreePath>();
            for (int counter = selPaths.length - 1; counter >= 0; counter--) {
                if (selPaths[counter] != null && path.isDescendant(selPaths[counter])
                        && (includePath || !path.equals(selPaths[counter]))) {
                    v.addElement(selPaths[counter]);
                }
            }
            if (v.size() > 0) {
                TreePath[] out = new TreePath[v.size()];
                v.copyInto(out);
                return out;
            }
        }
        return null;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * A selection model that does not allow anything to be chosen.
     *
     * <p>It is what is set when somebody asks for a tree with no selection; see
     * {@link JTree#setSelectionModel}. All its methods that would change something do nothing, so
     * the rest of the code does not need to ask whether there is a model.
     */
    public static class EmptySelectionModel extends DefaultTreeSelectionModel {

        /** The single one; it keeps no state, so one is enough for every tree. */
        protected static final EmptySelectionModel sharedInstance = new EmptySelectionModel();

        protected EmptySelectionModel() {
        }

        public static EmptySelectionModel sharedInstance() {
            return sharedInstance;
        }

        public void setSelectionPaths(TreePath[] paths) {
        }

        public void addSelectionPaths(TreePath[] paths) {
        }

        public void removeSelectionPaths(TreePath[] paths) {
        }

        public void setSelectionMode(int mode) {
        }

        public void setRowMapper(javax.swing.tree.RowMapper newMapper) {
        }

        public void addTreeSelectionListener(TreeSelectionListener x) {
        }

        public void removeTreeSelectionListener(TreeSelectionListener x) {
        }

        public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        }

        public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        }
    }

    /**
     * It forwards the selection model's notices as though they came from the tree.
     *
     * <p>It changes the event's source. Whoever listens to the tree expects the event to say that
     * it comes from the tree, not from a model they may know nothing about.
     */
    public static class TreeSelectionRedirector implements Serializable, TreeSelectionListener {

        private final JTree tree;

        protected TreeSelectionRedirector(JTree tree) {
            this.tree = tree;
        }

        public void valueChanged(TreeSelectionEvent e) {
            TreeSelectionEvent newE = (TreeSelectionEvent) e.cloneWithSource(tree);
            tree.fireValueChanged(newE);
        }
    }

    /**
     * It listens to the data model and settles what is unfolded and what is chosen.
     *
     * <p>It is where what was left hanging is cleaned up: a subtree that disappears takes its
     * unfolded state and its chosen nodes with it.
     */
    public static class TreeModelHandler implements TreeModelListener {

        private final JTree tree;

        protected TreeModelHandler(JTree tree) {
            this.tree = tree;
        }

        public void treeNodesChanged(TreeModelEvent e) {
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeStructureChanged(TreeModelEvent e) {
            if (e == null) {
                return;
            }
            TreePath parent = e.getTreePath();
            if (parent == null) {
                return;
            }
            if (parent.getPathCount() == 1) {
                // The root changed: it starts from scratch, with the root unfolded.
                tree.clearToggledPaths();
                Object treeRoot = tree.getModel().getRoot();
                if (treeRoot != null && !tree.getModel().isLeaf(treeRoot)) {
                    tree.expandedState.put(new TreePath(treeRoot), Boolean.TRUE);
                }
            } else if (tree.expandedState.get(parent) != null) {
                Vector<TreePath> toRemove = new Vector<TreePath>(1);
                boolean isExpanded = tree.isExpanded(parent);
                toRemove.addElement(parent);
                tree.removeDescendantToggledPaths(toRemove.elements());
                if (isExpanded) {
                    tree.expandedState.put(parent, Boolean.TRUE);
                }
            }
            tree.removeDescendantSelectedPaths(parent, false);
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            if (e == null) {
                return;
            }
            TreePath parent = e.getTreePath();
            Object[] children = e.getChildren();
            if (children == null) {
                return;
            }
            Vector<TreePath> toRemove = new Vector<TreePath>(children.length);
            for (int counter = children.length - 1; counter >= 0; counter--) {
                toRemove.addElement(parent.pathByAddingChild(children[counter]));
            }
            tree.removeDescendantToggledPaths(toRemove.elements());
            for (int counter = children.length - 1; counter >= 0; counter--) {
                tree.removeDescendantSelectedPaths(
                        parent.pathByAddingChild(children[counter]), true);
            }
        }
    }

    /**
     * A node that builds its children only when it is unfolded.
     *
     * <p>It is what the constructors that receive an array, a vector or a table use: the tree
     * builds itself only as far as it is looked at.
     */
    public static class DynamicUtilTreeNode extends DefaultMutableTreeNode {

        /** Whether the original value had children. */
        protected boolean hasChildren;

        /** The value the children come from. */
        protected Object childValue;

        /** Whether they have already been built. */
        protected boolean loadedChildren;

        /** It builds that node's children from that value. */
        public static void createChildren(DefaultMutableTreeNode parent, Object children) {
            if (children instanceof Vector) {
                Vector<?> childVector = (Vector<?>) children;
                int maxCounter = childVector.size();
                for (int counter = 0; counter < maxCounter; counter++) {
                    parent.add(new DynamicUtilTreeNode(childVector.elementAt(counter),
                            childVector.elementAt(counter)));
                }
            } else if (children instanceof Hashtable) {
                Hashtable<?, ?> childHT = (Hashtable<?, ?>) children;
                Enumeration<?> keys = childHT.keys();
                while (keys.hasMoreElements()) {
                    Object o = keys.nextElement();
                    parent.add(new DynamicUtilTreeNode(o, childHT.get(o)));
                }
            } else if (children instanceof Object[]) {
                Object[] childArray = (Object[]) children;
                int maxCounter = childArray.length;
                for (int counter = 0; counter < maxCounter; counter++) {
                    parent.add(new DynamicUtilTreeNode(childArray[counter],
                            childArray[counter]));
                }
            }
        }

        /** A node with that value, whose children come from that other one. */
        public DynamicUtilTreeNode(Object value, Object children) {
            super(value);
            loadedChildren = false;
            childValue = children;
            if (children != null) {
                if (children instanceof Vector) {
                    setAllowsChildren(true);
                    hasChildren = (((Vector<?>) children).size() > 0);
                } else if (children instanceof Hashtable) {
                    setAllowsChildren(true);
                    hasChildren = (((Hashtable<?, ?>) children).size() > 0);
                } else if (children instanceof Object[]) {
                    setAllowsChildren(true);
                    hasChildren = (((Object[]) children).length > 0);
                } else {
                    setAllowsChildren(false);
                }
            } else {
                setAllowsChildren(false);
            }
        }

        public boolean isLeaf() {
            return !getAllowsChildren();
        }

        public int getChildCount() {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.getChildCount();
        }

        /** It builds the children; it is called by itself the first time they are asked for. */
        protected void loadChildren() {
            loadedChildren = true;
            createChildren(this, childValue);
        }

        public TreeNode getChildAt(int index) {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.getChildAt(index);
        }

        public Enumeration<TreeNode> children() {
            if (!loadedChildren) {
                loadChildren();
            }
            return super.children();
        }
    }

    /**
     * Where what is being dragged would fall.
     *
     * <p>{@link #getChildIndex} tells dropping <em>over</em> a node from dropping
     * <em>between</em> two: -1 means over the path's node, and another number means inserting at
     * that position among its children.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final TreePath path;
        private final int index;

        DropLocation(java.awt.Point p, TreePath path, int index) {
            super(p);
            this.path = path;
            this.index = index;
        }

        /** At which position among the children, or -1 if it is over the node. */
        public int getChildIndex() {
            return index;
        }

        public TreePath getPath() {
            return path;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ","
                    + "path=" + path + ","
                    + "childIndex=" + index + "]";
        }
    }
}
