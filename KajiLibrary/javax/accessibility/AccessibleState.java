package javax.accessibility;

/**
 * En qué **condición** está un objeto: habilitado, elegido, con foco, expandido.
 *
 * <p>A diferencia del rol, que no cambia, los estados van y vienen todo el tiempo. Por eso no se
 * consultan de a uno sino como un {@link AccessibleStateSet}: preguntar diez veces por diez estados
 * dejaría ver un objeto a medio cambiar.
 */
public class AccessibleState extends AccessibleBundle {

    /** La categoría <b>active</b>. */
    public static final AccessibleState ACTIVE = new AccessibleState("active");

    /** La categoría <b>armed</b>. */
    public static final AccessibleState ARMED = new AccessibleState("armed");

    /** Está ocupado y puede no responder. */
    public static final AccessibleState BUSY = new AccessibleState("busy");

    /** Está marcado. */
    public static final AccessibleState CHECKED = new AccessibleState("checked");

    /** Está plegado. */
    public static final AccessibleState COLLAPSED = new AccessibleState("collapsed");

    /** Se puede editar. */
    public static final AccessibleState EDITABLE = new AccessibleState("editable");

    /** Responde a la entrada del usuario. */
    public static final AccessibleState ENABLED = new AccessibleState("enabled");

    /** La categoría <b>expandable</b>. */
    public static final AccessibleState EXPANDABLE = new AccessibleState("expandable");

    /** Está desplegado. */
    public static final AccessibleState EXPANDED = new AccessibleState("expanded");

    /** La categoría <b>focusable</b>. */
    public static final AccessibleState FOCUSABLE = new AccessibleState("focusable");

    /** Tiene el foco del teclado. */
    public static final AccessibleState FOCUSED = new AccessibleState("focused");

    /** La categoría <b>horizontal</b>. */
    public static final AccessibleState HORIZONTAL = new AccessibleState("horizontal");

    /** La categoría <b>iconified</b>. */
    public static final AccessibleState ICONIFIED = new AccessibleState("iconified");

    /** La categoría <b>indeterminate</b>. */
    public static final AccessibleState INDETERMINATE = new AccessibleState("indeterminate");

    /** La categoría <b>manages descendants</b>. */
    public static final AccessibleState MANAGES_DESCENDANTS = new AccessibleState("manages descendants");

    /** Bloquea al resto de la aplicación. */
    public static final AccessibleState MODAL = new AccessibleState("modal");

    /** La categoría <b>multiselectable</b>. */
    public static final AccessibleState MULTISELECTABLE = new AccessibleState("multiselectable");

    /** La categoría <b>multiple line</b>. */
    public static final AccessibleState MULTI_LINE = new AccessibleState("multiple line");

    /** Pinta todos sus píxeles. */
    public static final AccessibleState OPAQUE = new AccessibleState("opaque");

    /** Está apretado en este momento. */
    public static final AccessibleState PRESSED = new AccessibleState("pressed");

    /** La categoría <b>resizable</b>. */
    public static final AccessibleState RESIZABLE = new AccessibleState("resizable");

    /** La categoría <b>selectable</b>. */
    public static final AccessibleState SELECTABLE = new AccessibleState("selectable");

    /** Está elegido. */
    public static final AccessibleState SELECTED = new AccessibleState("selected");

    /** Se ve de verdad, contando a sus ancestros. */
    public static final AccessibleState SHOWING = new AccessibleState("showing");

    /** La categoría <b>single line</b>. */
    public static final AccessibleState SINGLE_LINE = new AccessibleState("single line");

    /** La categoría <b>transient</b>. */
    public static final AccessibleState TRANSIENT = new AccessibleState("transient");

    /** La categoría <b>truncated</b>. */
    public static final AccessibleState TRUNCATED = new AccessibleState("truncated");

    /** La categoría <b>vertical</b>. */
    public static final AccessibleState VERTICAL = new AccessibleState("vertical");

    /** Está declarado visible. */
    public static final AccessibleState VISIBLE = new AccessibleState("visible");

    /**
     * Con la clave dada.
     *
     * <p>Protegido por el mismo motivo que en {@link AccessibleRole}: los estados se comparan por
     * identidad, así que fabricar uno suelto lo vuelve incomparable.
     */
    protected AccessibleState(String key) {
        this.key = key;
    }
}
