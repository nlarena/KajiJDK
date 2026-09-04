package javax.swing.event;

import java.util.EventObject;

/**
 * El contenido de una lista cambio.
 *
 * <p>Describe un <strong>rango</strong> y no un elemento, porque agregar diez filas de a una
 * dispararia diez avisos y diez repintados. Los indices son inclusive los dos.
 *
 * <p>La distincion entre {@link #CONTENTS_CHANGED} y los otros dos es la que importa para quien
 * escucha: con el primero la cantidad de elementos no cambio, asi que alcanza con repintar; con los
 * otros hay que rehacer el layout.
 */
public class ListDataEvent extends EventObject {

    private static final long serialVersionUID = 2805090815656617888L;

    /** Cambiaron elementos, sin cambiar cuantos hay. */
    public static final int CONTENTS_CHANGED = 0;

    /** Se agregaron elementos. */
    public static final int INTERVAL_ADDED = 1;

    /** Se sacaron elementos. */
    public static final int INTERVAL_REMOVED = 2;

    private int type;
    private int index0;
    private int index1;

    /** Los indices se guardan ordenados, para que quien escuche no tenga que ordenarlos. */
    public ListDataEvent(Object source, int type, int index0, int index1) {
        super(source);
        this.type = type;
        this.index0 = Math.min(index0, index1);
        this.index1 = Math.max(index0, index1);
    }

    /** Cual de las tres clases de cambio fue. */
    public int getType() {
        return this.type;
    }

    /** El primer indice del rango. */
    public int getIndex0() {
        return this.index0;
    }

    /** El ultimo indice del rango, inclusive. */
    public int getIndex1() {
        return this.index1;
    }

    public String toString() {
        return getClass().getName() + "[type=" + String.valueOf(this.type)
                + ",index0=" + String.valueOf(this.index0)
                + ",index1=" + String.valueOf(this.index1) + "]";
    }
}
