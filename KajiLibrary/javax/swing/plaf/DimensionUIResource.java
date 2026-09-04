package javax.swing.plaf;

import java.awt.Dimension;

/** Una {@link Dimension} que puso el aspecto; ver {@link UIResource}. */
public class DimensionUIResource extends Dimension implements UIResource {

    public DimensionUIResource(int width, int height) {
        super(width, height);
    }
}
