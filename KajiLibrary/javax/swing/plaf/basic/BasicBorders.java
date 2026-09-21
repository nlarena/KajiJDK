package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource$CompoundBorderUIResource;
import javax.swing.plaf.BorderUIResource$LineBorderUIResource;
import javax.swing.plaf.UIResource;

/**
 * The basic look and feel's borders: button bevel, etched field, margin, menu bar.
 *
 * <p>The colours of the static factories are those {@code UIManager} would give under Metal,
 * measured in JDK 25: shadow (184, 207, 229), dark shadow (122, 138, 153), highlight and light
 * highlight white. With no {@code UIManager}, they are written here.
 *
 * <p>{@link MarginBorder} is the one that makes a button's margin count: it paints nothing, it
 * only declares as insets whatever {@code AbstractButton.getMargin} says. The JDK also applies
 * it to {@code JToolBar} and to the text components, which are not there; for any other
 * component the insets are zero.
 *
 * <p>{@code SplitPaneBorder} and {@code getSplitPaneBorder},
 * {@code getSplitPaneDividerBorder} and {@code getInternalFrameBorder} are not there: they
 * depend on {@code JSplitPane} and on {@code InternalFrame.*} colours that were not measured.
 */
public class BasicBorders {

    private static final Color SHADOW = new Color(184, 207, 229);
    private static final Color DARK_SHADOW = new Color(122, 138, 153);
    private static final Color HIGHLIGHT = new Color(255, 255, 255);
    private static final Color LIGHT_HIGHLIGHT = new Color(255, 255, 255);

    public BasicBorders() {
    }

    /** A button's border: bevel on the outside, margin on the inside. */
    public static Border getButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new ButtonBorder(SHADOW, DARK_SHADOW, HIGHLIGHT, LIGHT_HIGHLIGHT),
                new MarginBorder());
    }

    public static Border getRadioButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new RadioButtonBorder(SHADOW, DARK_SHADOW, HIGHLIGHT, LIGHT_HIGHLIGHT),
                new MarginBorder());
    }

    public static Border getToggleButtonBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new ToggleButtonBorder(SHADOW, DARK_SHADOW, HIGHLIGHT, LIGHT_HIGHLIGHT),
                new MarginBorder());
    }

    public static Border getMenuBarBorder() {
        return new MenuBarBorder(SHADOW, HIGHLIGHT);
    }

    public static Border getTextFieldBorder() {
        return new FieldBorder(SHADOW, DARK_SHADOW, HIGHLIGHT, LIGHT_HIGHLIGHT);
    }

    /**
     * A split pane's border: a line all around, and nothing where the divider goes.
     *
     * <p>The divider's gap is the interesting part: the border does not draw a stroke of its own
     * there, because the divider has its own and the two together would look like a double line.
     */
    public static Border getSplitPaneBorder() {
        return new SplitPaneBorder(HIGHLIGHT, DARK_SHADOW);
    }

    /** The divider's own border; a one-pixel line on each side. */
    public static Border getSplitPaneDividerBorder() {
        return new SplitPaneDividerBorder(HIGHLIGHT, DARK_SHADOW);
    }

    /** A progress bar's: two pixels of line. */
    public static Border getProgressBarBorder() {
        return new BorderUIResource$LineBorderUIResource(DARK_SHADOW, 2);
    }

    /** An internal frame's: two lines on the outside and one on the inside. */
    public static Border getInternalFrameBorder() {
        return new BorderUIResource$CompoundBorderUIResource(
                new BorderUIResource$LineBorderUIResource(DARK_SHADOW, 2),
                new BorderUIResource$LineBorderUIResource(SHADOW, 1));
    }

    /**
     * A split pane's border; see {@link #getSplitPaneBorder}.
     *
     * <p>The two colours are public-protected and are measured: the light one on top and on the
     * left, the dark one at the bottom and on the right, which is what makes the pane look sunken.
     */
    public static class SplitPaneBorder implements Border, UIResource {

        protected Color highlight;
        protected Color shadow;

        public SplitPaneBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        /**
         * The line all around, skipping the divider's width.
         *
         * <p>If the component is not a split pane the whole rectangle is drawn: it is the only
         * thing that can be done without knowing where the divider is.
         */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (!(c instanceof javax.swing.JSplitPane)) {
                g.setColor(shadow);
                g.drawRect(x, y, width - 1, height - 1);
                return;
            }
            javax.swing.JSplitPane splitPane = (javax.swing.JSplitPane) c;
            Component left = splitPane.getLeftComponent();
            Component der = splitPane.getRightComponent();
            g.setColor(highlight);
            g.drawLine(x, y, x + width - 1, y);
            g.drawLine(x, y, x, y + height - 1);
            g.setColor(shadow);
            g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            // The gap: where one child ends and the other begins nothing goes.
            if (left != null && der != null) {
                g.setColor(c.getBackground());
                if (splitPane.getOrientation() == javax.swing.JSplitPane.HORIZONTAL_SPLIT) {
                    int dx = left.getWidth() + x;
                    g.drawLine(dx, y, dx + splitPane.getDividerSize() - 1, y);
                    g.drawLine(dx, y + height - 1, dx + splitPane.getDividerSize() - 1,
                            y + height - 1);
                } else {
                    int dy = left.getHeight() + y;
                    g.drawLine(x, dy, x, dy + splitPane.getDividerSize() - 1);
                    g.drawLine(x + width - 1, dy, x + width - 1,
                            dy + splitPane.getDividerSize() - 1);
                }
            }
        }

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    /**
     * A split pane divider's border.
     *
     * <p>It is not public in the JDK and it is not here either: the only public thing is
     * {@link #getSplitPaneDividerBorder}, which returns one. The name shows through
     * {@code getClass()} all the same, so it is the JDK's.
     */
    static class SplitPaneDividerBorder implements Border, UIResource {

        Color highlight;
        Color shadow;

        SplitPaneDividerBorder(Color highlight, Color shadow) {
            this.highlight = highlight;
            this.shadow = shadow;
        }

        /** A line on each side, in the direction perpendicular to the one it divides. */
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Component parent = c.getParent();
            boolean horizontal = true;
            if (parent instanceof javax.swing.JSplitPane) {
                horizontal = ((javax.swing.JSplitPane) parent).getOrientation()
                        == javax.swing.JSplitPane.HORIZONTAL_SPLIT;
            }
            g.setColor(highlight);
            if (horizontal) {
                g.drawLine(x, y, x, y + height - 1);
                g.setColor(shadow);
                g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
            } else {
                g.drawLine(x, y, x + width - 1, y);
                g.setColor(shadow);
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            }
        }

        /**
         * One on each side, in the direction it divides.
         *
         * <p>A horizontal divider has its lines on the left and on the right, so its insets are
         * (0, 1, 0, 1); a vertical one, the other way round. And a component that is not a divider
         * -- or one that does not have a pane yet -- gets one on each side. The three cases are
         * measured.
         */
        public Insets getBorderInsets(Component c) {
            if (c instanceof BasicSplitPaneDivider) {
                BasicSplitPaneUI ui = ((BasicSplitPaneDivider) c).getBasicSplitPaneUI();
                if (ui != null) {
                    javax.swing.JSplitPane sp = ui.getSplitPane();
                    if (sp != null) {
                        if (sp.getOrientation() == javax.swing.JSplitPane.HORIZONTAL_SPLIT) {
                            return new Insets(0, 1, 0, 1);
                        }
                        return new Insets(1, 0, 1, 0);
                    }
                }
            }
            return new Insets(1, 1, 1, 1);
        }

        public boolean isBorderOpaque() {
            return true;
        }
    }

    /**
     * A button's bevel: raised at rest, sunken when pressed, with a frame if it is the default one.
     */
    public static class ButtonBorder extends AbstractBorder implements UIResource {

        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public ButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            boolean pressed = false;
            boolean byDefault = false;
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel model = b.getModel();
                pressed = model.isPressed() && model.isArmed();
                if (c instanceof JButton) {
                    byDefault = ((JButton) c).isDefaultButton();
                }
            }
            BasicGraphicsUtils.drawBezel(g, x, y, width, height, pressed, byDefault, shadow,
                    darkShadow, highlight, lightHighlight);
        }

        /**
         * Two on top and three on the other sides: the missing pixel on top leaves room for the
         * frame.
         */
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 3;
            insets.bottom = 3;
            insets.right = 3;
            return insets;
        }
    }

    /** The bevel of a button with state: sunken while it is selected. */
    public static class ToggleButtonBorder extends ButtonBorder {

        public ToggleButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            boolean sunken = false;
            if (c instanceof AbstractButton) {
                ButtonModel model = ((AbstractButton) c).getModel();
                sunken = (model.isArmed() && model.isPressed()) || model.isSelected();
            }
            if (sunken) {
                BasicGraphicsUtils.drawLoweredBezel(g, x, y, width, height, shadow, darkShadow,
                        highlight, lightHighlight);
            } else {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height, false, false, shadow,
                        darkShadow, highlight, lightHighlight);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** A radio button's bevel: sunken if it is selected, with a frame if it has the focus. */
    public static class RadioButtonBorder extends ButtonBorder {

        public RadioButtonBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            super(shadow, darkShadow, highlight, lightHighlight);
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                ButtonModel model = b.getModel();
                if ((model.isArmed() && model.isPressed()) || model.isSelected()) {
                    BasicGraphicsUtils.drawLoweredBezel(g, x, y, width, height, shadow,
                            darkShadow, highlight, lightHighlight);
                } else {
                    BasicGraphicsUtils.drawBezel(g, x, y, width, height, false,
                            b.isFocusPainted() && b.hasFocus(), shadow, darkShadow, highlight,
                            lightHighlight);
                }
            } else {
                BasicGraphicsUtils.drawBezel(g, x, y, width, height, false, false, shadow,
                        darkShadow, highlight, lightHighlight);
            }
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** A button's margin, as a border; see the class note. */
    public static class MarginBorder extends AbstractBorder implements UIResource {

        public MarginBorder() {
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;
            if (c instanceof AbstractButton) {
                margin = ((AbstractButton) c).getMargin();
            }
            insets.top = margin != null ? margin.top : 0;
            insets.left = margin != null ? margin.left : 0;
            insets.bottom = margin != null ? margin.bottom : 0;
            insets.right = margin != null ? margin.right : 0;
            return insets;
        }
    }

    /**
     * A text field's border: an etched rectangle.
     *
     * <p>In the JDK the insets add the {@code JTextComponent}'s margin; with no text components,
     * they are the etching's two pixels.
     */
    public static class FieldBorder extends AbstractBorder implements UIResource {

        protected Color shadow;
        protected Color darkShadow;
        protected Color highlight;
        protected Color lightHighlight;

        public FieldBorder(Color shadow, Color darkShadow, Color highlight,
                Color lightHighlight) {
            this.shadow = shadow;
            this.darkShadow = darkShadow;
            this.highlight = highlight;
            this.lightHighlight = lightHighlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            BasicGraphicsUtils.drawEtchedRect(g, x, y, width, height, shadow, darkShadow,
                    highlight, lightHighlight);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 2;
            insets.left = 2;
            insets.bottom = 2;
            insets.right = 2;
            return insets;
        }
    }

    /** A menu bar's border: a shadow line and a highlight one, at the bottom. */
    public static class MenuBarBorder extends AbstractBorder implements UIResource {

        private Color shadow;
        private Color highlight;

        public MenuBarBorder(Color shadow, Color highlight) {
            this.shadow = shadow;
            this.highlight = highlight;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color old = g.getColor();
            g.translate(x, y);
            g.setColor(shadow);
            g.drawLine(0, height - 2, width, height - 2);
            g.setColor(highlight);
            g.drawLine(0, height - 1, width, height - 1);
            g.translate(-x, -y);
            g.setColor(old);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 0;
            insets.left = 0;
            insets.bottom = 2;
            insets.right = 0;
            return insets;
        }
    }
}
