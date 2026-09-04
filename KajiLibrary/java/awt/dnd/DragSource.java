package java.awt.dnd;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * El lado que **entrega** en un arrastre.
 *
 * <p>Es el espejo de {@link DropTarget}: uno declara que un componente puede recibir, éste lleva
 * adelante el envío. Un solo `DragSource` alcanza para toda la aplicación —de ahí
 * {@link #getDefaultDragSource}— porque no guarda estado del arrastre; eso vive en el
 * {@link DragSourceContext} que se arma para cada uno.
 *
 * <p>Los seis cursores de fábrica son los que ve el usuario mientras arrastra, y vienen de a pares:
 * uno para cuando se puede soltar y otro para cuando no. Es la única realimentación que tiene sobre
 * si el destino lo va a aceptar.
 *
 * <p><strong>Esta implementación no puede empezar un arrastre.</strong> Un arrastre real lo maneja
 * el sistema operativo: captura el ratón, dibuja el cursor por encima de todas las ventanas y
 * negocia con programas ajenos. Sin sistema de ventanas no hay nada de eso, así que los cuatro
 * {@code startDrag} tiran {@link InvalidDnDOperationException} — que es exactamente la excepción que
 * declaran para "el sistema de arrastre no está en condiciones de hacer esto", y es verdad. Todo lo
 * demás de la clase —los cursores, los oyentes, el diccionario de formatos, el umbral— funciona.
 */
public class DragSource implements Serializable {

    private static final long serialVersionUID = 6236096958971414066L;

    /** El cursor de copiar sobre un destino que acepta. */
    public static final Cursor DefaultCopyDrop = cursor("DnD.Cursor.CopyDrop", Cursor.HAND_CURSOR);

    /** El cursor de mover sobre un destino que acepta. */
    public static final Cursor DefaultMoveDrop = cursor("DnD.Cursor.MoveDrop", Cursor.HAND_CURSOR);

    /** El cursor de enlazar sobre un destino que acepta. */
    public static final Cursor DefaultLinkDrop = cursor("DnD.Cursor.LinkDrop", Cursor.HAND_CURSOR);

    /** El cursor de copiar donde no se puede soltar. */
    public static final Cursor DefaultCopyNoDrop =
            cursor("DnD.Cursor.CopyNoDrop", Cursor.DEFAULT_CURSOR);

    /** El cursor de mover donde no se puede soltar. */
    public static final Cursor DefaultMoveNoDrop =
            cursor("DnD.Cursor.MoveNoDrop", Cursor.DEFAULT_CURSOR);

    /** El cursor de enlazar donde no se puede soltar. */
    public static final Cursor DefaultLinkNoDrop =
            cursor("DnD.Cursor.LinkNoDrop", Cursor.DEFAULT_CURSOR);

    private static DragSource defaultDragSource;

    private transient FlavorMap flavorMap = SystemFlavorMap.getDefaultFlavorMap();
    private transient final List<DragSourceListener> listeners =
            new ArrayList<DragSourceListener>();
    private transient final List<DragSourceMotionListener> motionListeners =
            new ArrayList<DragSourceMotionListener>();

    /**
     * El cursor del escritorio con ese nombre, o el predefinido si no está.
     *
     * <p>Es lo que hace el JDK cuando el escritorio no define el cursor: cae en uno predefinido en
     * vez de quedarse sin cursor. Acá **nunca** está, porque esta biblioteca no trae descriptores de
     * cursor, así que siempre se usa el predefinido — y eso es honesto: es el cursor que de verdad se
     * vería.
     */
    private static Cursor cursor(String nombre, int predefinido) {
        try {
            return Cursor.getSystemCustomCursor(nombre);
        } catch (AWTException e) {
            return Cursor.getPredefinedCursor(predefinido);
        }
    }

    /**
     * Un origen de arrastre nuevo.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public DragSource() throws HeadlessException {
    }

    /**
     * El origen que comparte toda la aplicación.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public static DragSource getDefaultDragSource() {
        synchronized (DragSource.class) {
            if (defaultDragSource == null) {
                defaultDragSource = new DragSource();
            }
            return defaultDragSource;
        }
    }

    /**
     * Si se puede mostrar una imagen que siga al puntero durante el arrastre.
     *
     * <p>Contesta `false`: dibujar por encima de todas las ventanas lo hace el sistema, y no hay.
     */
    public static boolean isDragImageSupported() {
        return false;
    }

    /** El mensaje único de todo lo que necesita el sistema de arrastre. */
    private static InvalidDnDOperationException sinSistema() {
        return new InvalidDnDOperationException("no hay sistema de arrastre nativo: empezar un "
                + "arrastre requiere capturar el ratón y dibujar por encima de todas las ventanas, "
                + "y esta biblioteca no trae sistema de ventanas");
    }

    /**
     * Arranca el arrastre con imagen y diccionario de formatos.
     *
     * @throws InvalidDnDOperationException siempre: no hay sistema de arrastre que lo lleve adelante
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point dragOffset, Transferable transferable, DragSourceListener dsl, FlavorMap fm)
            throws InvalidDnDOperationException {
        throw sinSistema();
    }

    /**
     * Arranca el arrastre con diccionario de formatos.
     *
     * @throws InvalidDnDOperationException siempre, por el mismo motivo
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Transferable transferable,
            DragSourceListener dsl, FlavorMap fm) throws InvalidDnDOperationException {
        throw sinSistema();
    }

    /**
     * Arranca el arrastre con imagen.
     *
     * @throws InvalidDnDOperationException siempre, por el mismo motivo
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Image dragImage,
            Point imageOffset, Transferable transferable, DragSourceListener dsl)
            throws InvalidDnDOperationException {
        throw sinSistema();
    }

    /**
     * Arranca el arrastre.
     *
     * @throws InvalidDnDOperationException siempre, por el mismo motivo
     */
    public void startDrag(DragGestureEvent trigger, Cursor dragCursor, Transferable transferable,
            DragSourceListener dsl) throws InvalidDnDOperationException {
        throw sinSistema();
    }

    /**
     * Arma el contexto de un arrastre; una subclase puede dar el suyo.
     *
     * @throws IllegalArgumentException si el disparador o el transferible son `null`
     */
    protected DragSourceContext createDragSourceContext(DragGestureEvent dgl, Cursor dragCursor,
            Image dragImage, Point imageOffset, Transferable t, DragSourceListener dsl) {
        return new DragSourceContext(dgl, dragCursor, dragImage, imageOffset, t, dsl);
    }

    /** El diccionario entre formatos de Java y nombres nativos. */
    public FlavorMap getFlavorMap() {
        return this.flavorMap;
    }

    /**
     * Arma un reconocedor de gesto de la clase pedida.
     *
     * @return el reconocedor, o `null` si la plataforma no tiene uno de esa clase
     */
    public <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> recognizerAbstractClass, Component c, int actions, DragGestureListener dgl) {
        return null;
    }

    /**
     * Arma el reconocedor de gesto que corresponda a esta plataforma.
     *
     * @return `null` siempre: el reconocedor concreto lo aporta el sistema de ventanas, y no hay
     */
    public DragGestureRecognizer createDefaultDragGestureRecognizer(Component c, int actions,
            DragGestureListener dgl) {
        return null;
    }

    /** Suma un oyente del estado del arrastre; un `null` se ignora. */
    public void addDragSourceListener(DragSourceListener dsl) {
        if (dsl == null) {
            return;
        }
        synchronized (this) {
            this.listeners.add(dsl);
        }
    }

    /** Saca a ese oyente. */
    public void removeDragSourceListener(DragSourceListener dsl) {
        if (dsl == null) {
            return;
        }
        synchronized (this) {
            this.listeners.remove(dsl);
        }
    }

    /** Los oyentes del estado del arrastre. */
    public DragSourceListener[] getDragSourceListeners() {
        synchronized (this) {
            return this.listeners.toArray(new DragSourceListener[this.listeners.size()]);
        }
    }

    /** Suma un oyente del movimiento; un `null` se ignora. */
    public void addDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml == null) {
            return;
        }
        synchronized (this) {
            this.motionListeners.add(dsml);
        }
    }

    /** Saca a ese oyente. */
    public void removeDragSourceMotionListener(DragSourceMotionListener dsml) {
        if (dsml == null) {
            return;
        }
        synchronized (this) {
            this.motionListeners.remove(dsml);
        }
    }

    /** Los oyentes del movimiento. */
    public DragSourceMotionListener[] getDragSourceMotionListeners() {
        synchronized (this) {
            return this.motionListeners.toArray(
                    new DragSourceMotionListener[this.motionListeners.size()]);
        }
    }

    /**
     * Los oyentes de esa clase.
     *
     * @throws ClassCastException si la clase no es una de las dos de oyente de arrastre
     */
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        EventListener[] out;
        if (listenerType == DragSourceListener.class) {
            out = this.getDragSourceListeners();
        } else if (listenerType == DragSourceMotionListener.class) {
            out = this.getDragSourceMotionListeners();
        } else {
            out = new EventListener[0];
        }
        @SuppressWarnings("unchecked")
        T[] tipado = (T[]) out;
        return tipado;
    }

    /**
     * Cuántos píxeles hay que moverse para que sea un arrastre y no un clic.
     *
     * <p>Sale de la propiedad `awt.dnd.drag.threshold` si está puesta, y si no vale 5, que es el
     * valor por omisión del JDK. Un valor no numérico o no positivo se ignora.
     */
    public static int getDragThreshold() {
        String prop = System.getProperty("awt.dnd.drag.threshold");
        if (prop != null) {
            try {
                int v = Integer.parseInt(prop);
                if (v > 0) {
                    return v;
                }
            } catch (NumberFormatException e) {
                // Propiedad mal escrita: se usa el valor por omisión.
            }
        }
        return 5;
    }
}
