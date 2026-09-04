package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Cuantas hojas de papel consume el trabajo.
 *
 * <p>Hojas fisicas, no caras: a dos caras, cien paginas son cincuenta hojas. Y aca las copias si
 * cuentan, al reves que en {@link JobKOctets}: es papel que se gasta.
 */
public class JobMediaSheets extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 408871131531979741L;

    public JobMediaSheets(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobMediaSheets de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobMediaSheets;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobMediaSheets.class;
    }

    public final String getName() {
        return "job-media-sheets";
    }
}
