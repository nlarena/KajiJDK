package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * The usual line renderer: a label with the element's {@code toString}.
 *
 * <h2>A label that is not a real component</h2>
 *
 * <p>It inherits from {@link JLabel} and overrides every repainting and notice method so that
 * they do nothing. It is not a minor optimization: this object is used as a stamp, once per
 * visible line and many times a second. If each change of text fired a property notice and a
 * repaint request, drawing a list of twenty lines would cost forty useless notices.
 *
 * <p>It is also the reason it cannot be used as an ordinary component: put into a window, it
 * would never repaint itself.
 *
 * <h2>The focus's border</h2>
 *
 * <p>The line with the focus carries a border and the others an empty one of the same size.
 * That the empty one is the same size is what keeps the text from moving a pixel when the focus
 * passes from one line to another.
 */
public class DefaultListCellRenderer extends JLabel implements ListCellRenderer<Object>,
        Serializable {

    /**
     * The border of the lines with no focus.
     *
     * @deprecated It is shared between every renderer; changing it changes everybody's.
     */
    @Deprecated
    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    private static final Border NO_FOCUS = new EmptyBorder(1, 1, 1, 1);

    /** A renderer aligned to the left and opaque. */
    public DefaultListCellRenderer() {
        super();
        setOpaque(true);
        setBorder(getNoFocusBorder());
        setName("List.cellRenderer");
    }

    private Border getNoFocusBorder() {
        return (noFocusBorder != null) ? noFocusBorder : NO_FOCUS;
    }

    /** It gets the label ready for that line and returns it. */
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        setComponentOrientation(list.getComponentOrientation());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (value instanceof Icon) {
            setIcon((Icon) value);
            setText("");
        } else {
            setIcon(null);
            setText((value == null) ? "" : value.toString());
        }

        setEnabled(list.isEnabled());
        setFont(list.getFont());
        setBorder(getNoFocusBorder());
        return this;
    }

    public boolean isOpaque() {
        Color back = getBackground();
        Component p = getParent();
        if (p != null) {
            p = p.getParent();
        }
        // With the same background as the list and nothing to hide, it is not worth filling.
        boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground())
                && p.isOpaque();
        return !colorMatch && super.isOpaque();
    }

    /** It does nothing; see the class note. */
    public void validate() {
    }

    public void invalidate() {
    }

    public void repaint() {
    }

    public void revalidate() {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    public void repaint(Rectangle r) {
    }

    /**
     * It only lets the text's notice through.
     *
     * <p>It is the only one anybody may need to listen to from a renderer, and letting the others
     * through would cost one notice per property and per line.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        // The JDK also lets "font" and "foreground" through when the text is HTML, so that the
                // HTML view is rebuilt. That case does not arrive here: `BasicHTML` does not exist
                // yet.
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

    /**
     * The same renderer, marked as set by the look and feel.
     *
     * <p>The mark is what allows changing the look and feel to replace this renderer and to
     * respect one the program may have set; see {@link javax.swing.plaf.UIResource}.
     */
    public static class UIResource extends DefaultListCellRenderer
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
