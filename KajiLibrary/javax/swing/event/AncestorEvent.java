package javax.swing.event;

import java.awt.AWTEvent;
import java.awt.Container;

import javax.swing.JComponent;

/**
 * Un ancestro del componente se agrego, se saco o se movio.
 *
 * <h2>Para que sirve escuchar a los ancestros</h2>
 *
 * <p>Un componente no se entera por si mismo de que dejo de estar en pantalla: lo que ocurrio fue
 * que <em>su abuelo</em> se saco de la ventana. Escuchar solo los cambios propios deja al componente
 * creyendo que sigue visible.
 *
 * <p>Es lo que permite que algo suelte recursos —un temporizador, una suscripcion— cuando deja de
 * verse, sin tener que vigilar la jerarquia entera a mano.
 *
 * <p>Lleva <strong>dos</strong> contenedores: el ancestro que cambio y el padre <em>que tenia</em>.
 * El segundo hace falta porque al momento de avisar el vinculo ya se rompio, y sin el no habria
 * forma de saber de donde salio.
 */
public class AncestorEvent extends AWTEvent {

    private static final long serialVersionUID = 1L;

    /** Un ancestro se agrego a la jerarquia o se hizo visible. */
    public static final int ANCESTOR_ADDED = 1;

    /** Un ancestro se saco o se escondio. */
    public static final int ANCESTOR_REMOVED = 2;

    /** Un ancestro se movio. */
    public static final int ANCESTOR_MOVED = 3;

    /** El ancestro que cambio. */
    Container ancestor;

    /** El padre que ese ancestro tenia. */
    Container ancestorParent;

    public AncestorEvent(JComponent source, int id, Container ancestor, Container ancestorParent) {
        super(source, id);
        this.ancestor = ancestor;
        this.ancestorParent = ancestorParent;
    }

    /** El ancestro que cambio. */
    public Container getAncestor() {
        return this.ancestor;
    }

    /** El padre que ese ancestro tenia; ver la nota de la clase. */
    public Container getAncestorParent() {
        return this.ancestorParent;
    }

    /** El componente que escucha, o sea el descendiente. */
    public JComponent getComponent() {
        return (JComponent) getSource();
    }
}
