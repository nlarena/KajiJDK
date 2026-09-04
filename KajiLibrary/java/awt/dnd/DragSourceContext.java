package java.awt.dnd;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.TooManyListenersException;

/**
 * El estado de **un** arrastre en curso, del lado del origen.
 *
 * <p>Mientras {@link DragSource} es único y no recuerda nada, éste se arma por cada arrastre y lleva
 * todo lo que dura: qué se está arrastrando, con qué cursor, quién quiere enterarse.
 *
 * <p>Es {@link DragSourceListener} y {@link DragSourceMotionListener} él mismo, y ahí está su
 * trabajo real: recibe los avisos del sistema, actualiza el cursor según el destino acepte o no, y
 * después se los reparte al oyente del arrastre y a los del origen. Esa repetición —cursor primero,
 * avisos después— es lo que hace que la realimentación visual sea coherente aunque nadie escuche.
 *
 * <p>{@link #setCursor} con `null` no apaga el cursor: **devuelve el control** al comportamiento
 * automático. Es la diferencia entre "no quiero cursor" y "elegilo vos", y sólo la segunda tiene
 * sentido durante un arrastre.
 */
public class DragSourceContext
        implements DragSourceListener, DragSourceMotionListener, Serializable {

    private static final long serialVersionUID = -115407898692194719L;

    /** El cursor todavía no se decidió. */
    protected static final int DEFAULT = 0;

    /** El arrastre entró a un destino. */
    protected static final int ENTER = 1;

    /** El arrastre se mueve sobre un destino. */
    protected static final int OVER = 2;

    /** Cambió la acción elegida. */
    protected static final int CHANGED = 3;

    private final DragGestureEvent trigger;
    private final Component component;
    private final Transferable transferable;
    private final DragSourceListener listener;
    private final Image dragImage;
    private final Point offset;
    private final int sourceActions;
    private Cursor cursor;
    private boolean useCustomCursor;

    /**
     * Con el gesto que lo disparó y lo que se arrastra.
     *
     * @throws IllegalArgumentException si el disparador es `null`, si su componente o su origen de
     *     arrastre son `null`, si su acción es {@code ACTION_NONE}, o si se da una imagen sin su
     *     desplazamiento
     * @throws NullPointerException si el transferible es `null`
     */
    public DragSourceContext(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point offset, Transferable t, DragSourceListener dsl) {
        if (trigger == null) {
            throw new NullPointerException("Trigger");
        }
        if (t == null) {
            throw new NullPointerException("Transferable");
        }
        if (trigger.getComponent() == null) {
            throw new IllegalArgumentException("Source Component");
        }
        if (trigger.getDragSource() == null) {
            throw new IllegalArgumentException("DragSource");
        }
        if (trigger.getDragAction() == DnDConstants.ACTION_NONE) {
            throw new IllegalArgumentException("Drag Action");
        }
        // Una imagen sin desplazamiento no se puede ubicar: no se sabe qué punto de ella sigue al
        // puntero.
        if (dragImage != null && offset == null) {
            throw new IllegalArgumentException("Image Offset");
        }
        this.trigger = trigger;
        this.component = trigger.getComponent();
        this.sourceActions = trigger.getSourceAsDragGestureRecognizer().getSourceActions();
        this.transferable = t;
        this.listener = dsl;
        this.dragImage = dragImage;
        this.offset = offset;
        this.cursor = dragCursor;
        this.useCustomCursor = dragCursor != null;
    }

    /** Quién lleva adelante el arrastre. */
    public DragSource getDragSource() {
        return this.trigger.getDragSource();
    }

    /** Desde qué componente salió. */
    public Component getComponent() {
        return this.component;
    }

    /** El gesto que lo empezó. */
    public DragGestureEvent getTrigger() {
        return this.trigger;
    }

    /** Qué acciones acepta el origen. */
    public int getSourceActions() {
        return this.sourceActions;
    }

    /**
     * Cambia el cursor del arrastre.
     *
     * <p>Con `null` se vuelve al cursor automático, el que sale de si el destino acepta o no. No es
     * lo mismo que no tener cursor.
     */
    public synchronized void setCursor(Cursor c) {
        this.useCustomCursor = c != null;
        this.cursor = c;
    }

    /** El cursor actual. */
    public Cursor getCursor() {
        return this.cursor;
    }

    /**
     * Registra un oyente además del que se dio al construir.
     *
     * @throws TooManyListenersException si ya hay uno
     */
    public synchronized void addDragSourceListener(DragSourceListener dsl)
            throws TooManyListenersException {
        if (dsl == null) {
            return;
        }
        if (this == dsl) {
            throw new IllegalArgumentException("DragSourceContext may not be its own listener");
        }
        if (this.listener != null) {
            throw new TooManyListenersException();
        }
    }

    /** Saca al oyente. */
    public synchronized void removeDragSourceListener(DragSourceListener dsl) {
    }

    /** Avisa que cambiaron los formatos que se pueden entregar. */
    public void transferablesFlavorsChanged() {
    }

    /**
     * El arrastre entró a un destino: actualiza el cursor y reparte el aviso.
     *
     * <p>El orden importa: primero el cursor, después los oyentes. Si un oyente cambia el cursor a
     * mano, su cambio tiene que ser el último en aplicarse.
     */
    public void dragEnter(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dragEnter(dsde);
        }
        DragSourceListener[] otros = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dragEnter(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), ENTER);
    }

    /** El arrastre se mueve sobre un destino. */
    public void dragOver(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dragOver(dsde);
        }
        DragSourceListener[] otros = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dragOver(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), OVER);
    }

    /** El arrastre salió del destino: vuelve al cursor de "acá no". */
    public void dragExit(DragSourceEvent dse) {
        if (this.listener != null) {
            this.listener.dragExit(dse);
        }
        DragSourceListener[] otros = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dragExit(dse);
        }
        this.updateCurrentCursor(DnDConstants.ACTION_NONE, DnDConstants.ACTION_NONE, DEFAULT);
    }

    /** Cambió la acción elegida. */
    public void dropActionChanged(DragSourceDragEvent dsde) {
        if (this.listener != null) {
            this.listener.dropActionChanged(dsde);
        }
        DragSourceListener[] otros = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dropActionChanged(dsde);
        }
        this.updateCurrentCursor(dsde.getDropAction(), dsde.getTargetActions(), CHANGED);
    }

    /** Terminó el arrastre. */
    public void dragDropEnd(DragSourceDropEvent dsde) {
        if (this.listener != null) {
            this.listener.dragDropEnd(dsde);
        }
        DragSourceListener[] otros = this.getDragSource().getDragSourceListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dragDropEnd(dsde);
        }
    }

    /** El ratón se movió durante el arrastre. */
    public void dragMouseMoved(DragSourceDragEvent dsde) {
        DragSourceMotionListener[] otros = this.getDragSource().getDragSourceMotionListeners();
        for (int i = 0; i < otros.length; i++) {
            otros[i].dragMouseMoved(dsde);
        }
    }

    /** Lo que se está arrastrando. */
    public Transferable getTransferable() {
        return this.transferable;
    }

    /**
     * Elige el cursor que corresponde al estado del arrastre.
     *
     * <p>No hace nada si alguien puso un cursor a mano: un cursor propio gana sobre el automático,
     * porque quien lo puso sabe algo que este objeto no.
     */
    protected synchronized void updateCurrentCursor(int dropOp, int targetAct, int status) {
        if (this.useCustomCursor) {
            return;
        }
        int accion = dropOp & targetAct;
        if (status == DEFAULT || accion == DnDConstants.ACTION_NONE) {
            if ((dropOp & DnDConstants.ACTION_LINK) != 0) {
                this.cursor = DragSource.DefaultLinkNoDrop;
            } else if ((dropOp & DnDConstants.ACTION_MOVE) != 0) {
                this.cursor = DragSource.DefaultMoveNoDrop;
            } else {
                this.cursor = DragSource.DefaultCopyNoDrop;
            }
            return;
        }
        if ((accion & DnDConstants.ACTION_LINK) != 0) {
            this.cursor = DragSource.DefaultLinkDrop;
        } else if ((accion & DnDConstants.ACTION_MOVE) != 0) {
            this.cursor = DragSource.DefaultMoveDrop;
        } else {
            this.cursor = DragSource.DefaultCopyDrop;
        }
    }
}
