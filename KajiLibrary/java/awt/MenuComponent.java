package java.awt;

import java.io.Serializable;
import java.util.Locale;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleStateSet;

/**
 * The root of everything that lives in a menu.
 *
 * <p>It is the equivalent of {@link Component} for menus, and that they are two separate
 * hierarchies is not a design oversight but a consequence: menus are drawn by the operating system,
 * not by the program. A menu item has no position or size in the window's space because it is not
 * in the window.
 *
 * <p>Hence this class being so small next to `Component`: name, font, parent, and the dispatching
 * of events. Everything geometric does not exist.
 *
 * <p>The constructors of this class and of the ones below it declare {@link HeadlessException}
 * because the JDK's do, and none of them throws it: with no screen ever, throwing would leave no
 * way to build a menu at all.
 */
public abstract class MenuComponent implements Serializable {

    private static final long serialVersionUID = -4536902356436156350L;

    private String name;
    private boolean nameExplicitlySet;
    private Font font;
    private MenuContainer parent;

    /** The accessibility context, built on demand. */
    protected AccessibleContext accessibleContext;

    private static int nameCounter;

    /** A menu component. */
    public MenuComponent() throws HeadlessException {
    }

    /** The default name, a different one for each. */
    String constructComponentName() {
        synchronized (MenuComponent.class) {
            String n = this.getClass().getName() + nameCounter;
            nameCounter = nameCounter + 1;
            return n;
        }
    }

    /**
     * What it is called.
     *
     * <p>If nobody gave it a name, one is built for it: it serves for debugging, and returning
     * `null` would force checking for it in every trace.
     */
    public String getName() {
        if (this.name == null && !this.nameExplicitlySet) {
            synchronized (this) {
                if (this.name == null && !this.nameExplicitlySet) {
                    this.name = this.constructComponentName();
                }
            }
        }
        return this.name;
    }

    /** Gives it a name. */
    public void setName(String name) {
        synchronized (this) {
            this.name = name;
            this.nameExplicitlySet = true;
        }
    }

    /** Which menu or bar it hangs from, or `null`. */
    public MenuContainer getParent() {
        return this.parent;
    }

    /** The container uses it when adding or removing it. */
    void setParent(MenuContainer p) {
        this.parent = p;
    }

    /**
     * The font it is drawn with.
     *
     * <p>If it has none of its own, the parent's is inherited. Returning `null` when there is none
     * in the whole chain is right: it means the system decides it.
     */
    public Font getFont() {
        Font f = this.font;
        if (f != null) {
            return f;
        }
        MenuContainer p = this.parent;
        if (p != null) {
            return p.getFont();
        }
        return null;
    }

    /** Gives it a font of its own. */
    public void setFont(Font f) {
        synchronized (this) {
            this.font = f;
        }
    }

    /** Notifies that it can no longer be shown. */
    public void removeNotify() {
    }

    /**
     * Sends it an event of the old model.
     *
     * @deprecated it is from the 1.0 event model. Use {@link #dispatchEvent}.
     */
    @Deprecated
    public boolean postEvent(Event evt) {
        MenuContainer p = this.parent;
        if (p != null) {
            return p.postEvent(evt);
        }
        return false;
    }

    /**
     * Delivers an event to it.
     *
     * <p>It is `final` and delegates to {@link #processEvent}: the dispatching is not overridden,
     * what is overridden is what to do with the event.
     */
    public final void dispatchEvent(AWTEvent e) {
        this.processEvent(e);
    }

    /** Handles the event; the subclasses override it. */
    protected void processEvent(AWTEvent e) {
    }

    /** The description of the component, without the class name. */
    protected String paramString() {
        return "name=" + this.getName();
    }

    public String toString() {
        return this.getClass().getName() + "[" + this.paramString() + "]";
    }

    /**
     * The lock the menu tree synchronises on.
     *
     * <p>It is the same object for all of AWT, and that is the point: a single global lock keeps
     * locking two branches of the tree in different orders from ending in a deadlock.
     */
    protected final Object getTreeLock() {
        return Component.LOCK;
    }

    /** The accessibility information of this menu component. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTMenuComponent();
        }
        return this.accessibleContext;
    }

    /**
     * The accessibility of a menu component.
     *
     * <p>It is the honest minimum: name, role and parent. Without a windowing system there are no
     * screen states to report, so the state set comes out empty instead of invented.
     */
    protected class AccessibleAWTMenuComponent extends AccessibleContext {

        /** For the subclasses. */
        protected AccessibleAWTMenuComponent() {
        }

        /** {@code AWT_COMPONENT}: the non-specific role, which the concrete subclasses refine. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.AWT_COMPONENT;
        }

        /** Empty: there is no screen to take states from. */
        public AccessibleStateSet getAccessibleStateSet() {
            return new AccessibleStateSet();
        }

        /** The name of the menu component. */
        public String getAccessibleName() {
            return MenuComponent.this.getName();
        }

        /** Zero: a plain menu component has no children. */
        public int getAccessibleChildrenCount() {
            return 0;
        }

        /** Always `null`: it has no children. */
        public javax.accessibility.Accessible getAccessibleChild(int i) {
            return null;
        }

        /** Always -1: the position inside the parent is not tracked here. */
        public int getAccessibleIndexInParent() {
            return -1;
        }

        /** The default locale: a menu does not have one of its own. */
        public Locale getLocale() {
            return Locale.getDefault();
        }
    }
}
