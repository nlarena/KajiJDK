package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * El nombre corto de la impresora, el que se usa para elegirla.
 *
 * <p>No tiene por que ser unico en el mundo; el identificador que si lo es es {@link PrinterURI}.
 */
public final class PrinterName extends TextSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 299740639137803127L;

    public PrinterName(String printerName, Locale locale) {
        super(printerName, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterName;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterName.class;
    }

    public final String getName() {
        return "printer-name";
    }
}
