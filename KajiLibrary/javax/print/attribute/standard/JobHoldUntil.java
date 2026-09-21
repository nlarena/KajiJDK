package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Until when to hold the job before printing it.
 *
 * <p>It is the only one of the family that is <em>requested</em> instead of reported. An
 * instant already past means "now": the job goes out at once instead of failing.
 */
public final class JobHoldUntil extends DateTimeSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = -1664471048860415024L;

    public JobHoldUntil(Date jobHoldUntil) {
        super(jobHoldUntil);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobHoldUntil;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobHoldUntil.class;
    }

    public final String getName() {
        return "job-hold-until";
    }
}
