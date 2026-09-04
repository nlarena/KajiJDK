package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Quien mando el trabajo, segun la impresora.
 *
 * <p>No es lo que el cliente pidio en {@link RequestingUserName}: este lo pone el servicio, con la
 * identidad que pudo autenticar, y por eso es el que sirve para cobrar o para auditar.
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
