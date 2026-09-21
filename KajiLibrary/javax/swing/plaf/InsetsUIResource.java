package javax.swing.plaf;

import java.awt.Insets;

/** An {@link Insets} the look and feel set; see {@link UIResource}. */
public class InsetsUIResource extends Insets implements UIResource {

    public InsetsUIResource(int top, int left, int bottom, int right) {
        super(top, left, bottom, right);
    }
}
