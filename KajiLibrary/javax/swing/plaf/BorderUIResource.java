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
 * Un {@link Border} que puso el aspecto; ver {@link UIResource}.
 *
 * <p>Dos formas de ponerle la etiqueta a un borde: envolver uno cualquiera con esta clase, o usar
 * las anidadas —{@code CompoundBorderUIResource}, {@code EmptyBorderUIResource}...—, que son cada
 * borde de {@code javax.swing.border} con la etiqueta puesta de nacimiento. Los aspectos usan las
 * anidadas; la envoltura es para bordes que no son de esa familia.
 *
 * <p>Los tres bordes compartidos —grabado, biseles— se crean una vez: son inmutables y no llevan
 * estado del componente, asi que uno alcanza para todos.
 */
public class BorderUIResource implements Border, UIResource, Serializable {

    private static Border etched;
    private static Border loweredBevel;
    private static Border raisedBevel;
    private static Border blackLine;

    private Border delegate;

    /** Envuelve ese borde. {@code null} no es un borde. */
    public BorderUIResource(Border delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("null border delegate argument");
        }
        this.delegate = delegate;
    }

    /** El borde grabado compartido. */
    public static Border getEtchedBorderUIResource() {
        if (etched == null) {
            etched = new EtchedBorderUIResource();
        }
        return etched;
    }

    /** El bisel hundido compartido. */
    public static Border getLoweredBevelBorderUIResource() {
        if (loweredBevel == null) {
            loweredBevel = new BevelBorderUIResource(BevelBorder.LOWERED);
        }
        return loweredBevel;
    }

    /** El bisel levantado compartido. */
    public static Border getRaisedBevelBorderUIResource() {
        if (raisedBevel == null) {
            raisedBevel = new BevelBorderUIResource(BevelBorder.RAISED);
        }
        return raisedBevel;
    }

    /** La linea negra de un pixel compartida. */
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

    /** Un {@link CompoundBorder} del aspecto. */
    public static class CompoundBorderUIResource extends CompoundBorder implements UIResource {
        public CompoundBorderUIResource(Border outsideBorder, Border insideBorder) {
            super(outsideBorder, insideBorder);
        }
    }

    /** Un {@link EmptyBorder} del aspecto. */
    public static class EmptyBorderUIResource extends EmptyBorder implements UIResource {
        public EmptyBorderUIResource(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }

        public EmptyBorderUIResource(Insets insets) {
            super(insets);
        }
    }

    /** Un {@link LineBorder} del aspecto. */
    public static class LineBorderUIResource extends LineBorder implements UIResource {
        public LineBorderUIResource(Color color) {
            super(color);
        }

        public LineBorderUIResource(Color color, int thickness) {
            super(color, thickness);
        }
    }

    /** Un {@link BevelBorder} del aspecto. */
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

    /** Un {@link EtchedBorder} del aspecto. */
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

    /** Un {@link MatteBorder} del aspecto. */
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

    /** Un {@link TitledBorder} del aspecto. */
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
