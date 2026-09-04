package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Un mensaje del operador sobre la impresora entera, no sobre un trabajo.
 *
 * <p>"Se quedo sin toner, vuelve el lunes". El equivalente por trabajo es {@link
 * JobMessageFromOperator}.
 */
public final class PrinterMessageFromOperator extends TextSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -4486871203218629318L;

    public PrinterMessageFromOperator(String printerMessageFromOperator, Locale locale) {
        super(printerMessageFromOperator, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterMessageFromOperator;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterMessageFromOperator.class;
    }

    public final String getName() {
        return "printer-message-from-operator";
    }
}
