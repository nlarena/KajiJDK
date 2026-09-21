package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.CellRendererPane;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.UIResource;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.VariableHeightLayoutCache;

/**
 * The basic look and feel of a tree.
 *
 * <h2>The look and feel does not know where each row is</h2>
 *
 * <p>{@link #treeState} knows, which is an {@link AbstractLayoutCache}: a table of which row is
 * at which coordinate. Everything this look and feel answers about positions
 * -- {@link #getPathBounds}, {@link #getRowForPath}, {@link #getClosestPathForLocation} -- it
 * asks that table.
 *
 * <p>There are two tables and the choice matters: with rows of a fixed height
 * {@link FixedHeightLayoutCache} is used, which resolves "which row is at coordinate y" with a
 * division; with different heights, {@link VariableHeightLayoutCache}, which has to walk along.
 * A tree of a million nodes with the second one is unusable, and that is why {@code largeModel}
 * exists.
 *
 * <h2>The indent is two numbers, not one</h2>
 *
 * <p>{@link #leftChildIndent} is what there is between the parent's edge and the handle's
 * centre, and {@link #rightChildIndent} between the handle's centre and the child's text. The
 * sum -- {@link #totalChildIndent} -- is what each level shifts. They are separate because the
 * handle is drawn <em>in the middle</em>, and the look and feel that wants it bigger changes
 * only one.
 *
 * <p>{@link #depthOffset} is the adjustment for whether the root is seen or not and for whether
 * it has a handle: a tree with no visible root has its children at level zero, not at one.
 *
 * <h2>The row height of zero</h2>
 *
 * <p>{@link #getRowHeight} returns zero, and that does not mean the rows measure zero: it means
 * "each one whatever it needs", which is what makes the table of positions the
 * variable-height one. A positive number there is what turns it fixed. Measured.
 *
 * <h2>What is left said</h2>
 *
 * <p>The two handle icons -- expanded and collapsed -- come from the look and feel's table,
 * which this library does not have: they are left {@code null} and the tree is drawn with no
 * handles. The preferred width is therefore smaller than the JDK's.
 *
 * <p>Editing a cell needs an installed editor and focus; the methods are there and
 * {@link #isEditing} answers no.
 */
public class BasicTreeUI extends TreeUI {

    protected transient Icon collapsedIcon;
    protected transient Icon expandedIcon;

    /** Between the parent's edge and the handle's centre; see the class note. */
    protected int leftChildIndent;

    /** Between the handle's centre and the child's text. */
    protected int rightChildIndent;

    /** The sum of the two. */
    protected int totalChildIndent;

    protected Dimension preferredMinSize;
    protected int lastSelectedRow;
    protected JTree tree;
    protected transient TreeCellRenderer currentCellRenderer;
    protected boolean createdRenderer;
    protected transient TreeCellEditor cellEditor;
    protected boolean createdCellEditor;

    /** Whether on finishing editing the editor has to be stopped or cancelled. */
    protected boolean stopEditingInCompleteEditing;

    protected CellRendererPane rendererPane;
    protected Dimension preferredSize;
    protected boolean validCachedPreferredSize;

    /** The table of positions; see the class note. */
    protected AbstractLayoutCache treeState;

    /** The paths already drawn, so as not to redo the arithmetic on every repaint. */
    protected Hashtable<TreePath, Boolean> drawingCache;

    protected boolean largeModel;
    protected AbstractLayoutCache.NodeDimensions nodeDimensions;
    protected TreeModel treeModel;
    protected TreeSelectionModel treeSelectionModel;

    /** The adjustment for visible root and handles; see the class note. */
    protected int depthOffset;

    protected Component editingComponent;
    protected TreePath editingPath;
    protected int editingRow;
    protected boolean editorHasDifferentSize;

    private Color hashColor;
    private PropertyChangeListener propertyChangeListener;
    private PropertyChangeListener selectionModelPropertyChangeListener;
    private MouseListener mouseListener;
    private FocusListener focusListener;
    private KeyListener keyListener;
    private TreeSelectionListener treeSelectionListener;
    private TreeModelListener treeModelListener;
    private TreeExpansionListener treeExpansionListener;
    private ComponentListener componentListener;
    private CellEditorListener cellEditorListener;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource LINE = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicTreeUI() {
        super();
    }

    /** A new one per tree: it keeps its table of positions. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicTreeUI();
    }

    public void installUI(JComponent c) {
        if (c == null) {
            throw new NullPointerException("null component passed to BasicTreeUI.installUI()");
        }
        tree = (JTree) c;
        prepareForUIInstall();
        installDefaults();
        installListeners();
        installKeyboardActions();
        installComponents();
        completeUIInstall();
    }

    public void uninstallUI(JComponent c) {
        completeEditing();
        prepareForUIUninstall();
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();
        uninstallComponents();
        completeUIUninstall();
    }

    /** It builds the state before installing anything. */
    protected void prepareForUIInstall() {
        drawingCache = new Hashtable<TreePath, Boolean>(7);
        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);
        lastSelectedRow = -1;
        preferredSize = new Dimension();
        stopEditingInCompleteEditing = true;
        setModel(tree.getModel());
    }

    /** And finishes building it afterwards. */
    protected void completeUIInstall() {
        setShowsRootHandles(tree.getShowsRootHandles());
        updateRenderer();
        updateDepthOffset();
        setSelectionModel(tree.getSelectionModel());
        treeState = createLayoutCache();
        configureLayoutCache();
        updateSize();
    }

    protected void prepareForUIUninstall() {
    }

    protected void completeUIUninstall() {
        if (createdRenderer) {
            tree.setCellRenderer(null);
        }
        if (createdCellEditor) {
            tree.setCellEditor(null);
        }
        cellEditor = null;
        currentCellRenderer = null;
        rendererPane = null;
        drawingCache = null;
        treeState = null;
        treeModel = null;
        treeSelectionModel = null;
        tree = null;
    }

    /** Colours, typeface and indents; the values are those of {@code Tree.*} in Metal. */
    protected void installDefaults() {
        if (tree.getBackground() == null || tree.getBackground() instanceof UIResource) {
            tree.setBackground(BACKGROUND);
        }
        if (getHashColor() == null || getHashColor() instanceof UIResource) {
            setHashColor(LINE);
        }
        Font font = tree.getFont();
        if (font == null || font instanceof UIResource) {
            tree.setFont(FONT);
        }
        setExpandedIcon(null);
        setCollapsedIcon(null);
        setLeftChildIndent(7);
        setRightChildIndent(13);
        LookAndFeel.installProperty(tree, "rowHeight", Integer.valueOf(0));
        LookAndFeel.installProperty(tree, "opaque", Boolean.TRUE);
        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** It sets the pane the cells are drawn in. */
    protected void installComponents() {
        if (rendererPane == null) {
            rendererPane = createCellRendererPane();
        }
        tree.add(rendererPane);
    }

    protected void uninstallComponents() {
        if (rendererPane != null) {
            tree.remove(rendererPane);
        }
    }

    protected CellRendererPane createCellRendererPane() {
        return new CellRendererPane();
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        tree.addPropertyChangeListener(propertyChangeListener);
        mouseListener = createMouseListener();
        tree.addMouseListener(mouseListener);
        focusListener = createFocusListener();
        tree.addFocusListener(focusListener);
        keyListener = createKeyListener();
        if (keyListener != null) {
            tree.addKeyListener(keyListener);
        }
        componentListener = createComponentListener();
        tree.addComponentListener(componentListener);
        treeExpansionListener = createTreeExpansionListener();
        tree.addTreeExpansionListener(treeExpansionListener);
        cellEditorListener = createCellEditorListener();
        selectionModelPropertyChangeListener = createSelectionModelPropertyChangeListener();
        treeSelectionListener = createTreeSelectionListener();
        treeModelListener = createTreeModelListener();
        if (treeModel != null && treeModelListener != null) {
            treeModel.addTreeModelListener(treeModelListener);
        }
        if (treeSelectionModel != null && treeSelectionListener != null) {
            treeSelectionModel.addTreeSelectionListener(treeSelectionListener);
        }
    }

    protected void uninstallListeners() {
        tree.removePropertyChangeListener(propertyChangeListener);
        tree.removeMouseListener(mouseListener);
        tree.removeFocusListener(focusListener);
        if (keyListener != null) {
            tree.removeKeyListener(keyListener);
        }
        tree.removeComponentListener(componentListener);
        tree.removeTreeExpansionListener(treeExpansionListener);
        if (treeModel != null && treeModelListener != null) {
            treeModel.removeTreeModelListener(treeModelListener);
        }
        if (treeSelectionModel != null && treeSelectionListener != null) {
            treeSelectionModel.removeTreeSelectionListener(treeSelectionListener);
        }
        propertyChangeListener = null;
        mouseListener = null;
        focusListener = null;
        keyListener = null;
        componentListener = null;
        treeExpansionListener = null;
        cellEditorListener = null;
        selectionModelPropertyChangeListener = null;
        treeSelectionListener = null;
        treeModelListener = null;
    }

    /** With no shortcuts of its own: the arrows are tied by the look and feel's table. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected PropertyChangeListener createSelectionModelPropertyChangeListener() {
        return new Handler(this);
    }

    protected MouseListener createMouseListener() {
        return new Handler(this);
    }

    protected FocusListener createFocusListener() {
        return new Handler(this);
    }

    /** None: keyboard navigation belongs to the action table. */
    protected KeyListener createKeyListener() {
        return null;
    }

    protected ComponentListener createComponentListener() {
        return new Handler(this);
    }

    protected TreeSelectionListener createTreeSelectionListener() {
        return new Handler(this);
    }

    protected TreeModelListener createTreeModelListener() {
        return new Handler(this);
    }

    protected TreeExpansionListener createTreeExpansionListener() {
        return new Handler(this);
    }

    protected CellEditorListener createCellEditorListener() {
        return new Handler(this);
    }

    protected TreeCellRenderer createDefaultCellRenderer() {
        return new DefaultTreeCellRenderer();
    }

    /** The variable-height one, unless the tree asks for the large model. */
    protected AbstractLayoutCache createLayoutCache() {
        if (isLargeModel()) {
            return new FixedHeightLayoutCache();
        }
        return new VariableHeightLayoutCache();
    }

    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler(this);
    }

    /** It passes the model, the renderer and the row height on to the table of positions. */
    protected void configureLayoutCache() {
        if (treeState == null) {
            return;
        }
        if (nodeDimensions == null) {
            nodeDimensions = createNodeDimensions();
        }
        treeState.setNodeDimensions(nodeDimensions);
        treeState.setRootVisible(tree.isRootVisible());
        treeState.setRowHeight(tree.getRowHeight());
        treeState.setModel(treeModel);
        treeState.setSelectionModel(treeSelectionModel);
    }

    protected Color getHashColor() {
        return hashColor;
    }

    protected void setHashColor(Color color) {
        hashColor = color;
    }

    public Icon getCollapsedIcon() {
        return collapsedIcon;
    }

    public void setCollapsedIcon(Icon newG) {
        collapsedIcon = newG;
    }

    public Icon getExpandedIcon() {
        return expandedIcon;
    }

    public void setExpandedIcon(Icon newG) {
        expandedIcon = newG;
    }

    public int getLeftChildIndent() {
        return leftChildIndent;
    }

    public void setLeftChildIndent(int newAmount) {
        leftChildIndent = newAmount;
        totalChildIndent = leftChildIndent + rightChildIndent;
        updateSize();
    }

    public int getRightChildIndent() {
        return rightChildIndent;
    }

    public void setRightChildIndent(int newAmount) {
        rightChildIndent = newAmount;
        totalChildIndent = leftChildIndent + rightChildIndent;
        updateSize();
    }

    public Dimension getPreferredMinSize() {
        if (preferredMinSize == null) {
            return null;
        }
        return new Dimension(preferredMinSize);
    }

    public void setPreferredMinSize(Dimension newSize) {
        preferredMinSize = newSize;
    }

    protected void setCellRenderer(TreeCellRenderer tcr) {
        completeEditing();
        updateRenderer();
        if (treeState != null) {
            treeState.invalidateSizes();
            updateSize();
        }
    }

    /** The tree's; and if it does not have one, a factory one that is noted as its own. */
    private void updateRenderer() {
        if (tree == null) {
            return;
        }
        TreeCellRenderer newCellRenderer = tree.getCellRenderer();
        if (newCellRenderer == null) {
            tree.setCellRenderer(createDefaultCellRenderer());
            createdRenderer = true;
            newCellRenderer = tree.getCellRenderer();
        } else {
            createdRenderer = false;
        }
        currentCellRenderer = newCellRenderer;
    }

    protected TreeCellRenderer getCellRenderer() {
        return currentCellRenderer;
    }

    protected void setCellEditor(TreeCellEditor editor) {
        updateCellEditor();
    }

    protected TreeCellEditor getCellEditor() {
        return cellEditor;
    }

    /**
     * A factory editor, wrapped in whatever renderer there already is.
     *
     * <p>Wrapping it matters: the editor draws the cell's icon to the left of the text field, and
     * it takes it from the renderer. With no renderer that serves, it goes with no icon.
     */
    protected TreeCellEditor createDefaultCellEditor() {
        if (currentCellRenderer instanceof DefaultTreeCellRenderer) {
            return new DefaultTreeCellEditor(tree, (DefaultTreeCellRenderer) currentCellRenderer);
        }
        return new DefaultTreeCellEditor(tree, null);
    }

    /**
     * The tree's editor.
     *
     * <p>It takes the one the tree has, whether it is editable or not: an editor that was set is
     * not thrown away when the tree stops being editable, because becoming editable again has to
     * give the same one back. It only makes one when there is none <em>and</em> the tree is
     * editable, and in that case -- and only in that one -- {@link #createdCellEditor} is left at
     * `true`, which is what afterwards authorizes removing it. Measured.
     */
    protected void updateCellEditor() {
        completeEditing();
        if (tree == null) {
            cellEditor = null;
            return;
        }
        TreeCellEditor newEditor = tree.getCellEditor();
        if (newEditor == null && tree.isEditable()) {
            newEditor = createDefaultCellEditor();
            if (newEditor != null) {
                // Setting it tells the tree, which comes back in here; that is why it is assigned
                // afterwards.
                tree.setCellEditor(newEditor);
                cellEditor = newEditor;
                createdCellEditor = true;
                return;
            }
        }
        cellEditor = newEditor;
        createdCellEditor = false;
    }

    protected void setModel(TreeModel model) {
        completeEditing();
        if (treeModel != null && treeModelListener != null) {
            treeModel.removeTreeModelListener(treeModelListener);
        }
        treeModel = model;
        if (treeModel != null && treeModelListener != null) {
            treeModel.addTreeModelListener(treeModelListener);
        }
        if (treeState != null) {
            treeState.setModel(model);
            updateLayoutCacheExpandedNodes();
            updateSize();
        }
    }

    protected TreeModel getModel() {
        return treeModel;
    }

    protected void setRootVisible(boolean newValue) {
        completeEditing();
        updateDepthOffset();
        if (treeState != null) {
            treeState.setRootVisible(newValue);
            treeState.invalidateSizes();
            updateSize();
        }
    }

    protected boolean isRootVisible() {
        return (tree != null) && tree.isRootVisible();
    }

    protected void setShowsRootHandles(boolean newValue) {
        completeEditing();
        updateDepthOffset();
        if (treeState != null) {
            treeState.invalidateSizes();
            updateSize();
        }
    }

    protected boolean getShowsRootHandles() {
        return (tree != null) && tree.getShowsRootHandles();
    }

    protected void setEditable(boolean newValue) {
        completeEditing();
        updateCellEditor();
    }

    protected boolean isEditable() {
        return (tree != null) && tree.isEditable();
    }

    protected void setLargeModel(boolean largeModel) {
        if (getRowHeight() < 1) {
            largeModel = false;
        }
        if (this.largeModel != largeModel) {
            completeEditing();
            this.largeModel = largeModel;
            treeState = createLayoutCache();
            configureLayoutCache();
            updateLayoutCacheExpandedNodes();
            updateSize();
        }
    }

    protected boolean isLargeModel() {
        return largeModel;
    }

    protected void setRowHeight(int rowHeight) {
        completeEditing();
        if (treeState != null) {
            setLargeModel(tree.isLargeModel());
            treeState.setRowHeight(rowHeight);
            updateSize();
        }
    }

    /** Zero: each row measures whatever it needs; see the class note. */
    protected int getRowHeight() {
        return (tree == null) ? -1 : tree.getRowHeight();
    }

    protected void setSelectionModel(TreeSelectionModel newLSM) {
        completeEditing();
        if (treeSelectionModel != null && treeSelectionListener != null) {
            treeSelectionModel.removeTreeSelectionListener(treeSelectionListener);
        }
        treeSelectionModel = newLSM;
        if (treeSelectionModel != null && treeSelectionListener != null) {
            treeSelectionModel.addTreeSelectionListener(treeSelectionListener);
        }
        if (treeState != null) {
            treeState.setSelectionModel(treeSelectionModel);
        }
        if (tree != null) {
            tree.repaint();
        }
    }

    protected TreeSelectionModel getSelectionModel() {
        return treeSelectionModel;
    }

    /** The adjustment for visible root and handles; see the class note. */
    protected void updateDepthOffset() {
        if (isRootVisible()) {
            depthOffset = getShowsRootHandles() ? 1 : 0;
        } else {
            depthOffset = getShowsRootHandles() ? 0 : -1;
        }
    }

    private void updateLayoutCacheExpandedNodes() {
        if (treeModel != null && treeModel.getRoot() != null) {
            updateExpandedDescendants(new TreePath(treeModel.getRoot()));
        }
    }

    /** It tells the table which paths are open. */
    protected void updateExpandedDescendants(TreePath path) {
        completeEditing();
        if (treeState == null) {
            return;
        }
        treeState.setExpandedState(path, true);
        Enumeration<?> descendants = tree.getExpandedDescendants(path);
        if (descendants != null) {
            while (descendants.hasMoreElements()) {
                Object o = descendants.nextElement();
                if (o instanceof TreePath) {
                    treeState.setExpandedState((TreePath) o, true);
                }
            }
        }
        updateLeadSelectionRow();
        updateSize();
    }

    /** That path's last child, in order to know how far the vertical line goes. */
    protected TreePath getLastChildPath(TreePath parent) {
        if (treeModel == null || parent == null) {
            return null;
        }
        int childCount = treeModel.getChildCount(parent.getLastPathComponent());
        if (childCount > 0) {
            return parent.pathByAddingChild(
                    treeModel.getChild(parent.getLastPathComponent(), childCount - 1));
        }
        return null;
    }

    protected void updateLeadSelectionRow() {
        lastSelectedRow = (tree == null) ? -1 : tree.getLeadSelectionRow();
    }

    protected int getLeadSelectionRow() {
        return lastSelectedRow;
    }

    /** How much a row shifts according to its depth; see the class note. */
    protected int getRowX(int row, int depth) {
        return totalChildIndent * (depth + depthOffset);
    }

    /** Zero: the basic one leaves no extra air between the handle and the line. */
    protected int getHorizontalLegBuffer() {
        return 0;
    }

    /** Zero. */
    protected int getVerticalLegBuffer() {
        return 0;
    }

    protected boolean isLeaf(int row) {
        TreePath path = getPathForRow(tree, row);
        if (path == null) {
            return true;
        }
        return treeModel.isLeaf(path.getLastPathComponent());
    }

    /** It marks the size as stale and tells the tree. */
    protected void updateSize() {
        validCachedPreferredSize = false;
        if (tree != null) {
            tree.revalidate();
        }
    }

    /** It rebuilds the preferred size from the table of positions. */
    protected void updateCachedPreferredSize() {
        if (treeState != null && tree != null) {
            Insets i = tree.getInsets();
            preferredSize.width = treeState.getPreferredWidth(null);
            preferredSize.height = treeState.getPreferredHeight();
            preferredSize.width += i.left + i.right;
            preferredSize.height += i.top + i.bottom;
        }
        validCachedPreferredSize = true;
    }

    public Dimension getPreferredSize(JComponent c) {
        return getPreferredSize(c, true);
    }

    /** The table of positions', never smaller than {@link #getPreferredMinSize}. */
    public Dimension getPreferredSize(JComponent c, boolean checkConsistency) {
        if (!validCachedPreferredSize) {
            updateCachedPreferredSize();
        }
        if (tree == null) {
            return new Dimension(0, 0);
        }
        Dimension pSize = getPreferredMinSize();
        if (pSize != null) {
            return new Dimension(Math.max(pSize.width, preferredSize.width),
                    Math.max(pSize.height, preferredSize.height));
        }
        return new Dimension(preferredSize.width, preferredSize.height);
    }

    /** Zero: a tree can be shrunk until it disappears. */
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(0, 0);
    }

    /** The same as the preferred one. */
    public Dimension getMaximumSize(JComponent c) {
        if (tree != null) {
            return getPreferredSize(tree);
        }
        return new Dimension(0, 0);
    }

    public Rectangle getPathBounds(JTree tree, TreePath path) {
        if (tree != null && treeState != null) {
            return getPathBounds(path, tree.getInsets(), new Rectangle());
        }
        return null;
    }

    private Rectangle getPathBounds(TreePath path, Insets insets, Rectangle bounds) {
        bounds = treeState.getBounds(path, bounds);
        if (bounds != null) {
            bounds.x += insets.left;
            bounds.y += insets.top;
        }
        return bounds;
    }

    public TreePath getPathForRow(JTree tree, int row) {
        return (treeState != null) ? treeState.getPathForRow(row) : null;
    }

    public int getRowForPath(JTree tree, TreePath path) {
        return (treeState != null) ? treeState.getRowForPath(path) : -1;
    }

    public int getRowCount(JTree tree) {
        return (treeState != null) ? treeState.getRowCount() : 0;
    }

    public TreePath getClosestPathForLocation(JTree tree, int x, int y) {
        if (tree == null || treeState == null) {
            return null;
        }
        Insets i = tree.getInsets();
        return treeState.getPathClosestTo(x - i.left, y - i.top);
    }

    public boolean isEditing(JTree tree) {
        return editingComponent != null;
    }

    public boolean stopEditing(JTree tree) {
        if (editingComponent != null && cellEditor.stopCellEditing()) {
            completeEditing(false, false, true);
            return true;
        }
        return false;
    }

    public void cancelEditing(JTree tree) {
        if (editingComponent != null) {
            completeEditing(false, true, false);
        }
    }

    public void startEditingAtPath(JTree tree, TreePath path) {
        tree.scrollPathToVisible(path);
        if (path != null && tree.isVisible(path)) {
            startEditing(path, null);
        }
    }

    public TreePath getEditingPath(JTree tree) {
        return editingPath;
    }

    /** It ends the editing as {@link #stopEditingInCompleteEditing} says. */
    protected void completeEditing() {
        if (stopEditingInCompleteEditing && editingComponent != null) {
            cellEditor.stopCellEditing();
        }
    }

    protected void completeEditing(boolean messageStop, boolean messageCancel,
            boolean messageTree) {
        if (editingComponent == null) {
            return;
        }
        if (messageStop) {
            cellEditor.stopCellEditing();
        } else if (messageCancel) {
            cellEditor.cancelCellEditing();
        }
        Component old = editingComponent;
        editingComponent = null;
        TreePath oldPath = editingPath;
        editingPath = null;
        if (old != null) {
            tree.remove(old);
        }
        if (messageTree && oldPath != null && cellEditor != null) {
            treeModel.valueForPathChanged(oldPath, cellEditor.getCellEditorValue());
        }
        updateSize();
    }

    /** It starts the editing of that path; it returns whether it could. */
    protected boolean startEditing(TreePath path, MouseEvent event) {
        if (isEditing(tree) && tree.getInvokesStopCellEditing() && !stopEditing(tree)) {
            return false;
        }
        completeEditing();
        if (cellEditor == null || !cellEditor.isCellEditable(event)) {
            return false;
        }
        int row = getRowForPath(tree, path);
        editingComponent = cellEditor.getTreeCellEditorComponent(tree,
                path.getLastPathComponent(), tree.isPathSelected(path),
                tree.isExpanded(path), treeModel.isLeaf(path.getLastPathComponent()), row);
        if (editingComponent == null) {
            return false;
        }
        editingPath = path;
        editingRow = row;
        tree.add(editingComponent);
        updateSize();
        return true;
    }

    /** It opens or closes that path. */
    protected void toggleExpandState(TreePath path) {
        if (!tree.isExpanded(path)) {
            tree.expandPath(path);
        } else {
            tree.collapsePath(path);
        }
    }

    protected void pathWasExpanded(TreePath path) {
        if (tree != null) {
            tree.fireTreeExpanded(path);
        }
    }

    protected void pathWasCollapsed(TreePath path) {
        if (tree != null) {
            tree.fireTreeCollapsed(path);
        }
    }

    /** A click on the handle, not on the text. */
    protected void handleExpandControlClick(TreePath path, int mouseX, int mouseY) {
        toggleExpandState(path);
    }

    /** If that point fell on the handle, it works it. */
    protected void checkForClickInExpandControl(TreePath path, int mouseX, int mouseY) {
        if (isLocationInExpandControl(path, mouseX, mouseY)) {
            handleExpandControlClick(path, mouseX, mouseY);
        }
    }

    /**
     * Whether that point falls on that row's handle.
     *
     * <p>It looks only at the horizontal, and on purpose: the vertical was already resolved by
     * whoever chose the path from the click's coordinate. Asking about the vertical again would
     * make it stricter than the rest of the look and feel, and a click flush with a row's edge
     * would stop opening.
     *
     * <p>A leaf has no handle, so never.
     *
     * <p>The box's width comes from the expanded icon; when there is none -- which is this
     * library's case, with no look and feel table -- it is eight pixels, which is what the JDK
     * uses in reserve.
     *
     * <p>The box goes centred on the point {@link #rightChildIndent} leaves to the left of the
     * text, and hence the {@code /2}: the handle is drawn centred on that point, not resting on
     * it. The interval is open on the left and closed on the right -- measured -- so a box of
     * eighteen accepts eighteen columns and not nineteen.
     */
    protected boolean isLocationInExpandControl(TreePath path, int mouseX, int mouseY) {
        if (tree == null || treeModel == null
                || treeModel.isLeaf(path.getLastPathComponent())) {
            return false;
        }
        int width = (getExpandedIcon() != null) ? getExpandedIcon().getIconWidth() : 8;
        Insets i = tree.getInsets();
        int left = getRowX(tree.getRowForPath(path), path.getPathCount() - 1)
                - getRightChildIndent() - width / 2 + i.left;
        return mouseX > left && mouseX <= left + width;
    }

    /**
     * It scrolls the tree so that that stretch of rows is seen.
     *
     * <p>When the stretch is a single row its whole band is asked for. When they are several the
     * complete block is not asked for: it is asked for from the first one and as tall as the
     * visible part, because asking for more than fits makes the tree show the end of the stretch
     * and hide the beginning, which is just the opposite of what whoever has just opened a branch
     * wants.
     *
     * <p>The width asked for is one single: moving horizontally is not this method's business.
     */
    protected void ensureRowsAreVisible(int beginRow, int endRow) {
        if (tree == null || beginRow < 0 || endRow >= getRowCount(tree)) {
            return;
        }
        if (beginRow == endRow) {
            Rectangle banda = getPathBounds(tree, getPathForRow(tree, beginRow));
            if (banda != null) {
                banda.x = tree.getVisibleRect().x;
                banda.width = 1;
                tree.scrollRectToVisible(banda);
            }
            return;
        }
        Rectangle first = getPathBounds(tree, getPathForRow(tree, beginRow));
        if (first == null) {
            return;
        }
        Rectangle visible = tree.getVisibleRect();
        tree.scrollRectToVisible(
                new Rectangle(visible.x, first.y, 1, visible.height));
    }

    /** Whether the row carries a handle: only those that have children. */
    protected boolean shouldPaintExpandControl(TreePath path, int row, boolean isExpanded,
            boolean hasBeenExpanded, boolean isLeaf) {
        if (isLeaf) {
            return false;
        }
        int depth = path.getPathCount() - 1;
        return !((depth == 0 || (depth == 1 && !isRootVisible()))
                && !getShowsRootHandles());
    }

    protected boolean isToggleSelectionEvent(MouseEvent event) {
        return javax.swing.SwingUtilities.isLeftMouseButton(event) && event.isControlDown();
    }

    protected boolean isMultiSelectEvent(MouseEvent event) {
        return javax.swing.SwingUtilities.isLeftMouseButton(event) && event.isShiftDown();
    }

    /** Double click: it opens or closes. */
    protected boolean isToggleEvent(MouseEvent event) {
        if (!javax.swing.SwingUtilities.isLeftMouseButton(event)) {
            return false;
        }
        int clickCount = tree.getToggleClickCount();
        return clickCount > 0 && event.getClickCount() == clickCount;
    }

    /** It chooses whatever applies according to the keys that are held down. */
    protected void selectPathForEvent(TreePath path, MouseEvent event) {
        if (isToggleSelectionEvent(event)) {
            if (tree.isPathSelected(path)) {
                tree.removeSelectionPath(path);
            } else {
                tree.addSelectionPath(path);
            }
        } else if (isMultiSelectEvent(event)) {
            TreePath anchor = tree.getAnchorSelectionPath();
            if (anchor == null) {
                tree.setSelectionPath(path);
            } else {
                int anchorRow = getRowForPath(tree, anchor);
                int row = getRowForPath(tree, path);
                tree.setSelectionInterval(Math.min(anchorRow, row), Math.max(anchorRow, row));
            }
        } else {
            tree.setSelectionPath(path);
        }
    }

    /**
     * Where the first row's text rests.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        if (tree == null || currentCellRenderer == null) {
            return -1;
        }
        Component renderer = currentCellRenderer.getTreeCellRendererComponent(
                tree, "a", false, false, true, -1, false);
        if (renderer == null) {
            return -1;
        }
        renderer.setFont(tree.getFont());
        Dimension pref = renderer.getPreferredSize();
        int baseline = renderer.getBaseline(pref.width, pref.height);
        if (baseline < 0) {
            return -1;
        }
        return baseline + tree.getInsets().top;
    }

    /**
     * {@code CONSTANT_ASCENT}: the first row is always at the very top.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    public void paint(Graphics g, JComponent c) {
        if (tree != c || treeState == null) {
            throw new InternalError("incorrect component");
        }
        Rectangle paintBounds = g.getClipBounds();
        Insets insets = tree.getInsets();
        TreePath initialPath = getClosestPathForLocation(tree, 0, paintBounds.y);
        if (initialPath == null) {
            return;
        }
        Enumeration<?> paintingEnumerator = treeState.getVisiblePathsFrom(initialPath);
        if (paintingEnumerator == null) {
            return;
        }
        int row = treeState.getRowForPath(initialPath);
        Rectangle boundsBuffer = new Rectangle();
        boolean done = false;
        drawingCache.clear();
        while (!done && paintingEnumerator.hasMoreElements()) {
            Object o = paintingEnumerator.nextElement();
            if (!(o instanceof TreePath)) {
                break;
            }
            TreePath path = (TreePath) o;
            Rectangle bounds = getPathBounds(path, insets, boundsBuffer);
            if (bounds == null) {
                break;
            }
            boolean isLeaf = treeModel.isLeaf(path.getLastPathComponent());
            boolean isExpanded = !isLeaf && treeState.getExpandedState(path);
            paintRow(g, paintBounds, insets, bounds, path, row, isExpanded, true, isLeaf);
            if ((bounds.y + bounds.height) >= paintBounds.y + paintBounds.height) {
                done = true;
            }
            row++;
        }
    }

    /** One row: the handle if it carries one, and the cell drawn by the renderer. */
    protected void paintRow(Graphics g, Rectangle clipBounds, Insets insets, Rectangle bounds,
            TreePath path, int row, boolean isExpanded, boolean hasBeenExpanded, boolean isLeaf) {
        if (editingComponent != null && editingRow == row) {
            return;
        }
        int leadIndex = tree.hasFocus() ? getLeadSelectionRow() : -1;
        Component component = currentCellRenderer.getTreeCellRendererComponent(tree,
                path.getLastPathComponent(), tree.isRowSelected(row), isExpanded, isLeaf, row,
                (leadIndex == row));
        rendererPane.paintComponent(g, component, tree, bounds.x, bounds.y,
                bounds.width, bounds.height, true);
        if (shouldPaintExpandControl(path, row, isExpanded, hasBeenExpanded, isLeaf)) {
            paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded,
                    hasBeenExpanded, isLeaf);
        }
    }

    /** The open or close handle. */
    protected void paintExpandControl(Graphics g, Rectangle clipBounds, Insets insets,
            Rectangle bounds, TreePath path, int row, boolean isExpanded,
            boolean hasBeenExpanded, boolean isLeaf) {
        Icon icon = isExpanded ? getExpandedIcon() : getCollapsedIcon();
        if (icon == null) {
            return;
        }
        int middleXOfKnob = bounds.x - getRightChildIndent() + 1;
        int middleYOfKnob = bounds.y + (bounds.height / 2);
        drawCentered(tree, g, icon, middleXOfKnob, middleYOfKnob);
    }

    /** The vertical part of the line that joins a parent with its children. */
    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            TreePath path) {
        if (!tree.isVisible(path)) {
            return;
        }
        int depth = path.getPathCount() - 1;
        if (depth == 0 && !getShowsRootHandles() && !isRootVisible()) {
            return;
        }
        int lineX = getRowX(-1, depth + 1) - getRightChildIndent() + insets.left;
        g.setColor(getHashColor());
        paintVerticalLine(g, tree, lineX, clipBounds.y, clipBounds.y + clipBounds.height);
    }

    /** And the horizontal one, from the trunk to the row. */
    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds, Insets insets,
            Rectangle bounds, TreePath path, int row, boolean isExpanded,
            boolean hasBeenExpanded, boolean isLeaf) {
        int depth = path.getPathCount() - 1;
        if ((depth == 0 || (depth == 1 && !isRootVisible())) && !getShowsRootHandles()) {
            return;
        }
        int clipLeft = clipBounds.x;
        int lineY = bounds.y + bounds.height / 2;
        int leftX = bounds.x - getRightChildIndent();
        g.setColor(getHashColor());
        paintHorizontalLine(g, tree, lineY, leftX, bounds.x - getHorizontalLegBuffer());
        if (clipLeft > leftX) {
            return;
        }
    }

    protected void paintHorizontalLine(Graphics g, JComponent c, int y, int left, int right) {
        drawDashedHorizontalLine(g, y, left, right);
    }

    protected void paintVerticalLine(Graphics g, JComponent c, int x, int top, int bottom) {
        drawDashedVerticalLine(g, x, top, bottom);
    }

    /** A dotted line: one pixel on and one off. */
    protected void drawDashedHorizontalLine(Graphics g, int y, int x1, int x2) {
        x1 += (x1 % 2);
        for (int x = x1; x <= x2; x += 2) {
            g.drawLine(x, y, x, y);
        }
    }

    protected void drawDashedVerticalLine(Graphics g, int x, int y1, int y2) {
        y1 += (y1 % 2);
        for (int y = y1; y <= y2; y += 2) {
            g.drawLine(x, y, x, y);
        }
    }

    /** It draws an icon centred on that point. */
    protected void drawCentered(Component c, Graphics graphics, Icon icon, int x, int y) {
        icon.paintIcon(c, graphics, x - icon.getIconWidth() / 2 - 1,
                y - icon.getIconHeight() / 2);
    }

    /** The line that marks where something that is being dragged would fall. */
    protected boolean isDropLine(JTree.DropLocation loc) {
        return loc != null && loc.getPath() != null && loc.getChildIndex() != -1;
    }

    protected Rectangle getDropLineRect(JTree.DropLocation loc) {
        return new Rectangle();
    }

    protected void paintDropLine(Graphics g) {
    }

    /**
     * How much each node measures; the table of positions asks it.
     *
     * <p>Static and with the look and feel as the first parameter, which is the signature the JDK
     * generates for an inner class; see finding #518.
     */
    public static class NodeDimensionsHandler extends AbstractLayoutCache.NodeDimensions {

        private final BasicTreeUI ui;

        public NodeDimensionsHandler(BasicTreeUI ui) {
            this.ui = ui;
        }

        public Rectangle getNodeDimensions(Object value, int row, int depth,
                boolean expanded, Rectangle size) {
            if (ui.currentCellRenderer == null || ui.tree == null) {
                return null;
            }
            Component aComponent = ui.currentCellRenderer.getTreeCellRendererComponent(
                    ui.tree, value, ui.tree.isRowSelected(row), expanded,
                    ui.treeModel != null && ui.treeModel.isLeaf(value), row, false);
            if (aComponent == null) {
                return null;
            }
            ui.rendererPane.add(aComponent);
            aComponent.validate();
            Dimension prefSize = aComponent.getPreferredSize();
            if (size == null) {
                size = new Rectangle(0, 0, prefSize.width, prefSize.height);
            } else {
                size.width = prefSize.width;
                size.height = prefSize.height;
            }
            size.x = getRowX(row, depth);
            return size;
        }

        protected int getRowX(int row, int depth) {
            return ui.getRowX(row, depth);
        }
    }

    /**
     * The one that listens to everything: the model, the selection, the expansion, the mouse,
     * the focus and the size.
     *
     * <p>Static and with the look and feel as a field, for the same reason as everywhere in the
     * package.
     */
    private static class Handler implements PropertyChangeListener, MouseListener, FocusListener,
            ComponentListener, TreeSelectionListener, TreeModelListener, TreeExpansionListener,
            CellEditorListener {

        private final BasicTreeUI ui;

        Handler(BasicTreeUI ui) {
            this.ui = ui;
        }

        public void propertyChange(PropertyChangeEvent e) {
            JTree tree = ui.tree;
            if (tree == null || e.getSource() != tree) {
                return;
            }
            String name = e.getPropertyName();
            if (JTree.ROOT_VISIBLE_PROPERTY.equals(name)) {
                ui.setRootVisible(Boolean.TRUE.equals(e.getNewValue()));
            } else if (JTree.SHOWS_ROOT_HANDLES_PROPERTY.equals(name)) {
                ui.setShowsRootHandles(Boolean.TRUE.equals(e.getNewValue()));
            } else if (JTree.ROW_HEIGHT_PROPERTY.equals(name)) {
                ui.setRowHeight(tree.getRowHeight());
            } else if (JTree.CELL_RENDERER_PROPERTY.equals(name)) {
                ui.setCellRenderer(tree.getCellRenderer());
            } else if (JTree.TREE_MODEL_PROPERTY.equals(name)) {
                ui.setModel(tree.getModel());
            } else if (JTree.SELECTION_MODEL_PROPERTY.equals(name)) {
                ui.setSelectionModel(tree.getSelectionModel());
            } else if (JTree.EDITABLE_PROPERTY.equals(name)) {
                ui.setEditable(tree.isEditable());
            } else if (JTree.LARGE_MODEL_PROPERTY.equals(name)) {
                ui.setLargeModel(tree.isLargeModel());
            } else if ("font".equals(name)) {
                if (ui.treeState != null) {
                    ui.treeState.invalidateSizes();
                }
                ui.updateSize();
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            JTree tree = ui.tree;
            if (tree == null || !tree.isEnabled()) {
                return;
            }
            if (tree.isRequestFocusEnabled()) {
                tree.requestFocus();
            }
            TreePath path = ui.getClosestPathForLocation(tree, e.getX(), e.getY());
            if (path == null) {
                return;
            }
            Rectangle bounds = ui.getPathBounds(tree, path);
            if (bounds == null) {
                return;
            }
            if (e.getX() < bounds.x) {
                // To the left of the text: it is the handle.
                if (ui.shouldPaintExpandControl(path, ui.getRowForPath(tree, path),
                        tree.isExpanded(path), true,
                        ui.treeModel.isLeaf(path.getLastPathComponent()))) {
                    ui.handleExpandControlClick(path, e.getX(), e.getY());
                }
                return;
            }
            if (e.getX() > bounds.x + bounds.width) {
                return;
            }
            if (ui.isToggleEvent(e)) {
                ui.toggleExpandState(path);
                return;
            }
            ui.selectPathForEvent(path, e);
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void focusGained(FocusEvent e) {
            if (ui.tree != null) {
                ui.tree.repaint();
            }
        }

        public void focusLost(FocusEvent e) {
            if (ui.tree != null) {
                ui.tree.repaint();
            }
        }

        public void componentResized(ComponentEvent e) {
            if (ui.isLargeModel() && ui.treeState != null) {
                ui.updateSize();
            }
        }

        public void componentMoved(ComponentEvent e) {
        }

        public void componentShown(ComponentEvent e) {
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void valueChanged(TreeSelectionEvent e) {
            ui.updateLeadSelectionRow();
            if (ui.tree != null) {
                ui.tree.repaint();
            }
        }

        public void treeNodesChanged(TreeModelEvent e) {
            if (ui.treeState != null) {
                ui.treeState.treeNodesChanged(e);
                ui.updateSize();
            }
        }

        public void treeNodesInserted(TreeModelEvent e) {
            if (ui.treeState != null) {
                ui.treeState.treeNodesInserted(e);
                ui.updateSize();
            }
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            if (ui.treeState != null) {
                ui.treeState.treeNodesRemoved(e);
                ui.updateSize();
            }
        }

        public void treeStructureChanged(TreeModelEvent e) {
            if (ui.treeState != null) {
                ui.treeState.treeStructureChanged(e);
                ui.updateSize();
            }
        }

        public void treeExpanded(TreeExpansionEvent e) {
            if (ui.treeState != null && e.getPath() != null) {
                ui.treeState.setExpandedState(e.getPath(), true);
                ui.updateSize();
            }
        }

        public void treeCollapsed(TreeExpansionEvent e) {
            if (ui.treeState != null && e.getPath() != null) {
                ui.completeEditing();
                ui.treeState.setExpandedState(e.getPath(), false);
                ui.updateSize();
            }
        }

        public void editingStopped(ChangeEvent e) {
            ui.completeEditing(false, false, true);
        }

        public void editingCanceled(ChangeEvent e) {
            ui.completeEditing(false, false, false);
        }
    }
}
