package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;

/**
 * When the printer accepted the job.
 *
 * <p>It is the moment it was queued, not when it started printing: that is
 * {@link DateTimeAtProcessing}.
 */
public final class DateTimeAtCreation extends DateTimeSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -2923732231056647903L;

    public DateTimeAtCreation(Date dateTimeAtCreation) {
        super(dateTimeAtCreation);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DateTimeAtCreation;
    }

    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtCreation.class;
    }

    public final String getName() {
        return "date-time-at-creation";
    }
}
