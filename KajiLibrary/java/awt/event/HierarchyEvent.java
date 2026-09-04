package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;

/**
 * Cambió el árbol de componentes por encima de un componente.
 *
 * <p>Sirve para enterarse de cosas que no pasan en el componente sino **arriba** suyo: que lo
 * agregaron a una ventana, que un ancestro se ocultó, que la ventana que lo contiene se movió. Un
 * componente no puede ver eso mirándose a sí mismo.
 *
 * <p>Las banderas dicen qué cambió, y `SHOWING_CHANGED` es la más útil: significa que el componente
 * pasó a verse o dejó de verse **de verdad**, contando que todos sus ancestros estén visibles. Es la
 * señal correcta para arrancar y parar una animación.
 */
public class HierarchyEvent extends AWTEvent {

    private static final long serialVersionUID = -5337576970038043990L;

    /** Un ancestro cambió de lugar. */
    public static final int ANCESTOR_MOVED = 1401;

    /** Un ancestro cambió de tamaño. */
    public static final int ANCESTOR_RESIZED = 1402;

    /** Cambió si el componente puede mostrarse. */
    public static final int DISPLAYABILITY_CHANGED = 2;

    /** Cambió el árbol. */
    public static final int HIERARCHY_CHANGED = 1400;

    /** El primer identificador de la familia. */
    public static final int HIERARCHY_FIRST = 1400;

    /** El último identificador de la familia. */
    public static final int HIERARCHY_LAST = 1402;

    /** El componente cambió de padre. */
    public static final int PARENT_CHANGED = 1;

    /** Cambió si el componente se ve de verdad. */
    public static final int SHOWING_CHANGED = 4;

    private final Component changed;
    private final Container changedParent;
    private final long changeFlags;

    /**
     * Sin banderas, para los eventos de ancestro.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public HierarchyEvent(Component source, int id, Component changed, Container changedParent) {
        this(source, id, changed, changedParent, 0);
    }

    /**
     * Con las banderas de qué cambió.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public HierarchyEvent(Component source, int id, Component changed, Container changedParent,
            long changeFlags) {
        super(source, id);
        this.changed = changed;
        this.changedParent = changedParent;
        this.changeFlags = changeFlags;
    }

    /** El componente que recibe el aviso. */
    public Component getComponent() {
        if (this.source instanceof Component) {
            return (Component) this.source;
        }
        return null;
    }

    /** El componente de arriba que efectivamente cambió. */
    public Component getChanged() {
        return this.changed;
    }

    /** El padre de ese componente, antes o después del cambio según qué pasó. */
    public Container getChangedParent() {
        return this.changedParent;
    }

    /** Qué cambió, como combinación de banderas. */
    public long getChangeFlags() {
        return this.changeFlags;
    }

    public String paramString() {
        String tipo;
        if (this.id == HIERARCHY_CHANGED) {
            tipo = "HIERARCHY_CHANGED";
        } else if (this.id == ANCESTOR_MOVED) {
            tipo = "ANCESTOR_MOVED";
        } else if (this.id == ANCESTOR_RESIZED) {
            tipo = "ANCESTOR_RESIZED";
        } else {
            tipo = "unknown type";
        }
        return tipo + " (" + this.changed + "," + this.changedParent + "),changeFlags="
                + this.changeFlags;
    }
}
