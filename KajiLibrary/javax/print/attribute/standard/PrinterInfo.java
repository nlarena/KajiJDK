package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.TextSyntax;

/**
 * A free description of the printer, for a person to read.
 *
 * <p>What an administrator wrote: "the colour one on the second floor".
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
