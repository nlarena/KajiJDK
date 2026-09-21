package javax.swing.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.io.Serializable;

import javax.swing.Icon;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;

/**
 * A {@link Border} the look and feel set; see {@link UIResource}.
 *
 * <p>Two ways of putting the label on a border: wrapping any one with this class, or using the
 * nested ones --{@code CompoundBorderUIResource}, {@code EmptyBorderUIResource}...--, which are
 * each border of {@code javax.swing.border} with the label put on at birth. The looks and feels
 * use the nested ones; the wrapper is for borders that are not of that family.
 *
 * <p>The three shared borders --etched, bevels-- are created once: they are immutable and carry
 * no state of the component, so one is enough for everybody.
 */
public class BorderUIResource implements Border, UIResource, Serializable {

    private static Border etched;
    private static Border loweredBevel;
    private static Border raisedBevel;
    private static Border blackLine;

    private Border delegate;

    /** Wraps that border. {@code null} is not a border. */
    public BorderUIResource(Border delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("null border delegate argument");
        }
        this.delegate = delegate;
    }

    /** The shared etched border. */
    public static Border getEtchedBorderUIResource() {
        if (etched == null) {
            etched = new EtchedBorderUIResource();
        }
        return etched;
    }

    /** The shared lowered bevel. */
    public static Border getLoweredBevelBorderUIResource() {
        if (loweredBevel == null) {
            loweredBevel = new BevelBorderUIResource(BevelBorder.LOWERED);
        }
        return loweredBevel;
    }

    /** The shared raised bevel. */
    public static Border getRaisedBevelBorderUIResource() {
        if (raisedBevel == null) {
            raisedBevel = new BevelBorderUIResource(BevelBorder.RAISED);
        }
        return raisedBevel;
    }

    /** The shared one-pixel black line. */
    public static Border getBlackLineBorderUIResource() {
        if (blackLine == null) {
            blackLine = new LineBorderUIResource(Color.black);
        }
        return blackLine;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        delegate.paintBorder(c, g, x, y, width, height);
    }

    public Insets getBorderInsets(Component c) {
        return delegate.getBorderInsets(c);
    }

    public boolean isBorderOpaque() {
        return delegate.isBorderOpaque();
    }

    /** A {@link CompoundBorder} of the look and feel. */
    public static class CompoundBorderUIResource extends CompoundBorder implements UIResource {
        public CompoundBorderUIResource(Border outsideBorder, Border insideBorder) {
            super(outsideBorder, insideBorder);
        }
    }

    /** An {@link EmptyBorder} of the look and feel. */
    public static class EmptyBorderUIResource extends EmptyBorder implements UIResource {
        public EmptyBorderUIResource(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }

        public EmptyBorderUIResource(Insets insets) {
            super(insets);
        }
    }

    /** A {@link LineBorder} of the look and feel. */
    public static class LineBorderUIResource extends LineBorder implements UIResource {
        public LineBorderUIResource(Color color) {
            super(color);
        }

        public LineBorderUIResource(Color color, int thickness) {
            super(color, thickness);
        }
    }

    /** A {@link BevelBorder} of the look and feel. */
    public static class BevelBorderUIResource extends BevelBorder implements UIResource {
        public BevelBorderUIResource(int bevelType) {
            super(bevelType);
        }

        public BevelBorderUIResource(int bevelType, Color highlight, Color shadow) {
            super(bevelType, highlight, shadow);
        }

        public BevelBorderUIResource(int bevelType, Color highlightOuter, Color highlightInner,
                Color shadowOuter, Color shadowInner) {
            super(bevelType, highlightOuter, highlightInner, shadowOuter, shadowInner);
        }
    }

    /** An {@link EtchedBorder} of the look and feel. */
    public static class EtchedBorderUIResource extends EtchedBorder implements UIResource {
        public EtchedBorderUIResource() {
            super();
        }

        public EtchedBorderUIResource(int etchType) {
            super(etchType);
        }

        public EtchedBorderUIResource(Color highlight, Color shadow) {
            super(highlight, shadow);
        }

        public EtchedBorderUIResource(int etchType, Color highlight, Color shadow) {
            super(etchType, highlight, shadow);
        }
    }

    /** A {@link MatteBorder} of the look and feel. */
    public static class MatteBorderUIResource extends MatteBorder implements UIResource {
        public MatteBorderUIResource(int top, int left, int bottom, int right, Color color) {
            super(top, left, bottom, right, color);
        }

        public MatteBorderUIResource(int top, int left, int bottom, int right, Icon tileIcon) {
            super(top, left, bottom, right, tileIcon);
        }

        public MatteBorderUIResource(Icon tileIcon) {
            super(tileIcon);
        }
    }

    /** A {@link TitledBorder} of the look and feel. */
    public static class TitledBorderUIResource extends TitledBorder implements UIResource {
        public TitledBorderUIResource(String title) {
            super(title);
        }

        public TitledBorderUIResource(Border border) {
            super(border);
        }

        public TitledBorderUIResource(Border border, String title) {
            super(border, title);
        }

        public TitledBorderUIResource(Border border, String title, int titleJustification,
                int titlePosition) {
            super(border, title, titleJustification, titlePosition);
        }

        public TitledBorderUIResource(Border border, String title, int titleJustification,
                int titlePosition, Font titleFont) {
            super(border, title, titleJustification, titlePosition, titleFont);
        }

        public TitledBorderUIResource(Border border, String title, int titleJustification,
                int titlePosition, Font titleFont, Color titleColor) {
            super(border, title, titleJustification, titlePosition, titleFont, titleColor);
        }
    }
}
