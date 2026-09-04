package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Cuantos trabajos hay adelante de este en la cola.
 *
 * <p>Cero significa que es el proximo, no que ya esta imprimiendo.
 */
public final class NumberOfInterveningJobs extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 2568141124844982746L;

    public NumberOfInterveningJobs(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un NumberOfInterveningJobs de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof NumberOfInterveningJobs;
    }

    public final Class<? extends Attribute> getCategory() {
        return NumberOfInterveningJobs.class;
    }

    public final String getName() {
        return "number-of-intervening-jobs";
    }
}
