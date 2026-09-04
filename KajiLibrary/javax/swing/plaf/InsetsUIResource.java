package javax.swing.plaf;

import java.awt.Insets;

/** Unos {@link Insets} que puso el aspecto; ver {@link UIResource}. */
public class InsetsUIResource extends Insets implements UIResource {

    public InsetsUIResource(int top, int left, int bottom, int right) {
        super(top, left, bottom, right);
    }
}
