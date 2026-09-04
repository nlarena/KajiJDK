package java.awt.dnd;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.io.Serializable;
import java.util.TooManyListenersException;

/**
 * Declara que un componente **puede recibir** cosas arrastradas.
 *
 * <p>Se le engancha a un componente con {@code setDropTarget} y con eso el componente pasa a ser un
 * destino válido. Todo lo demás —qué formatos acepta, qué hace al soltar— lo decide el
 * {@link DropTargetListener} que se le registre.
 *
 * <p>Es {@link DropTargetListener} él mismo, y eso permite dos formas de usarlo: registrarle un
 * oyente, o heredar de él y redefinir los cinco métodos. La primera es la normal; la segunda existe
 * porque a veces el destino y su lógica son la misma cosa.
 *
 * <p><strong>Admite un solo oyente</strong>, y por eso {@link #addDropTargetListener} tira
 * {@code TooManyListenersException}. No es una limitación arbitraria: dos oyentes podrían contestar
 * cosas distintas al mismo arrastre —uno aceptar y otro rechazar— y no hay forma de resolver ese
 * empate.
 *
 * <p>El desplazamiento automático se prende solo si el componente implementa {@link Autoscroll}. La
 * cuenta de si el puntero entró en la zona sensible está acá y no en el componente, para que cada
 * uno no tenga que rehacerla.
 */
public class DropTarget implements DropTargetListener, Serializable {

    private static final long serialVersionUID = -6283860791671019047L;

    private Component component;

    /** Qué acciones acepta este destino. */
    int actions = DnDConstants.ACTION_COPY_OR_MOVE;

    /** Si el destino está escuchando. */
    boolean active = true;

    private transient DropTargetContext dropTargetContext;
    private transient DropTargetListener dtListener;
    private transient FlavorMap flavorMap;
    private transient DropTargetAutoScroller autoScroller;

    /**
     * Con todo dado.
     *
     * @param dt el componente, o `null` para engancharlo después
     * @param ops las acciones que acepta
     * @param dtl el oyente, o `null`
     * @param act si arranca activo
     * @param fm el mapa de formatos, o `null` para el del sistema
     * @throws HeadlessException si no hay pantalla
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl, boolean act, FlavorMap fm)
            throws HeadlessException {
        this.component = dt;
        this.setDefaultActions(ops);
        this.dtListener = dtl;
        this.active = act;
        if (fm != null) {
            this.flavorMap = fm;
        } else {
            this.flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        }
        if (dt != null) {
            dt.setDropTarget(this);
        }
    }

    /**
     * Con el mapa de formatos del sistema.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl, boolean act)
            throws HeadlessException {
        this(dt, ops, dtl, act, null);
    }

    /**
     * Sin componente ni oyente; hay que engancharlos después.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public DropTarget() throws HeadlessException {
        this(null, DnDConstants.ACTION_COPY_OR_MOVE, null, true, null);
    }

    /**
     * Con el componente y el oyente, aceptando copiar o mover.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public DropTarget(Component dt, DropTargetListener dtl) throws HeadlessException {
        this(dt, DnDConstants.ACTION_COPY_OR_MOVE, dtl, true, null);
    }

    /**
     * Con el componente, las acciones y el oyente.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public DropTarget(Component dt, int ops, DropTargetListener dtl) throws HeadlessException {
        this(dt, ops, dtl, true, null);
    }

    /**
     * Engancha este destino a otro componente.
     *
     * <p>Desengancha el anterior: un destino pertenece a un componente por vez.
     */
    public synchronized void setComponent(Component c) {
        if (this.component == c) {
            return;
        }
        Component anterior = this.component;
        this.component = c;
        if (anterior != null) {
            this.clearAutoscroll();
            anterior.setDropTarget(null);
        }
        if (c != null && c.getDropTarget() != this) {
            c.setDropTarget(this);
        }
    }

    /** El componente al que está enganchado, o `null`. */
    public synchronized Component getComponent() {
        return this.component;
    }

    /**
     * Cambia qué acciones acepta.
     *
     * <p>Lo que no sea copiar, mover o enlazar se descarta en silencio, igual que el JDK.
     */
    public void setDefaultActions(int ops) {
        this.doSetDefaultActions(ops);
    }

    /** La parte que hace el trabajo, para que el contexto la pueda llamar. */
    void doSetDefaultActions(int ops) {
        this.actions = ops & (DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK);
    }

    /** Qué acciones acepta. */
    public int getDefaultActions() {
        return this.actions;
    }

    /**
     * Prende o apaga el destino.
     *
     * <p>Apagarlo mientras hay un arrastre encima lo corta: el desplazamiento automático se detiene
     * y el arrastre deja de recibir respuesta.
     */
    public synchronized void setActive(boolean isActive) {
        if (isActive != this.active) {
            this.active = isActive;
            if (!isActive) {
                this.clearAutoscroll();
            }
        }
    }

    /** Si está escuchando. */
    public boolean isActive() {
        return this.active;
    }

    /**
     * Registra el oyente.
     *
     * @throws TooManyListenersException si ya hay uno: dos oyentes podrían contestar cosas
     *     contradictorias al mismo arrastre
     */
    public synchronized void addDropTargetListener(DropTargetListener dtl)
            throws TooManyListenersException {
        if (dtl == null) {
            return;
        }
        if (this == dtl) {
            throw new IllegalArgumentException("DropTarget may not be its own Listener");
        }
        if (this.dtListener == null) {
            this.dtListener = dtl;
        } else {
            throw new TooManyListenersException();
        }
    }

    /** Saca al oyente. */
    public synchronized void removeDropTargetListener(DropTargetListener dtl) {
        if (dtl != null && this.dtListener != null) {
            if (this.dtListener == dtl) {
                this.dtListener = null;
            } else {
                throw new IllegalArgumentException("listener mismatch");
            }
        }
    }

    /** Le pasa el aviso al oyente y arranca el desplazamiento automático si corresponde. */
    public synchronized void dragEnter(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragEnter(dtde);
        } else {
            dtde.getDropTargetContext().setTargetActions(DnDConstants.ACTION_NONE);
        }
        this.initializeAutoscrolling(dtde.getLocation());
    }

    /** Le pasa el aviso al oyente y actualiza el desplazamiento automático. */
    public synchronized void dragOver(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragOver(dtde);
        }
        this.updateAutoscroll(dtde.getLocation());
    }

    /** Le pasa el aviso al oyente. */
    public synchronized void dropActionChanged(DropTargetDragEvent dtde) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dropActionChanged(dtde);
        }
        this.updateAutoscroll(dtde.getLocation());
    }

    /** Le pasa el aviso al oyente y para el desplazamiento automático. */
    public synchronized void dragExit(DropTargetEvent dte) {
        if (!this.active) {
            return;
        }
        if (this.dtListener != null) {
            this.dtListener.dragExit(dte);
        }
        this.clearAutoscroll();
    }

    /**
     * Le pasa el soltado al oyente.
     *
     * <p>Sin oyente, se rechaza: aceptar sin nadie que lea los datos dejaría al origen esperando un
     * {@code dropComplete} que no va a llegar.
     */
    public synchronized void drop(DropTargetDropEvent dtde) {
        this.clearAutoscroll();
        if (this.dtListener != null && this.active) {
            this.dtListener.drop(dtde);
        } else {
            dtde.rejectDrop();
        }
    }

    /** El diccionario de formatos que usa este destino. */
    public FlavorMap getFlavorMap() {
        return this.flavorMap;
    }

    /** Cambia el diccionario; `null` vuelve al del sistema. */
    public void setFlavorMap(FlavorMap fm) {
        if (fm == null) {
            this.flavorMap = SystemFlavorMap.getDefaultFlavorMap();
        } else {
            this.flavorMap = fm;
        }
    }

    /** Avisa que el componente pasó a poder mostrarse. */
    public void addNotify() {
    }

    /** Avisa que el componente dejó de poder mostrarse; corta el desplazamiento. */
    public void removeNotify() {
        this.clearAutoscroll();
    }

    /** El canal por el que este destino contesta. */
    public DropTargetContext getDropTargetContext() {
        if (this.dropTargetContext == null) {
            this.dropTargetContext = this.createDropTargetContext();
        }
        return this.dropTargetContext;
    }

    /** Arma el contexto; una subclase puede dar el suyo. */
    protected DropTargetContext createDropTargetContext() {
        return new DropTargetContext(this);
    }

    /** Arma el temporizador de desplazamiento; una subclase puede dar el suyo. */
    protected DropTargetAutoScroller createDropTargetAutoScroller(Component c, Point p) {
        return new DropTargetAutoScroller(c, p);
    }

    /**
     * Arranca el desplazamiento automático si el componente lo admite.
     *
     * <p>Se comprueba `instanceof` en vez de una bandera: implementar {@link Autoscroll} **es** la
     * forma de pedirlo.
     */
    protected void initializeAutoscrolling(Point p) {
        if (this.component == null || !(this.component instanceof Autoscroll)) {
            return;
        }
        this.autoScroller = this.createDropTargetAutoScroller(this.component, p);
    }

    /** Le avisa al desplazamiento dónde está ahora el puntero. */
    protected void updateAutoscroll(Point dragCursorLocn) {
        if (this.autoScroller != null) {
            this.autoScroller.updateLocation(dragCursorLocn);
        }
    }

    /** Para el desplazamiento automático. */
    protected void clearAutoscroll() {
        if (this.autoScroller != null) {
            this.autoScroller.stop();
            this.autoScroller = null;
        }
    }

    /**
     * Quien desplaza el componente mientras el puntero está cerca de un borde.
     *
     * <p>La cuenta que hace es una sola: si el punto cae dentro de los márgenes que declaró el
     * componente, se le pide un paso de desplazamiento. Vive acá y no en el componente para que cada
     * uno no tenga que repetirla.
     */
    protected static class DropTargetAutoScroller {

        private final Component component;
        private final Autoscroll autoScroll;
        private Point locn;

        /** Con el componente a desplazar y el punto inicial. */
        protected DropTargetAutoScroller(Component c, Point p) {
            this.component = c;
            this.autoScroll = (Autoscroll) c;
            this.locn = new Point(p);
        }

        /** Le avisa dónde está el puntero y desplaza si cae en la zona sensible. */
        protected void updateLocation(Point newLocn) {
            this.locn = new Point(newLocn);
            Insets margenes = this.autoScroll.getAutoscrollInsets();
            int w = this.component.getWidth();
            int h = this.component.getHeight();
            boolean dentroDeZona = this.locn.x < margenes.left
                    || this.locn.x > w - margenes.right
                    || this.locn.y < margenes.top
                    || this.locn.y > h - margenes.bottom;
            if (dentroDeZona) {
                this.autoScroll.autoscroll(this.locn);
            }
        }

        /** Deja de desplazar. */
        protected void stop() {
            this.locn = null;
        }
    }
}
