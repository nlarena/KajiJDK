package javax.swing;

import java.awt.Component;
import java.awt.FocusTraversalPolicy;

/**
 * Una politica de recorrido de foco que ademas sabe de ventanas internas.
 *
 * <h2>Que agrega, y por que hace falta</h2>
 *
 * <p>{@link FocusTraversalPolicy} sabe decir cual es el primer componente de un contenedor. Eso
 * alcanza para una ventana del sistema, que se abre una vez. Una ventana <em>interna</em> se activa
 * y se desactiva muchas veces, y cada vez que vuelve a activarse el foco tiene que caer donde el
 * usuario lo dejo -- no en el primer campo.
 *
 * <p>{@link #getInitialComponent} es esa pregunta: "cuando esta ventana se abre por primera vez,
 * donde va el foco". Es distinta de "cual es el primero" y de "cual es el ultimo que lo tuvo", y
 * tenerla aparte es lo que permite responder las tres cosas sin mezclarlas.
 */
public abstract class InternalFrameFocusTraversalPolicy extends FocusTraversalPolicy {

    /** Para las subclases. */
    protected InternalFrameFocusTraversalPolicy() {
    }

    /**
     * Donde va el foco la primera vez que se abre esa ventana interna.
     *
     * <p>Por omision, el mismo que {@code getDefaultComponent}. Una subclase la separa cuando la
     * ventana tiene un campo al que conviene ir de entrada.
     */
    public Component getInitialComponent(JInternalFrame frame) {
        return getDefaultComponent(frame);
    }
}
