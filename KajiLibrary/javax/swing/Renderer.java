package javax.swing;

import java.awt.Component;

/**
 * It draws a value with a borrowed component.
 *
 * <h2>The component is reused</h2>
 *
 * <p>{@link #setValue} loads it and {@link #getComponent} returns it, and the usual thing is for
 * it to always be the <em>same</em> component: in a list of a thousand elements it is configured
 * a thousand times and drawn a thousand times, instead of there being a thousand components.
 * That is why what it returns cannot be kept for later: on the next call it shows something
 * else.
 *
 * <p><strong>Nobody uses it.</strong> Swing ended up with two more specific interfaces --
 * {@link ListCellRenderer} and {@link javax.swing.table.TableCellRenderer} --, which pass the
 * renderer the context this one does not have: which list it is, which row it is in, whether it
 * has the focus. It stays because it is public.
 */
public interface Renderer {

    /** It loads the value; the second parameter says whether it is chosen. */
    void setValue(Object aValue, boolean isSelected);

    /** The loaded component; see the interface note. */
    Component getComponent();
}
