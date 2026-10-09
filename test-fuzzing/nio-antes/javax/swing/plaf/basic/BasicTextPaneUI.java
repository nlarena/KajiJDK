package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * The basic look and feel of a text pane with styles.
 *
 * <h2>The colour and the typeface go to the style, not to the component</h2>
 *
 * <p>It is the only difference with {@link BasicEditorPaneUI}, and it is not a small one. In a
 * pane with no styles the component's colour <em>is</em> the text's colour. In one with styles,
 * the text is drawn with whatever its style says, and nobody looks at the component's colour. So
 * what the look and feel installs -- the foreground and the typeface -- has to be put into the
 * document's default style, which is where all the others inherit from.
 *
 * <p>And only if the look and feel put it there: a colour the user set by hand is not touched,
 * which is {@link UIResource}'s rule. The same as everywhere, but here the consequence of
 * getting it wrong is worse: it would overwrite the colour of a paragraph the program set on
 * purpose.
 */
public class BasicTextPaneUI extends BasicEditorPaneUI {

    public BasicTextPaneUI() {
        super();
    }

    /** A new one per pane: a text look and feel keeps the component. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicTextPaneUI();
    }

    protected String getPropertyPrefix() {
        return "TextPane";
    }

    public void installUI(JComponent c) {
        super.installUI(c);
        updateForeground(c.getForeground());
        updateFont(c.getFont());
    }

    /** It carries to the default style whatever changed in the component. */
    protected void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);
        String name = evt.getPropertyName();
        if ("foreground".equals(name)) {
            updateForeground((Color) evt.getNewValue());
        } else if ("font".equals(name)) {
            updateFont((Font) evt.getNewValue());
        } else if ("document".equals(name)) {
            JComponent comp = (JComponent) evt.getSource();
            updateForeground(comp.getForeground());
            updateFont(comp.getFont());
        }
    }

    /** The colour to the default style; see the class note. */
    private void updateForeground(Color color) {
        StyledDocument doc = document();
        if (doc == null) {
            return;
        }
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (style == null) {
            return;
        }
        if (color == null) {
            style.removeAttribute(StyleConstants.Foreground);
            return;
        }
        if (color instanceof UIResource) {
            MutableAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, color);
            style.addAttributes(a);
        }
    }

    /** The typeface to the default style; see the class note. */
    private void updateFont(Font font) {
        StyledDocument doc = document();
        if (doc == null) {
            return;
        }
        Style style = doc.getStyle(StyleContext.DEFAULT_STYLE);
        if (style == null) {
            return;
        }
        if (font == null) {
            style.removeAttribute(StyleConstants.FontFamily);
            style.removeAttribute(StyleConstants.FontSize);
            style.removeAttribute(StyleConstants.Bold);
            style.removeAttribute(StyleConstants.Italic);
            return;
        }
        if (font instanceof UIResource) {
            MutableAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setFontFamily(a, font.getFamily());
            StyleConstants.setFontSize(a, font.getSize());
            StyleConstants.setBold(a, font.isBold());
            StyleConstants.setItalic(a, font.isItalic());
            style.addAttributes(a);
        }
    }

    private StyledDocument document() {
        JTextComponent c = getComponent();
        if (c == null) {
            return null;
        }
        javax.swing.text.Document d = c.getDocument();
        return (d instanceof StyledDocument) ? (StyledDocument) d : null;
    }
}
