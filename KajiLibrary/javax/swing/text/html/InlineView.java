package javax.swing.text.html;

import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * The view of the text that goes on the line.
 *
 * <h2>What it adds over a plain label</h2>
 *
 * <p>A {@link LabelView} draws text with a typeface. This one takes that typeface and that
 * colour from the style sheet instead of from the Swing attributes, and also obeys
 * <code>white-space: nowrap</code>.
 *
 * <h2>To break or not to break</h2>
 *
 * <p>{@link #getBreakWeight} answers how much it suits this view to split so that the paragraph
 * fits the width. With <code>nowrap</code> it answers that it does not split in any way, and
 * then the paragraph runs over. It is what that property asks for: better a long line than a
 * word broken where the author said not to.
 */
public class InlineView extends LabelView {

    private AttributeSet attr;
    private boolean noBreak;

    /** An inline text view on that element. */
    public InlineView(Element elem) {
        super(elem);
        StyleSheet sheet = getStyleSheet();
        attr = (sheet == null) ? null : sheet.getViewAttributes(this);
    }

    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.insertUpdate(e, a, f);
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.removeUpdate(e, a, f);
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e, a, f);
        StyleSheet sheet = getStyleSheet();
        attr = (sheet == null) ? null : sheet.getViewAttributes(this);
        preferenceChanged(null, true, true);
    }

    public AttributeSet getAttributes() {
        return (attr == null) ? super.getAttributes() : attr;
    }

    /** How much it suits to break here; zero if the CSS forbids it. */
    public int getBreakWeight(int axis, float pos, float len) {
        if (noBreak) {
            return BadBreakWeight;
        }
        return super.getBreakWeight(axis, pos, len);
    }

    public View breakView(int axis, int offset, float pos, float len) {
        if (noBreak) {
            return this;
        }
        return super.breakView(axis, offset, pos, len);
    }

    /** It reads from the CSS what changes how the text is drawn. */
    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        StyleSheet sheet = getStyleSheet();
        if (sheet == null) {
            return;
        }
        AttributeSet a = getAttributes();
        Object ws = a.getAttribute(CSS.Attribute.WHITE_SPACE);
        noBreak = (ws != null && "nowrap".equals(ws.toString()));
        // The colour and the typeface are not set from here: GlyphView asks the attributes for
                // them, and the attributes are already the ones the sheet resolved.
        Object dec = a.getAttribute(CSS.Attribute.TEXT_DECORATION);
        String d = (dec == null) ? "" : dec.toString();
        setUnderline(d.indexOf("underline") >= 0);
        setStrikeThrough(d.indexOf("line-through") >= 0);
        Object va = a.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
        if (va != null) {
            String v = va.toString();
            setSuperscript(v.indexOf("sup") >= 0);
            setSubscript(v.indexOf("sub") >= 0);
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
