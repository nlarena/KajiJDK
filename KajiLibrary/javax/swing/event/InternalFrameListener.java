package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse del ciclo de vida de una ventana interna.
 *
 * <p>Siete metodos, y quien atienda uno solo va a querer {@link InternalFrameAdapter}.
 */
public interface InternalFrameListener extends EventListener {

    /** Se abrio. */
    void internalFrameOpened(InternalFrameEvent e);

    /** Se esta por cerrar; todavia se puede cancelar. */
    void internalFrameClosing(InternalFrameEvent e);

    /** Se cerro. */
    void internalFrameClosed(InternalFrameEvent e);

    /** Se minimizo. */
    void internalFrameIconified(InternalFrameEvent e);

    /** Se restauro. */
    void internalFrameDeiconified(InternalFrameEvent e);

    /** Tomo el foco. */
    void internalFrameActivated(InternalFrameEvent e);

    /** Perdio el foco. */
    void internalFrameDeactivated(InternalFrameEvent e);
}
