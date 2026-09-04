package javax.swing;

import java.util.List;

import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;

/**
 * La traduccion entre el orden del modelo y el que se ve.
 *
 * <h2>Ordenar sin tocar los datos</h2>
 *
 * <p>Un usuario que hace clic en el encabezado de una columna espera ver las filas ordenadas. La
 * forma ingenua seria reordenar el modelo, y estaria mal: el modelo es de la aplicacion, y el orden
 * es una preferencia de <em>esta</em> vista. Dos tablas sobre los mismos datos tendrian que pelearse
 * por el.
 *
 * <p>Esta clase resuelve eso con un mapeo. El modelo no se toca; lo que cambia es que la fila 0 de
 * la vista puede ser la 37 del modelo. De ahi los dos metodos de conversion, y de ahi que sean el
 * origen de casi todos los bugs de una tabla ordenable: usar un indice de vista donde iba uno de
 * modelo devuelve el dato equivocado sin fallar.
 *
 * <p>Tambien filtra: {@link #getViewRowCount} puede ser menor que {@link #getModelRowCount}.
 *
 * @param <M> el tipo del modelo
 * @since 1.6
 */
public abstract class RowSorter<M> {

    private List<RowSorterListener> listeners;

    /** Para las subclases. */
    public RowSorter() {
        this.listeners = new java.util.ArrayList<RowSorterListener>();
    }

    /** El modelo cuyas filas se ordenan. */
    public abstract M getModel();

    /** Alterna el orden de esa columna del modelo: ascendente, descendente, sin ordenar. */
    public abstract void toggleSortOrder(int column);

    /** El indice en el modelo de la fila que se ve en {@code index}. */
    public abstract int convertRowIndexToModel(int index);

    /** Donde se ve la fila {@code index} del modelo, o {@code -1} si esta filtrada. */
    public abstract int convertRowIndexToView(int index);

    /** Fija por que columnas se ordena y en que sentido. */
    public abstract void setSortKeys(List<? extends SortKey> keys);

    /** Por que columnas se ordena. */
    public abstract List<? extends SortKey> getSortKeys();

    /** Cuantas filas se ven, ya filtradas. */
    public abstract int getViewRowCount();

    /** Cuantas filas tiene el modelo. */
    public abstract int getModelRowCount();

    /** Aviso de que el modelo cambio de forma por completo. */
    public abstract void modelStructureChanged();

    /** Aviso de que cambio el contenido de todas las filas. */
    public abstract void allRowsChanged();

    /** Aviso de que se insertaron filas en el modelo. */
    public abstract void rowsInserted(int firstRow, int endRow);

    /** Aviso de que se borraron filas del modelo. */
    public abstract void rowsDeleted(int firstRow, int endRow);

    /** Aviso de que cambiaron filas del modelo. */
    public abstract void rowsUpdated(int firstRow, int endRow);

    /** Aviso de que cambio una columna de un rango de filas. */
    public abstract void rowsUpdated(int firstRow, int endRow, int column);

    /** Agrega un oyente. */
    public void addRowSorterListener(RowSorterListener l) {
        this.listeners.add(l);
    }

    /** Saca un oyente. */
    public void removeRowSorterListener(RowSorterListener l) {
        this.listeners.remove(l);
    }

    /** Avisa que cambio por que columnas se ordena, sin que el orden de las filas se rehiciera. */
    protected void fireSortOrderChanged() {
        repartir(new RowSorterEvent(this));
    }

    /**
     * Avisa que las filas se reordenaron.
     *
     * @param lastRowIndexToModel donde estaba cada fila antes, o {@code null} si no se sabe. Es lo
     *     que le permite a una vista conservar la seleccion a traves del reordenamiento
     */
    protected void fireRowSorterChanged(int[] lastRowIndexToModel) {
        repartir(new RowSorterEvent(this, RowSorterEvent.Type.SORTED, lastRowIndexToModel));
    }

    private void repartir(RowSorterEvent e) {
        for (int i = this.listeners.size() - 1; i >= 0; i--) {
            this.listeners.get(i).sorterChanged(e);
        }
    }

    /**
     * Por que columna se ordena y en que sentido.
     *
     * <p>Inmutable, y es una lista y no una sola: ordenar por apellido y despues por nombre necesita
     * dos claves, y el orden de la lista es el de desempate.
     */
    public static class SortKey {

        private final int column;
        private final SortOrder sortOrder;

        /**
         * @throws IllegalArgumentException si {@code sortOrder} es {@code null}
         */
        public SortKey(int column, SortOrder sortOrder) {
            if (sortOrder == null) {
                throw new IllegalArgumentException("El sentido no puede ser null");
            }
            this.column = column;
            this.sortOrder = sortOrder;
        }

        /** La columna del modelo. */
        public final int getColumn() {
            return this.column;
        }

        /** El sentido. */
        public final SortOrder getSortOrder() {
            return this.sortOrder;
        }

        public int hashCode() {
            return 31 * this.column + this.sortOrder.hashCode();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof SortKey) {
                SortKey otra = (SortKey) o;
                return otra.column == this.column && otra.sortOrder == this.sortOrder;
            }
            return false;
        }
    }
}
