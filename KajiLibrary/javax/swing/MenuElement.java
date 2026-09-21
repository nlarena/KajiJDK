package javax.swing;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Any piece of a menu: the bar, a menu, an item, a separator.
 *
 * <h2>Why the events arrive with the path</h2>
 *
 * <p>{@link #processMouseEvent} and {@link #processKeyEvent} receive an array with
 * <strong>the whole path</strong> from the bar down to the element, and not only the element.
 * The reason is that an open menu is a living hierarchy: moving the mouse from one submenu to
 * another has to close what was left behind, and for that one has to know where one was coming
 * from.
 *
 * <p>Who hands those events out is {@link MenuSelectionManager}, which is also the one that
 * builds the path.
 *
 * <p>{@link #getComponent} exists because a menu element <em>is</em> also a component that is
 * drawn; the interface separates it so that the menu mechanism does not depend on its being a
 * {@link JComponent} in particular.
 */
public interface MenuElement {

    /** It attends a mouse event, with the whole path. */
    void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager);

    /** It attends a keyboard event, with the whole path. */
    void processKeyEvent(KeyEvent event, MenuElement[] path, MenuSelectionManager manager);

    /** Notice that this element came into or went out of the selection. */
    void menuSelectionChanged(boolean isIncluded);

    /** The sub-elements, in order. */
    MenuElement[] getSubElements();

    /** The component that draws this element. */
    Component getComponent();
}
