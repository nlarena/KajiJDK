package javax.swing.event;

import java.util.EventListener;

/**
 * Whoever wants to hear about an internal frame's life cycle.
 *
 * <p>Seven methods, and whoever attends to a single one will want
 * {@link InternalFrameAdapter}.
 */
public interface InternalFrameListener extends EventListener {

    /** It opened. */
    void internalFrameOpened(InternalFrameEvent e);

    /** It is about to close; it can still be cancelled. */
    void internalFrameClosing(InternalFrameEvent e);

    /** It closed. */
    void internalFrameClosed(InternalFrameEvent e);

    /** It was iconified. */
    void internalFrameIconified(InternalFrameEvent e);

    /** It was restored. */
    void internalFrameDeiconified(InternalFrameEvent e);

    /** It took the focus. */
    void internalFrameActivated(InternalFrameEvent e);

    /** It lost the focus. */
    void internalFrameDeactivated(InternalFrameEvent e);
}
