package javax.swing.table;

import java.io.Serializable;
import java.util.Vector;

import javax.swing.event.TableModelEvent;

/**
 * Un modelo de tabla hecho de vectores, para cuando no vale la pena escribir uno.
 *
 * <h2>Todo es {@code Object} y todo se edita</h2>
 *
 * <p>Guarda un vector de filas, cada una un vector de celdas, y no sabe nada de tipos: cada columna
 * es de {@link Object} y cada celda se puede editar. Es lo que lo hace comodo para empezar y lo que
 * lo vuelve insuficiente en cuanto la tabla tiene que ordenar numeros o mostrar tildes -- ahi
 * conviene una subclase que diga {@code getColumnClass}.
 *
 * <h2>Las filas se estiran solas</h2>
 *
 * <p>Agregar una fila mas corta que las columnas no es un error: se la rellena con nulos. Y agregar
 * una columna estira todas las filas. La estructura se mantiene rectangular sin que el llamador
 * tenga que cuidarlo, que es la mitad de la razon por la que existe esta clase.
 *
 * <h2>Los tres metodos con nombre raro</h2>
 *
 * <p>{@link #newDataAvailable}, {@link #newRowsAdded} y {@link #rowsRemoved} reciben un evento y
 * avisan. Vienen de una version vieja de Swing donde el llamador tocaba el vector directamente y
 * despues avisaba; siguen ahi por compatibilidad y no hay motivo para usarlos hoy.
 */
public class DefaultTableModel extends AbstractTableModel implements Serializable {

    /** Las filas; cada una un vector de celdas. */
    protected Vector<Vector> dataVector;

    /** Los nombres de las columnas. */
    protected Vector columnIdentifiers;

    /** Sin filas ni columnas. */
    public DefaultTableModel() {
        this(0, 0);
    }

    /** Con esa cantidad de filas y columnas, todas vacias. */
    public DefaultTableModel(int rowCount, int columnCount) {
        this(newVector(columnCount), rowCount);
    }

    /**
     * Con esos nombres de columna y esa cantidad de filas vacias.
     *
     * <p>Una cantidad negativa revienta, pero no aca: el error sale del vector que se intenta
     * crear, con su mensaje. Es lo que hace el JDK y esta medido.
     *
     * @throws IllegalArgumentException si la cantidad de filas es negativa
     */
    public DefaultTableModel(Vector<?> columnNames, int rowCount) {
        setDataVector(newVector(rowCount), columnNames);
    }

    /** Idem, con los nombres en un arreglo. */
    public DefaultTableModel(Object[] columnNames, int rowCount) {
        this(convertToVector(columnNames), rowCount);
    }

    /** Con esos datos y esos nombres. */
    public DefaultTableModel(Vector<? extends Vector> data, Vector<?> columnNames) {
        setDataVector(data, columnNames);
    }

    /** Idem, con arreglos. */
    public DefaultTableModel(Object[][] data, Object[] columnNames) {
        setDataVector(data, columnNames);
    }

    /** Los datos; no es copia -- tocarlos cambia el modelo sin que nadie se entere. */
    public Vector<Vector> getDataVector() {
        return dataVector;
    }

    /**
     * Reemplaza datos y nombres de columna.
     *
     * <p>Avisa un cambio de estructura, no de datos: cambian las columnas y la tabla tiene que
     * rearmarlas. Ver la nota de {@link AbstractTableModel}.
     */
    public void setDataVector(Vector<? extends Vector> dataVector, Vector<?> columnIdentifiers) {
        this.dataVector = new Vector<Vector>(0);
        this.columnIdentifiers = nonNullVector(columnIdentifiers);
        if (dataVector != null) {
            for (int i = 0; i < dataVector.size(); i++) {
                this.dataVector.addElement(dataVector.elementAt(i));
            }
        }
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    /** Idem, con arreglos. */
    public void setDataVector(Object[][] dataVector, Object[] columnIdentifiers) {
        setDataVector(convertToVector(dataVector), convertToVector(columnIdentifiers));
    }

    /** Avisa que los datos cambiaron; ver la nota de la clase. */
    public void newDataAvailable(TableModelEvent event) {
        fireTableChanged(event);
    }

    /**
     * Avisa que se agregaron filas, despues de emparejarlas.
     *
     * <p>Emparejar es lo que hace que una fila mas corta que las columnas no rompa nada.
     */
    public void newRowsAdded(TableModelEvent e) {
        justifyRows(e.getFirstRow(), e.getLastRow() + 1);
        fireTableChanged(e);
    }

    /** Avisa que se sacaron filas. */
    public void rowsRemoved(TableModelEvent event) {
        fireTableChanged(event);
    }

    /**
     * Cuantas filas hay; agrega vacias o saca las de mas.
     *
     * @deprecated Usar {@link #setRowCount}.
     */
    @Deprecated
    public void setNumRows(int rowCount) {
        int old = getRowCount();
        if (old == rowCount) {
            return;
        }
        dataVector.setSize(rowCount);
        if (rowCount <= old) {
            fireTableRowsDeleted(rowCount, old - 1);
        } else {
            justifyRows(old, rowCount);
            fireTableRowsInserted(old, rowCount - 1);
        }
    }

    /** Cuantas filas hay; agrega vacias o saca las de mas. */
    public void setRowCount(int rowCount) {
        setNumRows(rowCount);
    }

    /** Agrega una fila al final. */
    public void addRow(Vector<?> rowData) {
        insertRow(getRowCount(), rowData);
    }

    /** Idem, con un arreglo. */
    public void addRow(Object[] rowData) {
        addRow(convertToVector(rowData));
    }

    /**
     * Mete una fila en esa posicion.
     *
     * @throws ArrayIndexOutOfBoundsException si la posicion esta fuera de rango
     */
    public void insertRow(int row, Vector<?> rowData) {
        dataVector.insertElementAt(nonNullVector(rowData), row);
        justifyRows(row, row + 1);
        fireTableRowsInserted(row, row);
    }

    /** Idem, con un arreglo. */
    public void insertRow(int row, Object[] rowData) {
        insertRow(row, convertToVector(rowData));
    }

    /**
     * Mueve el bloque de filas de {@code start} a {@code end} para que empiece en {@code to}.
     *
     * @throws ArrayIndexOutOfBoundsException si algun indice esta fuera de rango
     */
    public void moveRow(int start, int end, int to) {
        int shift = to - start;
        int first;
        int last;
        if (shift < 0) {
            first = to;
            last = end;
        } else {
            first = start;
            last = to + end - start;
        }
        verifyRange(first, last);
        // Se rota el tramo que va del primero al ultimo afectado, no el bloque que se mueve: lo
        // que sale de un extremo tiene que entrar por el otro.
        rotate(dataVector, first, last + 1, to - start);
        fireTableRowsUpdated(first, last);
    }

    private void verifyRange(int first, int last) {
        if (first < 0 || last >= getRowCount()) {
            throw new ArrayIndexOutOfBoundsException("Range out of bounds");
        }
    }

    /**
     * Rota un tramo del vector.
     *
     * <p>Mover un bloque es rotar: lo que sale de un lado entra por el otro, sin espacio de mas.
     */
    private static void rotate(Vector<Vector> v, int a, int b, int shift) {
        int size = b - a;
        int r = size - shift;
        int g = gcd(size, r);
        for (int i = 0; i < g; i++) {
            int to = i;
            Vector tmp = v.elementAt(a + to);
            for (int from = (to + r) % size; from != i; from = (to + r) % size) {
                v.setElementAt(v.elementAt(a + from), a + to);
                to = from;
            }
            v.setElementAt(tmp, a + to);
        }
    }

    private static int gcd(int i, int j) {
        return (j == 0) ? i : gcd(j, i % j);
    }

    /**
     * Saca esa fila.
     *
     * @throws ArrayIndexOutOfBoundsException si la posicion esta fuera de rango
     */
    public void removeRow(int row) {
        dataVector.removeElementAt(row);
        fireTableRowsDeleted(row, row);
    }

    /**
     * Cambia los nombres de las columnas, y con ellos cuantas hay.
     *
     * <p>Nulo deja cero columnas.
     */
    public void setColumnIdentifiers(Vector<?> columnIdentifiers) {
        setDataVector(dataVector, columnIdentifiers);
    }

    /** Idem, con un arreglo. */
    public void setColumnIdentifiers(Object[] newIdentifiers) {
        setColumnIdentifiers(convertToVector(newIdentifiers));
    }

    /** Cuantas columnas hay; agrega sin nombre o saca las de mas. */
    public void setColumnCount(int columnCount) {
        columnIdentifiers.setSize(columnCount);
        justifyRows(0, getRowCount());
        fireTableStructureChanged();
    }

    /** Agrega una columna vacia con ese nombre. */
    public void addColumn(Object columnName) {
        addColumn(columnName, (Vector) null);
    }

    /**
     * Agrega una columna con esos valores.
     *
     * <p>Si hay menos valores que filas, las que sobran quedan en nulo.
     */
    public void addColumn(Object columnName, Vector columnData) {
        columnIdentifiers.addElement(columnName);
        if (columnData != null) {
            int columnSize = columnData.size();
            if (columnSize > getRowCount()) {
                dataVector.setSize(columnSize);
            }
            justifyRows(0, getRowCount());
            int newColumn = getColumnCount() - 1;
            for (int i = 0; i < columnSize; i++) {
                Vector row = dataVector.elementAt(i);
                row.setElementAt(columnData.elementAt(i), newColumn);
            }
        } else {
            justifyRows(0, getRowCount());
        }
        fireTableStructureChanged();
    }

    /** Idem, con un arreglo. */
    public void addColumn(Object columnName, Object[] columnData) {
        addColumn(columnName, convertToVector(columnData));
    }

    public int getRowCount() {
        return dataVector.size();
    }

    public int getColumnCount() {
        return columnIdentifiers.size();
    }

    /**
     * El nombre de esa columna.
     *
     * <p>Una columna sin nombre puesto se llama como diga {@link AbstractTableModel}: A, B, C...
     */
    public String getColumnName(int column) {
        Object id = null;
        if (column < columnIdentifiers.size() && (column >= 0)) {
            id = columnIdentifiers.elementAt(column);
        }
        return (id == null) ? super.getColumnName(column) : id.toString();
    }

    /** Cierto siempre: ver la nota de la clase. */
    public boolean isCellEditable(int row, int column) {
        return true;
    }

    /**
     * @throws ArrayIndexOutOfBoundsException si la fila o la columna estan fuera de rango
     */
    public Object getValueAt(int row, int column) {
        Vector rowVector = dataVector.elementAt(row);
        return rowVector.elementAt(column);
    }

    /**
     * @throws ArrayIndexOutOfBoundsException si la fila o la columna estan fuera de rango
     */
    public void setValueAt(Object aValue, int row, int column) {
        Vector rowVector = dataVector.elementAt(row);
        rowVector.setElementAt(aValue, column);
        fireTableCellUpdated(row, column);
    }

    /** Ese arreglo como vector; nulo da un vector vacio. */
    protected static Vector<Object> convertToVector(Object[] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Object> v = new Vector<Object>(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(anArray[i]);
        }
        return v;
    }

    /** Esa matriz como vector de vectores; nula da nulo. */
    protected static Vector<Vector<Object>> convertToVector(Object[][] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Vector<Object>> v = new Vector<Vector<Object>>(anArray.length);
        for (int i = 0; i < anArray.length; i++) {
            v.addElement(convertToVector(anArray[i]));
        }
        return v;
    }

    /** Empareja esas filas al ancho de las columnas, rellenando con nulos. */
    private void justifyRows(int from, int to) {
        dataVector.setSize(getRowCount());
        for (int i = from; i < to; i++) {
            if (dataVector.elementAt(i) == null) {
                dataVector.setElementAt(new Vector<Object>(), i);
            }
            dataVector.elementAt(i).setSize(getColumnCount());
        }
    }

    /**
     * Un vector de ese largo, lleno de nulos.
     *
     * <p>Crudo a proposito: sirve tanto para una lista de nombres de columna como para una lista de
     * filas, que son dos tipos distintos. Es como esta en el JDK.
     */
    @SuppressWarnings("rawtypes")
    private static Vector newVector(int size) {
        Vector v = new Vector(size);
        v.setSize(size);
        return v;
    }

    /**
     * Una copia de ese vector, o uno vacio si es nulo.
     *
     * <p>Se copia con {@code addAll} y no con el constructor de copia, que es lo que hace el JDK:
     * este compilador no acepta un argumento con comodin en un constructor de clase generica -- ver
     * el hallazgo #519 -- y por metodo si pasa.
     */
    @SuppressWarnings("rawtypes")
    private static Vector nonNullVector(Vector<?> v) {
        Vector<Object> copia = new Vector<Object>();
        if (v != null) {
            copia.addAll(v);
        }
        return copia;
    }
}
