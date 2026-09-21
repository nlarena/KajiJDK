package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * When the job stopped waiting in the queue and started being processed.
 *
 * <p>The difference against {@link DateTimeAtCreation} is how long it waited.
 */
public final class DateTimeAtProcessing extends DateTimeSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -3710068197278263244L;

    public DateTimeAtProcessing(Date dateTimeAtProcessing) {
        super(dateTimeAtProcessing);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DateTimeAtProcessing;
    }

    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtProcessing.class;
    }

    public final String getName() {
        return "date-time-at-processing";
    }
}
