package java.awt.event;

import java.awt.Window;

/**
 * Le pasó algo a una ventana: se abrió, se cerró, se minimizó, ganó o perdió el foco.
 *
 * <p>La distinción que más se usa mal es entre {@code WINDOW_CLOSING} y {@code WINDOW_CLOSED}. El
 * primero es el **pedido**: llega cuando el usuario aprieta la cruz, la ventana todavía está, y es
 * donde se pregunta si quiere guardar o se decide no cerrar. El segundo llega cuando ya se cerró y
 * no hay nada que decidir.
 *
 * <p>La "ventana opuesta" es la otra parte del cambio de foco: al perderlo, cuál se lo llevó; al
 * ganarlo, a cuál se lo sacó. Es `null` cuando la otra ventana es de otra aplicación.
 */
public class WindowEvent extends ComponentEvent {

    private static final long serialVersionUID = -1567959133147912127L;

    /** Pasó a ser la ventana activa. */
    public static final int WINDOW_ACTIVATED = 205;

    /** La ventana ya se cerró. */
    public static final int WINDOW_CLOSED = 202;

    /** El usuario pidió cerrarla; todavía está abierta. */
    public static final int WINDOW_CLOSING = 201;

    /** Dejó de ser la ventana activa. */
    public static final int WINDOW_DEACTIVATED = 206;

    /** Se restauró. */
    public static final int WINDOW_DEICONIFIED = 204;

    /** El primer identificador de la familia. */
    public static final int WINDOW_FIRST = 200;

    /** Ganó el foco del teclado. */
    public static final int WINDOW_GAINED_FOCUS = 207;

    /** Se minimizó. */
    public static final int WINDOW_ICONIFIED = 203;

    /** El último identificador de la familia. */
    public static final int WINDOW_LAST = 209;

    /** Perdió el foco del teclado. */
    public static final int WINDOW_LOST_FOCUS = 208;

    /** La ventana se abrió por primera vez. */
    public static final int WINDOW_OPENED = 200;

    /** Cambió entre normal, minimizada y maximizada. */
    public static final int WINDOW_STATE_CHANGED = 209;

    private final Window opposite;
    private final int oldState;
    private final int newState;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public WindowEvent(Window source, int id, Window opposite, int oldState, int newState) {
        super(source, id);
        this.opposite = opposite;
        this.oldState = oldState;
        this.newState = newState;
    }

    /**
     * Con la ventana opuesta, para los cambios de foco.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public WindowEvent(Window source, int id, Window opposite) {
        this(source, id, opposite, 0, 0);
    }

    /**
     * Con los dos estados, para los cambios de estado.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public WindowEvent(Window source, int id, int oldState, int newState) {
        this(source, id, null, oldState, newState);
    }

    /**
     * Sólo con la ventana y el identificador.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public WindowEvent(Window source, int id) {
        this(source, id, null, 0, 0);
    }

    /** La ventana a la que le pasó. */
    public Window getWindow() {
        if (this.source instanceof Window) {
            return (Window) this.source;
        }
        return null;
    }

    /** La otra parte del cambio de foco, o `null` si es de otra aplicación. */
    public Window getOppositeWindow() {
        return this.opposite;
    }

    /** Cómo estaba antes. */
    public int getOldState() {
        return this.oldState;
    }

    /** Cómo quedó. */
    public int getNewState() {
        return this.newState;
    }

    public String paramString() {
        String tipo;
        if (this.id == WINDOW_OPENED) {
            tipo = "WINDOW_OPENED";
        } else if (this.id == WINDOW_CLOSING) {
            tipo = "WINDOW_CLOSING";
        } else if (this.id == WINDOW_CLOSED) {
            tipo = "WINDOW_CLOSED";
        } else if (this.id == WINDOW_ICONIFIED) {
            tipo = "WINDOW_ICONIFIED";
        } else if (this.id == WINDOW_DEICONIFIED) {
            tipo = "WINDOW_DEICONIFIED";
        } else if (this.id == WINDOW_ACTIVATED) {
            tipo = "WINDOW_ACTIVATED";
        } else if (this.id == WINDOW_DEACTIVATED) {
            tipo = "WINDOW_DEACTIVATED";
        } else if (this.id == WINDOW_GAINED_FOCUS) {
            tipo = "WINDOW_GAINED_FOCUS";
        } else if (this.id == WINDOW_LOST_FOCUS) {
            tipo = "WINDOW_LOST_FOCUS";
        } else if (this.id == WINDOW_STATE_CHANGED) {
            tipo = "WINDOW_STATE_CHANGED";
        } else {
            tipo = "unknown type";
        }
        return tipo + ",opposite=" + this.opposite + ",oldState=" + this.oldState + ",newState="
                + this.newState;
    }
}
