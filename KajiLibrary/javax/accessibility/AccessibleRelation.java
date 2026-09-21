package javax.accessibility;

/**
 * A link between two objects that the component tree does not express.
 *
 * <p>The hierarchy says who is inside whom, and that is not enough. That a label describes a field,
 * that a button controls a panel, that a text follows another in reading order: none of that is
 * deduced from being side by side. These relations say it explicitly.
 *
 * <p>The most used is {@link #LABELED_BY}, and it is the one that fixes the classic problem of a
 * form: without it, an assistive technology reaches an empty text field and has no way of knowing
 * that the word to its left is its name.
 *
 * <p>Almost all come in pairs --{@code LABEL_FOR} and {@code LABELED_BY}-- because the link is
 * declared from both sides and whoever walks the tree may come in through either.
 */
public class AccessibleRelation extends AccessibleBundle {

    /** The name of the <b>childNodeOf</b> property. */
    public static final String CHILD_NODE_OF = "childNodeOf";

    /** The name of the <b>childNodeOfProperty</b> property. */
    public static final String CHILD_NODE_OF_PROPERTY = "childNodeOfProperty";

    /** The name of the <b>controlledBy</b> property. */
    public static final String CONTROLLED_BY = "controlledBy";

    /** The name of the <b>controlledByProperty</b> property. */
    public static final String CONTROLLED_BY_PROPERTY = "controlledByProperty";

    /** The name of the <b>controllerFor</b> property. */
    public static final String CONTROLLER_FOR = "controllerFor";

    /** The name of the <b>controllerForProperty</b> property. */
    public static final String CONTROLLER_FOR_PROPERTY = "controllerForProperty";

    /** The name of the <b>embeddedBy</b> property. */
    public static final String EMBEDDED_BY = "embeddedBy";

    /** The name of the <b>embeddedByProperty</b> property. */
    public static final String EMBEDDED_BY_PROPERTY = "embeddedByProperty";

    /** The name of the <b>embeds</b> property. */
    public static final String EMBEDS = "embeds";

    /** The name of the <b>embedsProperty</b> property. */
    public static final String EMBEDS_PROPERTY = "embedsProperty";

    /** The name of the <b>flowsFrom</b> property. */
    public static final String FLOWS_FROM = "flowsFrom";

    /** The name of the <b>flowsFromProperty</b> property. */
    public static final String FLOWS_FROM_PROPERTY = "flowsFromProperty";

    /** The name of the <b>flowsTo</b> property. */
    public static final String FLOWS_TO = "flowsTo";

    /** The name of the <b>flowsToProperty</b> property. */
    public static final String FLOWS_TO_PROPERTY = "flowsToProperty";

    /** The name of the <b>labeledBy</b> property. */
    public static final String LABELED_BY = "labeledBy";

    /** The name of the <b>labeledByProperty</b> property. */
    public static final String LABELED_BY_PROPERTY = "labeledByProperty";

    /** The name of the <b>labelFor</b> property. */
    public static final String LABEL_FOR = "labelFor";

    /** The name of the <b>labelForProperty</b> property. */
    public static final String LABEL_FOR_PROPERTY = "labelForProperty";

    /** The name of the <b>memberOf</b> property. */
    public static final String MEMBER_OF = "memberOf";

    /** The name of the <b>memberOfProperty</b> property. */
    public static final String MEMBER_OF_PROPERTY = "memberOfProperty";

    /** The name of the <b>parentWindowOf</b> property. */
    public static final String PARENT_WINDOW_OF = "parentWindowOf";

    /** The name of the <b>parentWindowOfProperty</b> property. */
    public static final String PARENT_WINDOW_OF_PROPERTY = "parentWindowOfProperty";

    /** The name of the <b>subwindowOf</b> property. */
    public static final String SUBWINDOW_OF = "subwindowOf";

    /** The name of the <b>subwindowOfProperty</b> property. */
    public static final String SUBWINDOW_OF_PROPERTY = "subwindowOfProperty";

    private Object[] target = new Object[0];

    /** With the key and no target. */
    public AccessibleRelation(String key) {
        this.key = key;
    }

    /** With the key and one target. */
    public AccessibleRelation(String key, Object target) {
        this.key = key;
        this.target = new Object[1];
        this.target[0] = target;
    }

    /** With the key and several targets. */
    public AccessibleRelation(String key, Object[] target) {
        this.key = key;
        this.target = target;
    }

    /** Which relation it is. */
    public String getKey() {
        return this.key;
    }

    /** Which objects it relates to. */
    public Object[] getTarget() {
        Object[] copy;
        if (this.target == null) {
            copy = new Object[0];
        } else {
            copy = new Object[this.target.length];
            System.arraycopy(this.target, 0, copy, 0, this.target.length);
        }
        return copy;
    }

    /** Changes the target. */
    public void setTarget(Object target) {
        Object[] single = new Object[1];
        single[0] = target;
        this.target = single;
    }

    /** Changes the targets. */
    public void setTarget(Object[] target) {
        if (target == null) {
            this.target = new Object[0];
        } else {
            this.target = target;
        }
    }
}
