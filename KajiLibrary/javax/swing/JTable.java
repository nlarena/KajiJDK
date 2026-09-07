package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TableUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Una tabla: filas y columnas de celdas.
 *
 * <h2>Cuatro modelos, no uno</h2>
 *
 * <p>Los <strong>datos</strong> ({@link TableModel}), las <strong>columnas</strong>
 * ({@link TableColumnModel}), la <strong>seleccion de filas</strong> ({@link ListSelectionModel}) y
 * -- adentro del modelo de columnas -- la seleccion de columnas. Estan separados porque cambian por
 * motivos distintos: mover una columna de lugar no toca los datos, y elegir una fila no toca
 * ninguno de los dos.
 *
 * <h2>Indices de vista e indices de modelo</h2>
 *
 * <p>Es lo que hay que tener presente todo el tiempo. Con un orden puesto o columnas movidas, la
 * fila 0 de la vista no es la fila 0 del modelo, ni la columna 2 de la vista la 2 del modelo.
 * <strong>Casi todos los metodos de esta clase hablan en indices de vista</strong> -- incluidos
 * {@link #getValueAt} y {@link #setValueAt}, que los traducen antes de tocar el modelo -- y los
 * cuatro {@code convertXxx} son el puente.
 *
 * <p>Usar un indice de vista contra el modelo no falla: devuelve otra celda. Es el error clasico con
 * tablas ordenables y no da ninguna senal.
 *
 * <h2>Un dibujante por tipo de columna</h2>
 *
 * <p>La tabla no tiene un componente por celda: tiene uno por <em>tipo</em>, y lo configura y lo
 * dibuja una vez por celda. Cual le toca a cada columna sale de {@code getColumnClass}, y de ahi que
 * un modelo que declara sus tipos se vea bien sin escribir una linea de dibujado.
 *
 * <h2>Editar es un estado, no una llamada</h2>
 *
 * <p>Mientras se edita, la tabla tiene un componente de verdad encima de la celda
 * ({@link #getEditorComponent}), y {@link #isEditing} es cierto. Termina cuando el editor avisa, y
 * la tabla escucha ese aviso: por eso implementa {@link CellEditorListener}.
 *
 * <h2>Lo que hace falta una pantalla para ver</h2>
 *
 * <p>El dibujado y el acomodado de columnas estan escritos y funcionan sobre cualquier
 * {@code Graphics}; lo que no hay es un aspecto instalado que ponga los colores, la grilla y el
 * encabezado. Con eso, {@link #getCellRenderer} devuelve el dibujante de base y la tabla se ve
 * gris. Es la brecha de siempre y no es de esta clase.
 */
public class JTable extends JComponent implements TableModelListener, Scrollable,
        TableColumnModelListener, ListSelectionListener, CellEditorListener, Accessible,
        RowSorterListener {

    private static final String uiClassID = "TableUI";

    /** Las columnas no se ajustan solas; aparece una barra horizontal. */
    public static final int AUTO_RESIZE_OFF = 0;

    /** Al agrandar una columna se achica la siguiente. */
    public static final int AUTO_RESIZE_NEXT_COLUMN = 1;

    /** Al agrandar una columna se reparte entre todas las que siguen. */
    public static final int AUTO_RESIZE_SUBSEQUENT_COLUMNS = 2;

    /** Al agrandar una columna se achica la ultima. */
    public static final int AUTO_RESIZE_LAST_COLUMN = 3;

    /** Al agrandar una columna se reparte entre todas. */
    public static final int AUTO_RESIZE_ALL_COLUMNS = 4;

    /** Los datos. */
    protected TableModel dataModel;

    /** Las columnas de la vista. */
    protected TableColumnModel columnModel;

    /** La seleccion de filas. */
    protected ListSelectionModel selectionModel;

    /** La barra de encabezados, o nulo si no se muestra. */
    protected JTableHeader tableHeader;

    /** El alto de una fila. */
    protected int rowHeight;

    /** El espacio vertical entre filas. */
    protected int rowMargin;

    /** El color de la grilla. */
    protected Color gridColor;

    /** Si se dibujan las lineas horizontales. */
    protected boolean showHorizontalLines;

    /** Si se dibujan las lineas verticales. */
    protected boolean showVerticalLines;

    /** Que hacer cuando cambia el ancho de una columna. */
    protected int autoResizeMode;

    /** Si las columnas se arman solas al cambiar el modelo. */
    protected boolean autoCreateColumnsFromModel;

    /** Cuanto pide medir cuando esta adentro de un panel con barras. */
    protected Dimension preferredViewportSize;

    /** Si se pueden elegir filas. */
    protected boolean rowSelectionAllowed;

    /** Si lo que se elige son celdas y no filas ni columnas enteras. */
    protected boolean cellSelectionEnabled;

    /** El componente con el que se esta editando, o nulo. */
    protected transient Component editorComp;

    /** El editor en uso, o nulo. */
    protected transient TableCellEditor cellEditor;

    /** La columna que se esta editando, o -1. */
    protected transient int editingColumn;

    /** La fila que se esta editando, o -1. */
    protected transient int editingRow;

    /** Los dibujantes por tipo de columna; ver la nota de la clase. */
    protected transient Hashtable<Object, Object> defaultRenderersByColumnClass;

    /** Los editores por tipo de columna. */
    protected transient Hashtable<Object, Object> defaultEditorsByColumnClass;

    /** El color del texto elegido. */
    protected Color selectionForeground;

    /** El fondo de lo elegido. */
    protected Color selectionBackground;

    private RowSorter<? extends TableModel> sortManager;
    private boolean autoCreateRowSorter;
    private boolean updateSelectionOnSort = true;
    private boolean fillsViewportHeight;
    private boolean surrendersFocusOnKeystroke;
    private boolean dragEnabled;
    private DropMode dropMode = DropMode.USE_SELECTION;
    private transient DropLocation dropLocation;
    private SizeSequence rowModel;

    /** Una tabla vacia, con un modelo por omision. */
    public JTable() {
        this(null, null, null);
    }

    /** Sobre esos datos. */
    public JTable(TableModel dm) {
        this(dm, null, null);
    }

    /** Sobre esos datos y esas columnas. */
    public JTable(TableModel dm, TableColumnModel cm) {
        this(dm, cm, null);
    }

    /**
     * Con los tres modelos.
     *
     * <p>Cualquiera de los tres en nulo se reemplaza por el de omision. Si no se dan columnas, se
     * arman a partir del modelo de datos.
     */
    public JTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super();
        setLayout(null);
        if (cm != null) {
            columnModel = cm;
            autoCreateColumnsFromModel = false;
        } else {
            columnModel = createDefaultColumnModel();
            autoCreateColumnsFromModel = true;
        }
        columnModel.addColumnModelListener(this);
        if (sm != null) {
            selectionModel = sm;
        } else {
            selectionModel = createDefaultSelectionModel();
        }
        selectionModel.addListSelectionListener(this);
        setTableHeader(createDefaultTableHeader());
        initializeLocalVars();
        if (dm != null) {
            setModel(dm);
        } else {
            setModel(createDefaultDataModel());
        }
        updateUI();
    }

    /** De ese tamano, con celdas vacias y editables. */
    public JTable(int numRows, int numColumns) {
        this(new DefaultTableModel(numRows, numColumns));
    }

    /** Con esos datos y esos nombres de columna. */
    public JTable(Vector<? extends Vector> rowData, Vector<?> columnNames) {
        this(new DefaultTableModel(rowData, columnNames));
    }

    /** Idem, con arreglos. */
    public JTable(final Object[][] rowData, final Object[] columnNames) {
        this(new AbstractTableModel() {
            public String getColumnName(int column) {
                return columnNames[column].toString();
            }

            public int getRowCount() {
                return rowData.length;
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            public Object getValueAt(int row, int col) {
                return rowData[row][col];
            }

            public boolean isCellEditable(int row, int column) {
                return true;
            }

            public void setValueAt(Object value, int row, int col) {
                rowData[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        });
    }

    /**
     * Al entrar en un panel con barras, le pone el encabezado arriba.
     *
     * <p>Es lo que hace que una tabla adentro de un {@code JScrollPane} muestre los titulos de
     * columna sin que nadie los agregue a mano.
     */
    public void addNotify() {
        super.addNotify();
        configureEnclosingScrollPane();
    }

    /** Ver {@link #addNotify}. */
    protected void configureEnclosingScrollPane() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            java.awt.Container gp = parent.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(getTableHeader());
            }
        }
    }

    /** Al salir, saca el encabezado. */
    public void removeNotify() {
        unconfigureEnclosingScrollPane();
        super.removeNotify();
    }

    /** Ver {@link #removeNotify}. */
    protected void unconfigureEnclosingScrollPane() {
        java.awt.Container parent = getParent();
        if (parent instanceof JViewport) {
            java.awt.Container gp = parent.getParent();
            if (gp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) gp;
                JViewport viewport = scrollPane.getViewport();
                if (viewport == null || viewport.getView() != this) {
                    return;
                }
                scrollPane.setColumnHeaderView(null);
            }
        }
    }

    /**
     * Un panel con barras que contiene esa tabla.
     *
     * @deprecated Usar {@code new JScrollPane(tabla)}, que hace lo mismo.
     */
    @Deprecated
    public static JScrollPane createScrollPaneForTable(JTable aTable) {
        return new JScrollPane(aTable);
    }

    /** La barra de titulos; nulo la saca. */
    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != tableHeader) {
            JTableHeader old = this.tableHeader;
            if (old != null) {
                old.setTable(null);
            }
            this.tableHeader = tableHeader;
            if (tableHeader != null) {
                tableHeader.setTable(this);
            }
            firePropertyChange("tableHeader", old, tableHeader);
        }
    }

    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    /**
     * El alto de todas las filas.
     *
     * @throws IllegalArgumentException si no es positivo
     */
    public void setRowHeight(int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        int old = this.rowHeight;
        this.rowHeight = rowHeight;
        rowModel = null;
        resizeAndRepaint();
        firePropertyChange("rowHeight", old, rowHeight);
    }

    public int getRowHeight() {
        return rowHeight;
    }

    /**
     * El alto de una fila sola.
     *
     * <p>Una tabla con filas de distinto alto es mas cara de dibujar y de recorrer: hasta que
     * alguien llama a esto, todas miden lo mismo y la fila de un pixel es una division.
     *
     * @throws IllegalArgumentException si el alto no es positivo o la fila esta fuera de rango
     */
    public void setRowHeight(int row, int rowHeight) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("New row height less than 1");
        }
        if (rowModel == null) {
            rowModel = new SizeSequence(getRowCount(), getRowHeight());
        }
        rowModel.setSize(row, rowHeight);
        resizeAndRepaint();
    }

    /**
     * El alto de esa fila.
     *
     * <p><strong>No valida el indice</strong>, y esta medido: con alturas por fila puestas, una
     * fila que no existe devuelve cero en vez de tirar. El aspecto lo pide mientras dibuja y no
     * tiene como saber donde termina.
     */
    public int getRowHeight(int row) {
        return (rowModel == null) ? getRowHeight() : rowModel.getSize(row);
    }

    /** El espacio vertical entre filas; sale del alto de la fila, no se suma. */
    public void setRowMargin(int rowMargin) {
        int old = this.rowMargin;
        this.rowMargin = rowMargin;
        resizeAndRepaint();
        firePropertyChange("rowMargin", old, rowMargin);
    }

    public int getRowMargin() {
        return rowMargin;
    }

    /** El espacio entre celdas: el ancho va al modelo de columnas, el alto aca. */
    public void setIntercellSpacing(Dimension intercellSpacing) {
        setRowMargin(intercellSpacing.height);
        getColumnModel().setColumnMargin(intercellSpacing.width);
        resizeAndRepaint();
    }

    public Dimension getIntercellSpacing() {
        return new Dimension(getColumnModel().getColumnMargin(), rowMargin);
    }

    public void setGridColor(Color gridColor) {
        if (gridColor == null) {
            throw new IllegalArgumentException("New color is null");
        }
        Color old = this.gridColor;
        this.gridColor = gridColor;
        firePropertyChange("gridColor", old, gridColor);
        repaint();
    }

    public Color getGridColor() {
        return gridColor;
    }

    /** Prende o apaga las dos direcciones de la grilla a la vez. */
    public void setShowGrid(boolean showGrid) {
        setShowHorizontalLines(showGrid);
        setShowVerticalLines(showGrid);
        repaint();
    }

    public void setShowHorizontalLines(boolean showHorizontalLines) {
        boolean old = this.showHorizontalLines;
        this.showHorizontalLines = showHorizontalLines;
        firePropertyChange("showHorizontalLines", old, showHorizontalLines);
        repaint();
    }

    public void setShowVerticalLines(boolean showVerticalLines) {
        boolean old = this.showVerticalLines;
        this.showVerticalLines = showVerticalLines;
        firePropertyChange("showVerticalLines", old, showVerticalLines);
        repaint();
    }

    public boolean getShowHorizontalLines() {
        return showHorizontalLines;
    }

    public boolean getShowVerticalLines() {
        return showVerticalLines;
    }

    /**
     * Que pasa con las demas columnas cuando una cambia de ancho.
     *
     * <p>Un modo que no es ninguno de los cinco se ignora en silencio, que es lo que hace el JDK.
     */
    public void setAutoResizeMode(int mode) {
        if ((mode == AUTO_RESIZE_OFF) || (mode == AUTO_RESIZE_NEXT_COLUMN)
                || (mode == AUTO_RESIZE_SUBSEQUENT_COLUMNS)
                || (mode == AUTO_RESIZE_LAST_COLUMN) || (mode == AUTO_RESIZE_ALL_COLUMNS)) {
            int old = autoResizeMode;
            autoResizeMode = mode;
            resizeAndRepaint();
            if (tableHeader != null) {
                tableHeader.resizeAndRepaint();
            }
            firePropertyChange("autoResizeMode", old, autoResizeMode);
        }
    }

    public int getAutoResizeMode() {
        return autoResizeMode;
    }

    /** Si cambiar el modelo rearma las columnas. */
    public void setAutoCreateColumnsFromModel(boolean autoCreateColumnsFromModel) {
        if (this.autoCreateColumnsFromModel != autoCreateColumnsFromModel) {
            boolean old = this.autoCreateColumnsFromModel;
            this.autoCreateColumnsFromModel = autoCreateColumnsFromModel;
            if (autoCreateColumnsFromModel) {
                createDefaultColumnsFromModel();
            }
            firePropertyChange("autoCreateColumnsFromModel", old, autoCreateColumnsFromModel);
        }
    }

    public boolean getAutoCreateColumnsFromModel() {
        return autoCreateColumnsFromModel;
    }

    /**
     * Tira las columnas y arma una por cada columna del modelo.
     *
     * <p>Se pierden los anchos ajustados a mano; es lo que hay que saber antes de llamarla.
     */
    public void createDefaultColumnsFromModel() {
        TableModel m = getModel();
        if (m != null) {
            TableColumnModel cm = getColumnModel();
            while (cm.getColumnCount() > 0) {
                cm.removeColumn(cm.getColumn(0));
            }
            for (int i = 0; i < m.getColumnCount(); i++) {
                TableColumn newColumn = new TableColumn(i);
                addColumn(newColumn);
            }
        }
    }

    /** El dibujante para ese tipo de columna. */
    public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        if (renderer != null) {
            defaultRenderersByColumnClass.put(columnClass, renderer);
        } else {
            defaultRenderersByColumnClass.remove(columnClass);
        }
    }

    /**
     * El dibujante de ese tipo, o el de su superclase si no hay uno propio.
     *
     * <p>Sube por la jerarquia: una columna de {@code Integer} sin dibujante propio usa el de
     * {@code Number}, y si tampoco hay, el de {@code Object}.
     */
    public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
        if (columnClass == null) {
            return null;
        }
        Object renderer = defaultRenderersByColumnClass.get(columnClass);
        if (renderer != null) {
            return (TableCellRenderer) renderer;
        }
        Class<?> c = columnClass.getSuperclass();
        if (c == null && columnClass != Object.class) {
            c = Object.class;
        }
        return getDefaultRenderer(c);
    }

    /** El editor para ese tipo de columna. */
    public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        if (editor != null) {
            defaultEditorsByColumnClass.put(columnClass, editor);
        } else {
            defaultEditorsByColumnClass.remove(columnClass);
        }
    }

    /** El editor de ese tipo, subiendo por la jerarquia como {@link #getDefaultRenderer}. */
    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        if (columnClass == null) {
            return null;
        }
        Object editor = defaultEditorsByColumnClass.get(columnClass);
        if (editor != null) {
            return (TableCellEditor) editor;
        }
        Class<?> c = columnClass.getSuperclass();
        if (c == null && columnClass != Object.class) {
            c = Object.class;
        }
        return getDefaultEditor(c);
    }

    public void setDragEnabled(boolean b) {
        dragEnabled = b;
    }

    public boolean getDragEnabled() {
        return dragEnabled;
    }

    /**
     * Como se interpreta una soltada sobre la tabla.
     *
     * @throws IllegalArgumentException si el modo no sirve para una tabla
     */
    public final void setDropMode(DropMode dropMode) {
        if (dropMode != null) {
            if (dropMode == DropMode.USE_SELECTION || dropMode == DropMode.ON
                    || dropMode == DropMode.INSERT || dropMode == DropMode.INSERT_ROWS
                    || dropMode == DropMode.INSERT_COLS || dropMode == DropMode.ON_OR_INSERT
                    || dropMode == DropMode.ON_OR_INSERT_ROWS
                    || dropMode == DropMode.ON_OR_INSERT_COLS) {
                this.dropMode = dropMode;
                return;
            }
        }
        throw new IllegalArgumentException(dropMode + ": Unsupported drop mode for table");
    }

    public final DropMode getDropMode() {
        return dropMode;
    }

    /** Donde caeria lo que se esta arrastrando, o nulo si no hay nada arrastrandose. */
    public final DropLocation getDropLocation() {
        return dropLocation;
    }

    /**
     * Si cambiar el modelo arma un ordenador de filas solo.
     *
     * <p>Apagado por omision: un ordenador cambia la numeracion de las filas, y prenderlo sin querer
     * rompe el codigo que usa indices de modelo contra la vista.
     */
    public void setAutoCreateRowSorter(boolean autoCreateRowSorter) {
        boolean old = this.autoCreateRowSorter;
        this.autoCreateRowSorter = autoCreateRowSorter;
        if (autoCreateRowSorter) {
            setRowSorter(new javax.swing.table.TableRowSorter<TableModel>(getModel()));
        }
        firePropertyChange("autoCreateRowSorter", old, autoCreateRowSorter);
    }

    public boolean getAutoCreateRowSorter() {
        return autoCreateRowSorter;
    }

    /** Si al reordenar se conserva lo elegido; prendido por omision. */
    public void setUpdateSelectionOnSort(boolean update) {
        if (updateSelectionOnSort != update) {
            updateSelectionOnSort = update;
            firePropertyChange("updateSelectionOnSort", !update, update);
        }
    }

    public boolean getUpdateSelectionOnSort() {
        return updateSelectionOnSort;
    }

    /** El ordenador de filas; nulo muestra las filas en el orden del modelo. */
    public void setRowSorter(RowSorter<? extends TableModel> sorter) {
        RowSorter<? extends TableModel> oldRowSorter = getRowSorter();
        if (oldRowSorter != null) {
            oldRowSorter.removeRowSorterListener(this);
        }
        sortManager = sorter;
        if (sorter != null) {
            sorter.addRowSorterListener(this);
        }
        firePropertyChange("rowSorter", oldRowSorter, sorter);
        firePropertyChange("sorter", oldRowSorter, sorter);
        clearSelection();
        resizeAndRepaint();
    }

    public RowSorter<? extends TableModel> getRowSorter() {
        return sortManager;
    }

    /** Una fila, un tramo, o cualquier cosa; ver {@link ListSelectionModel}. */
    public void setSelectionMode(int selectionMode) {
        clearSelection();
        getSelectionModel().setSelectionMode(selectionMode);
        getColumnModel().getSelectionModel().setSelectionMode(selectionMode);
    }

    public void setRowSelectionAllowed(boolean rowSelectionAllowed) {
        boolean old = this.rowSelectionAllowed;
        this.rowSelectionAllowed = rowSelectionAllowed;
        if (old != rowSelectionAllowed) {
            repaint();
        }
        firePropertyChange("rowSelectionAllowed", old, rowSelectionAllowed);
    }

    public boolean getRowSelectionAllowed() {
        return rowSelectionAllowed;
    }

    public void setColumnSelectionAllowed(boolean columnSelectionAllowed) {
        boolean old = columnModel.getColumnSelectionAllowed();
        columnModel.setColumnSelectionAllowed(columnSelectionAllowed);
        if (old != columnSelectionAllowed) {
            repaint();
        }
        firePropertyChange("columnSelectionAllowed", old, columnSelectionAllowed);
    }

    public boolean getColumnSelectionAllowed() {
        return columnModel.getColumnSelectionAllowed();
    }

    /**
     * Si lo que se elige son celdas.
     *
     * <p>Elegir celdas es tener prendidas las dos selecciones a la vez: una celda esta elegida
     * cuando su fila y su columna lo estan. No hay un tercer modelo.
     */
    public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
        setRowSelectionAllowed(cellSelectionEnabled);
        setColumnSelectionAllowed(cellSelectionEnabled);
        boolean old = this.cellSelectionEnabled;
        this.cellSelectionEnabled = cellSelectionEnabled;
        firePropertyChange("cellSelectionEnabled", old, cellSelectionEnabled);
    }

    public boolean getCellSelectionEnabled() {
        return getRowSelectionAllowed() && getColumnSelectionAllowed();
    }

    /** Elige todo lo que se pueda elegir. */
    public void selectAll() {
        if (isEditing()) {
            removeEditor();
        }
        if (getRowCount() > 0 && getColumnCount() > 0) {
            int oldLead = getSelectionModel().getLeadSelectionIndex();
            int oldAnchor = getSelectionModel().getAnchorSelectionIndex();
            int oldLeadCol = getColumnModel().getSelectionModel().getLeadSelectionIndex();
            int oldAnchorCol = getColumnModel().getSelectionModel().getAnchorSelectionIndex();
            setRowSelectionInterval(0, getRowCount() - 1);
            setColumnSelectionInterval(0, getColumnCount() - 1);
            // Se restituyen el ancla y la guia: elegir todo no tiene que mover el punto desde el
            // que el usuario venia extendiendo la seleccion.
            restoreSelection(getSelectionModel(), oldAnchor, oldLead);
            restoreSelection(getColumnModel().getSelectionModel(), oldAnchorCol, oldLeadCol);
        }
    }

    private static void restoreSelection(ListSelectionModel m, int anchor, int lead) {
        if (anchor == -1 || lead == -1) {
            return;
        }
        m.addSelectionInterval(anchor, anchor);
        m.addSelectionInterval(lead, lead);
    }

    public void clearSelection() {
        selectionModel.clearSelection();
        columnModel.getSelectionModel().clearSelection();
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void setRowSelectionInterval(int index0, int index1) {
        selectionModel.setSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void setColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().setSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void addRowSelectionInterval(int index0, int index1) {
        selectionModel.addSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void addColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().addSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void removeRowSelectionInterval(int index0, int index1) {
        selectionModel.removeSelectionInterval(boundRow(index0), boundRow(index1));
    }

    /**
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void removeColumnSelectionInterval(int index0, int index1) {
        columnModel.getSelectionModel().removeSelectionInterval(boundColumn(index0),
                boundColumn(index1));
    }

    private int boundRow(int row) {
        if (row < 0 || row >= getRowCount()) {
            throw new IllegalArgumentException("Row index out of range");
        }
        return row;
    }

    private int boundColumn(int col) {
        if (col < 0 || col >= getColumnCount()) {
            throw new IllegalArgumentException("Column index out of range");
        }
        return col;
    }

    /** La primera fila elegida, o -1. */
    public int getSelectedRow() {
        return selectionModel.getMinSelectionIndex();
    }

    /** La primera columna elegida, o -1. */
    public int getSelectedColumn() {
        return columnModel.getSelectionModel().getMinSelectionIndex();
    }

    public int[] getSelectedRows() {
        return indicesElegidos(selectionModel);
    }

    public int[] getSelectedColumns() {
        return columnModel.getSelectedColumns();
    }

    private static int[] indicesElegidos(ListSelectionModel m) {
        int iMin = m.getMinSelectionIndex();
        int iMax = m.getMaxSelectionIndex();
        if ((iMin == -1) || (iMax == -1)) {
            return new int[0];
        }
        int[] rvTmp = new int[1 + (iMax - iMin)];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (m.isSelectedIndex(i)) {
                rvTmp[n] = i;
                n = n + 1;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }

    public int getSelectedRowCount() {
        return getSelectedRows().length;
    }

    public int getSelectedColumnCount() {
        return columnModel.getSelectedColumnCount();
    }

    public boolean isRowSelected(int row) {
        return selectionModel.isSelectedIndex(row);
    }

    public boolean isColumnSelected(int column) {
        return columnModel.getSelectionModel().isSelectedIndex(column);
    }

    /** Una celda esta elegida cuando lo estan su fila y su columna; ver {@link #setCellSelectionEnabled}. */
    public boolean isCellSelected(int row, int column) {
        if (!getRowSelectionAllowed() && !getColumnSelectionAllowed()) {
            return false;
        }
        return (!getRowSelectionAllowed() || isRowSelected(row))
                && (!getColumnSelectionAllowed() || isColumnSelected(column));
    }

    /**
     * Lo que hace un clic sobre una celda.
     *
     * <p>Los dos booleanos son las dos teclas: {@code extend} es Mayusculas -- extiende desde el
     * ancla -- y {@code toggle} es Control -- suma o resta sin tocar el resto --. Con los dos en
     * falso, un clic limpio: se elige solo esa celda.
     */
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        ListSelectionModel rsm = getSelectionModel();
        ListSelectionModel csm = getColumnModel().getSelectionModel();
        if (extend) {
            if (toggle) {
                rsm.setAnchorSelectionIndex(rowIndex);
                csm.setAnchorSelectionIndex(columnIndex);
            } else {
                rsm.setLeadSelectionIndex(rowIndex);
                csm.setLeadSelectionIndex(columnIndex);
            }
        } else {
            if (toggle) {
                if (isCellSelected(rowIndex, columnIndex)) {
                    rsm.removeSelectionInterval(rowIndex, rowIndex);
                    csm.removeSelectionInterval(columnIndex, columnIndex);
                } else {
                    rsm.addSelectionInterval(rowIndex, rowIndex);
                    csm.addSelectionInterval(columnIndex, columnIndex);
                }
            } else {
                rsm.setSelectionInterval(rowIndex, rowIndex);
                csm.setSelectionInterval(columnIndex, columnIndex);
            }
        }
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public void setSelectionForeground(Color selectionForeground) {
        Color old = this.selectionForeground;
        this.selectionForeground = selectionForeground;
        firePropertyChange("selectionForeground", old, selectionForeground);
        if (old == null || !old.equals(selectionForeground)) {
            repaint();
        }
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public void setSelectionBackground(Color selectionBackground) {
        Color old = this.selectionBackground;
        this.selectionBackground = selectionBackground;
        firePropertyChange("selectionBackground", old, selectionBackground);
        if (old == null || !old.equals(selectionBackground)) {
            repaint();
        }
    }

    /**
     * La columna con ese identificador.
     *
     * @throws IllegalArgumentException si no hay ninguna
     */
    public TableColumn getColumn(Object identifier) {
        TableColumnModel cm = getColumnModel();
        int columnIndex = cm.getColumnIndex(identifier);
        return cm.getColumn(columnIndex);
    }

    /** De indice de vista a indice de modelo, para columnas. */
    public int convertColumnIndexToModel(int viewColumnIndex) {
        if (viewColumnIndex < 0) {
            return viewColumnIndex;
        }
        return getColumnModel().getColumn(viewColumnIndex).getModelIndex();
    }

    /** De indice de modelo a indice de vista; -1 si esa columna no se muestra. */
    public int convertColumnIndexToView(int modelColumnIndex) {
        if (modelColumnIndex < 0) {
            return modelColumnIndex;
        }
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < getColumnCount(); column++) {
            if (cm.getColumn(column).getModelIndex() == modelColumnIndex) {
                return column;
            }
        }
        return -1;
    }

    /**
     * De indice de modelo a indice de vista, para filas.
     *
     * @throws IndexOutOfBoundsException si esta fuera de rango
     */
    public int convertRowIndexToView(int modelRowIndex) {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.convertRowIndexToView(modelRowIndex);
        }
        return modelRowIndex;
    }

    /**
     * De indice de vista a indice de modelo, para filas.
     *
     * @throws IndexOutOfBoundsException si esta fuera de rango
     */
    public int convertRowIndexToModel(int viewRowIndex) {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.convertRowIndexToModel(viewRowIndex);
        }
        return viewRowIndex;
    }

    /** Cuantas filas se ven; con un filtro puesto, menos que las del modelo. */
    public int getRowCount() {
        RowSorter<? extends TableModel> sorter = getRowSorter();
        if (sorter != null) {
            return sorter.getViewRowCount();
        }
        return getModel().getRowCount();
    }

    public int getColumnCount() {
        return getColumnModel().getColumnCount();
    }

    /** El nombre de esa columna de la vista. */
    public String getColumnName(int column) {
        return getModel().getColumnName(convertColumnIndexToModel(column));
    }

    /** El tipo de esa columna de la vista. */
    public Class<?> getColumnClass(int column) {
        return getModel().getColumnClass(convertColumnIndexToModel(column));
    }

    /** El valor de esa celda de la vista; traduce los dos indices. */
    public Object getValueAt(int row, int column) {
        return getModel().getValueAt(convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /** Cambia esa celda de la vista; traduce los dos indices. */
    public void setValueAt(Object aValue, int row, int column) {
        getModel().setValueAt(aValue, convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    public boolean isCellEditable(int row, int column) {
        return getModel().isCellEditable(convertRowIndexToModel(row),
                convertColumnIndexToModel(column));
    }

    /** Agrega una columna al final de la vista. */
    public void addColumn(TableColumn aColumn) {
        if (aColumn.getHeaderValue() == null) {
            int modelColumn = aColumn.getModelIndex();
            String columnName = getModel().getColumnName(modelColumn);
            aColumn.setHeaderValue(columnName);
        }
        getColumnModel().addColumn(aColumn);
    }

    public void removeColumn(TableColumn aColumn) {
        getColumnModel().removeColumn(aColumn);
    }

    public void moveColumn(int column, int targetColumn) {
        getColumnModel().moveColumn(column, targetColumn);
    }

    /** La columna que cae en ese punto, o -1. */
    public int columnAtPoint(Point point) {
        int x = point.x;
        if (!getComponentOrientation().isLeftToRight()) {
            x = getWidth() - x - 1;
        }
        return getColumnModel().getColumnIndexAtX(x);
    }

    /** La fila que cae en ese punto, o -1. */
    public int rowAtPoint(Point point) {
        int y = point.y;
        int result = (rowModel == null) ? y / getRowHeight() : filaEn(y);
        if (result < 0) {
            return -1;
        } else if (result >= getRowCount()) {
            return -1;
        }
        return result;
    }

    /** La fila que cae en ese pixel cuando las filas miden distinto. */
    private int filaEn(int y) {
        if (y < 0) {
            return -1;
        }
        int acumulado = 0;
        int n = getRowCount();
        for (int i = 0; i < n; i++) {
            acumulado = acumulado + getRowHeight(i);
            if (y < acumulado) {
                return i;
            }
        }
        return n;
    }

    /**
     * El rectangulo de esa celda de la vista.
     *
     * <p>Con {@code includeSpacing} en falso se le sacan los margenes, que es lo que ocupa el
     * contenido; con cierto, la celda entera incluida la linea de grilla.
     */
    public Rectangle getCellRect(int row, int column, boolean includeSpacing) {
        Rectangle r = new Rectangle();
        boolean valid = true;
        if (row < 0 || row >= getRowCount()) {
            valid = false;
        } else {
            r.height = getRowHeight(row);
            r.y = 0;
            for (int i = 0; i < row; i++) {
                r.y = r.y + getRowHeight(i);
            }
        }
        if (column < 0 || column >= getColumnCount()) {
            valid = false;
        } else {
            TableColumnModel cm = getColumnModel();
            if (getComponentOrientation().isLeftToRight()) {
                for (int i = 0; i < column; i++) {
                    r.x = r.x + cm.getColumn(i).getWidth();
                }
            } else {
                for (int i = cm.getColumnCount() - 1; i > column; i--) {
                    r.x = r.x + cm.getColumn(i).getWidth();
                }
            }
            r.width = cm.getColumn(column).getWidth();
        }
        if (valid && !includeSpacing) {
            int rm = getRowMargin();
            int cm2 = getColumnModel().getColumnMargin();
            r.setBounds(r.x + cm2 / 2, r.y + rm / 2, r.width - cm2, r.height - rm);
        }
        return r;
    }

    /** Acomoda las columnas segun el modo de ajuste. */
    public void doLayout() {
        TableColumn resizingColumn = getResizingColumn();
        if (resizingColumn == null) {
            setWidthsFromPreferredWidths(false);
        } else {
            int columnIndex = viewIndexForColumn(resizingColumn);
            int delta = getWidth() - getColumnModel().getTotalColumnWidth();
            accommodateDelta(columnIndex, delta);
            delta = getWidth() - getColumnModel().getTotalColumnWidth();
            if (delta != 0) {
                resizingColumn.setWidth(resizingColumn.getWidth() + delta);
            }
            setWidthsFromPreferredWidths(true);
        }
        super.doLayout();
    }

    private TableColumn getResizingColumn() {
        return (tableHeader == null) ? null : tableHeader.getResizingColumn();
    }

    private int viewIndexForColumn(TableColumn aColumn) {
        TableColumnModel cm = getColumnModel();
        for (int column = 0; column < cm.getColumnCount(); column++) {
            if (cm.getColumn(column) == aColumn) {
                return column;
            }
        }
        return -1;
    }

    private void setWidthsFromPreferredWidths(boolean inverse) {
        int totalWidth = getWidth();
        int totalPreferred = 0;
        TableColumnModel cm = getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            totalPreferred = totalPreferred + cm.getColumn(i).getPreferredWidth();
        }
        if (getAutoResizeMode() == AUTO_RESIZE_OFF || totalPreferred == 0) {
            for (int i = 0; i < cm.getColumnCount(); i++) {
                TableColumn c = cm.getColumn(i);
                c.setWidth(c.getPreferredWidth());
            }
            return;
        }
        // Se reparte proporcionalmente al preferido, que es lo que hace que una columna que pidio
        // el doble se lleve el doble del espacio de mas.
        int repartido = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn c = cm.getColumn(i);
            int w;
            if (i == cm.getColumnCount() - 1) {
                w = totalWidth - repartido;
            } else {
                w = (int) ((long) c.getPreferredWidth() * totalWidth / totalPreferred);
            }
            c.setWidth(w);
            repartido = repartido + c.getWidth();
        }
    }

    private void accommodateDelta(int resizingColumnIndex, int delta) {
        // El reparto fino lo hace el aspecto al arrastrar; aca alcanza con volver a acomodar.
        setWidthsFromPreferredWidths(false);
    }

    /**
     * Ajusta los anchos.
     *
     * @deprecated Usar {@link #doLayout}.
     */
    @Deprecated
    public void sizeColumnsToFit(boolean lastColumnOnly) {
        int oldAutoResizeMode = autoResizeMode;
        setAutoResizeMode(lastColumnOnly ? AUTO_RESIZE_LAST_COLUMN : AUTO_RESIZE_ALL_COLUMNS);
        sizeColumnsToFit(-1);
        setAutoResizeMode(oldAutoResizeMode);
    }

    /** Ajusta los anchos tomando esa columna como la que se esta redimensionando. */
    public void sizeColumnsToFit(int resizingColumn) {
        if (resizingColumn == -1) {
            setWidthsFromPreferredWidths(false);
        } else {
            if (autoResizeMode == AUTO_RESIZE_OFF) {
                TableColumn aColumn = getColumnModel().getColumn(resizingColumn);
                aColumn.setPreferredWidth(aColumn.getWidth());
            } else {
                setWidthsFromPreferredWidths(true);
            }
        }
    }

    /** El texto de ayuda de la celda que esta bajo el puntero, si el dibujante da uno. */
    public String getToolTipText(MouseEvent event) {
        String tip = null;
        Point p = event.getPoint();
        int hitColumnIndex = columnAtPoint(p);
        int hitRowIndex = rowAtPoint(p);
        if ((hitColumnIndex != -1) && (hitRowIndex != -1)) {
            TableCellRenderer renderer = getCellRenderer(hitRowIndex, hitColumnIndex);
            Component component = prepareRenderer(renderer, hitRowIndex, hitColumnIndex);
            if (component instanceof JComponent) {
                Rectangle cellRect = getCellRect(hitRowIndex, hitColumnIndex, false);
                p.translate(-cellRect.x, -cellRect.y);
                MouseEvent newEvent = new MouseEvent(component, event.getID(), event.getWhen(),
                        event.getModifiersEx(), p.x, p.y, event.getXOnScreen(),
                        event.getYOnScreen(), event.getClickCount(), event.isPopupTrigger(),
                        MouseEvent.NOBUTTON);
                tip = ((JComponent) component).getToolTipText(newEvent);
            }
        }
        if (tip == null) {
            tip = getToolTipText();
        }
        return tip;
    }

    /** Si al empezar a editar el foco pasa al editor. */
    public void setSurrendersFocusOnKeystroke(boolean surrendersFocusOnKeystroke) {
        this.surrendersFocusOnKeystroke = surrendersFocusOnKeystroke;
    }

    public boolean getSurrendersFocusOnKeystroke() {
        return surrendersFocusOnKeystroke;
    }

    /** Empieza a editar esa celda, sin ningun gesto detras. */
    public boolean editCellAt(int row, int column) {
        return editCellAt(row, column, null);
    }

    /**
     * Empieza a editar esa celda por ese gesto.
     *
     * @return si la edicion empezo
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        if (cellEditor != null && !cellEditor.stopCellEditing()) {
            return false;
        }
        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }
        if (!isCellEditable(row, column)) {
            return false;
        }
        TableCellEditor editor = getCellEditor(row, column);
        if (editor != null && editor.isCellEditable(e)) {
            editorComp = prepareEditor(editor, row, column);
            if (editorComp == null) {
                removeEditor();
                return false;
            }
            editorComp.setBounds(getCellRect(row, column, false));
            add(editorComp);
            editorComp.validate();
            setCellEditor(editor);
            setEditingRow(row);
            setEditingColumn(column);
            editor.addCellEditorListener(this);
            return true;
        }
        return false;
    }

    public boolean isEditing() {
        return cellEditor != null;
    }

    public Component getEditorComponent() {
        return editorComp;
    }

    public int getEditingColumn() {
        return editingColumn;
    }

    public int getEditingRow() {
        return editingRow;
    }

    public TableUI getUI() {
        return (TableUI) ui;
    }

    public void setUI(TableUI ui) {
        if (this.ui != ui) {
            super.setUI(ui);
            repaint();
        }
    }

    /** Vuelve a pedir el aspecto, y les avisa al encabezado y a los editores. */
    public void updateUI() {
        // El encabezado se actualiza solo; los dibujantes y editores de base se rearman porque sus
        // colores salen del aspecto.
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.updateUI();
        }
        createDefaultRenderers();
        createDefaultEditors();
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Cambia los datos.
     *
     * <p>Si las columnas se arman solas, se rearman: el modelo nuevo puede tener otras.
     *
     * @throws IllegalArgumentException si el modelo es nulo
     */
    public void setModel(TableModel dataModel) {
        if (dataModel == null) {
            throw new IllegalArgumentException("Cannot set a null TableModel");
        }
        if (this.dataModel != dataModel) {
            TableModel old = this.dataModel;
            if (old != null) {
                old.removeTableModelListener(this);
            }
            this.dataModel = dataModel;
            dataModel.addTableModelListener(this);
            tableChanged(new TableModelEvent(dataModel, TableModelEvent.HEADER_ROW));
            firePropertyChange("model", old, dataModel);
            if (getAutoCreateRowSorter()) {
                setRowSorter(new javax.swing.table.TableRowSorter<TableModel>(dataModel));
            }
        }
    }

    public TableModel getModel() {
        return dataModel;
    }

    /**
     * @throws IllegalArgumentException si el modelo es nulo
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
            if (tableHeader != null) {
                tableHeader.setColumnModel(columnModel);
            }
            firePropertyChange("columnModel", old, columnModel);
            resizeAndRepaint();
        }
    }

    public TableColumnModel getColumnModel() {
        return columnModel;
    }

    /**
     * @throws IllegalArgumentException si el modelo es nulo
     */
    public void setSelectionModel(ListSelectionModel newModel) {
        if (newModel == null) {
            throw new IllegalArgumentException("Cannot set a null SelectionModel");
        }
        ListSelectionModel oldModel = selectionModel;
        if (newModel != oldModel) {
            if (oldModel != null) {
                oldModel.removeListSelectionListener(this);
            }
            selectionModel = newModel;
            newModel.addListSelectionListener(this);
            firePropertyChange("selectionModel", oldModel, newModel);
            repaint();
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    /** El orden cambio: la seleccion se conserva si asi se pidio. */
    public void sorterChanged(RowSorterEvent e) {
        if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
            if (getUpdateSelectionOnSort()) {
                // La seleccion es de indices de vista, y la vista se reordeno.
                clearSelection();
            }
        }
        resizeAndRepaint();
    }

    /**
     * Los datos cambiaron.
     *
     * <p>Un cambio de estructura -- fila {@link TableModelEvent#HEADER_ROW} -- rearma las columnas
     * si asi esta pedido, y limpia la seleccion: los indices ya no significan lo mismo.
     */
    public void tableChanged(TableModelEvent e) {
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            clearSelectionAndLeadAnchor();
            rowModel = null;
            if (getAutoCreateColumnsFromModel()) {
                createDefaultColumnsFromModel();
                return;
            }
            resizeAndRepaint();
            return;
        }
        if (e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
            rowModel = null;
        }
        resizeAndRepaint();
    }

    private void clearSelectionAndLeadAnchor() {
        selectionModel.setValueIsAdjusting(true);
        columnModel.getSelectionModel().setValueIsAdjusting(true);
        clearSelection();
        selectionModel.setAnchorSelectionIndex(-1);
        selectionModel.setLeadSelectionIndex(-1);
        columnModel.getSelectionModel().setAnchorSelectionIndex(-1);
        columnModel.getSelectionModel().setLeadSelectionIndex(-1);
        selectionModel.setValueIsAdjusting(false);
        columnModel.getSelectionModel().setValueIsAdjusting(false);
    }

    public void columnAdded(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    public void columnRemoved(TableColumnModelEvent e) {
        if (isEditing()) {
            removeEditor();
        }
        resizeAndRepaint();
    }

    public void columnMoved(TableColumnModelEvent e) {
        if (isEditing() && !getCellEditor().stopCellEditing()) {
            getCellEditor().cancelCellEditing();
        }
        repaint();
    }

    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing() && !getCellEditor().stopCellEditing()) {
            getCellEditor().cancelCellEditing();
        }
        TableColumn resizingColumn = getResizingColumn();
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }
        resizeAndRepaint();
    }

    public void columnSelectionChanged(ListSelectionEvent e) {
        repaint();
    }

    public void valueChanged(ListSelectionEvent e) {
        repaint();
    }

    /** El editor termino: se guarda el valor y se lo saca. */
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            setValueAt(value, editingRow, editingColumn);
            removeEditor();
        }
    }

    /** El editor se cancelo: no se guarda nada. */
    public void editingCanceled(ChangeEvent e) {
        removeEditor();
    }

    public void setPreferredScrollableViewportSize(Dimension size) {
        preferredViewportSize = size;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return preferredViewportSize;
    }

    /** Un paso de scroll es una fila o una columna, segun la direccion. */
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            int row = rowAtPoint(new Point(0, visibleRect.y));
            if (row == -1) {
                return 0;
            }
            return getRowHeight(row);
        }
        int col = columnAtPoint(new Point(visibleRect.x, 0));
        if (col == -1) {
            return 100;
        }
        return getColumnModel().getColumn(col).getWidth();
    }

    /** Un bloque de scroll es lo que se ve. */
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation,
            int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height;
        }
        return visibleRect.width;
    }

    /** Cierto salvo con el ajuste apagado: entonces aparece la barra horizontal. */
    public boolean getScrollableTracksViewportWidth() {
        return getAutoResizeMode() != AUTO_RESIZE_OFF;
    }

    /**
     * Si la tabla se estira para llenar el alto visible.
     *
     * <p>Apagado por omision, y por eso una tabla con pocas filas deja ver el fondo del panel
     * debajo. {@link #setFillsViewportHeight} lo cambia.
     */
    public boolean getScrollableTracksViewportHeight() {
        java.awt.Container parent = getParent();
        return getFillsViewportHeight() && parent instanceof JViewport
                && parent.getHeight() > getPreferredSize().height;
    }

    public void setFillsViewportHeight(boolean fillsViewportHeight) {
        boolean old = this.fillsViewportHeight;
        this.fillsViewportHeight = fillsViewportHeight;
        resizeAndRepaint();
        firePropertyChange("fillsViewportHeight", old, fillsViewportHeight);
    }

    public boolean getFillsViewportHeight() {
        return fillsViewportHeight;
    }

    /**
     * Le da la tecla al editor si hay uno abierto, y si no la resuelve como cualquier componente.
     */
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
            boolean pressed) {
        boolean retValue = super.processKeyBinding(ks, e, condition, pressed);
        if (!retValue && condition == WHEN_ANCESTOR_OF_FOCUSED_COMPONENT && isFocusOwner()) {
            return false;
        }
        return retValue;
    }

    /**
     * Arma los dibujantes de base, uno por tipo.
     *
     * <p>Los cuatro del JDK: texto para {@link Object}, numeros alineados a la derecha, fechas
     * formateadas y un tilde para los booleanos. Son clases anidadas privadas y llevan los mismos
     * nombres que en el JDK a proposito: el nombre se ve por {@code getClass()}, asi que cambiarlo
     * seria una diferencia observable sin ninguna ganancia.
     */
    protected void createDefaultRenderers() {
        defaultRenderersByColumnClass = new Hashtable<Object, Object>(8);
        defaultRenderersByColumnClass.put(Object.class,
                new DefaultTableCellRenderer.UIResource());
        defaultRenderersByColumnClass.put(Number.class, new NumberRenderer());
        defaultRenderersByColumnClass.put(Float.class, new DoubleRenderer());
        defaultRenderersByColumnClass.put(Double.class, new DoubleRenderer());
        defaultRenderersByColumnClass.put(java.util.Date.class, new DateRenderer());
        defaultRenderersByColumnClass.put(Icon.class, new IconRenderer());
        // El JDK registra tambien `ImageIcon.class`, que esta biblioteca todavia no tiene. No se lo
        // nombra: un literal de clase de un tipo inexistente compila y da `Object.class` -- ver el
        // hallazgo #520 --, y esa linea pisaba al dibujante de Object.
        defaultRenderersByColumnClass.put(Boolean.class, new BooleanRenderer());
    }

    /**
     * Arma los editores de base.
     *
     * <p>El de {@link Object} no es un editor de texto pelado: es uno que <strong>construye el
     * valor del tipo de la columna a partir de lo escrito</strong>, buscando su constructor de un
     * {@code String}. Es lo que hace que escribir 77 en una columna de {@code Integer} devuelva un
     * {@code Integer} y no la cadena "77", y que escribir "abc" ahi no se guarde.
     */
    protected void createDefaultEditors() {
        defaultEditorsByColumnClass = new Hashtable<Object, Object>(3);
        defaultEditorsByColumnClass.put(Object.class, new GenericEditor());
        defaultEditorsByColumnClass.put(Number.class, new NumberEditor());
        defaultEditorsByColumnClass.put(Boolean.class, new BooleanEditor());
    }

    /** Numeros a la derecha, que es como se leen alineados. */
    private static class NumberRenderer extends DefaultTableCellRenderer.UIResource {

        NumberRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    /** Como el de numeros, pero formateando los decimales del idioma. */
    private static class DoubleRenderer extends NumberRenderer {

        private final java.text.NumberFormat formatter =
                java.text.NumberFormat.getInstance();

        DoubleRenderer() {
            super();
        }

        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    /** Fechas con el formato del idioma. */
    private static class DateRenderer extends DefaultTableCellRenderer.UIResource {

        private final java.text.DateFormat formatter =
                java.text.DateFormat.getDateInstance();

        DateRenderer() {
            super();
        }

        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }

    /** Un icono centrado, sin texto. */
    private static class IconRenderer extends DefaultTableCellRenderer.UIResource {

        IconRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        public void setValue(Object value) {
            setIcon((value instanceof Icon) ? (Icon) value : null);
        }
    }

    /** Un tilde centrado; el unico que no es una etiqueta. */
    private static class BooleanRenderer extends JCheckBox implements TableCellRenderer,
            javax.swing.plaf.UIResource {

        BooleanRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            setSelected((value != null && ((Boolean) value).booleanValue()));
            return this;
        }
    }

    /**
     * El editor de texto que construye el valor del tipo de la columna.
     *
     * <p>Busca el constructor que toma un {@code String} y lo llama con lo escrito. Si no hay tal
     * constructor, no se puede editar esa columna; si lo hay y lo escrito no sirve, la edicion no
     * termina y el campo se marca en rojo -- que es como el JDK dice "corregilo".
     */
    private static class GenericEditor extends DefaultCellEditor {

        private final Class<?>[] argTypes = new Class<?>[] {String.class};
        private java.lang.reflect.Constructor<?> constructor;
        private Object value;

        GenericEditor() {
            super(new JTextField());
            getComponent().setName("Table.editor");
        }

        public boolean stopCellEditing() {
            String s = (String) super.getCellEditorValue();
            try {
                if ("".equals(s)) {
                    if (constructor.getDeclaringClass() == String.class) {
                        value = s;
                    }
                    return super.stopCellEditing();
                }
                value = constructor.newInstance(new Object[] {s});
            } catch (Exception e) {
                ((JComponent) getComponent()).setBorder(
                        new javax.swing.border.LineBorder(Color.red));
                return false;
            }
            return super.stopCellEditing();
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.value = null;
            ((JComponent) getComponent()).setBorder(
                    new javax.swing.border.LineBorder(Color.black));
            try {
                Class<?> type = table.getColumnClass(column);
                // Una columna de Object acepta un String: un String es un Object.
                if (type == Object.class) {
                    type = String.class;
                }
                constructor = type.getConstructor(argTypes);
            } catch (Exception e) {
                return null;
            }
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public Object getCellEditorValue() {
            return value;
        }
    }

    /** El mismo, con el texto a la derecha. */
    private static class NumberEditor extends GenericEditor {

        NumberEditor() {
            super();
            ((JTextField) getComponent()).setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    /** Un tilde. */
    private static class BooleanEditor extends DefaultCellEditor {

        BooleanEditor() {
            super(new JCheckBox());
            JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    /** Los valores de arranque; los llama el constructor. */
    protected void initializeLocalVars() {
        setOpaque(true);
        createDefaultRenderers();
        createDefaultEditors();
        setTableHeader(createDefaultTableHeader());
        setShowGrid(true);
        setAutoResizeMode(AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setRowHeight(16);
        setRowMargin(1);
        setRowSelectionAllowed(true);
        setCellEditor(null);
        setEditingColumn(-1);
        setEditingRow(-1);
        setSurrendersFocusOnKeystroke(false);
        setPreferredScrollableViewportSize(new Dimension(450, 400));
        // Los tres colores que en el JDK pone el aspecto y no el constructor. Aca se ponen
        // marcados como del aspecto y con los valores medidos en Metal (JDK 25), por dos motivos:
        // sin ellos una tabla sin aspecto instalado dibujaria con nulos, y marcados los reemplaza
        // `BasicTableUI.installDefaults` sin pisar los que ponga el programa.
        setGridColor(new javax.swing.plaf.ColorUIResource(122, 138, 153));
        setSelectionForeground(new javax.swing.plaf.ColorUIResource(51, 51, 51));
        setSelectionBackground(new javax.swing.plaf.ColorUIResource(184, 207, 229));
    }

    protected TableModel createDefaultDataModel() {
        return new DefaultTableModel();
    }

    protected TableColumnModel createDefaultColumnModel() {
        return new DefaultTableColumnModel();
    }

    protected ListSelectionModel createDefaultSelectionModel() {
        return new DefaultListSelectionModel();
    }

    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel);
    }

    /** Vuelve a medir y a dibujar. */
    protected void resizeAndRepaint() {
        revalidate();
        repaint();
    }

    public TableCellEditor getCellEditor() {
        return cellEditor;
    }

    public void setCellEditor(TableCellEditor anEditor) {
        TableCellEditor old = cellEditor;
        cellEditor = anEditor;
        firePropertyChange("tableCellEditor", old, anEditor);
    }

    public void setEditingColumn(int aColumn) {
        editingColumn = aColumn;
    }

    public void setEditingRow(int aRow) {
        editingRow = aRow;
    }

    /**
     * El dibujante de esa celda.
     *
     * <p>Primero el de la columna, si tiene uno propio; si no, el del tipo de la columna.
     */
    public TableCellRenderer getCellRenderer(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        if (renderer == null) {
            renderer = getDefaultRenderer(getColumnClass(column));
        }
        return renderer;
    }

    /** El dibujante ya configurado para esa celda. */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Object value = getValueAt(row, column);
        boolean isSelected = false;
        boolean hasFocus = false;
        if (!isPaintingForPrint()) {
            isSelected = isCellSelected(row, column);
            boolean rowIsLead = (selectionModel.getLeadSelectionIndex() == row);
            boolean colIsLead = (columnModel.getSelectionModel().getLeadSelectionIndex()
                    == column);
            hasFocus = (rowIsLead && colIsLead) && isFocusOwner();
        }
        return renderer.getTableCellRendererComponent(this, value, isSelected, hasFocus, row,
                column);
    }

    /** El editor de esa celda; como {@link #getCellRenderer}, primero el de la columna. */
    public TableCellEditor getCellEditor(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellEditor editor = tableColumn.getCellEditor();
        if (editor == null) {
            editor = getDefaultEditor(getColumnClass(column));
        }
        return editor;
    }

    /** El editor ya cargado con el valor de esa celda. */
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Object value = getValueAt(row, column);
        boolean isSelected = isCellSelected(row, column);
        Component comp = editor.getTableCellEditorComponent(this, value, isSelected, row,
                column);
        if (comp instanceof JComponent) {
            JComponent jComp = (JComponent) comp;
            if (jComp.getNextFocusableComponent() == null) {
                jComp.setNextFocusableComponent(this);
            }
        }
        return comp;
    }

    /** Saca el editor y vuelve al estado de no estar editando. */
    public void removeEditor() {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            editor.removeCellEditorListener(this);
            if (editorComp != null) {
                remove(editorComp);
            }
            Rectangle cellRect = getCellRect(editingRow, editingColumn, false);
            setCellEditor(null);
            setEditingColumn(-1);
            setEditingRow(-1);
            editorComp = null;
            repaint(cellRect);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    /**
     * Imprime la tabla.
     *
     * <p><strong>Sin impresora no hay impresion.</strong> Esta biblioteca corre sin pantalla y sin
     * servicio de impresion, asi que estos metodos arman el trabajo y fallan al pedir la impresora,
     * que es lo que hace el JDK en la misma situacion. {@link #getPrintable} si sirve: devuelve algo
     * que sabe dibujar la tabla en cualquier {@code Graphics}, incluido el de un PDF.
     *
     * @throws PrinterException si no se puede imprimir
     */
    public boolean print() throws PrinterException {
        return print(PrintMode.FIT_WIDTH);
    }

    /**
     * @throws PrinterException si no se puede imprimir
     */
    public boolean print(PrintMode printMode) throws PrinterException {
        return print(printMode, null, null);
    }

    /**
     * @throws PrinterException si no se puede imprimir
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) throws PrinterException {
        return print(printMode, headerFormat, footerFormat, true, null, true);
    }

    /**
     * @throws PrinterException si no se puede imprimir
     * @throws java.awt.HeadlessException si se pide interaccion y no hay pantalla
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat, boolean showPrintDialog,
            javax.print.attribute.PrintRequestAttributeSet attr, boolean interactive)
            throws PrinterException, java.awt.HeadlessException {
        return print(printMode, headerFormat, footerFormat, showPrintDialog, attr, interactive,
                null);
    }

    /**
     * @throws PrinterException si no se puede imprimir
     * @throws java.awt.HeadlessException si se pide interaccion y no hay pantalla
     */
    public boolean print(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat, boolean showPrintDialog,
            javax.print.attribute.PrintRequestAttributeSet attr, boolean interactive,
            javax.print.PrintService service) throws PrinterException,
            java.awt.HeadlessException {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        if (service != null) {
            job.setPrintService(service);
        }
        job.setPrintable(getPrintable(printMode, headerFormat, footerFormat));
        if (showPrintDialog && !job.printDialog()) {
            return false;
        }
        job.print();
        return true;
    }

    /**
     * Algo que sabe dibujar la tabla, pagina por pagina.
     *
     * @throws NullPointerException si el modo es nulo
     */
    public Printable getPrintable(PrintMode printMode, MessageFormat headerFormat,
            MessageFormat footerFormat) {
        if (printMode == null) {
            throw new NullPointerException("printMode");
        }
        return new ImpresionDeTabla(this, printMode, headerFormat, footerFormat);
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** Como se reparte la tabla en paginas. */
    public enum PrintMode {

        /** Las columnas que no entran a lo ancho van en paginas aparte. */
        NORMAL,

        /** Se achica todo para que el ancho entre en una pagina. */
        FIT_WIDTH;
    }

    /**
     * Donde caeria lo que se esta arrastrando.
     *
     * <p>Una fila y una columna, y dos banderas que dicen si es <em>sobre</em> esa celda o
     * <em>entre</em> dos. La diferencia importa: soltar sobre una fila la reemplaza, soltar entre
     * dos inserta.
     */
    public static final class DropLocation extends TransferHandler.DropLocation {

        private final int row;
        private final int col;
        private final boolean isInsertRow;
        private final boolean isInsertCol;

        DropLocation(Point p, int row, int col, boolean isInsertRow, boolean isInsertCol) {
            super(p);
            this.row = row;
            this.col = col;
            this.isInsertRow = isInsertRow;
            this.isInsertCol = isInsertCol;
        }

        /** La fila, en indices de vista; -1 si no hay. */
        public int getRow() {
            return row;
        }

        /** La columna, en indices de vista; -1 si no hay. */
        public int getColumn() {
            return col;
        }

        /** Si es entre dos filas y no sobre una. */
        public boolean isInsertRow() {
            return isInsertRow;
        }

        /** Si es entre dos columnas y no sobre una. */
        public boolean isInsertColumn() {
            return isInsertCol;
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + getDropPoint() + ","
                    + "row=" + row + ","
                    + "column=" + col + ","
                    + "insertRow=" + isInsertRow + ","
                    + "insertColumn=" + isInsertCol + "]";
        }
    }

    /**
     * Dibuja la tabla pagina por pagina.
     *
     * <p>Reparte las filas segun lo que entre a lo alto y, en modo {@link PrintMode#NORMAL}, las
     * columnas segun lo que entre a lo ancho. No dibuja lineas de corte ni encabezados repetidos
     * mas alla de lo que digan los dos formatos.
     */
    private static class ImpresionDeTabla implements Printable {

        private final JTable table;
        private final PrintMode printMode;
        private final MessageFormat headerFormat;
        private final MessageFormat footerFormat;

        ImpresionDeTabla(JTable table, PrintMode printMode, MessageFormat headerFormat,
                MessageFormat footerFormat) {
            this.table = table;
            this.printMode = printMode;
            this.headerFormat = headerFormat;
            this.footerFormat = footerFormat;
        }

        public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat,
                int pageIndex) throws PrinterException {
            int alto = (int) pageFormat.getImageableHeight();
            int porPagina = Math.max(1, alto / Math.max(1, table.getRowHeight()));
            int primera = pageIndex * porPagina;
            if (primera >= table.getRowCount()) {
                return NO_SUCH_PAGE;
            }
            graphics.translate((int) pageFormat.getImageableX(),
                    (int) pageFormat.getImageableY());
            java.awt.Graphics g2 = graphics.create();
            g2.translate(0, -primera * table.getRowHeight());
            table.paint(g2);
            g2.dispose();
            return PAGE_EXISTS;
        }
    }
}
