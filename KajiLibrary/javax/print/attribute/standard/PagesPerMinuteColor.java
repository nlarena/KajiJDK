package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Cuantas paginas por minuto tira la impresora en color.
 *
 * <p>Casi siempre menos que {@link PagesPerMinute}. Una impresora que no imprime en color no
 * reporta este atributo --no lo reporta en cero-- y eso lo dice {@link ColorSupported}.
 */
public final class PagesPerMinuteColor extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 1684993151687470944L;

    public PagesPerMinuteColor(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un PagesPerMinuteColor de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PagesPerMinuteColor;
    }

    public final Class<? extends Attribute> getCategory() {
        return PagesPerMinuteColor.class;
    }

    public final String getName() {
        return "pages-per-minute-color";
    }
}
