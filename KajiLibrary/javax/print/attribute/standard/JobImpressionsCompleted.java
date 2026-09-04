package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Cuantas impresiones del trabajo ya salieron.
 *
 * <p>Es el avance contra el total que declara {@link JobImpressions}. Empieza en cero, que es por
 * que el rango arranca ahi y no en uno.
 */
public final class JobImpressionsCompleted extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 6722648442432393294L;

    public JobImpressionsCompleted(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobImpressionsCompleted de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobImpressionsCompleted;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobImpressionsCompleted.class;
    }

    public final String getName() {
        return "job-impressions-completed";
    }
}
