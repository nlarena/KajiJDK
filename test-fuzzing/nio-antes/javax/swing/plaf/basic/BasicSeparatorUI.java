package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SeparatorUI;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel of a separator: two one-pixel lines, one on top of the other.
 *
 * <p>The top one with the foreground colour and the bottom one with the background one. That
 * pair is what gives the illusion of a dent: a dark line and a light one stuck together read as
 * a groove. The colours come from {@code Separator.foreground} and
 * {@code Separator.background}, measured in Metal (JDK 25): (99, 130, 191) and white.
 *
 * <h2>Two fields that are not used</h2>
 *
 * <p>{@link #shadow} and {@link #highlight} are protected, they exist, and they are left
 * {@code null}: nobody writes them and {@link #paint} does not look at them, because it paints
 * with the component's foreground and background. They are from an earlier version of the JDK
 * and stayed for compatibility. It is measured, and it is copied as it is: a subclass that
 * reads them has to see {@code null} just as in the JDK.
 *
 * <h2>One object per separator</h2>
 *
 * <p>Unlike almost every other one, {@link #createUI} returns a new instance each time. It does
 * not keep anything either, so it is not needed; it is like that in the JDK and it is measured.
 *
 * <h2>No minimum</h2>
 *
 * <p>{@link #getMinimumSize} returns {@code null}, not a size. It is what the JDK does, and the
 * caller has to be ready for it: {@code JComponent.getMinimumSize} reads it as "I have no
 * opinion" and answers whatever the layout says.
 */
public class BasicSeparatorUI extends SeparatorUI {

    /** Unused; see the class note. */
    protected Color shadow;

    /** Unused; see the class note. */
    protected Color highlight;

    private static final ColorUIResource DEFAULT_BACKGROUND = new ColorUIResource(255, 255, 255);
    private static final ColorUIResource DEFAULT_FOREGROUND = new ColorUIResource(99, 130, 191);

    public BasicSeparatorUI() {
    }

    /** A new one each time; see the class note. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicSeparatorUI();
    }

    public void installUI(JComponent c) {
        installDefaults((JSeparator) c);
        installListeners((JSeparator) c);
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults((JSeparator) c);
        uninstallListeners((JSeparator) c);
    }

    /** The two colours and transparency: a separator is not opaque. */
    protected void installDefaults(JSeparator s) {
        Color background = s.getBackground();
        if (background == null || background instanceof UIResource) {
            s.setBackground(DEFAULT_BACKGROUND);
        }
        Color foreground = s.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            s.setForeground(DEFAULT_FOREGROUND);
        }
        LookAndFeel.installProperty(s, "opaque", Boolean.FALSE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults(JSeparator s) {
    }

    /** It listens to nothing: a separator does not change by itself. */
    protected void installListeners(JSeparator s) {
    }

    /** The same. */
    protected void uninstallListeners(JSeparator s) {
    }

    /** The two lines; see the class note. */
    public void paint(Graphics g, JComponent c) {
        Dimension s = c.getSize();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, 0, s.height);
            g.setColor(c.getBackground());
            g.drawLine(1, 0, 1, s.height);
        } else {
            g.setColor(c.getForeground());
            g.drawLine(0, 0, s.width, 0);
            g.setColor(c.getBackground());
            g.drawLine(0, 1, s.width, 1);
        }
    }

    /** Two pixels thick and no length: it is stretched by the layout. */
    public Dimension getPreferredSize(JComponent c) {
        Insets insets = c.getInsets();
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2 + insets.left + insets.right, insets.top + insets.bottom);
        }
        return new Dimension(insets.left + insets.right, 2 + insets.top + insets.bottom);
    }

    /** {@code null}; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    /** As long as it takes, two pixels thick. */
    public Dimension getMaximumSize(JComponent c) {
        if (((JSeparator) c).getOrientation() == SwingConstants.VERTICAL) {
            return new Dimension(2, Short.MAX_VALUE);
        }
        return new Dimension(Short.MAX_VALUE, 2);
    }
}
