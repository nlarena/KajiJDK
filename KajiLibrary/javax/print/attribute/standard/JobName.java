package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.TextSyntax;

/**
 * El nombre del trabajo, el que se ve en la cola y en la caratula.
 *
 * <p>Si no se pide, la impresora inventa uno --normalmente el del primer documento.
 */
public final class JobName extends TextSyntax implements PrintRequestAttribute, PrintJobAttribute {

    private static final long serialVersionUID = 4660359192078689545L;

    public JobName(String jobName, Locale locale) {
        super(jobName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobName;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobName.class;
    }

    public final String getName() {
        return "job-name";
    }
}
