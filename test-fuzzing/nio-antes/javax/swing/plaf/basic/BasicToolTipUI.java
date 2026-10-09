package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.LookAndFeel;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ToolTipUI;
import javax.swing.plaf.UIResource;
import javax.swing.text.View;

/**
 * The basic look and feel of a tool tip: the text over a rectangle with a one-line border.
 *
 * <h2>The six pixels</h2>
 *
 * <p>The preferred width is the margins plus the text's width <em>plus six</em>, and the text
 * is drawn three pixels to the right of the left margin. That six does not come from any
 * property: it is a constant of the JDK, and it is what keeps the text from being stuck to the
 * border. It is measured: a tip with {@code "hola"} in Dialog 12 measures 32 x 18, and one with no
 * text measures 2 x 2 -- only the border --.
 *
 * <p>A tip with no text reserves no line height: {@link #getPreferredSize} only adds the
 * typeface's height when there is something to write.
 *
 * <h2>The three sizes are the same</h2>
 *
 * <p>Minimum, preferred and maximum give the same. A tool tip neither stretches nor shrinks:
 * the popup window puts it at whatever size it asks for and that is that.
 *
 * <h2>What it installs</h2>
 *
 * <p>The values of {@code ToolTip.*} measured in Metal (JDK 25): background (184, 207, 229),
 * foreground (51, 51, 51), Dialog 12 and a one-pixel line border in (99, 130, 191). The tip is
 * left opaque.
 *
 * <h2>Text with tags</h2>
 *
 * <p>If the text is HTML -- see {@link BasicHTML#isHTMLString} -- the tip keeps a built view
 * and it is that one that measures and paints. {@link #installUI} and the change of text update
 * it.
 */
public class BasicToolTipUI extends ToolTipUI {

    private static BasicToolTipUI sharedInstance = new BasicToolTipUI();

    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(184, 207, 229);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final Font DEFAULT_FONT = new FontUIResource("Dialog", Font.PLAIN, 12);
    private static final Border DEFAULT_BORDER =
            new BorderUIResource.LineBorderUIResource(new ColorUIResource(99, 130, 191), 1);

    public BasicToolTipUI() {
        super();
    }

    /** The shared look and feel: it keeps nothing of the tip. */
    public static ComponentUI createUI(JComponent c) {
        return sharedInstance;
    }

    public void installUI(JComponent c) {
        installDefaults(c);
        installComponents(c);
        installListeners(c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults(c);
        uninstallComponents(c);
        uninstallListeners(c);
    }

    /** Colours, typeface, border and opacity; see the class note. */
    protected void installDefaults(JComponent c) {
        Color background = c.getBackground();
        if (background == null || background instanceof UIResource) {
            c.setBackground(DEFAULT_BACKGROUND);
        }
        Color foreground = c.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            c.setForeground(DEFAULT_FOREGROUND);
        }
        Font font = c.getFont();
        if (font == null || font instanceof UIResource) {
            c.setFont(DEFAULT_FONT);
        }
        Border border = c.getBorder();
        if (border == null || border instanceof UIResource) {
            c.setBorder(DEFAULT_BORDER);
        }
        LookAndFeel.installProperty(c, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JComponent c) {
    }

    /** It builds the view if the text is HTML; see the class note. */
    private void installComponents(JComponent c) {
        BasicHTML.updateRenderer(c, ((JToolTip) c).getTipText());
    }

    private void uninstallComponents(JComponent c) {
        BasicHTML.updateRenderer(c, "");
    }

    /** It listens to nothing: the tip is handled by {@code ToolTipManager}. */
    protected void installListeners(JComponent c) {
    }

    /** The same. */
    protected void uninstallListeners(JComponent c) {
    }

    /** The text, three pixels to the right of the margin; see the class note. */
    public void paint(Graphics g, JComponent c) {
        Font font = c.getFont();
        FontMetrics metrics = c.getFontMetrics(font);
        Dimension size = c.getSize();

        g.setColor(c.getForeground());
        String tipText = ((JToolTip) c).getTipText();
        if (tipText == null) {
            tipText = "";
        }

        Insets insets = c.getInsets();
        Rectangle paintTextR = new Rectangle(
                insets.left + 3,
                insets.top,
                size.width - (insets.left + insets.right) - 6,
                size.height - (insets.top + insets.bottom));
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            v.paint(g, paintTextR);
        } else {
            g.setFont(font);
            g.drawString(tipText, paintTextR.x, paintTextR.y + metrics.getAscent());
        }
    }

    /** Margins plus the text plus six; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        Font font = c.getFont();
        FontMetrics fm = c.getFontMetrics(font);
        Insets insets = c.getInsets();

        Dimension prefSize = new Dimension(insets.left + insets.right,
                insets.top + insets.bottom);
        String text = ((JToolTip) c).getTipText();

        if (text != null && !text.equals("")) {
            View v = (View) c.getClientProperty(BasicHTML.propertyKey);
            if (v != null) {
                prefSize.width += (int) v.getPreferredSpan(View.X_AXIS);
                prefSize.height += (int) v.getPreferredSpan(View.Y_AXIS);
            } else {
                prefSize.width += fm.stringWidth(text) + 6;
                prefSize.height += fm.getHeight();
            }
        }
        return prefSize;
    }

    /** The same as the preferred one; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    /** The same as the preferred one; see the class note. */
    public Dimension getMaximumSize(JComponent c) {
        Dimension d = getPreferredSize(c);
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }
        return d;
    }
}
