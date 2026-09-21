package javax.swing;

import java.awt.Component;

/**
 * Who turns an element of the model into something that can be drawn.
 *
 * <h2>A single component for the whole list</h2>
 *
 * <p>The method returns a component, and the usual thing is for it to return <em>the same</em>
 * one each time, with the values changed. A list of a thousand lines does not build a thousand
 * components: it builds one and uses it as a stamp.
 *
 * <p>That explains why {@link DefaultListCellRenderer} leaves its repainting methods empty: a
 * component that is in no window has nothing to repaint, and giving notice would be pure cost.
 *
 * @param <E> the elements' type.
 */
public interface ListCellRenderer<E> {

    /**
     * The component that draws that element.
     *
     * @param isSelected whether the line is chosen.
     * @param cellHasFocus whether the line has the focus.
     */
    Component getListCellRendererComponent(JList<? extends E> list, E value, int index,
            boolean isSelected, boolean cellHasFocus);
}
