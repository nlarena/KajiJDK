package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Quien dice el cliente que esta mandando el trabajo.
 *
 * <p>Es una <em>peticion</em>, no una identidad: la impresora puede ignorarla y poner la que
 * autentico, que es la que termina en {@link JobOriginatingUserName}.
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
