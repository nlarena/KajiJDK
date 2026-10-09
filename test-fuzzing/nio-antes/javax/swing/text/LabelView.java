package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;

/**
 * A stretch of styled text: {@link GlyphView} with the attributes already resolved and kept.
 *
 * <h2>Why it keeps what it already knows how to ask for</h2>
 *
 * <p>{@code GlyphView} asks the document for the font and the colour every time. This one
 * resolves them once ({@link #setPropertiesFromAttributes}) and keeps them in fields. Drawing a
 * line consults the colour for every stretch and for every frame; with the style resolved, that
 * query is reading a field.
 *
 * <p>What is kept is redone when the attributes change, and hence {@link #changedUpdate} is the
 * only method that does anything more than forward.
 */
public class LabelView extends GlyphView implements TabableView {

    private Font font;
    private Color fg;
    private Color bg;
    private boolean underline;
    private boolean strike;
    private boolean superscript;
    private boolean subscript;

    /** Whether what is kept still holds. */
    private boolean valid;

    public LabelView(Element elem) {
        super(elem);
    }

    /** It redoes what is kept if it was needed. */
    final void sync() {
        if (!valid) {
            setPropertiesFromAttributes();
        }
    }

    protected void setUnderline(boolean u) {
        underline = u;
    }

    protected void setStrikeThrough(boolean s) {
        strike = s;
    }

    protected void setSuperscript(boolean s) {
        superscript = s;
    }

    protected void setSubscript(boolean s) {
        subscript = s;
    }

    protected void setBackground(Color bg) {
        this.bg = bg;
    }

    /** It resolves font, colours and decorations from the attributes, once. */
    protected void setPropertiesFromAttributes() {
        AttributeSet attr = getAttributes();
        if (attr != null) {
            Document d = getDocument();
            if (d instanceof StyledDocument) {
                StyledDocument doc = (StyledDocument) d;
                font = doc.getFont(attr);
                fg = doc.getForeground(attr);
                if (attr.isDefined(StyleConstants.Background)) {
                    bg = doc.getBackground(attr);
                } else {
                    bg = null;
                }
            }
            setUnderline(StyleConstants.isUnderline(attr));
            setStrikeThrough(StyleConstants.isStrikeThrough(attr));
            setSuperscript(StyleConstants.isSuperscript(attr));
            setSubscript(StyleConstants.isSubscript(attr));
            valid = true;
        }
    }

    /** The metrics of the resolved font. */
    protected FontMetrics getFontMetrics() {
        sync();
        java.awt.Container c = getContainer();
        if (c != null && font != null) {
            return c.getFontMetrics(font);
        }
        if (font != null) {
            return java.awt.Toolkit.getDefaultToolkit().getFontMetrics(font);
        }
        return null;
    }

    public Color getBackground() {
        sync();
        return bg;
    }

    public Color getForeground() {
        sync();
        return fg;
    }

    public Font getFont() {
        sync();
        return font;
    }

    public boolean isUnderline() {
        sync();
        return underline;
    }

    public boolean isStrikeThrough() {
        sync();
        return strike;
    }

    public boolean isSubscript() {
        sync();
        return subscript;
    }

    public boolean isSuperscript() {
        sync();
        return superscript;
    }

    /** The attributes changed: what is kept stopped holding. */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        valid = false;
        super.changedUpdate(e, a, f);
    }
}
