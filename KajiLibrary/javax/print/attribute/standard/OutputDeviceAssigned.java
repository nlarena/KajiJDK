package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Which physical device the job went to.
 *
 * <p>It makes sense when a single print service handles several machines: the service's name is
 * {@link PrinterName} and this one says which of its machines did it.
 */
public final class OutputDeviceAssigned extends TextSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 5486733778854271081L;

    public OutputDeviceAssigned(String outputDeviceAssigned, Locale locale) {
        super(outputDeviceAssigned, locale);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof OutputDeviceAssigned;
    }

    public final Class<? extends Attribute> getCategory() {
        return OutputDeviceAssigned.class;
    }

    public final String getName() {
        return "output-device-assigned";
    }
}
