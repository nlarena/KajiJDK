package javax.accessibility;

/**
 * What **condition** an object is in: enabled, chosen, focused, expanded.
 *
 * <p>Unlike the role, which does not change, states come and go all the time. That is why they are
 * not queried one by one but as an {@link AccessibleStateSet}: asking ten times about ten states
 * would show an object halfway through changing.
 */
public class AccessibleState extends AccessibleBundle {

    /** The <b>active</b> category. */
    public static final AccessibleState ACTIVE = new AccessibleState("active");

    /** The <b>armed</b> category. */
    public static final AccessibleState ARMED = new AccessibleState("armed");

    /** It is busy and may not respond. */
    public static final AccessibleState BUSY = new AccessibleState("busy");

    /** It is checked. */
    public static final AccessibleState CHECKED = new AccessibleState("checked");

    /** It is collapsed. */
    public static final AccessibleState COLLAPSED = new AccessibleState("collapsed");

    /** It can be edited. */
    public static final AccessibleState EDITABLE = new AccessibleState("editable");

    /** It responds to user input. */
    public static final AccessibleState ENABLED = new AccessibleState("enabled");

    /** The <b>expandable</b> category. */
    public static final AccessibleState EXPANDABLE = new AccessibleState("expandable");

    /** It is expanded. */
    public static final AccessibleState EXPANDED = new AccessibleState("expanded");

    /** The <b>focusable</b> category. */
    public static final AccessibleState FOCUSABLE = new AccessibleState("focusable");

    /** It has the keyboard focus. */
    public static final AccessibleState FOCUSED = new AccessibleState("focused");

    /** The <b>horizontal</b> category. */
    public static final AccessibleState HORIZONTAL = new AccessibleState("horizontal");

    /** The <b>iconified</b> category. */
    public static final AccessibleState ICONIFIED = new AccessibleState("iconified");

    /** The <b>indeterminate</b> category. */
    public static final AccessibleState INDETERMINATE = new AccessibleState("indeterminate");

    /** The <b>manages descendants</b> category. */
    public static final AccessibleState MANAGES_DESCENDANTS = new AccessibleState("manages descendants");

    /** It blocks the rest of the application. */
    public static final AccessibleState MODAL = new AccessibleState("modal");

    /** The <b>multiselectable</b> category. */
    public static final AccessibleState MULTISELECTABLE = new AccessibleState("multiselectable");

    /** The <b>multiple line</b> category. */
    public static final AccessibleState MULTI_LINE = new AccessibleState("multiple line");

    /** It paints all its pixels. */
    public static final AccessibleState OPAQUE = new AccessibleState("opaque");

    /** It is pressed at this moment. */
    public static final AccessibleState PRESSED = new AccessibleState("pressed");

    /** The <b>resizable</b> category. */
    public static final AccessibleState RESIZABLE = new AccessibleState("resizable");

    /** The <b>selectable</b> category. */
    public static final AccessibleState SELECTABLE = new AccessibleState("selectable");

    /** It is chosen. */
    public static final AccessibleState SELECTED = new AccessibleState("selected");

    /** It is really seen, counting its ancestors. */
    public static final AccessibleState SHOWING = new AccessibleState("showing");

    /** The <b>single line</b> category. */
    public static final AccessibleState SINGLE_LINE = new AccessibleState("single line");

    /** The <b>transient</b> category. */
    public static final AccessibleState TRANSIENT = new AccessibleState("transient");

    /** The <b>truncated</b> category. */
    public static final AccessibleState TRUNCATED = new AccessibleState("truncated");

    /** The <b>vertical</b> category. */
    public static final AccessibleState VERTICAL = new AccessibleState("vertical");

    /** It is declared visible. */
    public static final AccessibleState VISIBLE = new AccessibleState("visible");

    /**
     * With the given key.
     *
     * <p>Protected for the same reason as in {@link AccessibleRole}: states are compared by
     * identity, so making a loose one makes it incomparable.
     */
    protected AccessibleState(String key) {
        this.key = key;
    }
}
