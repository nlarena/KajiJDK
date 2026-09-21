package javax.swing.plaf;

import java.awt.Dimension;

/** A {@link Dimension} the look and feel set; see {@link UIResource}. */
public class DimensionUIResource extends Dimension implements UIResource {

    public DimensionUIResource(int width, int height) {
        super(width, height);
    }
}
