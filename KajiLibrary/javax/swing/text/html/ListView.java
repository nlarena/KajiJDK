package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Element;
import javax.swing.text.View;

/**
 * The view of a list: {@code <ul>}, {@code <ol>}, {@code <dir>} or {@code <menu>}.
 *
 * <h2>Who draws the bullet</h2>
 *
 * <p>The list, not the row. It looks backwards, and it is not: the bullet goes outside the row,
 * in the margin, and the row does not know how much margin it has nor which number it gets. The
 * list does know both things, so it draws the mark before letting the row draw itself.
 *
 * <p>That is why {@link #paintChild} is overridden: it is the hook that runs once per row.
 */
public class ListView extends BlockView {

    private StyleSheet.ListPainter painter;

    /** A list on that element; it stacks vertically. */
    public ListView(Element elem) {
        super(elem, View.Y_AXIS);
    }

    public float getAlignment(int axis) {
        if (axis == View.Y_AXIS) {
            return 0.5f;
        }
        return super.getAlignment(axis);
    }

    public void paint(Graphics g, Shape allocation) {
        super.paint(g, allocation);
    }

    /** It draws the row's mark and then the row. */
    protected void paintChild(Graphics g, Rectangle alloc, int index) {
        if (painter != null) {
            painter.paint(g, alloc.x, alloc.y, alloc.width, alloc.height, this, index);
        }
        super.paintChild(g, alloc, index);
    }

    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        StyleSheet sheet = getStyleSheet();
        if (sheet != null) {
            painter = sheet.getListPainter(getAttributes());
        }
    }
}
