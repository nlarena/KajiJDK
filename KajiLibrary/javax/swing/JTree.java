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
 * Un arbol de nodos que se pueden desplegar y plegar.
 *
 * <h2>Que sabe el arbol y que sabe la vista</h2>
 *
 * <p>El arbol guarda los datos ({@link TreeModel}), lo elegido ({@link TreeSelectionModel}) y
 * <em>que esta desplegado</em>. Ese ultimo es suyo y no del modelo: dos arboles sobre los mismos
 * datos pueden tener desplegadas cosas distintas.
 *
 * <p>Lo que no sabe es la geometria. En que fila cae un camino, que camino hay en un punto, cuanto
 * mide una fila: todo eso lo contesta el {@link TreeUI}, porque depende de como se dibuje.
 *
 * <h2>Caminos y filas</h2>
 *
 * <p>Casi todo tiene dos versiones, una por camino y otra por fila. No son intercambiables: un
 * camino existe siempre, una fila solo si sus padres estan desplegados. Guardar filas es lo que
 * hace que una seleccion se corra sola al plegar algo mas arriba.
 *
 * <h2>Avisar antes de desplegar</h2>
 *
 * <p>{@link TreeWillExpandListener} llega antes y puede vetar con
 * {@link ExpandVetoException}. Es lo que permite cargar los hijos al desplegar -- y negarse si la
 * carga falla -- en lugar de tener todo el arbol en memoria.
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

    /** Los datos. */
    protected transient TreeModel treeModel;

    /** Lo elegido. */
    protected transient TreeSelectionModel selectionModel;

    /** Si la raiz se ve. */
    protected boolean rootVisible;

    /** Quien dibuja cada nodo. */
    protected transient TreeCellRenderer cellRenderer;

    /** El alto de una fila; cero o menos significa medir cada una. */
    protected int rowHeight;

    /** Si la raiz lleva el triangulito de desplegar. */
    protected boolean showsRootHandles;

    /** El puente entre el modelo de seleccion y quien escucha al arbol. */
    protected transient TreeSelectionRedirector selectionRedirector;

    /** Quien deja editar un nodo. */
    protected transient TreeCellEditor cellEditor;

    /** Si se pueden editar los nodos. */
    protected boolean editable;

    /** Si el arbol es grande y conviene no medir cada fila. */
    protected boolean largeModel;

    /** Cuantas filas se ven sin desplazar. */
    protected int visibleRowCount;

    /** Si al perder el foco la edicion se guarda en lugar de cancelarse. */
    protected boolean invokesStopCellEditing;

    /** Si desplegar desplaza para que se vea lo que aparecio. */
    protected boolean scrollsOnExpand;

    /** Cuantos clics despliegan un nodo. */
    protected int toggleClickCount;

    /** Quien escucha al modelo de datos. */
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
     * Un arbol de ejemplo.
     *
     * <p>Existe para que {@code new JTree()} muestre algo. Un arbol vacio se veria roto y hace
     * pensar que falta configurar cuando lo que falta es el modelo.
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

    /** Arma un modelo a partir de un arreglo, un vector o una tabla. */
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

    /** Un arbol de ejemplo; ver {@link #getDefaultTreeModel}. */
    public JTree() {
        this(getDefaultTreeModel());
    }

    /** Un arbol con esos elementos como hijos de una raiz escondida. */
    public JTree(Object[] value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /** Un arbol con los elementos de ese vector. */
    public JTree(Vector<?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /**
     * Un arbol a partir de una tabla.
     *
     * <p>Las claves son los nodos y los valores sus hijos, que a su vez pueden ser tablas. El
     * orden de los hermanos es el de la tabla, o sea que no hay orden garantizado.
     */
    public JTree(Hashtable<?, ?> value) {
        this(createTreeModel(value));
        setRootVisible(false);
        setShowsRootHandles(true);
    }

    /** Un arbol sobre ese nodo como raiz. */
    public JTree(TreeNode root) {
        this(root, false);
    }

    /** Un arbol sobre esa raiz, eligiendo como se decide que es hoja. */
    public JTree(TreeNode root, boolean asksAllowsChildren) {
        this(new DefaultTreeModel(root, asksAllowsChildren));
    }

    /** Un arbol sobre ese modelo. */
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

    /** Si se pueden editar los nodos; apagarlo corta una edicion en curso. */
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
     * Cambia los datos.
     *
     * <p>Olvida lo desplegado y lo elegido: los caminos viejos apuntan a nodos que ya no estan, y
     * conservarlos dejaria el arbol mostrando algo que no existe.
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
            // La raiz arranca desplegada: si no, un arbol nuevo se veria como una sola fila.
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
     * Si la raiz se ve.
     *
     * <p>Esconderla es lo que hace que un arbol parezca varios: los hijos de la raiz quedan como
     * raices sueltas. Por eso los constructores que reciben una lista la esconden.
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
     * El alto de todas las filas.
     *
     * <p>Cero o menos significa medir cada una. Medir es correcto y cuesta: con un arbol grande, un
     * alto fijo es la diferencia entre desplazarse suave y a saltos.
     */
    public void setRowHeight(int rowHeight) {
        int oldValue = this.rowHeight;
        this.rowHeight = rowHeight;
        rowHeightSet = true;
        firePropertyChange(ROW_HEIGHT_PROPERTY, oldValue, this.rowHeight);
        invalidate();
    }

    /** Si el alto de fila lo puso el programa; entonces el aspecto no lo pisa. */
    private boolean rowHeightSet;

    /**
     * El aspecto propone el alto de fila; ver {@link JComponent#customSetUIProperty}.
     *
     * <p>Es la unica propiedad que el arbol agrega a la lista. Un aspecto con filas de alto fijo
     * propone el suyo, y el basico propone cero -- "medi cada una" --; en las dos, si el programa
     * ya llamo a {@link #setRowHeight}, la propuesta se descarta.
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

    /** Si el arbol es grande; el aspecto puede usarlo para no medir todo. */
    public void setLargeModel(boolean newValue) {
        boolean oldValue = largeModel;
        largeModel = newValue;
        firePropertyChange(LARGE_MODEL_PROPERTY, oldValue, newValue);
    }

    public boolean isLargeModel() {
        return largeModel;
    }

    /**
     * Que hacer con una edicion en curso cuando pasa algo.
     *
     * <p>Guardar o descartar. No hay una respuesta buena: guardar puede meter un valor a medio
     * escribir, descartar puede perder lo escrito.
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

    /** Cuantos clics despliegan un nodo; cero significa que el clic no despliega. */
    public void setToggleClickCount(int clickCount) {
        int oldCount = toggleClickCount;
        toggleClickCount = clickCount;
        firePropertyChange(TOGGLE_CLICK_COUNT_PROPERTY, oldCount, clickCount);
    }

    public int getToggleClickCount() {
        return toggleClickCount;
    }

    /**
     * Si elegir un nodo escondido despliega lo que haga falta para verlo.
     *
     * <p>Apagarlo permite tener elegido algo que no se ve, que es lo que quiere un programa que
     * elige por su cuenta y no quiere mover lo que el usuario dejo plegado.
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
     * Como se muestra donde va a caer lo que se arrastra.
     *
     * @throws IllegalArgumentException si el modo no sirve para un arbol.
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

    /** Si ese nodo se puede editar; por omision, si el arbol lo es. */
    public boolean isPathEditable(TreePath path) {
        return isEditable();
    }

    public String getToolTipText(MouseEvent event) {
        return super.getToolTipText(event);
    }

    /**
     * El texto de un nodo.
     *
     * <p>Por omision su {@code toString}. Sobrescribirlo es la forma barata de cambiar como se ven
     * los nodos sin escribir un dibujante entero.
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

    /** Cuantas filas se ven; lo contesta el aspecto. */
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

    /** El camino desde donde se extiende con Shift. */
    public void setLeadSelectionPath(TreePath newPath) {
        TreePath oldValue = leadPath;
        leadPath = newPath;
        firePropertyChange(LEAD_SELECTION_PATH_PROPERTY, oldValue, newPath);
    }

    /** El camino donde empezo la seleccion. */
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

    /** El nodo del primer camino elegido, no el camino. */
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
     * Los descendientes desplegados de ese camino.
     *
     * <p>Sirve para guardar y restaurar el estado del arbol: al rearmarlo, volver a desplegar estos
     * caminos lo deja como estaba.
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
     * Si ese camino estuvo desplegado alguna vez.
     *
     * <p>Distinto de estar desplegado ahora: sirve para saber si sus hijos ya se cargaron.
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
        // Un nodo desplegado con un padre plegado no se ve, y entonces no cuenta.
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

    /** Despliega lo que haga falta para que ese camino se vea. */
    public void makeVisible(TreePath path) {
        if (path != null) {
            TreePath parentPath = path.getParentPath();
            if (parentPath != null) {
                expandPath(parentPath);
            }
        }
    }

    /** Si ese camino se ve, o sea si todos sus padres estan desplegados. */
    public boolean isVisible(TreePath path) {
        if (path != null) {
            TreePath parentPath = path.getParentPath();
            if (parentPath != null) {
                return isExpanded(parentPath);
            }
            // La raiz se ve si esta puesta como visible.
            return true;
        }
        return false;
    }

    /** El rectangulo de ese camino; lo contesta el aspecto. */
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

    /** Despliega y desplaza para que ese camino se vea. */
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

    /** Despliega ese camino y todos sus padres. */
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

    /** El camino que hay en ese punto, o nulo si no hay ninguno. */
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

    /** El camino mas cercano a ese punto, aunque el punto no caiga sobre ninguno. */
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

    /** Termina la edicion guardando; devuelve si habia una. */
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
     * Cambia el modelo de seleccion.
     *
     * <p>Nulo pone uno que no deja elegir nada, no deja el arbol sin modelo: asi el resto del
     * codigo nunca tiene que preguntar si hay.
     */
    public void setSelectionModel(TreeSelectionModel selectionModel) {
        TreeSelectionModel nuevo = (selectionModel == null)
                ? EmptySelectionModel.sharedInstance() : selectionModel;
        TreeSelectionModel oldValue = this.selectionModel;
        if (this.selectionModel != null && selectionRedirector != null) {
            this.selectionModel.removeTreeSelectionListener(selectionRedirector);
        }
        this.selectionModel = nuevo;
        if (selectionRedirector == null) {
            selectionRedirector = new TreeSelectionRedirector(this);
        }
        this.selectionModel.addTreeSelectionListener(selectionRedirector);
        firePropertyChange(SELECTION_MODEL_PROPERTY, oldValue, this.selectionModel);
    }

    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** Los caminos de las filas entre esas dos, en orden. */
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
     * Avisa antes de desplegar.
     *
     * @throws ExpandVetoException si alguien se niega; ver la nota de la clase.
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
     * Avisa antes de plegar.
     *
     * @throws ExpandVetoException si alguien se niega.
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

    /** Avisa que algo cambio y hay que rehacer el dibujo. */
    public void treeDidChange() {
        revalidate();
        repaint();
    }

    /** Cuantas filas se ven; es lo que el arbol le pide al desplazador. */
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
     * El proximo nodo cuyo texto empieza con eso.
     *
     * <p>Igual que en {@link JList}: es lo que hace que escribir salte al nodo. Usa el texto que se
     * muestra, no el objeto.
     *
     * @throws IllegalArgumentException si el prefijo es nulo o la fila no existe.
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

    /** El alto sale de {@link #getVisibleRowCount} filas, no de todas. */
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

    /** De a cuanto avanza la rueda: una fila. */
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

    /** De a cuanto avanza al hacer clic en la barra: una pantalla. */
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
     * Despliega o pliega un camino, avisando antes y despues.
     *
     * <p>Desplegar despliega tambien todos los padres: no tiene sentido dejar desplegado algo que
     * no se ve. Plegar no toca a los hijos, que quedan como estaban para cuando se vuelva a
     * desplegar.
     */
    protected void setExpandedState(TreePath path, boolean state) {
        if (path == null) {
            return;
        }
        if (state) {
            // De arriba hacia abajo: un padre plegado esconderia lo que se acaba de desplegar.
            Vector<TreePath> cadena = new Vector<TreePath>();
            for (TreePath p = path; p != null; p = p.getParentPath()) {
                cadena.insertElementAt(p, 0);
            }
            for (int i = 0; i < cadena.size(); i++) {
                TreePath p = cadena.elementAt(i);
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

    /** Los descendientes de ese camino que se desplegaron alguna vez. */
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

    /** Olvida lo desplegado de esos caminos. */
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
     * Saca de la seleccion lo que cuelgue de ese camino.
     *
     * <p>Se llama cuando un subarbol desaparece: dejar elegido un nodo que ya no esta haria que el
     * arbol informara una seleccion imposible.
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
     * Un modelo de seleccion que no deja elegir nada.
     *
     * <p>Es lo que se pone cuando alguien pide un arbol sin seleccion; ver
     * {@link JTree#setSelectionModel}. Todos sus metodos que cambiarian algo no hacen nada, asi que
     * el resto del codigo no necesita preguntar si hay modelo.
     */
    public static class EmptySelectionModel extends DefaultTreeSelectionModel {

        /** El unico; no guarda estado, asi que uno alcanza para todos los arboles. */
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
     * Reenvia los avisos del modelo de seleccion como si vinieran del arbol.
     *
     * <p>Cambia el origen del evento. Quien escucha al arbol espera que el evento diga que viene
     * del arbol, no de un modelo del que puede no saber nada.
     */
    public static class TreeSelectionRedirector implements Serializable, TreeSelectionListener {

        private final JTree arbol;

        protected TreeSelectionRedirector(JTree arbol) {
            this.arbol = arbol;
        }

        public void valueChanged(TreeSelectionEvent e) {
            TreeSelectionEvent newE = (TreeSelectionEvent) e.cloneWithSource(arbol);
            arbol.fireValueChanged(newE);
        }
    }

    /**
     * Escucha al modelo de datos y acomoda lo desplegado y lo elegido.
     *
     * <p>Es donde se limpia lo que quedo colgando: un subarbol que desaparece se lleva su estado de
     * desplegado y sus nodos elegidos.
     */
    public static class TreeModelHandler implements TreeModelListener {

        private final JTree arbol;

        protected TreeModelHandler(JTree arbol) {
            this.arbol = arbol;
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
                // Cambio la raiz: se empieza de cero, con la raiz desplegada.
                arbol.clearToggledPaths();
                Object treeRoot = arbol.getModel().getRoot();
                if (treeRoot != null && !arbol.getModel().isLeaf(treeRoot)) {
                    arbol.expandedState.put(new TreePath(treeRoot), Boolean.TRUE);
                }
            } else if (arbol.expandedState.get(parent) != null) {
                Vector<TreePath> toRemove = new Vector<TreePath>(1);
                boolean isExpanded = arbol.isExpanded(parent);
                toRemove.addElement(parent);
                arbol.removeDescendantToggledPaths(toRemove.elements());
                if (isExpanded) {
                    arbol.expandedState.put(parent, Boolean.TRUE);
                }
            }
            arbol.removeDescendantSelectedPaths(parent, false);
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
            arbol.removeDescendantToggledPaths(toRemove.elements());
            for (int counter = children.length - 1; counter >= 0; counter--) {
                arbol.removeDescendantSelectedPaths(
                        parent.pathByAddingChild(children[counter]), true);
            }
        }
    }

    /**
     * Un nodo que arma sus hijos recien cuando se lo despliega.
     *
     * <p>Es lo que usan los constructores que reciben un arreglo, un vector o una tabla: el arbol
     * se arma solo hasta donde se mira.
     */
    public static class DynamicUtilTreeNode extends DefaultMutableTreeNode {

        /** Si el valor original tenia hijos. */
        protected boolean hasChildren;

        /** El valor del que salen los hijos. */
        protected Object childValue;

        /** Si ya se armaron. */
        protected boolean loadedChildren;

        /** Arma los hijos de ese nodo a partir de ese valor. */
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

        /** Un nodo con ese valor, cuyos hijos salen de ese otro. */
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

        /** Arma los hijos; se llama sola la primera vez que se los pide. */
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
     * Donde caeria lo que se esta arrastrando.
     *
     * <p>{@link #getChildIndex} distingue soltar <em>sobre</em> un nodo de soltar <em>entre</em>
     * dos: -1 significa sobre el nodo del camino, y otro numero significa insertar en esa posicion
     * entre sus hijos.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final TreePath path;
        private final int index;

        DropLocation(java.awt.Point p, TreePath path, int index) {
            super(p);
            this.path = path;
            this.index = index;
        }

        /** En que posicion entre los hijos, o -1 si es sobre el nodo. */
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
