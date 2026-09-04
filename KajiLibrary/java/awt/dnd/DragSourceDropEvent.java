package java.awt.dnd;

/**
 * Terminó el arrastre, visto desde el origen.
 *
 * <p>Es el único evento que **siempre** llega, se haya soltado o cancelado, y trae la única
 * información que el origen realmente necesita: si el destino se quedó con los datos y con qué
 * acción.
 *
 * <p>De ahí sale la decisión de borrar el original: hay que borrarlo sólo si
 * {@link #getDropSuccess} es cierto **y** {@link #getDropAction} es mover. Un soltado exitoso con
 * copiar no borra nada, y un arrastre cancelado tampoco.
 */
public class DragSourceDropEvent extends DragSourceEvent {

    private static final long serialVersionUID = -5571321229470821891L;

    private final boolean dropSuccess;
    private final int dropAction;

    /**
     * Un arrastre que terminó **sin** soltarse.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc) {
        super(dsc);
        this.dropSuccess = false;
        this.dropAction = DnDConstants.ACTION_NONE;
    }

    /**
     * Un arrastre que terminó soltándose, sin posición.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success) {
        super(dsc);
        this.dropSuccess = success;
        this.dropAction = action;
    }

    /**
     * Como el anterior, con la posición en pantalla.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceDropEvent(DragSourceContext dsc, int action, boolean success, int x, int y) {
        super(dsc, x, y);
        this.dropSuccess = success;
        this.dropAction = action;
    }

    /** Si el destino se quedó con los datos. */
    public boolean getDropSuccess() {
        return this.dropSuccess;
    }

    /** Con qué acción los tomó. */
    public int getDropAction() {
        return this.dropAction;
    }
}
