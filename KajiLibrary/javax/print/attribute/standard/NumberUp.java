package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Cuantas paginas del documento entran en una cara de la hoja.
 *
 * <p>En que orden se llenan esas celdas lo dice {@link PresentationDirection}. Arranca en 1 porque
 * cero paginas por cara no significa nada.
 */
public final class NumberUp extends IntegerSyntax implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -3040436486786527811L;

    public NumberUp(int value) {
        super(value, 1, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un NumberUp de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberUp;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberUp.class;
    }

    public final String getName() {
        return "number-up";
    }
}
