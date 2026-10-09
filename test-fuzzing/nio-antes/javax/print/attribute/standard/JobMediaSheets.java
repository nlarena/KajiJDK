package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * How many sheets of paper the job consumes.
 *
 * <p>Physical sheets, not sides: two-sided, a hundred pages are fifty sheets. And here the copies
 * do count, unlike in {@link JobKOctets}: it is paper being used up.
 */
public class JobMediaSheets extends IntegerSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 408871131531979741L;

    public JobMediaSheets(int value) {
        super(value, 0, Integer.MAX_VALUE);
    }

    /**
     * The {@code instanceof} is what keeps a JobMediaSheets from being equal to another
     * integer attribute with the same number.
     */
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
