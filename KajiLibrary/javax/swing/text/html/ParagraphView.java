package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

/**
 * La vista de un parrafo de HTML.
 *
 * <h2>Que agrega sobre el parrafo comun</h2>
 *
 * <p>Toma del CSS los margenes, el fondo y la alineacion, y sabe esconderse. Lo de esconderse no es
 * un adorno: un parrafo que el analizador invento y que quedo sin texto -- porque el HTML tenia dos
 * etiquetas de bloque seguidas -- no tiene que dejar un renglon en blanco.
 */
public class ParagraphView extends javax.swing.text.ParagraphView {

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;

    /** Una vista de parrafo sobre ese elemento. */
    public ParagraphView(Element elem) {
        super(elem);
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = (sheet == null) ? super.getAttributes() : sheet.getViewAttributes(this);
        }
        return attr;
    }

    /** Lee margenes, fondo y alineacion de la hoja de estilos. */
    protected void setPropertiesFromAttributes() {
        attr = null;
        StyleSheet sheet = getStyleSheet();
        if (sheet == null) {
            return;
        }
        AttributeSet a = getAttributes();
        painter = sheet.getBoxPainter(a);
        setInsets((short) painter.getInset(TOP, this), (short) painter.getInset(LEFT, this),
                (short) painter.getInset(BOTTOM, this), (short) painter.getInset(RIGHT, this));
        Object al = a.getAttribute(CSS.Attribute.TEXT_ALIGN);
        if (al != null) {
            String s = al.toString();
            if (s.equals("left")) {
                setJustification(StyleConstants.ALIGN_LEFT);
            } else if (s.equals("center")) {
                setJustification(StyleConstants.ALIGN_CENTER);
            } else if (s.equals("right")) {
                setJustification(StyleConstants.ALIGN_RIGHT);
            } else if (s.equals("justify")) {
                setJustification(StyleConstants.ALIGN_JUSTIFIED);
            }
        }
    }

    /** La hoja de estilos del documento, o nulo si el documento no es de HTML. */
    protected StyleSheet getStyleSheet() {
        Document d = getDocument();
        if (d instanceof HTMLDocument) {
            return ((HTMLDocument) d).getStyleSheet();
        }
        return null;
    }

    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        return super.calculateMinorAxisRequirements(axis, r);
    }

    /**
     * Si el parrafo se ve.
     *
     * <p>Un parrafo se ve si alguno de sus renglones tiene algo. Uno inventado y vacio no, y por
     * eso desaparece en lugar de dejar un hueco; ver la nota de la clase.
     */
    public boolean isVisible() {
        int n = getLayoutViewCount() - 1;
        for (int i = 0; i < n; i++) {
            View v = getLayoutView(i);
            if (v.getEndOffset() - v.getStartOffset() > 0) {
                return true;
            }
        }
        if (n > 0) {
            View v = getLayoutView(n);
            if ((v.getEndOffset() - v.getStartOffset()) > 1) {
                return true;
            }
        }
        if (getStartOffset() == getDocument().getLength()) {
            // El ultimo parrafo del documento se ve aunque este vacio: es donde va el cursor.
            return true;
        }
        return false;
    }

    public void paint(Graphics g, Shape a) {
        if (!isVisible()) {
            return;
        }
        if (painter != null) {
            Rectangle r = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            painter.paint(g, r.x, r.y, r.width, r.height, this);
        }
        super.paint(g, a);
    }

    public float getPreferredSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getPreferredSpan(axis);
    }

    public float getMinimumSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMinimumSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        if (!isVisible()) {
            return 0;
        }
        return super.getMaximumSpan(axis);
    }
}
