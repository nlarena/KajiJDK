package javax.swing.plaf;

import java.awt.Font;

/** Una {@link Font} que puso el aspecto; ver {@link UIResource}. */
public class FontUIResource extends Font implements UIResource {

    public FontUIResource(String name, int style, int size) {
        super(name, style, size);
    }

    public FontUIResource(Font font) {
        super(font);
    }
}
