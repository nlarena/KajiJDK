package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;

/*
 * FAMILY HEADER -- this package's {@code DateTimeSyntax} attributes.
 *
 * <p>An instant. The mechanism is in {@link javax.print.attribute.DateTimeSyntax DateTimeSyntax},
 * which copies the {@link java.util.Date} on the way out but keeps the reference it is given: the
 * attribute is protected from its readers, not from whoever built it. (The note said it copies on
 * the way in too; see DateTimeSyntax's own note, which is right.)
 *
 * <p>Three of the four are timestamps the job reports --when it was created, when it started, when
 * it finished-- and only {@link JobHoldUntil} is requested.
 */

/**
 * When the job reached a terminal state: {@code COMPLETED}, {@code CANCELED} or {@code ABORTED}.
 *
 * <p>The attribute does not exist until the job finishes, and it does not tell how it finished --
 * {@link JobState} says that.
 */
public final class DateTimeAtCompleted extends DateTimeSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 6497399708058490000L;

    public DateTimeAtCompleted(Date dateTimeAtCompleted) {
        super(dateTimeAtCompleted);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DateTimeAtCompleted;
    }

    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtCompleted.class;
    }

    public final String getName() {
        return "date-time-at-completed";
    }
}
