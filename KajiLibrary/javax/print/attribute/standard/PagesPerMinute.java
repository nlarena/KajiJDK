package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Cuantas paginas por minuto tira la impresora en monocromo.
 *
 * <p>Cero es una respuesta legitima: significa que tarda mas de un minuto por pagina, no que no
 * imprime.
 */
public final class PagesPerMinute extends IntegerSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -6366403993072862015L;

    public PagesPerMinute(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un PagesPerMinute de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PagesPerMinute;
    }

    public final Class<? extends Attribute> getCategory() {
        return PagesPerMinute.class;
    }

    public final String getName() {
        return "pages-per-minute";
    }
}
