package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * What turns a label's or a button's text into drawable HTML.
 *
 * <h2>Why it exists</h2>
 *
 * <p>A {@code JLabel} paints its text with {@code drawString} and that is that. But if the text
 * starts with {@code <html>}, Swing paints it with bold, line breaks and colours. That second
 * way is not known by the label: it is known by the {@code javax.swing.text.html} engine, and
 * this class is the bridge.
 *
 * <p>The bridge is two calls. {@link #updateRenderer} builds a view and keeps it in the
 * component under the key {@link #propertyKey}; each look and feel that draws text looks it up
 * there, and if it is there, it measures and paints with it instead of with the typeface. If
 * the text stops being HTML, the view is erased.
 *
 * <h2>What counts as HTML</h2>
 *
 * <p>{@link #isHTMLString} is deliberately dim: the text has to start <em>exactly</em> with
 * {@code <html>} -- six characters, with no space in front, with no attributes --.
 * {@code "  <html>x"} does not count, and neither does {@code "<html"}. It is measured, and
 * the reason is that the test runs on every change of text of every label on the screen: it has
 * to cost five comparisons and nothing more.
 *
 * <h2>The view at the very top</h2>
 *
 * <p>The view that is kept is not the document's: it is a wrapper ({@code Renderer}) that gives
 * it a working width and translates the size questions. It is needed because the text engine
 * expects to be inside a {@code JTextComponent} with a real {@code Container}, and here there
 * is none: only the label that asked to draw.
 */
public class BasicHTML {

    /** Where the view is kept inside the component. */
    public static final String propertyKey = "html";

    /** Where the component may leave the {@link URL} the links are resolved against. */
    public static final String documentBaseKey = "html.base";

    public BasicHTML() {
    }

    /**
     * It builds the view for that text.
     *
     * <p>The component's typeface and colour go in as the body's style, so that HTML with no style
     * of its own looks like the rest of the component.
     */
    public static View createHTMLView(JComponent c, String html) {
        HTMLEditorKit kit = new HTMLEditorKit();
        Document doc = kit.createDefaultDocument();
        if (doc instanceof HTMLDocument) {
            HTMLDocument hdoc = (HTMLDocument) doc;
            Object base = c.getClientProperty(documentBaseKey);
            if (base instanceof URL) {
                hdoc.setBase((URL) base);
            }
            style(hdoc.getStyleSheet(), c.getFont(), c.getForeground());
        }
        Reader r = new StringReader(html);
        try {
            kit.read(r, doc, 0);
        } catch (Exception e) {
            // Broken HTML does not break the label: whatever could be read is drawn. It is what the
                        // JDK does, and it is the only reasonable thing -- the text was written by
                        // whoever programmed the screen, and an exception here would show up while
                        // painting, far from the mistake.
        }
        ViewFactory f = kit.getViewFactory();
        View hview = f.create(doc.getDefaultRootElement());
        return new Renderer(c, f, hview);
    }

    /** It puts the component's typeface and colour into the style sheet. */
    private static void style(StyleSheet leaf, Font font, Color color) {
        if (leaf == null || font == null) {
            return;
        }
        StringBuilder rule = new StringBuilder("body {font-family:");
        rule.append(font.getFamily()).append(";font-size:").append(font.getSize()).append("pt");
        if (font.isBold()) {
            rule.append(";font-weight:700");
        }
        if (font.isItalic()) {
            rule.append(";font-style:italic");
        }
        if (color != null) {
            rule.append(";color:#").append(hex(color));
        }
        rule.append("}");
        try {
            leaf.addRule(rule.toString());
        } catch (Exception e) {
            // A sheet that does not accept the rule leaves the HTML with its usual values, which is
                        // worse but is not an error: it goes on being seen.
        }
    }

    private static String hex(Color c) {
        String s = Integer.toHexString(c.getRGB() & 0xffffff);
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    /**
     * Whether that text is HTML; see the class note.
     *
     * @return `false` if it is null, short, or does not start with {@code <html>}
     */
    public static boolean isHTMLString(String s) {
        if (s != null) {
            if ((s.length() >= 6) && (s.charAt(0) == '<') && (s.charAt(5) == '>')) {
                String tag = s.substring(1, 5);
                return tag.equalsIgnoreCase(propertyKey);
            }
        }
        return false;
    }

    /**
     * It puts the view into the component or takes it out according to the text.
     *
     * <p>It is what has to be called every time the text changes: if it stopped being HTML, the
     * old view is erased and the component goes back to painting with {@code drawString}.
     */
    public static void updateRenderer(JComponent c, String text) {
        View value = null;
        if (isHTMLString(text)) {
            value = createHTMLView(c, text);
        }
        View oldValue = (View) c.getClientProperty(propertyKey);
        if (value != oldValue && oldValue != null) {
            for (int i = 0; i < oldValue.getViewCount(); i++) {
                oldValue.getView(i).setParent(null);
            }
        }
        c.putClientProperty(propertyKey, value);
    }

    /**
     * An HTML view's baseline.
     *
     * <p>It only has one if the HTML is a single paragraph; otherwise, -1. The question is asked
     * by {@code JLabel.getBaseline} in order to line a label up with the field beside it, and with
     * two paragraphs there is no answer that serves.
     *
     * @throws IllegalArgumentException if the width or the height are negative
     */
    public static int getHTMLBaseline(View view, int w, int h) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("Width and height must be >= 0");
        }
        if (view instanceof Renderer) {
            return baseline(view.getView(0), w, h);
        }
        return -1;
    }

    private static int baseline(View view, int w, int h) {
        if (!singleParagraph(view)) {
            return -1;
        }
        view.setSize(w, h);
        return baseline(view, new Rectangle(0, 0, w, h));
    }

    /**
     * It goes down to the paragraph and only there measures: above it everything is box, not text.
     */
    private static int baseline(View view, Shape bounds) {
        if (view.getViewCount() == 0) {
            return -1;
        }
        int index = 0;
        if (isLabel(view, "html") && view.getViewCount() > 1) {
            // The head is the first child and takes up no room; what is seen is the body.
            index = 1;
        }
        Shape hija = view.getChildAllocation(index, bounds);
        if (hija == null) {
            return -1;
        }
        View child = view.getView(index);
        if (view instanceof javax.swing.text.ParagraphView) {
            Rectangle rect = (hija instanceof Rectangle) ? (Rectangle) hija : hija.getBounds();
            return rect.y + (int) (child.getPreferredSpan(View.Y_AXIS)
                    * child.getAlignment(View.Y_AXIS));
        }
        return baseline(child, hija);
    }

    /** Whether there is exactly one paragraph: two no longer have a common baseline. */
    private static boolean singleParagraph(View view) {
        if (view instanceof javax.swing.text.ParagraphView) {
            return true;
        }
        int n = view.getViewCount();
        int paragraphs = 0;
        for (int i = 0; i < n; i++) {
            if (singleParagraph(view.getView(i))) {
                paragraphs++;
            }
            if (paragraphs > 1) {
                return false;
            }
        }
        return paragraphs == 1;
    }

    private static boolean isLabel(View view, String name) {
        Element e = view.getElement();
        if (e == null) {
            return false;
        }
        AttributeSet a = e.getAttributes();
        if (a == null) {
            return false;
        }
        Object n = a.getAttribute(javax.swing.text.StyleConstants.NameAttribute);
        return n != null && name.equalsIgnoreCase(n.toString());
    }

    /**
     * The view at the very top; see the class note.
     *
     * <p>It does not inherit from {@code javax.swing.text.View} out of convenience: it has to be
     * one because it is what is kept in the component and what the looks and feels are going to
     * use in order to measure and paint.
     */
    private static class Renderer extends View {

        private int width;
        private final View view;
        private final ViewFactory factory;
        private final JComponent host;

        Renderer(JComponent c, ViewFactory f, View v) {
            super(null);
            host = c;
            factory = f;
            view = v;
            view.setParent(this);
            // The working width starts at the preferred one: with nobody to lay it out, the view
                        // measures itself before anybody tells it how much room it has.
            setSize(view.getPreferredSpan(X_AXIS), view.getPreferredSpan(Y_AXIS));
        }

        /** The width rules: the height comes out of how the text is split at that width. */
        public void setSize(float width, float height) {
            this.width = (int) width;
            view.setSize(width, height);
        }

        public AttributeSet getAttributes() {
            return null;
        }

        public float getPreferredSpan(int axis) {
            if (axis == X_AXIS) {
                // The working width is returned and not the natural one: it is what makes a label
                // that
                                // has already been laid out not change its mind when measured
                                // again.
                return width;
            }
            return view.getPreferredSpan(axis);
        }

        public float getMinimumSpan(int axis) {
            return view.getMinimumSpan(axis);
        }

        public float getMaximumSpan(int axis) {
            // With no horizontal cap: HTML stretches as far as it is given.
            if (axis == X_AXIS) {
                return Integer.MAX_VALUE;
            }
            return view.getMaximumSpan(axis);
        }

        public void preferenceChanged(View child, boolean width, boolean height) {
            host.revalidate();
            host.repaint();
        }

        public float getAlignment(int axis) {
            return view.getAlignment(axis);
        }

        public void paint(Graphics g, Shape allocation) {
            Rectangle alloc = allocation.getBounds();
            view.setSize(alloc.width, alloc.height);
            view.paint(g, allocation);
        }

        public void setParent(View parent) {
            throw new Error("Can't set parent on root view");
        }

        public int getViewCount() {
            return 1;
        }

        public View getView(int n) {
            return view;
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b)
                throws javax.swing.text.BadLocationException {
            return view.modelToView(pos, a, b);
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            return view.viewToModel(x, y, a, bias);
        }

        public Document getDocument() {
            return view.getDocument();
        }

        public int getStartOffset() {
            return view.getStartOffset();
        }

        public int getEndOffset() {
            return view.getEndOffset();
        }

        public Element getElement() {
            return view.getElement();
        }

        public ViewFactory getViewFactory() {
            return factory;
        }
    }
}
