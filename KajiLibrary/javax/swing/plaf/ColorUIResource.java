package javax.swing.plaf;

import java.awt.Color;

/** Un {@link Color} que puso el aspecto; ver {@link UIResource}. */
public class ColorUIResource extends Color implements UIResource {

    public ColorUIResource(int r, int g, int b) {
        super(r, g, b);
    }

    public ColorUIResource(int rgb) {
        super(rgb);
    }

    public ColorUIResource(float r, float g, float b) {
        super(r, g, b);
    }

    /** El mismo color, con la etiqueta; el alfa se conserva si lo tenia. */
    public ColorUIResource(Color c) {
        super(c.getRGB(), (c.getRGB() & 0xFF000000) != 0xFF000000);
    }
}
