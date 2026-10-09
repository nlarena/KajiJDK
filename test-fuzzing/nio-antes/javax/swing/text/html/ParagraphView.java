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
 * The view of an HTML paragraph.
 *
 * <h2>What it adds over the plain paragraph</h2>
 *
 * <p>It takes the margins, the background and the alignment from the CSS, and knows how to hide
 * itself. The hiding is not an ornament: a paragraph the parser invented and that was left
 * without text -- because the HTML had two block tags in a row -- must not leave a blank line.
 */
public class ParagraphView extends javax.swing.text.ParagraphView {

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;

    /** A paragraph view on that element. */
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

    /** It reads margins, background and alignment from the style sheet. */
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

    /** The document's style sheet, or null if the document is not an HTML one. */
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
     * Whether the paragraph is seen.
     *
     * <p>A paragraph is seen if any of its rows has something. An invented and empty one is not,
     * and that is why it disappears instead of leaving a gap; see the class note.
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
            // The document's last paragraph is seen even if it is empty: it is where the cursor
            // goes.
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
