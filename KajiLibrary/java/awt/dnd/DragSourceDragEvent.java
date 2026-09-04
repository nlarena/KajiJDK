package java.awt.dnd;

/**
 * El arrastre está pasando por encima de un destino, visto desde el origen.
 *
 * <p>Trae **tres** acciones distintas y confundirlas es fácil:
 *
 * <ul>
 *   <li>{@link #getUserAction}: lo que el usuario pide, según las teclas que tenga apretadas;
 *   <li>{@link #getTargetActions}: lo que el destino declaró que acepta;
 *   <li>{@link #getDropAction}: la intersección de las dos, que es lo que efectivamente va a pasar.
 * </ul>
 *
 * <p>La tercera es la que decide el cursor, y es cero cuando el usuario pide algo que el destino no
 * acepta.
 */
public class DragSourceDragEvent extends DragSourceEvent {

    private static final long serialVersionUID = 481346297933902471L;

    private final int targetActions;
    private final int dropAction;
    private final int gestureModifiers;

    /**
     * Sin posición.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action, int modifiers) {
        super(dsc);
        this.targetActions = action;
        this.dropAction = dropAction;
        this.gestureModifiers = modifiers;
    }

    /**
     * Con la posición en pantalla.
     *
     * @throws IllegalArgumentException si el contexto es `null`
     */
    public DragSourceDragEvent(DragSourceContext dsc, int dropAction, int action, int modifiers,
            int x, int y) {
        super(dsc, x, y);
        this.targetActions = action;
        this.dropAction = dropAction;
        this.gestureModifiers = modifiers;
    }

    /** Qué acepta el destino. */
    public int getTargetActions() {
        return this.targetActions;
    }

    /**
     * Los modificadores en la codificación vieja.
     *
     * @deprecated mezcla teclas con botones de forma ambigua. Usar {@link #getGestureModifiersEx}.
     */
    @Deprecated
    public int getGestureModifiers() {
        return this.gestureModifiers;
    }

    /** Los modificadores en la codificación nueva. */
    public int getGestureModifiersEx() {
        return this.gestureModifiers;
    }

    /**
     * Qué acción pide el usuario, según las teclas.
     *
     * <p>Se saca de los modificadores y **no** de las acciones del destino: es lo que el usuario
     * quiere, aunque no se pueda.
     */
    public int getUserAction() {
        return this.dropAction;
    }

    /**
     * Qué va a pasar de verdad: lo que el usuario pide **y** el destino acepta.
     *
     * <p>Cero si no coinciden, que es cuando el cursor muestra que ahí no se puede soltar.
     */
    public int getDropAction() {
        return this.dropAction & this.targetActions;
    }
}
