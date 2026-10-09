package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.URISyntax;

/**
 * A manufacturer's web page about the <em>model</em>, not about this machine.
 *
 * <p>It is where the drivers and the spare parts are; what is specific to this printer is in
 * {@link PrinterMoreInfo}.
 */
public final class PrinterMoreInfoManufacturer extends URISyntax implements PrintServiceAttribute {

    private static final long serialVersionUID = 3323271346485076608L;

    public PrinterMoreInfoManufacturer(URI uri) {
        super(uri);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof PrinterMoreInfoManufacturer;
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterMoreInfoManufacturer.class;
    }

    public final String getName() {
        return "printer-more-info-manufacturer";
    }
}
