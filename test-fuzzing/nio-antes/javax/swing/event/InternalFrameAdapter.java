package javax.swing.event;

/**
 * An {@link InternalFrameListener} with the seven methods empty.
 *
 * <p>The adapter pattern: attending only to the closing should not force writing six methods that
 * do nothing. It is abstract even though it has no abstract methods --it inherits them all with a
 * body-- because instantiating it as it is would serve no purpose.
 */
public abstract class InternalFrameAdapter implements InternalFrameListener {

    /** For the subclasses. */
    protected InternalFrameAdapter() {
    }

    public void internalFrameOpened(InternalFrameEvent e) {
    }

    public void internalFrameClosing(InternalFrameEvent e) {
    }

    public void internalFrameClosed(InternalFrameEvent e) {
    }

    public void internalFrameIconified(InternalFrameEvent e) {
    }

    public void internalFrameDeiconified(InternalFrameEvent e) {
    }

    public void internalFrameActivated(InternalFrameEvent e) {
    }

    public void internalFrameDeactivated(InternalFrameEvent e) {
    }
}
