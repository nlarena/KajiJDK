package java.awt.dnd;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

/**
 * Decide **cuándo** un movimiento del ratón deja de ser un clic torpe y pasa a ser un arrastre.
 *
 * <p>La pregunta parece tonta hasta que hay que contestarla: apretar el botón y moverse dos píxeles
 * no es arrastrar, moverse veinte sí, y el umbral depende de la plataforma y hasta del dispositivo.
 * Poner ese criterio acá, y no en cada aplicación, es lo que hace que arrastrar se sienta igual en
 * todos lados.
 *
 * <p>La clase junta los eventos del gesto mientras lo va reconociendo, y cuando se convence dispara
 * un {@link DragGestureEvent} con **todos** ellos. Por eso guarda la lista en vez de sólo el último:
 * quien decida qué se arrastra suele necesitar dónde arrancó el gesto y no dónde está ahora.
 *
 * <p>Como {@link DropTarget}, admite **un solo** oyente: dos podrían empezar dos arrastres con el
 * mismo gesto.
 */
public abstract class DragGestureRecognizer implements Serializable {

    private static final long serialVersionUID = 8996673345831063337L;

    /** Quién va a llevar adelante el arrastre. */
    protected DragSource dragSource;

    /** Sobre qué componente se vigila el gesto. */
    protected Component component;

    /** A quién avisarle cuando el gesto se reconozca. */
    protected transient DragGestureListener dragGestureListener;

    /** Qué acciones acepta el origen. */
    protected int sourceActions;

    /** Los eventos que se juntaron del gesto en curso. */
    /**
     * El tipo declarado es `ArrayList` y no `List`, contra lo que uno escribiria hoy: es un campo
     * **protegido** desde 1.2, asi que su tipo forma parte de la API y una subclase puede depender
     * de el.
     */
    protected ArrayList<InputEvent> events = new ArrayList<InputEvent>(1);

    /**
     * Con todo dado.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c, int sa, DragGestureListener dgl) {
        if (ds == null) {
            throw new IllegalArgumentException("null DragSource");
        }
        this.dragSource = ds;
        this.component = c;
        this.sourceActions = sa & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        if (dgl != null) {
            try {
                this.addDragGestureListener(dgl);
            } catch (TooManyListenersException e) {
                // No puede pasar: acabamos de construirlo y no tiene oyentes.
                throw new InternalError(e.toString());
            }
        }
        if (c != null) {
            this.registerListeners();
        }
    }

    /**
     * Sin oyente.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c, int sa) {
        this(ds, c, sa, null);
    }

    /**
     * Aceptando cualquier acción.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected DragGestureRecognizer(DragSource ds, Component c) {
        this(ds, c, DnDConstants.ACTION_NONE);
    }

    /**
     * Sin componente todavía.
     *
     * @throws IllegalArgumentException si el origen de arrastre es `null`
     */
    protected DragGestureRecognizer(DragSource ds) {
        this(ds, null);
    }

    /** Empieza a escuchar al componente; lo escribe cada reconocedor concreto. */
    protected abstract void registerListeners();

    /** Deja de escucharlo. */
    protected abstract void unregisterListeners();

    /** Quién va a llevar adelante el arrastre. */
    public DragSource getDragSource() {
        return this.dragSource;
    }

    /** Sobre qué componente vigila. */
    public synchronized Component getComponent() {
        return this.component;
    }

    /**
     * Cambia el componente que vigila.
     *
     * <p>Deja de escuchar al anterior y empieza con el nuevo: es lo que hace que el reconocedor no
     * siga colgado de un componente que ya no se usa.
     */
    public synchronized void setComponent(Component c) {
        if (this.component != null && this.dragGestureListener != null) {
            this.unregisterListeners();
        }
        this.component = c;
        if (this.component != null && this.dragGestureListener != null) {
            this.registerListeners();
        }
    }

    /** Qué acciones acepta el origen. */
    public synchronized int getSourceActions() {
        return this.sourceActions;
    }

    /** Cambia qué acciones acepta. */
    public synchronized void setSourceActions(int actions) {
        this.sourceActions = actions
                & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }

    /**
     * El evento que empezó el gesto.
     *
     * @return el primero, o `null` si todavía no hay gesto
     */
    public InputEvent getTriggerEvent() {
        if (this.events.isEmpty()) {
            return null;
        }
        return this.events.get(0);
    }

    /** Descarta el gesto a medio reconocer y vuelve a empezar. */
    public void resetRecognizer() {
        this.events.clear();
    }

    /**
     * Registra el oyente.
     *
     * @throws TooManyListenersException si ya hay uno
     */
    public synchronized void addDragGestureListener(DragGestureListener dgl)
            throws TooManyListenersException {
        if (this.dragGestureListener != null) {
            throw new TooManyListenersException();
        }
        this.dragGestureListener = dgl;
        if (this.component != null) {
            this.registerListeners();
        }
    }

    /**
     * Saca al oyente.
     *
     * @throws IllegalArgumentException si no es el que estaba registrado
     */
    public synchronized void removeDragGestureListener(DragGestureListener dgl) {
        if (this.dragGestureListener != dgl) {
            throw new IllegalArgumentException();
        }
        this.dragGestureListener = null;
        if (this.component != null) {
            this.unregisterListeners();
        }
    }

    /**
     * Dispara el aviso de que el gesto se reconoció.
     *
     * <p>Vacía la lista de eventos después de avisar: el gesto ya se consumió, y dejarlos haría que
     * el siguiente arrastre arrancara con la basura del anterior.
     */
    protected synchronized void fireDragGestureRecognized(int dragAction, Point p) {
        try {
            if (this.dragGestureListener != null) {
                this.dragGestureListener.dragGestureRecognized(
                        new DragGestureEvent(this, dragAction, p, this.events));
            }
        } finally {
            this.events.clear();
        }
    }

    /** Suma un evento al gesto en curso. */
    protected synchronized void appendEvent(InputEvent awtie) {
        this.events.add(awtie);
    }
}
