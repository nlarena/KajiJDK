package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Who the client says is sending the job.
 *
 * <p>It is a <em>request</em>, not an identity: the printer may ignore it and put the one it
 * authenticated, which is the one that ends up in {@link JobOriginatingUserName}.
 */
public final class RequestingUserName extends TextSyntax implements PrintRequestAttribute {

    private static final long serialVersionUID = -2683049894310331454L;

    public RequestingUserName(String requestingUserName, Locale locale) {
        super(requestingUserName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof RequestingUserName;
    }

    public final Class<? extends Attribute> getCategory() {
        return RequestingUserName.class;
    }

    public final String getName() {
        return "requesting-user-name";
    }
}
