package javax.swing.event;

import java.awt.AWTEvent;

import javax.swing.JInternalFrame;

/**
 * Algo paso con una ventana interna.
 *
 * <p>Los identificadores replican los de {@link java.awt.event.WindowEvent} en un rango propio, y
 * esa simetria es a proposito: una ventana interna tiene el mismo ciclo de vida que una de verdad
 * —se abre, se activa, se minimiza, se cierra— aunque viva adentro de otra.
 *
 * <p>La distincion que importa es {@link #INTERNAL_FRAME_CLOSING} contra
 * {@link #INTERNAL_FRAME_CLOSED}: el primero se puede cancelar, el segundo ya paso.
 */
public class InternalFrameEvent extends AWTEvent {

    private static final long serialVersionUID = 1L;

    /** El primero de los identificadores de este rango. */
    public static final int INTERNAL_FRAME_FIRST = 25549;

    /** El ultimo de los identificadores de este rango. */
    public static final int INTERNAL_FRAME_LAST = 25555;

    /** Se abrio. */
    public static final int INTERNAL_FRAME_OPENED = 25549;

    /** Se esta por cerrar; todavia se puede cancelar. */
    public static final int INTERNAL_FRAME_CLOSING = 25550;

    /** Se cerro. */
    public static final int INTERNAL_FRAME_CLOSED = 25551;

    /** Se minimizo a un icono. */
    public static final int INTERNAL_FRAME_ICONIFIED = 25552;

    /** Se restauro. */
    public static final int INTERNAL_FRAME_DEICONIFIED = 25553;

    /** Tomo el foco. */
    public static final int INTERNAL_FRAME_ACTIVATED = 25554;

    /** Perdio el foco. */
    public static final int INTERNAL_FRAME_DEACTIVATED = 25555;

    public InternalFrameEvent(JInternalFrame source, int id) {
        super(source, id);
    }

    public String paramString() {
        int id = getID();
        if (id == INTERNAL_FRAME_OPENED) {
            return "INTERNAL_FRAME_OPENED";
        }
        if (id == INTERNAL_FRAME_CLOSING) {
            return "INTERNAL_FRAME_CLOSING";
        }
        if (id == INTERNAL_FRAME_CLOSED) {
            return "INTERNAL_FRAME_CLOSED";
        }
        if (id == INTERNAL_FRAME_ICONIFIED) {
            return "INTERNAL_FRAME_ICONIFIED";
        }
        if (id == INTERNAL_FRAME_DEICONIFIED) {
            return "INTERNAL_FRAME_DEICONIFIED";
        }
        if (id == INTERNAL_FRAME_ACTIVATED) {
            return "INTERNAL_FRAME_ACTIVATED";
        }
        if (id == INTERNAL_FRAME_DEACTIVATED) {
            return "INTERNAL_FRAME_DEACTIVATED";
        }
        return "unknown type";
    }

    /** La ventana interna. */
    public JInternalFrame getInternalFrame() {
        return (JInternalFrame) getSource();
    }
}
