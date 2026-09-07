package javax.swing.table;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

/**
 * Las columnas de una tabla: cuales hay, en que orden y cuanto miden.
 *
 * <h2>Las columnas de la vista no son las del modelo</h2>
 *
 * <p>Este modelo guarda las columnas <em>como se ven</em>: se pueden mover, sacar y repetir sin que
 * el modelo de datos se entere. Cada {@link TableColumn} sabe de que columna del modelo saca sus
 * valores, y por eso mover una columna de lugar no mueve ningun dato.
 *
 * <h2>El ancho total se guarda</h2>
 *
 * <p>Sumar los anchos en cada repintado seria caro con muchas columnas, asi que el total se guarda y
 * se recalcula cuando algo cambia. De ahi que este modelo escuche los cambios de propiedad de cada
 * columna: una columna que cambia de ancho tiene que avisar, y el que suma es este.
 *
 * <h2>Tambien lleva la seleccion de columnas</h2>
 *
 * <p>Con un {@link ListSelectionModel}, el mismo tipo que usa una lista. Que la seleccion de
 * columnas viva aca y no en la tabla es lo que permite que dos tablas compartan columnas y
 * seleccion.
 */
public class DefaultTableColumnModel implements TableColumnModel, PropertyChangeListener,
        ListSelectionListener, Serializable {

    /** Las columnas, en el orden en que se ven. */
    protected Vector<TableColumn> tableColumns;

    /** La seleccion de columnas. */
    protected ListSelectionModel selectionModel;

    /** El espacio entre una columna y la siguiente. */
    protected int columnMargin;

    /** Los oyentes, por tipo. */
    protected EventListenerList listenerList = new EventListenerList();

    /** El evento de cambio de margen, armado una vez. */
    protected transient ChangeEvent changeEvent = null;

    /** Si se pueden elegir columnas. */
    protected boolean columnSelectionAllowed;

    /** La suma de los anchos, guardada; ver la nota de la clase. */
    protected int totalColumnWidth;

    /** Sin columnas, con margen de uno y sin seleccion de columnas. */
    public DefaultTableColumnModel() {
        super();
        tableColumns = new Vector<TableColumn>();
        setSelectionModel(createSelectionModel());
        setColumnMargin(1);
        invalidateWidthCache();
        setColumnSelectionAllowed(false);
    }

    /**
     * Agrega una columna al final.
     *
     * @throws IllegalArgumentException si es nula
     */
    public void addColumn(TableColumn aColumn) {
        if (aColumn == null) {
            throw new IllegalArgumentException("Object is null");
        }
        tableColumns.addElement(aColumn);
        aColumn.addPropertyChangeListener(this);
        invalidateWidthCache();
        fireColumnAdded(new TableColumnModelEvent(this, 0, getColumnCount() - 1));
    }

    /**
     * Saca esa columna.
     *
     * <p>Una columna que no esta se ignora en silencio: sacar algo que no estaba deja el modelo
     * igual, que es lo que el llamador queria.
     */
    public void removeColumn(TableColumn column) {
        int columnIndex = tableColumns.indexOf(column);
        if (columnIndex != -1) {
            if (selectionModel != null) {
                selectionModel.removeIndexInterval(columnIndex, columnIndex);
            }
            column.removePropertyChangeListener(this);
            tableColumns.removeElementAt(columnIndex);
            invalidateWidthCache();
            fireColumnRemoved(new TableColumnModelEvent(this, columnIndex, 0));
        }
    }

    /**
     * Mueve una columna a otra posicion.
     *
     * <p>La seleccion se mueve con ella: si no, arrastrar una columna elegida dejaria elegida a la
     * que quedo en su lugar.
     *
     * @throws IllegalArgumentException si algun indice esta fuera de rango
     */
    public void moveColumn(int columnIndex, int newIndex) {
        if ((columnIndex < 0) || (columnIndex >= getColumnCount())
                || (newIndex < 0) || (newIndex >= getColumnCount())) {
            throw new IllegalArgumentException("moveColumn() - Index out of range");
        }
        if (columnIndex == newIndex) {
            fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
            return;
        }
        TableColumn aColumn = tableColumns.elementAt(columnIndex);
        tableColumns.removeElementAt(columnIndex);
        boolean selected = selectionModel.isSelectedIndex(columnIndex);
        selectionModel.removeIndexInterval(columnIndex, columnIndex);
        tableColumns.insertElementAt(aColumn, newIndex);
        selectionModel.insertIndexInterval(newIndex, 1, true);
        if (selected) {
            selectionModel.addSelectionInterval(newIndex, newIndex);
        } else {
            selectionModel.removeSelectionInterval(newIndex, newIndex);
        }
        fireColumnMoved(new TableColumnModelEvent(this, columnIndex, newIndex));
    }

    /** El espacio entre columnas; cambia el ancho total. */
    public void setColumnMargin(int newMargin) {
        if (newMargin != columnMargin) {
            columnMargin = newMargin;
            fireColumnMarginChanged();
        }
    }

    public int getColumnCount() {
        return tableColumns.size();
    }

    public Enumeration<TableColumn> getColumns() {
        return tableColumns.elements();
    }

    /**
     * La primera columna con ese identificador.
     *
     * @throws IllegalArgumentException si el identificador es nulo o no hay ninguna
     */
    public int getColumnIndex(Object identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier is null");
        }
        int index = 0;
        Enumeration<TableColumn> e = getColumns();
        while (e.hasMoreElements()) {
            TableColumn aColumn = e.nextElement();
            if (identifier.equals(aColumn.getIdentifier())) {
                return index;
            }
            index = index + 1;
        }
        throw new IllegalArgumentException("Identifier not found");
    }

    public TableColumn getColumn(int columnIndex) {
        return tableColumns.elementAt(columnIndex);
    }

    public int getColumnMargin() {
        return columnMargin;
    }

    /**
     * La columna que ocupa ese pixel.
     *
     * <p>Un pixel a la izquierda de la primera o a la derecha de la ultima devuelve -1: no hay
     * columna ahi, y devolver la mas cercana haria que un clic afuera de la tabla eligiera algo.
     */
    public int getColumnIndexAtX(int x) {
        if (x < 0) {
            return -1;
        }
        int cc = getColumnCount();
        for (int column = 0; column < cc; column++) {
            x = x - getColumn(column).getWidth();
            if (x < 0) {
                return column;
            }
        }
        return -1;
    }

    /** La suma de los anchos, mas los margenes. */
    public int getTotalColumnWidth() {
        if (totalColumnWidth == -1) {
            recalcWidthCache();
        }
        return totalColumnWidth;
    }

    /**
     * Cambia el modelo de seleccion de columnas.
     *
     * @throws IllegalArgumentException si es nulo
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
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setColumnSelectionAllowed(boolean flag) {
        columnSelectionAllowed = flag;
    }

    public boolean getColumnSelectionAllowed() {
        return columnSelectionAllowed;
    }

    /** Las columnas elegidas; arreglo vacio si no hay o si no se pueden elegir. */
    public int[] getSelectedColumns() {
        if (selectionModel != null) {
            int iMin = selectionModel.getMinSelectionIndex();
            int iMax = selectionModel.getMaxSelectionIndex();
            if ((iMin == -1) || (iMax == -1)) {
                return new int[0];
            }
            int[] rvTmp = new int[1 + (iMax - iMin)];
            int n = 0;
            for (int i = iMin; i <= iMax; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    rvTmp[n] = i;
                    n = n + 1;
                }
            }
            int[] rv = new int[n];
            System.arraycopy(rvTmp, 0, rv, 0, n);
            return rv;
        }
        return new int[0];
    }

    public int getSelectedColumnCount() {
        if (selectionModel != null) {
            int iMin = selectionModel.getMinSelectionIndex();
            int iMax = selectionModel.getMaxSelectionIndex();
            int count = 0;
            for (int i = iMin; i <= iMax; i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    count = count + 1;
                }
            }
            return count;
        }
        return 0;
    }

    public void addColumnModelListener(TableColumnModelListener x) {
        listenerList.add(TableColumnModelListener.class, x);
    }

    public void removeColumnModelListener(TableColumnModelListener x) {
        listenerList.remove(TableColumnModelListener.class, x);
    }

    public TableColumnModelListener[] getColumnModelListeners() {
        return listenerList.getListeners(TableColumnModelListener.class);
    }

    protected void fireColumnAdded(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnAdded(e);
            }
        }
    }

    protected void fireColumnRemoved(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnRemoved(e);
            }
        }
    }

    protected void fireColumnMoved(TableColumnModelEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnMoved(e);
            }
        }
    }

    protected void fireColumnSelectionChanged(ListSelectionEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                ((TableColumnModelListener) listeners[i + 1]).columnSelectionChanged(e);
            }
        }
    }

    /** El evento se arma una vez y se reusa: no dice que cambio, solo que cambio. */
    protected void fireColumnMarginChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == TableColumnModelListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((TableColumnModelListener) listeners[i + 1]).columnMarginChanged(changeEvent);
            }
        }
    }

    /** Los oyentes de ese tipo anotados en este modelo. */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /**
     * Una columna cambio de ancho: hay que volver a sumar.
     *
     * <p>Solo el ancho y el ancho preferido cambian el total; los demas cambios de una columna --
     * su titulo, su dibujante -- no.
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        if ("width".equals(name) || "preferredWidth".equals(name)) {
            invalidateWidthCache();
            fireColumnMarginChanged();
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        fireColumnSelectionChanged(e);
    }

    /** El modelo de seleccion que se usa si nadie da otro. */
    protected ListSelectionModel createSelectionModel() {
        return new DefaultListSelectionModel();
    }

    /** Vuelve a sumar los anchos; ver la nota de la clase. */
    protected void recalcWidthCache() {
        Enumeration<TableColumn> e = getColumns();
        totalColumnWidth = 0;
        while (e.hasMoreElements()) {
            totalColumnWidth = totalColumnWidth + e.nextElement().getWidth();
        }
    }

    private void invalidateWidthCache() {
        totalColumnWidth = -1;
    }
}
