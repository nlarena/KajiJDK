package java.awt.event;

import java.awt.AWTEvent;
import java.awt.Component;

/**
 * A component changed size, place or visibility.
 *
 * <p>It is also the root of nearly every event that has a component behind it: keyboard, mouse,
 * focus, window and container inherit from here. All it adds over {@link AWTEvent} is being able to
 * return the source already typed as a {@link Component}, which is what they all need.
 *
 * <p>The four events the class is named after arrive **after** the change: they are a notice, not a
 * permission.
 */
public class ComponentEvent extends AWTEvent {

    private static final long serialVersionUID = 8101406823902991265L;

    /** The family's first identifier. */
    public static final int COMPONENT_FIRST = 100;

    /** The component was hidden. */
    public static final int COMPONENT_HIDDEN = 103;

    /** The family's last identifier. */
    public static final int COMPONENT_LAST = 103;

    /** The component changed place. */
    public static final int COMPONENT_MOVED = 100;

    /** The component changed size. */
    public static final int COMPONENT_RESIZED = 101;

    /** The component became visible. */
    public static final int COMPONENT_SHOWN = 102;

    /**
     * With the component and the identifier.
     *
     * @throws IllegalArgumentException if the source is `null`
     */
    public ComponentEvent(Component source, int id) {
        super(source, id);
    }

    /**
     * The component it happened to.
     *
     * @return the source if it is a component, or `null` if not
     */
    public Component getComponent() {
        if (this.source instanceof Component) {
            return (Component) this.source;
        }
        return null;
    }

    public String paramString() {
        String type;
        if (this.id == COMPONENT_SHOWN) {
            type = "COMPONENT_SHOWN";
        } else if (this.id == COMPONENT_HIDDEN) {
            type = "COMPONENT_HIDDEN";
        } else if (this.id == COMPONENT_MOVED) {
            type = "COMPONENT_MOVED";
        } else if (this.id == COMPONENT_RESIZED) {
            type = "COMPONENT_RESIZED";
        } else {
            type = "unknown type";
        }
        Component c = this.getComponent();
        String box = c == null ? "" : " (" + c.getX() + "," + c.getY() + " " + c.getWidth()
                + "x" + c.getHeight() + ")";
        return type + box;
    }
}
