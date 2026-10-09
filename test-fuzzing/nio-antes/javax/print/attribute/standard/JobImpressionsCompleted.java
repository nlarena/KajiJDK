package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * How many of the job's impressions already came out.
 *
 * <p>It is the progress against the total {@link JobImpressions} declares. It starts at zero, which
 * is why the range starts there and not at one.
 */
public final class JobImpressionsCompleted extends IntegerSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 6722648442432393294L;

    public JobImpressionsCompleted(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobImpressionsCompleted from being equal to another
     * integer attribute with the same number.
     */
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
