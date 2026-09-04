package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Un mensaje que el operador de la impresora le deja al duenio del trabajo.
 *
 * <p>Es la explicacion en castellano de lo que {@link JobStateReasons} dice en codigos.
 */
public final class JobMessageFromOperator extends TextSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = -4620751846003142047L;

    public JobMessageFromOperator(String jobMessageFromOperator, Locale locale) {
        super(jobMessageFromOperator, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobMessageFromOperator;
    }

    public final Class<? extends Attribute> getCategory() {
        return JobMessageFromOperator.class;
    }

    public final String getName() {
        return "job-message-from-operator";
    }
}
