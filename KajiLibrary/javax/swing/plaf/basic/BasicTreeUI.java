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
 * El aspecto basico de un arbol.
 *
 * <h2>El UI no sabe donde esta cada fila</h2>
 *
 * <p>Lo sabe {@link #treeState}, que es un {@link AbstractLayoutCache}: una tabla de que fila esta
 * en que coordenada. Todo lo que este UI contesta sobre posiciones -- {@link #getPathBounds},
 * {@link #getRowForPath}, {@link #getClosestPathForLocation} -- se lo pregunta a esa tabla.
 *
 * <p>Hay dos tablas y la eleccion importa: con filas de altura fija se usa
 * {@link FixedHeightLayoutCache}, que resuelve "que fila esta en la coordenada y" con una division;
 * con alturas distintas, {@link VariableHeightLayoutCache}, que tiene que recorrer. Un arbol de un
 * millon de nodos con la segunda es inusable, y por eso existe {@code largeModel}.
 *
 * <h2>La sangria son dos numeros, no uno</h2>
 *
 * <p>{@link #leftChildIndent} es lo que hay entre el borde del padre y el centro de la manija, y
 * {@link #rightChildIndent} entre el centro de la manija y el texto del hijo. La suma
 * -- {@link #totalChildIndent} -- es lo que se corre cada nivel. Estan separados porque la manija se
 * dibuja <em>en el medio</em>, y el aspecto que la quiera mas grande cambia uno solo.
 *
 * <p>{@link #depthOffset} es el ajuste por si la raiz se ve o no y por si tiene manija: un arbol sin
 * raiz visible tiene sus hijos al nivel cero, no al uno.
 *
 * <h2>El alto de fila cero</h2>
 *
 * <p>{@link #getRowHeight} devuelve cero, y eso no significa que las filas midan cero: significa
 * "cada una lo que necesite", que es lo que hace que la tabla de posiciones sea la de altura
 * variable. Un numero positivo ahi es lo que la vuelve fija. Medido.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Los dos iconos de manija -- expandido y colapsado -- vienen de la tabla del aspecto, que esta
 * biblioteca no tiene: quedan en {@code null} y el arbol se dibuja sin manijas. El ancho preferido
 * es por eso menor que el del JDK.
 *
 * <p>Editar una celda necesita un editor instalado y foco; los metodos estan y
 * {@link #isEditing} contesta que no.
 */
public class BasicTreeUI extends TreeUI {

    protected transient Icon collapsedIcon;
    protected transient Icon expandedIcon;

    /** Entre el borde del padre y el centro de la manija; ver la nota de la clase. */
    protected int leftChildIndent;

    /** Entre el centro de la manija y el texto del hijo. */
    protected int rightChildIndent;

    /** La suma de los dos. */
    protected int totalChildIndent;

    protected Dimension preferredMinSize;
    protected int lastSelectedRow;
    protected JTree tree;
    protected transient TreeCellRenderer currentCellRenderer;
    protected boolean createdRenderer;
    protected transient TreeCellEditor cellEditor;
    protected boolean createdCellEditor;

    /** Si al terminar de editar hay que parar el editor o cancelarlo. */
    protected boolean stopEditingInCompleteEditing;

    protected CellRendererPane rendererPane;
    protected Dimension preferredSize;
    protected boolean validCachedPreferredSize;

    /** La tabla de posiciones; ver la nota de la clase. */
    protected AbstractLayoutCache treeState;

    /** Los caminos ya dibujados, para no rehacer la cuenta en cada repintado. */
    protected Hashtable<TreePath, Boolean> drawingCache;

    protected boolean largeModel;
    protected AbstractLayoutCache.NodeDimensions nodeDimensions;
    protected TreeModel treeModel;
    protected TreeSelectionModel treeSelectionModel;

    /** El ajuste por raiz visible y manijas; ver la nota de la clase. */
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

    private static final ColorUIResource FONDO = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource LINEA = new ColorUIResource(184, 207, 229);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicTreeUI() {
        super();
    }

    /** Uno nuevo por arbol: guarda su tabla de posiciones. */
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

    /** Arma el estado antes de instalar nada. */
    protected void prepareForUIInstall() {
        drawingCache = new Hashtable<TreePath, Boolean>(7);
        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);
        lastSelectedRow = -1;
        preferredSize = new Dimension();
        stopEditingInCompleteEditing = true;
        setModel(tree.getModel());
    }

    /** Y termina de armarlo despues. */
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

    /** Colores, fuente y sangrias; los valores son los de {@code Tree.*} en Metal. */
    protected void installDefaults() {
        if (tree.getBackground() == null || tree.getBackground() instanceof UIResource) {
            tree.setBackground(FONDO);
        }
        if (getHashColor() == null || getHashColor() instanceof UIResource) {
            setHashColor(LINEA);
        }
        Font fuente = tree.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            tree.setFont(FUENTE);
        }
        setExpandedIcon(null);
        setCollapsedIcon(null);
        setLeftChildIndent(7);
        setRightChildIndent(13);
        LookAndFeel.installProperty(tree, "rowHeight", Integer.valueOf(0));
        LookAndFeel.installProperty(tree, "opaque", Boolean.TRUE);
        largeModel = (tree.isLargeModel() && tree.getRowHeight() > 0);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    /** Pone el panel donde se dibujan las celdas. */
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

    /** Sin atajos propios: las flechas las ata la tabla del aspecto. */
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

    /** Ninguno: la navegacion con teclas es de la tabla de acciones. */
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

    /** La de altura variable, salvo que el arbol pida el modelo grande. */
    protected AbstractLayoutCache createLayoutCache() {
        if (isLargeModel()) {
            return new FixedHeightLayoutCache();
        }
        return new VariableHeightLayoutCache();
    }

    protected AbstractLayoutCache.NodeDimensions createNodeDimensions() {
        return new NodeDimensionsHandler(this);
    }

    /** Le pasa a la tabla de posiciones el modelo, el dibujante y el alto de fila. */
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

    /** El del arbol; y si no tiene, uno de fabrica que se anota como propio. */
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
     * Un editor de fabrica, envuelto en el dibujante que ya haya.
     *
     * <p>Envolverlo importa: el editor dibuja el icono de la celda a la izquierda del campo de
     * texto, y lo saca del dibujante. Sin dibujante que le sirva, va sin icono.
     */
    protected TreeCellEditor createDefaultCellEditor() {
        if (currentCellRenderer instanceof DefaultTreeCellRenderer) {
            return new DefaultTreeCellEditor(tree, (DefaultTreeCellRenderer) currentCellRenderer);
        }
        return new DefaultTreeCellEditor(tree, null);
    }

    /**
     * El editor del arbol.
     *
     * <p>Toma el que el arbol tenga, sea o no editable: un editor puesto no se tira cuando el arbol
     * deja de ser editable, porque volver a serlo tiene que devolver el mismo. Solo fabrica uno
     * cuando no hay ninguno <em>y</em> el arbol es editable, y en ese caso -- y solo en ese --
     * {@link #createdCellEditor} queda en `true`, que es lo que despues autoriza a sacarlo. Medido.
     */
    protected void updateCellEditor() {
        completeEditing();
        if (tree == null) {
            cellEditor = null;
            return;
        }
        TreeCellEditor nuevo = tree.getCellEditor();
        if (nuevo == null && tree.isEditable()) {
            nuevo = createDefaultCellEditor();
            if (nuevo != null) {
                // Ponerlo avisa al arbol, que vuelve a entrar aca; por eso se asigna despues.
                tree.setCellEditor(nuevo);
                cellEditor = nuevo;
                createdCellEditor = true;
                return;
            }
        }
        cellEditor = nuevo;
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

    /** Cero: cada fila mide lo que necesite; ver la nota de la clase. */
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

    /** El ajuste por raiz visible y manijas; ver la nota de la clase. */
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

    /** Le cuenta a la tabla que caminos estan abiertos. */
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

    /** El ultimo hijo de ese camino, para saber hasta donde llega la linea vertical. */
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

    /** Cuanto se corre una fila segun su profundidad; ver la nota de la clase. */
    protected int getRowX(int row, int depth) {
        return totalChildIndent * (depth + depthOffset);
    }

    /** Cero: el basico no deja aire extra entre la manija y la linea. */
    protected int getHorizontalLegBuffer() {
        return 0;
    }

    /** Cero. */
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

    /** Marca el tamano como viejo y le avisa al arbol. */
    protected void updateSize() {
        validCachedPreferredSize = false;
        if (tree != null) {
            tree.revalidate();
        }
    }

    /** Rehace el tamano preferido a partir de la tabla de posiciones. */
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

    /** El de la tabla de posiciones, nunca menor que {@link #getPreferredMinSize}. */
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

    /** Cero: un arbol se puede achicar hasta desaparecer. */
    public Dimension getMinimumSize(JComponent c) {
        return new Dimension(0, 0);
    }

    /** El mismo que el preferido. */
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

    /** Termina la edicion como diga {@link #stopEditingInCompleteEditing}. */
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

    /** Arranca la edicion de ese camino; devuelve si pudo. */
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

    /** Abre o cierra ese camino. */
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

    /** Un click sobre la manija, no sobre el texto. */
    protected void handleExpandControlClick(TreePath path, int mouseX, int mouseY) {
        toggleExpandState(path);
    }

    /** Si ese punto cayo en la manija, la acciona. */
    protected void checkForClickInExpandControl(TreePath path, int mouseX, int mouseY) {
        if (isLocationInExpandControl(path, mouseX, mouseY)) {
            handleExpandControlClick(path, mouseX, mouseY);
        }
    }

    /**
     * Si ese punto cae en la manija de esa fila.
     *
     * <p>Solo mira la horizontal, y a proposito: la vertical ya la resolvio quien eligio el camino
     * a partir de la coordenada del click. Preguntar de nuevo por la vertical la haria mas
     * estricta que el resto del aspecto, y un click al ras del borde de una fila dejaria de abrir.
     *
     * <p>Una hoja no tiene manija, asi que nunca.
     *
     * <p>El ancho de la caja sale del icono expandido; cuando no hay -- que es el caso de esta
     * biblioteca, sin tabla de aspecto -- son ocho pixeles, que es lo que el JDK usa de reserva.
     *
     * <p>La caja va centrada en el punto que {@link #rightChildIndent} deja a la izquierda del
     * texto, y de ahi el {@code /2}: la manija se dibuja centrada en ese punto, no apoyada en el.
     * El intervalo es abierto por izquierda y cerrado por derecha -- medido -- asi que una caja de
     * dieciocho acepta dieciocho columnas y no diecinueve.
     */
    protected boolean isLocationInExpandControl(TreePath path, int mouseX, int mouseY) {
        if (tree == null || treeModel == null
                || treeModel.isLeaf(path.getLastPathComponent())) {
            return false;
        }
        int ancho = (getExpandedIcon() != null) ? getExpandedIcon().getIconWidth() : 8;
        Insets i = tree.getInsets();
        int izquierda = getRowX(tree.getRowForPath(path), path.getPathCount() - 1)
                - getRightChildIndent() - ancho / 2 + i.left;
        return mouseX > izquierda && mouseX <= izquierda + ancho;
    }

    /**
     * Corre el arbol para que ese tramo de filas se vea.
     *
     * <p>Cuando el tramo es una sola fila se pide su banda entera. Cuando son varias no se pide el
     * bloque completo: se pide desde la primera y tan alto como la parte visible, porque pedir mas
     * de lo que entra hace que el arbol muestre el final del tramo y esconda el principio, que es
     * justo al reves de lo que quiere quien acaba de abrir una rama.
     *
     * <p>El ancho pedido es uno solo: mover en horizontal no es asunto de este metodo.
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
        Rectangle primera = getPathBounds(tree, getPathForRow(tree, beginRow));
        if (primera == null) {
            return;
        }
        Rectangle visible = tree.getVisibleRect();
        tree.scrollRectToVisible(
                new Rectangle(visible.x, primera.y, 1, visible.height));
    }

    /** Si la fila lleva manija: solo las que tienen hijos. */
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

    /** Doble click: abre o cierra. */
    protected boolean isToggleEvent(MouseEvent event) {
        if (!javax.swing.SwingUtilities.isLeftMouseButton(event)) {
            return false;
        }
        int clickCount = tree.getToggleClickCount();
        return clickCount > 0 && event.getClickCount() == clickCount;
    }

    /** Elige lo que corresponda segun las teclas que esten apretadas. */
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
     * Donde apoya el texto de la primera fila.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
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
     * {@code CONSTANT_ASCENT}: la primera fila esta siempre arriba de todo.
     *
     * @throws NullPointerException si el componente es nulo
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

    /** Una fila: la manija si lleva, y la celda dibujada por el dibujante. */
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

    /** La manija de abrir o cerrar. */
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

    /** La parte vertical de la linea que une un padre con sus hijos. */
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

    /** Y la horizontal, del tronco a la fila. */
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

    /** Una linea punteada: un pixel si y uno no. */
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

    /** Dibuja un icono centrado en ese punto. */
    protected void drawCentered(Component c, Graphics graphics, Icon icon, int x, int y) {
        icon.paintIcon(c, graphics, x - icon.getIconWidth() / 2 - 1,
                y - icon.getIconHeight() / 2);
    }

    /** La linea que marca donde caeria algo que se esta arrastrando. */
    protected boolean isDropLine(JTree.DropLocation loc) {
        return loc != null && loc.getPath() != null && loc.getChildIndex() != -1;
    }

    protected Rectangle getDropLineRect(JTree.DropLocation loc) {
        return new Rectangle();
    }

    protected void paintDropLine(Graphics g) {
    }

    /**
     * Cuanto mide cada nodo; se lo pregunta la tabla de posiciones.
     *
     * <p>Estatica y con el UI como primer parametro, que es la firma que el JDK genera para una
     * clase interna; ver el hallazgo #518.
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
     * El que escucha todo: el modelo, la seleccion, la expansion, el mouse, el foco y el tamano.
     *
     * <p>Estatico y con el UI como campo, por lo mismo que en todo el paquete.
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
                // A la izquierda del texto: es la manija.
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
