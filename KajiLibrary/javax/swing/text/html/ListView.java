package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.Element;
import javax.swing.text.View;

/**
 * La vista de una lista: {@code <ul>}, {@code <ol>}, {@code <dir>} o {@code <menu>}.
 *
 * <h2>Quien dibuja la vineta</h2>
 *
 * <p>La lista, no el renglon. Parece al reves, y no lo es: la vineta va afuera del renglon, en el
 * margen, y el renglon no sabe cuanto margen tiene ni que numero le toca. La lista si sabe las dos
 * cosas, asi que dibuja la marca antes de dejar que el renglon se dibuje solo.
 *
 * <p>Por eso {@link #paintChild} esta sobrescrito: es el gancho que corre una vez por renglon.
 */
public class ListView extends BlockView {

    private StyleSheet.ListPainter painter;

    /** Una lista sobre ese elemento; se apila verticalmente. */
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

    /** Dibuja la marca del renglon y despues el renglon. */
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
