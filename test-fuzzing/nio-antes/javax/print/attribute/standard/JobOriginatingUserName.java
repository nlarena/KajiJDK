package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Who sent the job, according to the printer.
 *
 * <p>It is not what the client asked for in {@link RequestingUserName}: this one is set by the
 * service, with the identity it could authenticate, and that is why it is the one that serves for
 * charging or auditing.
 */
public final class JobOriginatingUserName extends TextSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -8052537926362933477L;

    public JobOriginatingUserName(String jobOriginatingUserName, Locale locale) {
        super(jobOriginatingUserName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobOriginatingUserName;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobOriginatingUserName.class;
    }

    public final String getName() {
        return "job-originating-user-name";
    }
}
