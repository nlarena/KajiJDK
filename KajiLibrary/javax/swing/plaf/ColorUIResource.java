package javax.swing.plaf;

import java.awt.Color;

/** A {@link Color} the look and feel set; see {@link UIResource}. */
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

    /** The same colour, with the label; the alpha is kept if it had one. */
    public ColorUIResource(Color c) {
        super(c.getRGB(), (c.getRGB() & 0xFF000000) != 0xFF000000);
    }
}
