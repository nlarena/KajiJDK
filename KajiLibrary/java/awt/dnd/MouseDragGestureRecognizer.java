package java.awt.dnd;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * La base de los reconocedores de gesto que escuchan el **ratón**.
 *
 * <p>Es donde se conecta el reconocedor con el componente: implementa las dos interfaces de ratón, y
 * {@link #registerListeners} lo enchufa. Lo que **no** hace es decidir cuándo el gesto está
 * reconocido — los siete métodos de ratón están vacíos a propósito.
 *
 * <p>Esa decisión depende de la plataforma —cuántos píxeles, qué botón, con qué teclas— y por eso
 * la clase es abstracta aunque no tenga ningún método abstracto propio: hereda los dos de
 * {@link DragGestureRecognizer} y deja el criterio para el reconocedor concreto de cada sistema.
 */
public abstract class MouseDragGestureRecognizer extends DragGestureRecognizer
        implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 6220099344182281120L;

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act,
            DragGestureListener dgl) {
        super(ds, c, act, dgl);
    }

    /**
     * Sin oyente.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c, int act) {
        this(ds, c, act, null);
    }

    /**
     * Aceptando cualquier acción.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * Sin componente todavía.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected MouseDragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /** Se engancha a los eventos de ratón del componente. */
    protected void registerListeners() {
        this.component.addMouseListener(this);
        this.component.addMouseMotionListener(this);
    }

    /** Se desengancha. */
    protected void unregisterListeners() {
        this.component.removeMouseListener(this);
        this.component.removeMouseMotionListener(this);
    }

    /** No hace nada: el criterio lo pone el reconocedor concreto. */
    public void mouseClicked(MouseEvent e) {
    }

    /** No hace nada. */
    public void mousePressed(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseReleased(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseEntered(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseExited(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseDragged(MouseEvent e) {
    }

    /** No hace nada. */
    public void mouseMoved(MouseEvent e) {
    }
}
