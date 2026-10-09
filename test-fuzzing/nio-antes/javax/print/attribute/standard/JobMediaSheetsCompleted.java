package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * How many of the job's sheets already came out.
 *
 * <p>The progress against {@link JobMediaSheets}.
 */
public final class JobMediaSheetsCompleted extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 1739595973810840475L;

    public JobMediaSheetsCompleted(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobMediaSheetsCompleted from being equal to another
     * integer attribute with the same number.
     */
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
