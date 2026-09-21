package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A menu that shows up where it is asked to, instead of hanging from a bar.
 *
 * <p>It is the context menu: it is attached to a component with {@code Component.add(PopupMenu)}
 * and shown from the handler of whichever gesture corresponds on each platform —what {@code
 * MouseEvent.isPopupTrigger} answers.
 *
 * <p>It inherits from {@link Menu}, so it is filled like any other; the only different thing is how
 * it is shown.
 *
 * <p><strong>{@link #show} shows nothing here.</strong> A popup menu is drawn by the operating
 * system in a window of its own that floats above everything else, and this library has no
 * windowing system. The argument checks are made all the same —a `null` origin is still an error—
 * and what does not happen is the appearing. The method returns nothing, so it does not claim to
 * have shown something it did not show.
 *
 * <p>Its constructors declare {@link HeadlessException} like the JDK's and never throw it; see
 * {@link MenuComponent}.
 */
public class PopupMenu extends Menu {

    private static final long serialVersionUID = -4620452533522760060L;

    /**
     * The tray icon it belongs to, or `null` if it is an ordinary menu.
     *
     * <p>It exists so that one same menu does not end up in two icons: {@link
     * TrayIcon#setPopupMenu} consults it before keeping it. It is package-private because it is a
     * detail of that negotiation, not something whoever builds the menu has to see.
     */
    TrayIcon trayOwner;

    /** A popup menu without a label. */
    public PopupMenu() throws HeadlessException {
        this("");
    }

    /**
     * With that label.
     *
     * <p>The label is only seen if the menu is used as a submenu of another: as a popup menu it has
     * nowhere to show it.
     */
    public PopupMenu(String label) throws HeadlessException {
        super(label);
    }

    /**
     * What it hangs from.
     *
     * <p>It may be a {@link Component} and not only a {@link MenuContainer}, which is the
     * difference with the rest of the menus: this one is attached to any component.
     */
    public MenuContainer getParent() {
        return super.getParent();
    }

    /** Notifies that it can be shown. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Shows it at that point of the given component.
     *
     * <p>Nothing appears: a popup window of the system is needed, which this library does not have.
     * The argument checks are made anyway.
     *
     * <p>Those checks are not the JDK's. Here a `null` origin is a {@code NullPointerException} and
     * a menu attached to nothing is an {@code IllegalArgumentException}; the JDK does it the other
     * way round —an unattached menu is the {@code NullPointerException}— and on top of that it
     * demands that the origin belong to the hierarchy of the component the menu is attached to,
     * which is not checked here.
     *
     * @throws NullPointerException if the origin is `null`
     * @throws IllegalArgumentException if this menu is not attached to any component
     */
    public void show(Component origin, int x, int y) {
        if (origin == null) {
            throw new NullPointerException("origin");
        }
        MenuContainer p = this.getParent();
        if (p == null) {
            throw new IllegalArgumentException(
                    "PopupMenu is not attached to any component");
        }
        // Without a windowing system there is nothing to show; the state was checked all the same.
    }

    /** The accessibility information of this popup menu. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTPopupMenu();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a popup menu. */
    protected class AccessibleAWTPopupMenu extends AccessibleAWTMenu {

        /** For the subclasses. */
        protected AccessibleAWTPopupMenu() {
        }

        /** It is a popup menu. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.POPUP_MENU;
        }
    }
}
