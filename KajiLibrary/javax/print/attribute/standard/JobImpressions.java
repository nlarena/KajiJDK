package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * How many <em>impressions</em> the job has: an impression is one side of a sheet as it comes out.
 *
 * <p>It is not the same as pages nor as sheets. With a {@link NumberUp} of 4 and two-sided {@link
 * Sides}, eight pages are two impressions. It serves so that the printer can estimate the job
 * before starting it.
 */
public final class JobImpressions extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 8225537206784322464L;

    public JobImpressions(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobImpressions from being equal to another
     * integer attribute with the same number.
     */
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
