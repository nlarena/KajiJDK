package java.awt;

import java.util.EventObject;

/**
 * La raíz de todos los eventos de AWT.
 *
 * <p>Un evento es tres cosas: **de dónde** salió (la fuente, que hereda de {@link EventObject}),
 * **qué** pasó (el identificador) y **si ya lo atendieron** (consumido). Todo lo demás lo agregan
 * las subclases.
 *
 * <p>Que el tipo sea un `int` y no una jerarquía de clases es una decisión que se nota: un mismo
 * {@link java.awt.event.MouseEvent} sirve para apretar, soltar y arrastrar, y quien lo recibe mira
 * el identificador. Eso permite un despacho por rangos —cada familia tiene su bloque de números—
 * pero obliga a comparar enteros donde un `switch` sobre tipos sería más claro.
 *
 * <p>**Consumir** un evento es decir "yo me encargo": el sistema no le va a dar el tratamiento por
 * omisión. Es lo que hace que un componente pueda quedarse con una tecla y evitar que además dispare
 * el atajo de menú.
 *
 * <p>Las máscaras son bits que un componente prende para pedir que le lleguen los eventos de esa
 * familia. Entregar eventos que nadie escucha cuesta caro, así que por omisión no se entrega casi
 * nada y cada `addXListener` prende la máscara que corresponde.
 */
public abstract class AWTEvent extends EventObject {

    private static final long serialVersionUID = -1825314779160409405L;

    /** Los eventos de tamaño y visibilidad de un componente. */
    public static final long COMPONENT_EVENT_MASK = 0x01;

    /** Los de agregar y sacar hijos de un contenedor. */
    public static final long CONTAINER_EVENT_MASK = 0x02;

    /** Los de ganar y perder el foco. */
    public static final long FOCUS_EVENT_MASK = 0x04;

    /** Los de teclado. */
    public static final long KEY_EVENT_MASK = 0x08;

    /** Los de botones del ratón. */
    public static final long MOUSE_EVENT_MASK = 0x10;

    /** Los de movimiento del ratón, separados porque son muchísimos más. */
    public static final long MOUSE_MOTION_EVENT_MASK = 0x20;

    /** Los de ventana. */
    public static final long WINDOW_EVENT_MASK = 0x40;

    /** Los de acción: el botón que se apretó, la opción que se eligió. */
    public static final long ACTION_EVENT_MASK = 0x80;

    /** Los de barra de desplazamiento. */
    public static final long ADJUSTMENT_EVENT_MASK = 0x100;

    /** Los de selección de un elemento. */
    public static final long ITEM_EVENT_MASK = 0x200;

    /** Los de cambio de texto. */
    public static final long TEXT_EVENT_MASK = 0x400;

    /** Los del método de entrada. */
    public static final long INPUT_METHOD_EVENT_MASK = 0x800;

    /** Los de repintado. */
    public static final long PAINT_EVENT_MASK = 0x2000;

    /** Los que llevan trabajo para correr en el hilo de eventos. */
    public static final long INVOCATION_EVENT_MASK = 0x4000;

    /** Los de cambios en el árbol de componentes. */
    public static final long HIERARCHY_EVENT_MASK = 0x8000;

    /** Los de cambios de tamaño dentro del árbol. */
    public static final long HIERARCHY_BOUNDS_EVENT_MASK = 0x10000;

    /** Los de la rueda del ratón. */
    public static final long MOUSE_WHEEL_EVENT_MASK = 0x20000;

    /** Los de minimizar y restaurar una ventana. */
    public static final long WINDOW_STATE_EVENT_MASK = 0x40000;

    /** Los de foco a nivel de ventana. */
    public static final long WINDOW_FOCUS_EVENT_MASK = 0x80000;

    /**
     * El identificador más alto reservado por AWT.
     *
     * <p>Quien invente sus propios eventos tiene que numerarlos por encima de esto, o va a chocar
     * con una familia que AWT agregue más adelante.
     */
    public static final int RESERVED_ID_MAX = 1999;

    /** Qué pasó. */
    protected int id;

    /** Si alguien ya se hizo cargo. */
    protected boolean consumed;

    /**
     * Con la fuente y el identificador.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public AWTEvent(Object source, int id) {
        super(source);
        this.id = id;
        this.consumed = false;
    }

    /**
     * A partir de un evento del modelo viejo.
     *
     * @throws NullPointerException si el evento es `null`
     */
    public AWTEvent(Event event) {
        this(event.target, event.id);
    }

    /**
     * Cambia la fuente.
     *
     * <p>Sirve para volver a despachar el mismo evento desde otro componente, que es cómo un
     * contenedor le pasa a un hijo un evento que le llegó a él.
     */
    public void setSource(Object newSource) {
        this.source = newSource;
    }

    /** Qué pasó. */
    public int getID() {
        return this.id;
    }

    public String toString() {
        String nombre = "";
        if (this.source instanceof Component) {
            nombre = ((Component) this.source).getName();
        }
        return this.getClass().getName() + "[" + this.paramString() + "] on "
                + (nombre != null && !nombre.isEmpty() ? nombre : this.source);
    }

    /**
     * La descripción del evento, sin el nombre de la clase ni la fuente.
     *
     * <p>Cada subclase agrega lo suyo; {@link #toString} pone el envoltorio una sola vez.
     */
    public String paramString() {
        return "";
    }

    /** Marca que alguien se hizo cargo. */
    protected void consume() {
        this.consumed = true;
    }

    /** Si alguien ya se hizo cargo. */
    protected boolean isConsumed() {
        return this.consumed;
    }
}
