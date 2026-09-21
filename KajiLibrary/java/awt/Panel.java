package java.awt;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * The simplest container there is: a rectangle to group other components in.
 *
 * <p>It draws nothing, has no border or title, and its only differences from a bare {@link
 * Container} are that it comes with a {@link FlowLayout} set and that it is concrete. That is
 * enough: most AWT interface layouts are nested panels, each with its own layout.
 */
public class Panel extends Container implements Accessible {

    private static final long serialVersionUID = -2728009084054400034L;

    /** A panel with {@link FlowLayout}. */
    public Panel() {
        this(new FlowLayout());
    }

    /** A panel with that layout. */
    public Panel(LayoutManager layout) {
        this.setLayout(layout);
    }

    String constructComponentName() {
        synchronized (Panel.class) {
            String n = "panel" + panelCounter;
            panelCounter = panelCounter + 1;
            return n;
        }
    }

    private static int panelCounter = 0;

    /** Declares it displayable; without a screen there is nothing more to do. */
    public void addNotify() {
        super.addNotify();
    }

    /** The panel's accessibility. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTPanel();
        }
        return this.accessibleContext;
    }

    /** A panel, for accessibility, is a panel: it groups and nothing more. */
    protected class AccessibleAWTPanel extends AccessibleAWTContainer {

        /** For subclasses. */
        protected AccessibleAWTPanel() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PANEL;
        }
    }
}
