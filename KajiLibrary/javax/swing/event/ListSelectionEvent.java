package javax.swing.event;

import java.util.EventObject;

/**
 * La seleccion de una lista cambio.
 *
 * <p>El rango que trae es <strong>donde pudo haber cambiado algo</strong>, no lo que quedo
 * seleccionado. Es una diferencia real: quien escucha tiene que preguntarle al modelo por el estado
 * final de esas filas. El evento acota el trabajo, no lo hace.
 *
 * <p>{@link #getValueIsAdjusting} en {@code true} significa que vienen mas: el usuario esta
 * arrastrando. Recalcular en cada paso intermedio es trabajo tirado, y el ultimo evento —con la
 * bandera apagada— es el que vale.
 */
public class ListSelectionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private int firstIndex;
    private int lastIndex;
    private boolean isAdjusting;

    public ListSelectionEvent(Object source, int firstIndex, int lastIndex, boolean isAdjusting) {
        super(source);
        this.firstIndex = firstIndex;
        this.lastIndex = lastIndex;
        this.isAdjusting = isAdjusting;
    }

    /** La primera fila que pudo cambiar. */
    public int getFirstIndex() {
        return this.firstIndex;
    }

    /** La ultima fila que pudo cambiar, inclusive. */
    public int getLastIndex() {
        return this.lastIndex;
    }

    /** Si vienen mas cambios. */
    public boolean getValueIsAdjusting() {
        return this.isAdjusting;
    }

    public String toString() {
        return getClass().getName() + "[firstIndex=" + String.valueOf(this.firstIndex)
                + ",lastIndex=" + String.valueOf(this.lastIndex)
                + ",isAdjusting=" + String.valueOf(this.isAdjusting) + "]";
    }
}
