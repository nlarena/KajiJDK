package java.awt.dnd;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

/**
 * El usuario hizo el gesto de empezar a arrastrar.
 *
 * <p>Trae **todos** los eventos del gesto, no sólo el último: el apretón de botón y los movimientos
 * que hicieron falta para reconocerlo. Sirve para decidir qué se está arrastrando a partir de dónde
 * arrancó el gesto, que no es donde está el puntero ahora.
 *
 * <p>Los {@code startDrag} de acá son el atajo cómodo: le pasan todo al {@link DragSource} con este
 * evento como disparador, para no tener que repetir los datos que el evento ya tiene.
 */
public class DragGestureEvent extends EventObject {

    private static final long serialVersionUID = 9080172649166731306L;

    private final transient List<InputEvent> events;
    private final DragSource dragSource;
    private final Component component;
    private final Point origin;
    private final int action;

    /**
     * Con el reconocedor, la acción, el origen y los eventos del gesto.
     *
     * @throws IllegalArgumentException si falta el reconocedor, su componente o su origen de
     *     arrastre, si el punto o la lista son `null`, o si la acción no es una de las de
     *     {@link DnDConstants}
     */
    public DragGestureEvent(DragGestureRecognizer dgr, int act, Point ori,
            List<? extends InputEvent> evs) {
        super(dgr);
        if (evs == null || evs.isEmpty()) {
            throw new IllegalArgumentException("null or empty list of events");
        }
        if (act != DnDConstants.ACTION_COPY && act != DnDConstants.ACTION_MOVE
                && act != DnDConstants.ACTION_LINK) {
            throw new IllegalArgumentException("bad action");
        }
        if (ori == null) {
            throw new IllegalArgumentException("null origin");
        }
        this.component = dgr.getComponent();
        this.dragSource = dgr.getDragSource();
        if (this.component == null) {
            throw new IllegalArgumentException("null component");
        }
        if (this.dragSource == null) {
            throw new IllegalArgumentException("null DragSource");
        }
        this.events = new ArrayList<InputEvent>(evs);
        this.action = act;
        this.origin = ori;
    }

    /** El reconocedor que lo disparó. */
    public DragGestureRecognizer getSourceAsDragGestureRecognizer() {
        return (DragGestureRecognizer) this.getSource();
    }

    /** Sobre qué componente se hizo el gesto. */
    public Component getComponent() {
        return this.component;
    }

    /** Quién va a llevar adelante el arrastre. */
    public DragSource getDragSource() {
        return this.dragSource;
    }

    /** Dónde arrancó el gesto, relativo al componente. */
    public Point getDragOrigin() {
        return this.origin;
    }

    /** Todos los eventos que formaron el gesto, en orden. */
    public Iterator<InputEvent> iterator() {
        return this.events.iterator();
    }

    /** Los eventos del gesto, como arreglo. */
    public Object[] toArray() {
        return this.events.toArray();
    }

    /**
     * Los eventos del gesto, en el arreglo dado.
     *
     * @throws ArrayStoreException si el tipo del arreglo no acepta eventos de entrada
     */
    public Object[] toArray(Object[] array) {
        return this.events.toArray(array);
    }

    /** Qué acción pide el usuario. */
    public int getDragAction() {
        return this.action;
    }

    /** El primer evento del gesto: el que lo empezó. */
    public InputEvent getTriggerEvent() {
        return this.getSourceAsDragGestureRecognizer().getTriggerEvent();
    }

    /**
     * Arranca el arrastre.
     *
     * @throws InvalidDnDOperationException si el arrastre no se puede empezar
     */
    public void startDrag(Cursor dragCursor, Transferable transferable)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, transferable, null);
    }

    /**
     * Arranca el arrastre con un oyente que siga su evolución.
     *
     * @throws InvalidDnDOperationException si el arrastre no se puede empezar
     */
    public void startDrag(Cursor dragCursor, Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, transferable, dsl);
    }

    /**
     * Arranca el arrastre con una imagen que sigue al puntero.
     *
     * @throws InvalidDnDOperationException si el arrastre no se puede empezar
     */
    public void startDrag(Cursor dragCursor, Image dragImage, Point imageOffset,
            Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        this.dragSource.startDrag(this, dragCursor, dragImage, imageOffset, transferable, dsl);
    }
}
