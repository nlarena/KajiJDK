package javax.print.attribute.standard;

import java.util.Locale;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.TextSyntax;

/**
 * A que dispositivo fisico le toco el trabajo.
 *
 * <p>Tiene sentido cuando un solo servicio de impresion maneja varias maquinas: el nombre del
 * servicio es {@link PrinterName} y este dice cual de sus maquinas lo hizo.
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
