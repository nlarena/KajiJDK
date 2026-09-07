package javax.swing.table;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableHeaderUI;

/**
 * La barra de titulos de una tabla.
 *
 * <h2>Es un componente aparte, y por una razon concreta</h2>
 *
 * <p>Adentro de un panel con barras de desplazamiento, el encabezado va en la franja de arriba y la
 * tabla en la parte que se desplaza. Si fueran un solo componente, los titulos se irian hacia arriba
 * al bajar. Que sean dos es lo que hace que los titulos queden fijos.
 *
 * <p>De ahi que comparta el <em>modelo de columnas</em> con su tabla y no los datos: lo unico que
 * necesita saber es cuales columnas hay y cuanto miden.
 *
 * <h2>Dos permisos distintos</h2>
 *
 * <p>{@link #setReorderingAllowed} deja arrastrar una columna a otro lugar;
 * {@link #setResizingAllowed} deja cambiarle el ancho tirando del borde. Son independientes: es
 * comun dejar cambiar el ancho y no el orden.
 *
 * <h2>Lo que se ve mientras se arrastra</h2>
 *
 * <p>{@link #getDraggedColumn} y {@link #getDraggedDistance} son el estado de un arrastre en curso,
 * y los lee el aspecto para dibujar la columna a medio mover. Sin aspecto instalado nadie los
 * escribe, y quedan en nulo y cero.
 */
public class JTableHeader extends JComponent implements TableColumnModelListener, Accessible {

    private static final String uiClassID = "TableHeaderUI";

    /** La tabla a la que pertenece, o nulo. */
    protected JTable table;

    /** El modelo de columnas, compartido con la tabla. */
    protected TableColumnModel columnModel;

    /** Si se pueden arrastrar columnas a otro lugar. */
    protected boolean reorderingAllowed;

    /** Si se les puede cambiar el ancho. */
    protected boolean resizingAllowed;

    /** Si la tabla se reacomoda mientras se arrastra, o recien al soltar. */
    protected boolean updateTableInRealTime;

    /** La columna cuyo ancho se esta cambiando, o nulo. */
    protected transient TableColumn resizingColumn;

    /** La columna que se esta arrastrando, o nulo. */
    protected transient TableColumn draggedColumn;

    /** Cuanto se lleva arrastrada. */
    protected transient int draggedDistance;

    private TableCellRenderer defaultRenderer;

    /** Un encabezado con un modelo de columnas propio. */
    public JTableHeader() {
        this(null);
    }

    /** Un encabezado sobre ese modelo de columnas; nulo arma uno vacio. */
    public JTableHeader(TableColumnModel cm) {
        super();
        if (cm == null) {
            cm = createDefaultColumnModel();
        }
        setColumnModel(cm);
        initializeLocalVars();
        updateUI();
    }

    /** La tabla a la que pertenece; la pone la tabla, no el llamador. */
    public void setTable(JTable table) {
        JTable old = this.table;
        this.table = table;
        firePropertyChange("table", old, table);
    }

    public JTable getTable() {
        return table;
    }

    /** Si se pueden arrastrar columnas; ver la nota de la clase. */
    public void setReorderingAllowed(boolean reorderingAllowed) {
        boolean old = this.reorderingAllowed;
        this.reorderingAllowed = reorderingAllowed;
        firePropertyChange("reorderingAllowed", old, reorderingAllowed);
    }

    public boolean getReorderingAllowed() {
        return reorderingAllowed;
    }

    /** Si se les puede cambiar el ancho. */
    public void setResizingAllowed(boolean resizingAllowed) {
        boolean old = this.resizingAllowed;
        this.resizingAllowed = resizingAllowed;
        firePropertyChange("resizingAllowed", old, resizingAllowed);
    }

    public boolean getResizingAllowed() {
        return resizingAllowed;
    }

    public TableColumn getDraggedColumn() {
        return draggedColumn;
    }

    public int getDraggedDistance() {
        return draggedDistance;
    }

    public TableColumn getResizingColumn() {
        return resizingColumn;
    }

    /**
     * Si la tabla se reacomoda mientras se arrastra.
     *
     * @deprecated Como en el JDK: el valor se guarda y se devuelve, y ya no lo mira nadie.
     */
    @Deprecated
    public void setUpdateTableInRealTime(boolean flag) {
        updateTableInRealTime = flag;
    }

    /**
     * @deprecated Ver {@link #setUpdateTableInRealTime}.
     */
    @Deprecated
    public boolean getUpdateTableInRealTime() {
        return updateTableInRealTime;
    }

    /** El dibujante de los titulos; nulo devuelve la decision al aspecto. */
    public void setDefaultRenderer(TableCellRenderer defaultRenderer) {
        this.defaultRenderer = defaultRenderer;
    }

    public TableCellRenderer getDefaultRenderer() {
        return defaultRenderer;
    }

    /** La columna que cae en ese punto, o -1. */
    public int columnAtPoint(Point point) {
        int x = point.x;
        if (!getComponentOrientation().isLeftToRight()) {
            x = getWidthInRightToLeft() - x - 1;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    private int getWidthInRightToLeft() {
        if ((table != null) && (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF)) {
            return table.getWidth();
        }
        return super.getWidth();
    }

    /**
     * El rectangulo del titulo de esa columna.
     *
     * <p>Un indice fuera de rango devuelve un rectangulo vacio en la posicion que le tocaria, no un
     * error: el aspecto lo pide mientras dibuja y no tiene como saber donde termina.
     */
    public Rectangle getHeaderRect(int column) {
        Rectangle r = new Rectangle();
        TableColumnModel cm = getColumnModel();
        r.height = getHeight();
        if (column < 0) {
            if (!getComponentOrientation().isLeftToRight()) {
                r.x = getWidthInRightToLeft();
            }
        } else if (column >= cm.getColumnCount()) {
            if (getComponentOrientation().isLeftToRight()) {
                r.x = getWidth();
            }
        } else {
            for (int i = 0; i < column; i++) {
                r.x = r.x + cm.getColumn(i).getWidth();
            }
            if (!getComponentOrientation().isLeftToRight()) {
                r.x = getWidthInRightToLeft() - r.x - cm.getColumn(column).getWidth();
            }
            r.width = cm.getColumn(column).getWidth();
        }
        return r;
    }

    /** El texto de ayuda del titulo que esta bajo el puntero, si su dibujante da uno. */
    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int column = columnAtPoint(p);
        if (column != -1) {
            TableColumn aColumn = columnModel.getColumn(column);
            TableCellRenderer renderer = aColumn.getHeaderRenderer();
            if (renderer == null) {
                renderer = defaultRenderer;
            }
            if (renderer != null) {
                java.awt.Component component = renderer.getTableCellRendererComponent(
                        getTable(), aColumn.getHeaderValue(), false, false, -1, column);
                if (component instanceof JComponent) {
                    Rectangle cellRect = getHeaderRect(column);
                    p.translate(-cellRect.x, -cellRect.y);
                    MouseEvent newEvent = new MouseEvent(component, event.getID(),
                            event.getWhen(), event.getModifiersEx(), p.x, p.y,
                            event.getXOnScreen(), event.getYOnScreen(), event.getClickCount(),
                            event.isPopupTrigger(), MouseEvent.NOBUTTON);
                    tip = ((JComponent) component).getToolTipText(newEvent);
                }
            }
        }
        if (tip == null) {
            tip = getToolTipText();
        }
        return tip;
    }

    /**
     * Lo que ocupa: tan ancho como la suma de las columnas.
     *
     * <p>El alto lo decide el aspecto; sin aspecto queda en el que tenga puesto.
     */
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        long width = 0;
        java.util.Enumeration<TableColumn> enumeration = columnModel.getColumns();
        while (enumeration.hasMoreElements()) {
            TableColumn aColumn = enumeration.nextElement();
            width = width + aColumn.getPreferredWidth();
        }
        Dimension d = super.getPreferredSize();
        return new Dimension((int) Math.min(width, Integer.MAX_VALUE), d.height);
    }

    public TableHeaderUI getUI() {
        return (TableHeaderUI) ui;
    }

    public void setUI(TableHeaderUI ui) {
        if (this.ui != ui) {
            super.setUI(ui);
            repaint();
        }
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Cambia el modelo de columnas.
     *
     * @throws IllegalArgumentException si es nulo
     */
    public void setColumnModel(TableColumnModel columnModel) {
        if (columnModel == null) {
            throw new IllegalArgumentException("Cannot set a null ColumnModel");
        }
        TableColumnModel old = this.columnModel;
        if (columnModel != old) {
            if (old != null) {
                old.removeColumnModelListener(this);
            }
            this.columnModel = columnModel;
            columnModel.addColumnModelListener(this);
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    public void columnAdded(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        resizeAndRepaint();
    }

    public void columnMoved(TableColumnModelEvent e) {
        repaint();
    }

    public void columnMarginChanged(ChangeEvent e) {
        resizeAndRepaint();
    }

    /** No hace nada: el encabezado no muestra la seleccion de columnas. */
    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    /**
     * El dibujante de titulos de base.
     *
     * <p>En el JDK es una subclase de {@link DefaultTableCellRenderer} centrada y con el borde del
     * aspecto. Aca es el mismo dibujante de celda: sin aspecto instalado no hay borde de encabezado
     * que pedir, y el texto se ve igual.
     */
    protected TableCellRenderer createDefaultRenderer() {
        DefaultTableCellRenderer label = new DefaultTableCellRenderer.UIResource();
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return label;
    }

    /** Los valores de arranque; lo llama el constructor. */
    protected void initializeLocalVars() {
        setOpaque(true);
        table = null;
        reorderingAllowed = true;
        resizingAllowed = true;
        draggedColumn = null;
        draggedDistance = 0;
        resizingColumn = null;
        updateTableInRealTime = true;
        setDefaultRenderer(createDefaultRenderer());
    }

    /** Vuelve a medir y a dibujar. */
    public void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    /** Anota que columna se esta arrastrando; lo escribe el aspecto. */
    public void setDraggedColumn(TableColumn aColumn) {
        draggedColumn = aColumn;
    }

    /** Anota cuanto se lleva arrastrada. */
    public void setDraggedDistance(int distance) {
        draggedDistance = distance;
    }

    /** Anota que columna se esta redimensionando. */
    public void setResizingColumn(TableColumn aColumn) {
        resizingColumn = aColumn;
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
