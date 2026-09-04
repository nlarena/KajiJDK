package javax.swing.event;

/**
 * Un {@link InternalFrameListener} con los siete metodos vacios.
 *
 * <p>El patron adaptador: atender solo el cierre no deberia obligar a escribir seis metodos que no
 * hacen nada. Es abstracta aunque no tenga metodos abstractos —heredo todos con cuerpo— porque
 * instanciarla tal cual no serviria para nada.
 */
public abstract class InternalFrameAdapter implements InternalFrameListener {

    /** Para las subclases. */
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
