package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * The view of an HTML element that makes a block.
 *
 * <h2>What it adds over a plain box</h2>
 *
 * <p>A {@link BoxView} stacks its children. This one also looks at the CSS: it takes the
 * margins and the background from the style sheet, and obeys a declared <code>width</code> or
 * <code>height</code>.
 *
 * <p>The attributes are read in {@link #setPropertiesFromAttributes}, and that happens when it
 * is hung from a parent and every time the document changes the attributes. It cannot be done in
 * the constructor: without a parent there is no document, and without a document there is no
 * style sheet to consult.
 */
public class BlockView extends BoxView {

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;
    private float requestedWidth = -1;
    private float requestedHeight = -1;

    /** A block view on that axis. */
    public BlockView(Element elem, int axis) {
        super(elem, axis);
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
        SizeRequirements rr = super.calculateMajorAxisRequirements(axis, r);
        return adjust(axis, rr);
    }

    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        SizeRequirements rr = super.calculateMinorAxisRequirements(axis, r);
        return adjust(axis, rr);
    }

    /**
     * It imposes the size declared in the CSS, if there is one.
     *
     * <p>A declared <code>width</code> sets the minimum, the preferred and the maximum to the same
     * value. Setting only the preferred one would not be enough: the sharing out done by the box
     * above would stretch it all the same.
     */
    private SizeRequirements adjust(int axis, SizeRequirements r) {
        float requested = (axis == X_AXIS) ? requestedWidth : requestedHeight;
        if (requested > 0) {
            r.minimum = (int) requested;
            r.preferred = (int) requested;
            r.maximum = (int) requested;
        }
        return r;
    }

    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        super.layoutMinorAxis(targetSpan, axis, offsets, spans);
    }

    /** It draws the block's background and then the children. */
    public void paint(Graphics g, Shape allocation) {
        Rectangle a = (Rectangle) allocation;
        if (painter != null) {
            painter.paint(g, a.x, a.y, a.width, a.height, this);
        }
        super.paint(g, a);
    }

    /**
     * The view's attributes: the element's plus whatever the sheet contributes.
     *
     * <p>They are computed once and kept. Resolving them again on every query would be right and
     * would be slow: they are consulted on every line that is drawn.
     */
    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = (sheet == null) ? super.getAttributes()
                    : sheet.getViewAttributes(this);
        }
        return attr;
    }

    /** A block stretches on the minor axis and not on the major one. */
    public int getResizeWeight(int axis) {
        if (axis == X_AXIS) {
            return 1;
        }
        return 0;
    }

    public float getAlignment(int axis) {
        if (axis == X_AXIS) {
            return 0;
        }
        return super.getAlignment(axis);
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.changedUpdate(changes, a, f);
        int pos = changes.getOffset();
        if (pos <= getStartOffset() && (pos + changes.getLength()) >= getEndOffset()) {
            setPropertiesFromAttributes();
        }
    }

    public float getPreferredSpan(int axis) {
        return super.getPreferredSpan(axis);
    }

    public float getMinimumSpan(int axis) {
        return super.getMinimumSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        return super.getMaximumSpan(axis);
    }

    /** It reads margins, background and size from the style sheet. */
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
        requestedWidth = measured(a, CSS.Attribute.WIDTH);
        requestedHeight = measured(a, CSS.Attribute.HEIGHT);
    }

    /** A length declared in pixels, or -1 if there is none or it is not understood. */
    private static float measured(AttributeSet a, CSS.Attribute key) {
        Object o = a.getAttribute(key);
        if (o == null) {
            return -1;
        }
        String s = o.toString().trim();
        if (s.endsWith("px") || s.endsWith("pt")) {
            s = s.substring(0, s.length() - 2).trim();
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException nfe) {
            // A percentage or something odd: the box is left to decide.
            return -1;
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
}
