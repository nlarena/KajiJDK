package javax.swing.text.html;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * The view of an {@code <img>}.
 *
 * <h2>Three things it may be showing</h2>
 *
 * <p>The image, if it has arrived. A "loading" icon, while it travels. Or a "could not"
 * icon, if it failed. All three take up room, and that is why the view has a size from the
 * start: if it measured zero until the image arrived, the page would rebuild itself entirely as
 * each one arrived.
 *
 * <p>That is also the reason <code>width</code> and <code>height</code> are worth it in the
 * HTML: with them the view already knows how much it is going to take up before having
 * anything.
 *
 * <h2>Loading one at a time or waiting</h2>
 *
 * <p>{@link #setLoadsSynchronously} decides whether the loading blocks. By default it does not
 * block: a slow image must not leave the window still. Blocking serves for printing or for
 * drawing off screen, where there is nobody to repaint when the image arrives.
 *
 * <p>Without a window there is no image to load, so here the alternative text is always shown.
 */
public class ImageView extends View {

    private AttributeSet attr;
    private Image image;
    private int width;
    private int height;
    private boolean loadsSynchronously;
    private Color borderColor;
    private short borderSize;
    private short leftInset;
    private short rightInset;
    private short topInset;
    private short bottomInset;

    /** An image view on that element. */
    public ImageView(Element elem) {
        super(elem);
        loadsSynchronously = false;
    }

    /** The <code>alt</code> attribute's text. */
    public String getAltText() {
        return (String) getElement().getAttributes().getAttribute(HTML.Attribute.ALT);
    }

    /**
     * The image's address, resolved against the document's base.
     *
     * <p>It returns null if there is no <code>src</code> or if the address cannot be formed. A
     * broken <code>src</code> is normal in real HTML, and should not cost an exception.
     */
    public URL getImageURL() {
        String src = (String) getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
        if (src == null) {
            return null;
        }
        URL base = ((HTMLDocument) getDocument()).getBase();
        try {
            return new URL(base, src);
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /** The icon shown when the image could not be fetched. */
    public Icon getNoImageIcon() {
        return null;
    }

    /** The icon shown while the image travels. */
    public Icon getLoadingImageIcon() {
        return null;
    }

    /** The image, or null if it has not arrived yet. */
    public Image getImage() {
        return image;
    }

    /** Whether the loading blocks; see the class note. */
    public void setLoadsSynchronously(boolean newValue) {
        loadsSynchronously = newValue;
    }

    public boolean getLoadsSynchronously() {
        return loadsSynchronously;
    }

    /** The document's style sheet, or null if the document is not an HTML one. */
    protected StyleSheet getStyleSheet() {
        Document d = getDocument();
        if (d instanceof HTMLDocument) {
            return ((HTMLDocument) d).getStyleSheet();
        }
        return null;
    }

    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = (sheet == null) ? super.getAttributes() : sheet.getViewAttributes(this);
        }
        return attr;
    }

    /** The tooltip text: the <code>alt</code> itself. */
    public String getToolTipText(float x, float y, Shape allocation) {
        return getAltText();
    }

    /** It reads size and border from the attributes. */
    protected void setPropertiesFromAttributes() {
        attr = null;
        AttributeSet a = getAttributes();
        width = HTML.getIntegerAttributeValue(a, HTML.Attribute.WIDTH, -1);
        height = HTML.getIntegerAttributeValue(a, HTML.Attribute.HEIGHT, -1);
        borderSize = (short) HTML.getIntegerAttributeValue(a, HTML.Attribute.BORDER, 0);
        int h = HTML.getIntegerAttributeValue(a, HTML.Attribute.HSPACE, 0);
        int v = HTML.getIntegerAttributeValue(a, HTML.Attribute.VSPACE, 0);
        leftInset = (short) (h + borderSize);
        rightInset = (short) (h + borderSize);
        topInset = (short) (v + borderSize);
        bottomInset = (short) (v + borderSize);
        borderColor = Color.black;
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e, a, f);
        setPropertiesFromAttributes();
        preferenceChanged(null, true, true);
    }

    /** It draws the image if it arrived, and otherwise the alternative text with its frame. */
    public void paint(Graphics g, Shape a) {
        Rectangle rect = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
        if (borderSize > 0 && borderColor != null) {
            g.setColor(borderColor);
            for (int i = 0; i < borderSize; i++) {
                g.drawRect(rect.x + i, rect.y + i, rect.width - 2 * i - 1,
                        rect.height - 2 * i - 1);
            }
        }
        if (image != null) {
            g.drawImage(image, rect.x + leftInset, rect.y + topInset,
                    rect.width - leftInset - rightInset,
                    rect.height - topInset - bottomInset, null);
            return;
        }
        String alt = getAltText();
        if (alt != null) {
            java.awt.FontMetrics fm = g.getFontMetrics();
            g.drawString(alt, rect.x + leftInset, rect.y + topInset + fm.getAscent());
        }
    }

    /**
     * The size it takes up.
     *
     * <p>The declared one if there is one; if not, the image's; if not that either, a box where the
     * alternative text fits. Never zero: see the class note.
     */
    public float getPreferredSpan(int axis) {
        if (axis == X_AXIS) {
            if (width > 0) {
                return width + leftInset + rightInset;
            }
            String alt = getAltText();
            int base = (alt == null) ? 32 : Math.max(32, alt.length() * 7);
            return base + leftInset + rightInset;
        }
        if (height > 0) {
            return height + topInset + bottomInset;
        }
        return 32 + topInset + bottomInset;
    }

    /** An image rests on the text's baseline. */
    public float getAlignment(int axis) {
        if (axis == Y_AXIS) {
            return 1.0f;
        }
        return super.getAlignment(axis);
    }

    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        int p0 = getStartOffset();
        int p1 = getEndOffset();
        if ((pos >= p0) && (pos <= p1)) {
            Rectangle r = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            if (pos == p1) {
                r.x = r.x + r.width;
            }
            r.width = 0;
            return r;
        }
        throw new BadLocationException(pos + " not in range " + p0 + "," + p1, pos);
    }

    public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
        Rectangle alloc = (Rectangle) a;
        if (x < alloc.x + alloc.width / 2f) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
        bias[0] = Position.Bias.Backward;
        return getEndOffset();
    }

    public void setSize(float width, float height) {
    }
}
