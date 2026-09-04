package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Cuantas <em>impresiones</em> tiene el trabajo: una impresion es una cara de una hoja tal como
 * sale.
 *
 * <p>No es lo mismo que paginas ni que hojas. Con {@link NumberUp} de 4 y {@link Sides} a dos
 * caras, ocho paginas son dos impresiones. Sirve para que la impresora estime el trabajo antes de
 * empezarlo.
 */
public final class JobImpressions extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 8225537206784322464L;

    public JobImpressions(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobImpressions de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobImpressions;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobImpressions.class;
    }

    public final String getName() {
        return "job-impressions";
    }
}
