package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Donde esta fisicamente la impresora, en palabras.
 *
 * <p>Para que alguien la encuentre y vaya a buscar el papel.
 */
public final class PrinterLocation extends TextSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = -1598610039865566337L;

    public PrinterLocation(String printerLocation, Locale locale) {
        super(printerLocation, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterLocation;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterLocation.class;
    }

    public final String getName() {
        return "printer-location";
    }
}
