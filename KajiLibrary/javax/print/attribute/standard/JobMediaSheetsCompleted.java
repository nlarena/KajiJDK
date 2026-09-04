package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * Cuantas hojas del trabajo ya salieron.
 *
 * <p>El avance contra {@link JobMediaSheets}.
 */
public final class JobMediaSheetsCompleted extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 1739595973810840475L;

    public JobMediaSheetsCompleted(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /** El {@code instanceof} es lo que impide que un JobMediaSheetsCompleted de igual a otro atributo
     * entero con el mismo numero. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobMediaSheetsCompleted;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobMediaSheetsCompleted.class;
    }

    public final String getName() {
        return "job-media-sheets-completed";
    }
}
