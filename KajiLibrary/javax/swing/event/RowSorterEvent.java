package javax.swing.event;

import java.util.EventObject;

import javax.swing.RowSorter;

/**
 * El orden de las filas cambio.
 *
 * <p>{@link #convertPreviousRowIndexToModel} es lo que justifica esta clase: dice donde estaba cada
 * fila <strong>antes</strong> del reordenamiento. Sin eso, una vista que tenia seleccionada la fila
 * 5 no podria saber que fila del modelo era y perderia la seleccion en cada clic al encabezado.
 *
 * <p>Puede devolver {@code -1}: quien reordena no siempre guarda el mapeo anterior, y decirlo es
 * mejor que inventarlo.
 */
public class RowSorterEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private Type type;
    private int[] oldViewToModel;

    /** Cambiaron las claves de ordenamiento, sin rehacerse las filas. */
    public RowSorterEvent(RowSorter<?> source) {
        this(source, Type.SORT_ORDER_CHANGED, null);
    }

    /**
     * @param previousRowIndexToModel donde estaba cada fila antes, o {@code null}
     * @throws IllegalArgumentException si {@code type} es {@code null}
     */
    public RowSorterEvent(RowSorter<?> source, Type type, int[] previousRowIndexToModel) {
        super(source);
        if (type == null) {
            throw new IllegalArgumentException("El tipo no puede ser null");
        }
        this.type = type;
        this.oldViewToModel = previousRowIndexToModel;
    }

    /** Quien reordeno. */
    public RowSorter<?> getSource() {
        return (RowSorter) super.getSource();
    }

    /** Que clase de cambio fue. */
    public Type getType() {
        return this.type;
    }

    /** Que fila del modelo se veia en {@code index} antes del cambio, o {@code -1}. */
    public int convertPreviousRowIndexToModel(int index) {
        if (this.oldViewToModel != null && index >= 0 && index < this.oldViewToModel.length) {
            return this.oldViewToModel[index];
        }
        return -1;
    }

    /** Cuantas filas se veian antes del cambio. */
    public int getPreviousRowCount() {
        return this.oldViewToModel == null ? 0 : this.oldViewToModel.length;
    }

    /** Que clase de cambio fue. */
    public enum Type {

        /** Cambiaron las claves de ordenamiento; las filas todavia no se movieron. */
        SORT_ORDER_CHANGED,
        /** El contenido se reordeno o se filtro. */
        SORTED
    }
}
