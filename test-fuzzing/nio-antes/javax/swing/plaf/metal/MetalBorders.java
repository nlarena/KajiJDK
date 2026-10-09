package javax.swing.plaf.metal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.BorderUIResource$CompoundBorderUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicBorders$MarginBorder;

/**
 * The borders of the Metal look and feel; for now, the button's.
 *
 * <p>Metal has two themes, Steel and Ocean, and since JDK 6 the one seen is Ocean. This border
 * paints what Ocean paints, measured in JDK 25: a one-pixel rectangle in the theme's dark
 * shadow (122, 138, 153); pressed, that same colour two pixels at the top and on the left and
 * one at the bottom and on the right; disabled, the inactive text grey (153, 153, 153). The
 * colours are here as constants because {@code MetalLookAndFeel}, which would have them as a
 * theme, is not.
 *
 * <p>What Ocean also paints --the gradient of the button's background-- does not belong to the
 * border but to {@code MetalButtonUI.update}, and is not there: the background is flat.
 */
public class MetalBorders {

    private static final Color DARK_SHADOW = new Color(122, 138, 153);
    private static final Color PRIMARY_CONTROL = new Color(184, 207, 229);
    private static final Color INACTIVE_TEXT = new Color(153, 153, 153);
    private static final Color HIGHLIGHT = new Color(255, 255, 255);
    private static final Color CONTROL = new Color(238, 238, 238);

    private static Border buttonBorder;
    private static Border toggleButtonBorder;

    public MetalBorders() {
    }

    /** A button's border in Ocean; see the class note. */
    public static class ButtonBorder extends AbstractBorder implements UIResource {

        /** Three pixels per side; the button's margin goes inside these. */
        protected static Insets borderInsets = new Insets(3, 3, 3, 3);

        public ButtonBorder() {
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!(c instanceof AbstractButton)) {
                return;
            }
            AbstractButton button = (AbstractButton) c;
            ButtonModel model = button.getModel();
            g.translate(x, y);
            if (model.isEnabled()) {
                boolean byDefault = (c instanceof JButton) && ((JButton) c).isDefaultButton();
                if (byDefault) {
                    g.setColor(DARK_SHADOW);
                    g.drawRect(0, 0, w - 1, h - 1);
                    g.drawRect(1, 1, w - 3, h - 3);
                } else if (model.isPressed()) {
                    g.setColor(DARK_SHADOW);
                    g.fillRect(0, 0, w, 2);
                    g.fillRect(0, 2, 2, h - 2);
                    g.fillRect(w - 1, 1, 1, h - 1);
                    g.fillRect(1, h - 1, w - 2, 1);
                } else if (model.isRollover() && button.isRolloverEnabled()) {
                    g.setColor(PRIMARY_CONTROL);
                    g.drawRect(0, 0, w - 1, h - 1);
                    g.drawRect(1, 1, w - 3, h - 3);
                    g.setColor(DARK_SHADOW);
                    g.drawRect(0, 0, w - 1, h - 1);
                } else {
                    g.setColor(DARK_SHADOW);
                    g.drawRect(0, 0, w - 1, h - 1);
                }
            } else {
                g.setColor(INACTIVE_TEXT);
                g.drawRect(0, 0, w - 1, h - 1);
                if ((c instanceof JButton) && ((JButton) c).isDefaultButton()) {
                    g.drawRect(1, 1, w - 3, h - 3);
                }
            }
            g.translate(-x, -y);
        }

        public Insets getBorderInsets(Component c, Insets newInsets) {
            newInsets.top = 3;
            newInsets.left = 3;
            newInsets.bottom = 3;
            newInsets.right = 3;
            return newInsets;
        }
    }

    /**
     * The border of a button with state in Ocean.
     *
     * <p>It is the same stroke as {@link ButtonBorder}, measured: selected without being pressed it
     * looks as at rest, and pressed it sinks just like an ordinary button. What distinguishes a
     * selected stateful button is the background, painted by its UI, not the border.
     */
    public static class ToggleButtonBorder extends ButtonBorder {

        public ToggleButtonBorder() {
        }
    }

    /**
     * The border of a pane with scroll bars.
     *
     * <p>A dark rectangle, two highlight lines outside it at the bottom and on the right --white,
     * that is invisible over a white background-- and <strong>two loose pixels</strong> of the
     * control's colour: one at the top right and another at the bottom left, where the headers
     * end. Those two pixels are what keeps the frame from closing right where a row or column
     * header rests against it, and they are measured like the rest.
     *
     * <p>The insets are asymmetric --(1, 1, 2, 2)-- because the highlight lines go outside the
     * rectangle, at the bottom and on the right.
     */
    public static class ScrollPaneBorder extends AbstractBorder implements UIResource {

        private static final Insets INSETS = new Insets(1, 1, 2, 2);

        public ScrollPaneBorder() {
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!(c instanceof JScrollPane)) {
                return;
            }
            JScrollPane scroll = (JScrollPane) c;
            JComponent colHeader = scroll.getColumnHeader();
            int colHeaderHeight = 0;
            if (colHeader != null) {
                colHeaderHeight = colHeader.getHeight();
            }
            JComponent rowHeader = scroll.getRowHeader();
            int rowHeaderWidth = 0;
            if (rowHeader != null) {
                rowHeaderWidth = rowHeader.getWidth();
            }

            g.translate(x, y);

            g.setColor(DARK_SHADOW);
            g.drawRect(0, 0, w - 2, h - 2);
            g.setColor(HIGHLIGHT);
            g.drawLine(w - 1, 1, w - 1, h - 1);
            g.drawLine(1, h - 1, w - 1, h - 1);

            g.setColor(CONTROL);
            g.drawLine(w - 2, 2 + colHeaderHeight, w - 2, 2 + colHeaderHeight);
            g.drawLine(1 + rowHeaderWidth, h - 2, 1 + rowHeaderWidth, h - 2);

            g.translate(-x, -y);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = INSETS.top;
            insets.left = INSETS.left;
            insets.bottom = INSETS.bottom;
            insets.right = INSETS.right;
            return insets;
        }
    }

    /**
     * The border Metal installs on a {@code JButton}: Ocean's on the outside and the button's
     * margin inside. Shared: it keeps nothing of the button.
     */
    public static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = new BorderUIResource$CompoundBorderUIResource(new ButtonBorder(),
                    new BasicBorders$MarginBorder());
        }
        return buttonBorder;
    }

    /** The border Metal installs on a {@code JToggleButton}, with the margin inside. */
    public static Border getToggleButtonBorder() {
        if (toggleButtonBorder == null) {
            toggleButtonBorder = new BorderUIResource$CompoundBorderUIResource(
                    new ToggleButtonBorder(), new BasicBorders$MarginBorder());
        }
        return toggleButtonBorder;
    }

    /**
     * An internal frame's frame.
     *
     * <p>Four pixels thick, and the colour depends on whether the window is active: the theme's
     * dark primary when it is and the shadow when it is not. It is the only thing that tells at a
     * glance the window being worked in from those behind, because both have a title bar.
     */
    public static class InternalFrameBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGINS = new Insets(4, 4, 4, 4);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            boolean active = (c instanceof javax.swing.JInternalFrame)
                    && ((javax.swing.JInternalFrame) c).isSelected();
            g.setColor(active
                    ? MetalLookAndFeel.getPrimaryControlDarkShadow()
                    : MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
            g.drawRect(x + 1, y + 1, w - 3, h - 3);
            g.setColor(active
                    ? MetalLookAndFeel.getPrimaryControlShadow()
                    : MetalLookAndFeel.getControlShadow());
            g.drawRect(x + 2, y + 2, w - 5, h - 5);
            g.drawRect(x + 3, y + 3, w - 7, h - 7);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGINS.top, MARGINS.left, MARGINS.bottom, MARGINS.right);
            return insets;
        }
    }

    /**
     * An internal frame's frame in palette mode: a single pixel.
     *
     * <p>Four pixels would be a quarter of a small palette's height. That it is one is what makes
     * a palette look like a little window and not like a frame with something inside.
     */
    public static class PaletteBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGINS = new Insets(1, 1, 1, 1);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGINS.top, MARGINS.left, MARGINS.bottom, MARGINS.right);
            return insets;
        }
    }

    private static Border paletteBorder;

    /** A palette's frame; see {@link PaletteBorder}. */
    public static Border getPaletteBorder() {
        if (paletteBorder == null) {
            paletteBorder = new PaletteBorder();
        }
        return paletteBorder;
    }

    private static Border internalFrameBorder;

    /** An internal frame's frame; see {@link InternalFrameBorder}. */
    public static Border getInternalFrameBorder() {
        if (internalFrameBorder == null) {
            internalFrameBorder = new InternalFrameBorder();
        }
        return internalFrameBorder;
    }

    private static Border textBorder;
    private static Border textFieldBorder;
    private static Border desktopIconBorder;

    /**
     * A text component's border: one pixel of frame and one pixel of air.
     *
     * <p>The margins are {@code (2,2,2,2)} and not {@code (1,1,1,1)}, and that extra pixel is what
     * keeps the cursor from being stuck to the frame's line. Measured.
     *
     * <p>It is the same object {@link #getTextFieldBorder} returns: both exist because the JDK
     * declares them separately, but they are worth the same.
     */
    public static Border getTextBorder() {
        if (textBorder == null) {
            textBorder = new BorderUIResource$CompoundBorderUIResource(
                    new Flush3DBorder(), new BasicBorders$MarginBorder());
        }
        return textBorder;
    }

    /** The one for a {@code JTextField}; see {@link #getTextBorder}. */
    public static Border getTextFieldBorder() {
        if (textFieldBorder == null) {
            textFieldBorder = new BorderUIResource$CompoundBorderUIResource(
                    new Flush3DBorder(), new BasicBorders$MarginBorder());
        }
        return textFieldBorder;
    }

    /**
     * The one for a minimized internal frame.
     *
     * <p>The margins are {@code (3,3,2,3)}: one less at the bottom. The asymmetry is the JDK's and
     * is measured; the desktop icon is drawn as a little window resting, and the base carries less
     * air than the sides.
     */
    public static Border getDesktopIconBorder() {
        if (desktopIconBorder == null) {
            desktopIconBorder = new BorderUIResource$CompoundBorderUIResource(
                    new LineBorder(MetalLookAndFeel.getControlDarkShadow(), 1),
                    new MatteBorder(2, 2, 1, 2, MetalLookAndFeel.getControl()));
        }
        return desktopIconBorder;
    }

    /** A one-pixel frame in the theme's shadow, with one pixel of air inside. */
    public static class Flush3DBorder extends AbstractBorder implements UIResource {

        private static final Insets MARGINS = new Insets(2, 2, 2, 2);

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(MetalLookAndFeel.getControlDarkShadow());
            g.drawRect(x, y, w - 1, h - 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(MARGINS.top, MARGINS.left, MARGINS.bottom, MARGINS.right);
            return insets;
        }
    }

}
