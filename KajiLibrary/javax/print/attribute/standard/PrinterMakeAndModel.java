package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Marca y modelo de la impresora, en una sola cadena.
 */
public final class PrinterMakeAndModel extends TextSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 4580461489499351411L;

    public PrinterMakeAndModel(String printerMakeAndModel, Locale locale) {
        super(printerMakeAndModel, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterMakeAndModel;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterMakeAndModel.class;
    }

    public final String getName() {
        return "printer-make-and-model";
    }
}
