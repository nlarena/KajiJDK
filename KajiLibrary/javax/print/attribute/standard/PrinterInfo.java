package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Una descripcion libre de la impresora, para que la lea una persona.
 *
 * <p>Lo que un administrador escribio: "la de color del segundo piso".
 */
public final class PrinterInfo extends TextSyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 7765280618777599727L;

    public PrinterInfo(String printerInfo, Locale locale) {
        super(printerInfo, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterInfo;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterInfo.class;
    }

    public final String getName() {
        return "printer-info";
    }
}
