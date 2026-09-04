package java.awt.event;

import java.awt.Component;

/**
 * Un componente ganó o perdió el foco del teclado.
 *
 * <p>Lo **temporal** es la distinción importante y la que se pasa por alto. Un foco que se pierde
 * temporalmente —porque se abrió un menú, porque la ventana pasó a segundo plano— va a volver, y no
 * es el momento de validar el campo ni de guardar. Uno permanente sí.
 *
 * <p>La causa, que se agregó mucho después, dice **por qué**: si fue un clic, un tabulador, un
 * pedido explícito o el arranque de la ventana. Sirve para tratar distinto un foco que el usuario
 * pidió de uno que le tocó.
 */
public class FocusEvent extends ComponentEvent {

    private static final long serialVersionUID = 523753786457416396L;

    /** Por qué cambió el foco. */
    public static enum Cause {

        /** No se sabe. */
        UNKNOWN,

        /** Un clic del ratón. */
        MOUSE_EVENT,

        /** Un recorrido con el tabulador. */
        TRAVERSAL,

        /** Un recorrido hacia arriba en el árbol. */
        TRAVERSAL_UP,

        /** Un recorrido hacia abajo en el árbol. */
        TRAVERSAL_DOWN,

        /** Un recorrido hacia adelante. */
        TRAVERSAL_FORWARD,

        /** Un recorrido hacia atrás. */
        TRAVERSAL_BACKWARD,

        /** Alguien lo pidió explícitamente. */
        MANUAL_REQUEST,

        /** El sistema lo movió solo. */
        AUTOMATIC_TRAVERSAL,

        /** Se volvió al foco anterior porque el nuevo no lo aceptó. */
        ROLLBACK,

        /** La ventana pasó a ser la activa. */
        ACTIVATION,

        /** Se soltó el foco global. */
        CLEAR_GLOBAL_FOCUS_OWNER,

        /** Pasó algo que no encaja en ninguna de las otras. */
        UNEXPECTED
    }

    /** El primer identificador de la familia. */
    public static final int FOCUS_FIRST = 1004;

    /** El componente ganó el foco. */
    public static final int FOCUS_GAINED = 1004;

    /** El último identificador de la familia. */
    public static final int FOCUS_LAST = 1005;

    /** El componente perdió el foco. */
    public static final int FOCUS_LOST = 1005;

    private final boolean temporary;
    private final Component opposite;
    private final Cause cause;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     * @throws NullPointerException si la causa es `null`
     */
    public FocusEvent(Component source, int id, boolean temporary, Component opposite,
            Cause cause) {
        super(source, id);
        if (cause == null) {
            throw new NullPointerException("null cause");
        }
        this.temporary = temporary;
        this.opposite = opposite;
        this.cause = cause;
    }

    /**
     * Sin decir la causa.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public FocusEvent(Component source, int id, boolean temporary, Component opposite) {
        this(source, id, temporary, opposite, Cause.UNKNOWN);
    }

    /**
     * Sin el componente opuesto.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public FocusEvent(Component source, int id, boolean temporary) {
        this(source, id, temporary, null, Cause.UNKNOWN);
    }

    /**
     * Permanente y sin opuesto.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public FocusEvent(Component source, int id) {
        this(source, id, false, null, Cause.UNKNOWN);
    }

    /** Si el foco va a volver. */
    public boolean isTemporary() {
        return this.temporary;
    }

    /** La otra parte del cambio, o `null` si es de otra aplicación. */
    public Component getOppositeComponent() {
        return this.opposite;
    }

    /** Por qué cambió el foco. */
    public final Cause getCause() {
        return this.cause;
    }

    public String paramString() {
        String tipo;
        if (this.id == FOCUS_GAINED) {
            tipo = "FOCUS_GAINED";
        } else if (this.id == FOCUS_LOST) {
            tipo = "FOCUS_LOST";
        } else {
            tipo = "unknown type";
        }
        return tipo + (this.temporary ? ",temporary" : ",permanent") + ",opposite="
                + this.opposite + ",cause=" + this.cause;
    }
}
